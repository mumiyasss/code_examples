package com.md.matur.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import com.md.matur.data.entity.user.Profile
import com.md.matur.feature.offers.map.filter.main.FilterActivity


class Config(context: Context) : BaseConfig(context) {

    var token: String
        get() = prefs.getString(Constants.USERTOKEN) ?: ""
        set(value) = prefs.edit().putString(Constants.USERTOKEN, value).apply()

    var currentProfileJson: String
        get() = prefs.getString(Constants.USERPROFILE) ?: ""
        set(value) = prefs.edit().putString(Constants.USERPROFILE, value).apply()

    var currentProfile: Profile?
        get() = gson.fromJson(currentProfileJson, Profile::class.java)
        set(value) {
            currentProfileJson = (gson.toJson(value))
        }

    val currentProfileLiveData: LiveData<Profile?>
        get() = SharedPreferenceProfileLiveData(prefs, Constants.USERPROFILE, defValue = null)

    var logged: Boolean
        get() = prefs.getBoolean(Constants.LOGGED, false)
        set(value) = prefs.edit().putBoolean(Constants.LOGGED, value).apply()


    var loggedAsMaster: Boolean
        get() = prefs.getBoolean(Constants.LOGGED_AS_MASTER, false)
        set(value) = prefs.edit().putBoolean(Constants.LOGGED_AS_MASTER, value).apply()


    var isAlpha: Boolean
        get() = prefs.getBoolean(isAlphaPrefKey, false)
        set(value) {
            prefs.edit().putBoolean(isAlphaPrefKey, value).commit()
        }

    var shouldShowCalendarHint: Boolean
        get() = prefs.getBoolean(isCalendarHintPrefKey, true)
        set(value) = prefs.edit().putBoolean(isCalendarHintPrefKey, value).apply()

    var shouldShowMiuiHint: Boolean
        get() = prefs.getBoolean(isMiuiHintPrefKey, true)
        set(value) = prefs.edit().putBoolean(isMiuiHintPrefKey, value).apply()
}