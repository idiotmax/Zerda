package org.mozilla.rocket.content.news

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import javax.inject.Inject

class NewsViewModel @Inject constructor(
    private val loadNewsCategoryUseCase: LoadNewsCategoryUseCase
) : ViewModel() {

    private var newCategoryResult: MediatorLiveData<Result<LoadNewsCategoryByLangResult>> = loadNewsCategoryUseCase.observe()

    val categories: LiveData<List<NewsCategory>> =
        Transformations.map(this.newCategoryResult) { (it as? Result.Success)?.data?.categories }

    private val newsMap = HashMap<String, MediatorLiveData<List<NewsItem>>>()

    private val useCaseMap = HashMap<String, LoadNewsUseCase>()

    lateinit var newsSettingsRepository: NewsSettingsRepository

    init {
        updateCategory()
    }

    private fun updateCategory() {
        val param = LoadNewsCategoryByLangParameter()
        loadNewsCategoryUseCase.execute(param)
    }

    fun clear() {
        newsMap.clear()
        useCaseMap.clear()
    }

    fun getNews(category: String, lang: String, repo: Repository<out NewsItem>): MediatorLiveData<List<NewsItem>> {
        val items = newsMap[category] ?: MediatorLiveData()
        if (newsMap[category] == null) {
            newsMap[category] = items
            HashMap<String, String>().apply {
                this["category"] = category
                this["lang"] = lang
            }
            val loadNewsCase = LoadNewsUseCase(repo)
            useCaseMap[category] = loadNewsCase
            items.addSource(loadNewsCase.observe()) { result ->
                (result as? Result.Success)?.data?.let {
                    items.value = it.items
                }
            }
        }
        return items
    }

    fun loadMore(category: String) {
        useCaseMap[category]?.execute(LoadNewsParameter(category))
    }
}