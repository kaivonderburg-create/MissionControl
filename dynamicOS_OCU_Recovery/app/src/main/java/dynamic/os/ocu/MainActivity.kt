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
import android.view.WindowInsets
import android.widget.*

class MainActivity : Activity() {
    private lateinit var probe: ClassicAdapterProbe
    private lateinit var content: LinearLayout
    private val dark = Color.rgb(8, 14, 25)
    private val surface = Color.rgb(22, 34, 52)
    private val foreground = Color.rgb(239, 244, 255)
    private val muted = Color.rgb(164, 179, 201)
    private val cyan = Color.rgb(106, 213, 255)
    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); probe = ClassicAdapterProbe(this); showHome() }
    override fun onDestroy() { probe.close(); super.onDestroy() }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(19), dp(20), dp(19))
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(surface, Color.rgb(17, 27, 43))).apply {
            cornerRadius = dp(23).toFloat(); setStroke(dp(1), Color.rgb(43, 61, 81))
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
    }
    private fun label(text: String, size: Float = 15f, color: Int = foreground, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(3), 0, dp(7))
    }
    private fun button(text: String, action: () -> Unit): Button = Button(this).apply {
        this.text = text; isAllCaps = false; textSize = 14f; setTextColor(Color.rgb(4, 24, 37))
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(cyan, Color.rgb(131, 232, 255))).apply { cornerRadius = dp(16).toFloat() }
        setPadding(dp(14), dp(6), dp(14), dp(6)); setOnClickListener { action() }
    }
    private fun begin(title: String) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(dark) }
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(20)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(navigation(title))
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(0, bars.top, 0, bars.bottom)
                insets
            }
            root.requestApplyInsets()
        } else root.fitsSystemWindows = true
        val brand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        brand.addView(ImageView(this).apply {
            setImageResource(dynamic.os.ocu.R.drawable.dynamic_os_mark)
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
        })
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        brandText.addView(label("dynamicOS", 22f, foreground, true))
        brandText.addView(label("OCU CONTROL CENTER  ·  2026", 10f, cyan, true))
        brand.addView(brandText)
        content.addView(brand)
        content.addView(label(title, 30f, foreground, true))
    }
    private fun navigation(selected: String): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(dp(5), dp(7), dp(5), dp(7))
            setBackgroundColor(Color.rgb(17, 27, 42))
        }
        val entries = listOf(Triple("⌂", "Übersicht", { showHome() }), Triple("◉", "Adapter", { showAdapter() }), Triple("▤", "Operationen", { showCatalog() }), Triple("✦", "Recovery", { showRecovery() }))
        entries.forEach { (symbol, name, action) ->
            val active = selected.contains(name, true) || (name == "Übersicht" && selected == "OCU Recovery") || (name == "Recovery" && selected.contains("B200049"))
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, dp(5), 0, dp(5)); setOnClickListener { action() } }
            item.addView(label(symbol, 23f, if (active) cyan else muted, true).apply { gravity = Gravity.CENTER })
            item.addView(label(name, 10f, if (active) cyan else muted, active).apply { gravity = Gravity.CENTER })
            bar.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        return bar
    }
    private fun addPanel(title: String, body: String, action: Pair<String, () -> Unit>? = null) {
        val p = panel(); p.addView(label(title, 18f, foreground, true)); p.addView(label(body, 14f, muted))
        action?.let { p.addView(button(it.first, it.second)) }; content.addView(p)
    }
    private fun showHome() {
        begin("OCU Recovery")
        addPanel("●  SYSTEMSTATUS  /  DIAGNOSE", "VW T-Cross R-Line 2025\nSteuergerät 75  ·  J949\nLG 2GA 035 284 A  |  H11 / SW 0619\n\nB200049  –  Interner Elektronikfehler")
        addPanel("◉  Adapter verbinden", "Carista EVO über Bluetooth prüfen. Zunächst wird nur die Adapteridentifikation angefordert.", "VERBINDUNG PRÜFEN" to { showAdapter() })
        addPanel("▤  Diagnose & Operationen", "Alle verfügbaren Dienste mit Freigabe-, Risiko- und Implementierungsstatus.", "OPERATIONEN ANSEHEN" to { showCatalog() })
        addPanel("✦  Geführte Wiederherstellung", "Sicherer Prüfpfad für B200049. Keine ungeprüften Änderungen am Notrufsystem.", "RECOVERY ÖFFNEN" to { showRecovery() })
        addPanel("Vorabcheck", "OCU vollständig eingebaut; Steckverbindungen, Versorgung und Batterie prüfen. Ohne nachweislich intakte SOS-Funktion ist die Warnung sicherheitsrelevant.")
    }
    private fun showAdapter() {
        begin("Adapter")
        content.addView(button("← Übersicht") { showHome() })
        if (Build.VERSION.SDK_INT >= 31 && (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            addPanel("Berechtigung erforderlich", "Bluetooth-Verbindungen und Suche nach gekoppelten Adaptern für dynamicOS OCU Recovery erlauben.", "Berechtigungen anfragen" to {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 100)
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
        begin("B200049 · Recovery")
        content.addView(button("← Übersicht") { showHome() })
        addPanel("1  Diagnose sichern", "Steuergerät 75 identifizieren, DTC-Status, Umgebungsbedingungen, Messwerte und Protokoll exportieren.")
        addPanel("2  Hardware prüfen", "Versorgung, Notrufbatterie, Steckkontakte und Antennenpfade auf realen Fehler prüfen. Keine Annahme eines Antennendefekts allein aus B200049.")
        addPanel("3  OEM-Prüfplan", "Herstellervorgaben für Teilenummer 2GA035284A prüfen; ggf. dokumentierten ECU-Neustart oder Inbetriebnahme über ODIS ausführen.")
        addPanel("4  Fehlerstatus neu bewerten", "Nur nach behobener Ursache und mit geeigneter Diagnosefreigabe Fehler löschen. Zündung wechseln und Fehler erneut lesen.")
        addPanel("5  Notruffunktion validieren", "SOS-LED, Netzregistrierung, Antennen- und Batterie-Messwerte kontrollieren. Keine absichtliche Auslösung eines echten Notrufs zum Test.")
    }
}
