package org.mozilla.rocket.content.news.data.dailyhunt

import android.content.Context
import android.net.Uri
import androidx.paging.PageKeyedDataSource
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsDataSourceFactory.PageKey
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsSourceInfo
import org.mozilla.rocket.content.news.domain.GetAdditionalSourceInfoUseCase
import org.mozilla.rocket.util.sendHttpRequest
import org.mozilla.rocket.util.sha256
import org.mozilla.rocket.util.toJsonObject
import java.net.URLEncoder

class DailyHuntNewsRemoteDataSource(
    private val appContext: Context,
    private val getAdditionalSourceInfo: GetAdditionalSourceInfoUseCase,
    private val newsProvider: DailyHuntProvider?,
    private val category: String,
    private val language: String
) : PageKeyedDataSource<PageKey, NewsItem>() {

    override fun loadInitial(params: LoadInitialParams<PageKey>, callback: LoadInitialCallback<PageKey, NewsItem>) {
        val pageSize = params.requestedLoadSize

        when (val result = fetchNewsItems(newsProvider, category, language, pageSize, 0)) {
            is Result.Success -> {
                val (nextPageKey, items) = result.data
                val itemsWithHeader = addHeader(items, getAdditionalSourceInfo())
                callback.onResult(itemsWithHeader, null, nextPageKey)
            }
            is Result.Error -> result.exception.printStackTrace()
        }
    }

    private fun addHeader(
        firstPageItems: List<NewsItem>,
        newsSourceInfo: NewsSourceInfo?
    ): List<NewsItem> = mutableListOf<NewsItem>().apply {
        newsSourceInfo?.let {
            add(0, NewsItem.NewsTitleItem(it.resourceId))
        }
        addAll(firstPageItems)
    }

    override fun loadBefore(params: LoadParams<PageKey>, callback: LoadCallback<PageKey, NewsItem>) {
        // Do nothing
    }

    override fun loadAfter(params: LoadParams<PageKey>, callback: LoadCallback<PageKey, NewsItem>) {
        val pageKey = params.key as PageKey.PageUrlKey

        when (val result = fetchNewsItemsNextPage(pageKey.url)) {
            is Result.Success -> {
                val (nextPageKey, items) = result.data
                callback.onResult(items, nextPageKey)
            }
            is Result.Error -> result.exception.printStackTrace()
        }
    }

    private fun fetchNewsItems(
        newsProvider: DailyHuntProvider?,
        category: String,
        language: String,
        pageSize: Int,
        pages: Int
    ): Result<Pair<PageKey.PageUrlKey, List<NewsItem>>> {
        val params = createApiParams(
            partner = newsProvider?.partnerCode ?: "",
            timestamp = System.currentTimeMillis().toString(),
            uid = newsProvider?.userId ?: "",
            category = category,
            languageCode = language,
            pages = pages,
            pageSize = pageSize
        )
        return sendHttpRequest(
            request = Request(
                url = getApiEndpoint(params),
                method = Request.Method.GET,
                headers = createApiHeaders(params)
            ),
            onSuccess = {
                try {
                    val body = it.body.string()
                    val nextPageKey = PageKey.PageUrlKey(parseNextPageUrl(body))
                    val items = fromJson(body)
                    trackItemsShown(items)
                    Result.Success(nextPageKey to items)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            },
            onError = {
                Result.Error(it)
            }
        )
    }

    private fun fetchNewsItemsNextPage(nextPageUrl: String): Result<Pair<PageKey.PageUrlKey, List<NewsItem>>> {
        val params = parseUrlParams(nextPageUrl).toMutableMap().apply {
            put("ts", System.currentTimeMillis().toString())
        }
        return sendHttpRequest(
            request = Request(
                url = getApiEndpoint(params),
                method = Request.Method.GET,
                headers = createApiHeaders(params)
            ),
            onSuccess = {
                try {
                    val body = it.body.string()
                    val nextPageKey = PageKey.PageUrlKey(parseNextPageUrl(body))
                    val items = fromJson(body)
                    trackItemsShown(items)
                    Result.Success(nextPageKey to items)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            },
            onError = {
                Result.Error(it)
            }
        )
    }

    private fun createApiParams(
        partner: String,
        timestamp: String,
        uid: String,
        category: String,
        languageCode: String,
        pageSize: Int,
        pages: Int
    ): Map<String, String> = mapOf(
        "partner" to partner,
        "ts" to timestamp,
        "puid" to uid,
        "cid" to category,
        "langCode" to languageCode,
        "pageNumber" to pages.toString(),
        "pageSize" to pageSize.toString(),
        "pfm" to "0",
        "fm" to "0",
        "fields" to "none"
    )

    private fun parseUrlParams(url: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val uri = Uri.parse(url)
            val args: Set<String> = uri.queryParameterNames
            args.forEach { key ->
                map[key] = uri.getQueryParameter(key) ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return map
    }

    private fun getApiEndpoint(params: Map<String, String>): String = Uri.parse(API_URL)
            .buildUpon()
            .apply {
                for ((key, value) in params.entries) {
                    appendQueryParameter(key, value)
                }
            }
            .build()
            .toString()

    private fun createApiHeaders(params: Map<String, String>) = MutableHeaders().apply {
        newsProvider?.apiKey?.let {
            set("Authorization", it)
        }

        newsProvider?.secretKey?.let {
            val signature = DailyHuntUtils.generateSignature(it, Request.Method.GET.name, urlEncodeParams(params))
            set("Signature", signature)
        }
    }

    private fun urlEncodeParams(params: Map<String, String>): Map<String, String> {
        val encodedParams = mutableMapOf<String, String>()
        params.forEach {
            encodedParams[it.key] = URLEncoder.encode(it.value, "UTF-8")
        }

        return encodedParams
    }

    private fun parseNextPageUrl(jsonString: String): String = jsonString.toJsonObject()
            .optJSONObject("data")
            ?.optString("nextPageUrl") ?: ""

    private fun fromJson(jsonString: String): List<NewsItem> {
        val jsonObject = jsonString.toJsonObject()
        val newsArray = jsonObject.optJSONObject("data").optJSONArray("rows")
        val trackingUrl = jsonObject.optJSONObject("data")?.optString("trackUrl") ?: ""
        val attributionUrl = jsonObject.optJSONObject("track")?.optJSONArray("comscoreUrls")?.optString(0) ?: ""
        val targetImageDimension = appContext.resources.getDimensionPixelSize(R.dimen.item_news_inner_width).toString()
        return (0 until newsArray.length())
            .map { index ->
                val item = newsArray.getJSONObject(index)
                val imageUrl = try {
                    item.optJSONArray("images")?.getString(0)?.run {
                        this.replace("{CMD}", "crop")
                            .replace("{W}", targetImageDimension)
                            .replace("{H}", targetImageDimension)
                            .replace("{Q}", "75")
                            .replace("{EXT}", "webp")
                    } ?: ""
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }

                val linkUrl = if (item.optString("deepLinkUrl").isNotEmpty()) {
                    item.optString("deepLinkUrl") + "&puid=${URLEncoder.encode(newsProvider?.userId, "UTF-8")}"
                } else {
                    ""
                }

                NewsItem.NewsContentItem(
                    item.optString("title"),
                    linkUrl,
                    imageUrl,
                    item.optString("source"),
                    item.optLong("publishTime"),
                    linkUrl.sha256(),
                    feed = "dailyhunt",
                    trackingUrl = trackingUrl,
                    trackingId = item.optString("id"),
                    trackingData = item.optString("trackData"),
                    attributionUrl = attributionUrl
                )
            }
    }

    private fun trackItemsShown(items: List<NewsItem>) {
        if (items.isEmpty() || items[0] !is NewsItem.NewsContentItem) {
            return
        }
        val params = parseUrlParams((items[0] as NewsItem.NewsContentItem).trackingUrl).toMutableMap().apply {
            put("partner", newsProvider?.partnerCode ?: "")
            put("puid", newsProvider?.userId ?: "")
            put("ts", System.currentTimeMillis().toString())
        }
        sendHttpRequest(
            request = Request(
                url = getTrackingApiEndpoint(params),
                method = Request.Method.POST,
                headers = createTrackingApiHeaders(params),
                body = Request.Body.fromString(createTrackingBody(items))
            ),
            onSuccess = {
                // do noting
            },
            onError = {
                // do noting
            }
        )

        sendHttpRequest(
            request = Request(
                url = (items[0] as NewsItem.NewsContentItem).attributionUrl,
                method = Request.Method.GET
            ),
            onSuccess = {
                // do noting
            },
            onError = {
                // do noting
            }
        )
    }

    private fun getTrackingApiEndpoint(params: Map<String, String>): String = Uri.parse(TRACKING_API_URL)
        .buildUpon()
        .apply {
            for ((key, value) in params.entries) {
                appendQueryParameter(key, value)
            }
        }
        .build()
        .toString()

    private fun createTrackingApiHeaders(params: Map<String, String>) = MutableHeaders().apply {
        set("Content-Type", "application/json")

        newsProvider?.apiKey?.let {
            set("Authorization", it)
        }

        newsProvider?.secretKey?.let {
            val signature = DailyHuntUtils.generateSignature(it, Request.Method.POST.name, urlEncodeParams(params))
            set("Signature", signature)
        }
    }

    private fun createTrackingBody(items: List<NewsItem>): String {
        val json = JSONObject()
        json.put("viewedDate", System.currentTimeMillis())

        val jsonArray = JSONArray()
        for (item in items) {
            if (item is NewsItem.NewsContentItem) {
                jsonArray.put(
                    JSONObject()
                        .put("id", item.trackingId)
                        .put("trackData", item.trackingData)
                )
            }
        }
        json.put("stories", jsonArray)
        return json.toString()
    }

    companion object {
        private const val API_URL = "http://feed.dailyhunt.in/api/v2/syndication/items"
        private const val TRACKING_API_URL = "http://track.dailyhunt.in/api/v2/syndication/tracking"
    }
}