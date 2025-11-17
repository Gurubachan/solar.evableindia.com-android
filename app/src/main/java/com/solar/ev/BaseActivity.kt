package com.solar.ev

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.util.LogoutEventManager
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    private val INACTIVITY_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes
    // More aggressive for testing:
    // private val INACTIVITY_TIMEOUT_MS = 15 * 1000L // 15 seconds

    private lateinit var sessionManager: SessionManager
    private val logoutHandler = Handler(Looper.getMainLooper())
    private val logoutRunnable = Runnable {
        // Perform logout only if this activity is not LoginActivity to avoid loops
        if (this !is LoginActivity) {
            sessionManager.clearSession()
            Toast.makeText(this, "Logged out due to inactivity.", Toast.LENGTH_LONG).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity() 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        // Observe logout events from LogoutEventManager
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                LogoutEventManager.logoutEvent.collect {
                    // Perform logout only if this activity is not LoginActivity to avoid loops
                    // and if not already on LoginActivity (e.g. from inactivity timer on another activity)
                    if (this@BaseActivity !is LoginActivity && !isFinishing) {
                         // Check if already LoginActivity to prevent re-triggering if already on it
                        if (this@BaseActivity.javaClass != LoginActivity::class.java) {
                            performLogout(true) // Pass true to indicate it's from a global event
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Only reset timer if user is actually logged in and this is not LoginActivity
        if (this !is LoginActivity && sessionManager.isLoggedIn()) {
            resetLogoutTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLogoutTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        // Only reset timer if user is actually logged in and this is not LoginActivity
        if (this !is LoginActivity && sessionManager.isLoggedIn()) {
            resetLogoutTimer()
        }
    }

    private fun resetLogoutTimer() {
        logoutHandler.removeCallbacks(logoutRunnable)
        logoutHandler.postDelayed(logoutRunnable, INACTIVITY_TIMEOUT_MS)
    }

    private fun stopLogoutTimer() {
        logoutHandler.removeCallbacks(logoutRunnable)
    }

    protected fun performLogout(isFromGlobalEvent: Boolean = false) {
        if (this is LoginActivity && isFromGlobalEvent) {
            // If this is LoginActivity and the logout was triggered by a global event (e.g., 401),
            // it might mean it's already in the process of logging out from another activity.
            // We might not need to do much here, or just ensure UI is clean.
            return
        }
        
        stopLogoutTimer() 
        if (!sessionManager.isLoggedIn() && !isFromGlobalEvent) {
            // If not logged in and not a global event, no need to proceed with logout UI/navigation
            // unless it's a forced logout (like from 401)
            return
        }
        
        sessionManager.clearSession()
        if (!isFinishing) { // Check if activity is finishing
            if (isFromGlobalEvent) {
                 Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            } else {
                 Toast.makeText(this, "You have been logged out.", Toast.LENGTH_LONG).show()
            }
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
        }
    }
}
