package com.solar.ev.viewModel.suryaghar

/**
 * A generic class that holds a value with its loading status.
 * @param <T>דה
 */
sealed class SuryaGharApiResult<out T> {
    data class Success<out T>(val data: T) : SuryaGharApiResult<T>()
    data class Error(val message: String, val errors: Map<String, List<String>>? = null) : SuryaGharApiResult<Nothing>()
    object Loading : SuryaGharApiResult<Nothing>()
}
