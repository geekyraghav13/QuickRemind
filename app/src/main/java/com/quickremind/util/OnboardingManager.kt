package com.quickremind.util

import android.content.Context
import android.content.SharedPreferences

object OnboardingManager {
    private const val PREFS_NAME = "OnboardingPrefs"
    private const val KEY_ONBOARDING_COMPLETE = "IsOnboardingComplete"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingComplete(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setOnboardingComplete(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }
}