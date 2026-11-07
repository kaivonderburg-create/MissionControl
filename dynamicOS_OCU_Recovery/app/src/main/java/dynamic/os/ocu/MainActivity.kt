package dynamic.os.ocu

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var probe: ClassicAdapterProbe
    private lateinit var content: LinearLayout
    private val dark = Color.rgb(12, 17, 27)
    private val surface = Color.rgb(25, 34, 50)
    private val foreground = Color.rgb(239, 244, 255)
    private val muted = Color.rgb(164, 179, 201)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); probe = ClassicAdapterProbe(this); showHome() }
    override fun onDestroy() { probe.close(); super.onDestroy() }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(22, 20, 22, 20)
        background = GradientDrawable().apply { setColor(surface); cornerRadius = 24f }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 }
    }
    private fun label(text: String, size: Float = 15f, color: Int = foreground, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(0, 3, 0, 6)
    }
    private fun button(text: String, action: () -> Unit): Button = Button(this).apply {
        this.text = text; isAllCaps = false; setOnClickListener { action() }
    }
    private fun begin(title: String) {
        val scroll = ScrollView(this).apply { setBackgroundColor(dark); fillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 32) }
        scroll.addView(content); setContentView(scroll)
        content.addView(label("dynamicOS  /  DIAGNOSTICS", 13f, Color.rgb(104, 195, 246), true))
        content.addView(label(title, 28f, foreground, true))
    }
    private fun addPanel(title: String, body: String, action: Pair<String, () -> Unit>? = null) {
        val p = panel(); p.addView(label(title, 18f, foreground, true)); p.addView(label(body, 14f, muted))
        action?.let { p.addView(button(it.first, it.second)) }; content.addView(p)
    }
    private fun showHome() {
        begin("OCU Recovery")
        addPanel("Zielsystem", "VW T-Cross R-Line 2025\nSteuergerät 75 · J949\nLG 2GA 035 284 A · H11 / SW 0619\nFehler: B200049 – Interner Elektronikfehler")
        addPanel("Bluetooth-Schnittstelle prüfen", "Gekoppelte Bluetooth-Classic-Geräte anzeigen, SPP-Verbindung versuchen und ELM-Adapteridentifikation lesen. Der EVO kann auch BLE verwenden; in diesem Fall ist SPP nicht verfügbar.", "Adapterprüfung" to { showAdapter() })
        addPanel("Operationskatalog", "Alle Diagnose- und Wiederherstellungsbereiche mit Risiko- und Verfügbarkeitsstatus.", "Katalog öffnen" to { showCatalog() })
        addPanel("Recovery-Ablauf", "Gesicherte Prüfreihenfolge für B200049; keine automatische Fehlerunterdrückung.", "Ablauf ansehen" to { showRecovery() })
        addPanel("Vorabcheck", "OCU vollständig eingebaut; Steckverbindungen, Versorgung und Batterie prüfen. Ohne nachweislich intakte SOS-Funktion ist die Warnung sicherheitsrelevant.")
    }
    private fun showAdapter() {
        begin("Adapterprüfung")
        content.addView(button("← Übersicht") { showHome() })
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            addPanel("Berechtigung erforderlich", "Bluetooth-Verbindungen für dynamicOS OCU Recovery erlauben.", "Berechtigung anfragen" to {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 100)
            })
            return
        }
        val devices = try { probe.pairedDevices() } catch (e: Exception) {
            addPanel("Bluetooth nicht verfügbar", e.message ?: "Adapter nicht aktiviert"); return
        }
        if (devices.isEmpty()) addPanel("Keine gekoppelten SPP-Geräte", "Das EVO-Gerät muss ggf. über BLE verbunden werden. Die App führt keine unkontrollierte Suche oder ECU-Kommandos aus.")
        devices.forEach { device ->
            val name = try { device.name ?: device.address } catch (_: SecurityException) { "Bluetooth-Gerät" }
            addPanel(name, "Nur Classic-SPP-Erkennung. Kein Eingriff in das Fahrzeug.", "SPP testen" to {
                begin("SPP-Test · $name")
                content.addView(button("← Zurück") { probe.close(); showAdapter() })
                val status = label("Verbinde…", 14f, muted)
                content.addView(status)
                Thread {
                    val result = try { probe.connect(device) + "\n\nATI:\n" + probe.probeElm() } catch (e: Exception) { "SPP nicht erreichbar: ${e.javaClass.simpleName}: ${e.message}" }
                    runOnUiThread { status.text = result }
                }.start()
            })
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) showAdapter()
    }
    private fun showCatalog() {
        begin("Operationen")
        content.addView(button("← Übersicht") { showHome() })
        DiagnosticCatalog.operations.forEach { op ->
            val risk = when(op.risk) { Risk.READ_ONLY -> "LESEN"; Risk.STATE_CHANGE -> "ÄNDERND"; Risk.SECURITY_CRITICAL -> "AUTORISIERT / OEM" }
            addPanel("${op.title}  ·  $risk", "${op.service}\n${op.purpose}\nStatus: ${op.availability}")
        }
    }
    private fun showRecovery() {
        begin("B200049 · Prüfpfad")
        content.addView(button("← Übersicht") { showHome() })
        addPanel("1  Diagnose sichern", "Steuergerät 75 identifizieren, DTC-Status, Umgebungsbedingungen, Messwerte und Protokoll exportieren.")
        addPanel("2  Hardware prüfen", "Versorgung, Notrufbatterie, Steckkontakte und Antennenpfade auf realen Fehler prüfen. Keine Annahme eines Antennendefekts allein aus B200049.")
        addPanel("3  OEM-Prüfplan", "Herstellervorgaben für Teilenummer 2GA035284A prüfen; ggf. dokumentierten ECU-Neustart oder Inbetriebnahme über ODIS ausführen.")
        addPanel("4  Fehlerstatus neu bewerten", "Nur nach behobener Ursache und mit geeigneter Diagnosefreigabe Fehler löschen. Zündung wechseln und Fehler erneut lesen.")
        addPanel("5  Notruffunktion validieren", "SOS-LED, Netzregistrierung, Antennen- und Batterie-Messwerte kontrollieren. Keine absichtliche Auslösung eines echten Notrufs zum Test.")
    }
}
