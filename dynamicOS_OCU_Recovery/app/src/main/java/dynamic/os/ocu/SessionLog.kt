package dynamic.os.ocu

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionLog(private val context: Context) {
    private val entries = ArrayDeque<String>()
    @Synchronized fun add(message: String) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.GERMANY).format(Date())
        entries.addLast("[$stamp] $message")
        while (entries.size > 400) entries.removeFirst()
    }
    @Synchronized fun text(): String = buildString {
        appendLine("dynamicOS OCU Recovery · Diagnosebericht")
        appendLine("Nur Adapter-/App-Diagnose; keine bestätigte Steuergeräte-Reparatur")
        appendLine("Ziel: J949 / Adresse 75 / 2GA035284A / B200049")
        appendLine("App-Version: 0.4.0")
        appendLine()
        entries.forEach { appendLine(it) }
    }
    @Synchronized fun clear() { entries.clear(); add("Protokoll manuell geleert") }
}
