/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsSettings
import org.mozilla.rocket.content.news.data.NewsSettingsRepositoryProvider
import org.mozilla.rocket.content.succeeded
import java.util.Locale

open class LoadNewsSettingsUseCase(repositoryProvider: NewsSettingsRepositoryProvider) {

    val repository = repositoryProvider.provideNewsSettingsRepository()

    suspend operator fun invoke(): Result<NewsSettings> {
        var defaultLanguage = LoadNewsLanguagesUseCase.DEFAULT_LANGUAGE
        val supportLanguagesResult = repository.getLanguages()
        if (supportLanguagesResult is Result.Success &&
            supportLanguagesResult.isNotEmpty &&
            supportLanguagesResult.data.find { newsLanguage -> newsLanguage.isSelected } == null) {
            supportLanguagesResult.data
                .find { language -> Locale.getDefault().displayName.contains(language.name) }
                ?.let { defaultLanguage = it }
        }

        val result = repository.getNewsSettings(defaultLanguage)
        return if (result.succeeded) {
            result
        } else {
            Result.Success(NewsSettings(LoadNewsLanguagesUseCase.DEFAULT_LANGUAGE, DEFAULT_CATEGORY_LIST, false))
        }
    }

    companion object {
        private const val DEFAULT_CATEGORY_ID = "top-news"
        private val DEFAULT_CATEGORY_LIST = listOf(
            NewsCategory.getCategoryById(DEFAULT_CATEGORY_ID)!!
        )
    }
}