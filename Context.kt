package com.md.matur.utils.extensions

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.md.matur.R
import com.md.matur.utils.*


fun Context.showToast(message: Any, lenght: Int = Toast.LENGTH_LONG) =
    Toast.makeText(
        this, message.toString(),
        lenght
    ).show()

fun Context.getSharedPrefs() =
    getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE)

val Context.baseConfig: BaseConfig
    get() = BaseConfig(
        this
    )

val Context.config: Config
    get() = Config(this)

fun Context.hasPermission(permId: Int) =
    ContextCompat.checkSelfPermission(
        this,
        getPermissionString(permId)
    ) == PackageManager.PERMISSION_GRANTED

fun Context.getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    PERMISSION_TAKE_PHOTO -> Manifest.permission.CAMERA
    PERMISSION_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
    else -> ""
}

fun Context.vibrate(time: Long = 25) {
    if (Build.VERSION.SDK_INT >= 26) {
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(
            VibrationEffect.createOneShot(
                time, VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    } else {
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(time)
    }
}

fun Context.showKeyboard(view: View) {
    val inputMethodManager: InputMethodManager =
        this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(
        view,
        InputMethodManager.SHOW_FORCED
    )
}

/**
 * Перезапуск приложения.
 */
fun Context.triggerRebirth() {
    val packageManager = packageManager
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    val componentName = intent!!.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}

fun Context.copyText(text: String, handler: SimpleHandler? = null) {
    val clipboard: ClipboardManager =
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("label", text)
    clipboard.setPrimaryClip(clip)
    handler?.invoke()
}