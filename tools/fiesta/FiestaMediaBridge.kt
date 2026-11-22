package de.vd3.fiesta

import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.connection.CommManager

/**
 * Direct Android Auto media transport from the native SYNC shell.
 * No fake local playback session or overlay is required.
 */
object FiestaMediaBridge {
    @JvmStatic
    fun sendMediaKey(context: Context, keyCode: Int): Boolean {
        if (keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
            keyCode != KeyEvent.KEYCODE_MEDIA_NEXT &&
            keyCode != KeyEvent.KEYCODE_MEDIA_PREVIOUS) return false
        val manager = App.provide(context).commManager
        if (manager.connectionState.value !is CommManager.ConnectionState.TransportStarted) return false
        val timestamp = SystemClock.uptimeMillis()
        manager.sendKey(keyCode, true, timestamp, "sync")
        manager.sendKey(keyCode, false, timestamp, "sync")
        return true
    }
}
