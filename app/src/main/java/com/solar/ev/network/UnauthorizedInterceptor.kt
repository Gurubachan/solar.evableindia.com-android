package com.solar.ev.network

import com.solar.ev.SolarEvApplication
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.util.LogoutEventManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class UnauthorizedInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 401) {
            // Access SessionManager via application context
            val sessionManager = SessionManager(SolarEvApplication.appContext)
            
            // Perform logout operations in a background thread / coroutine
            // to avoid blocking the network interceptor chain.
            CoroutineScope(Dispatchers.IO).launch {
                // Check if user was actually logged in before attempting to clear session and trigger logout event
                // This prevents multiple logout triggers if 401 is hit repeatedly for a logged-out user.
                if (sessionManager.isLoggedIn()) {
                    sessionManager.clearSession()
                    LogoutEventManager.triggerLogout()
                }
            }
        }
        return response
    }
}
