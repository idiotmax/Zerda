package org.mozilla.rocket.content

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import org.mozilla.focus.Inject
import org.mozilla.focus.utils.Settings

class ChromeViewModelFactory private constructor(private val settings: Settings) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChromeViewModel::class.java)) {
            return ChromeViewModel(settings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile private var INSTANCE: ChromeViewModelFactory? = null

        @JvmStatic
        fun getInstance(settings: Settings): ChromeViewModelFactory? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ChromeViewModelFactory(settings).also { INSTANCE = it }
                }
    }
}
