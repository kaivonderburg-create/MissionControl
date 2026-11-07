package dynamic.os.ocu

import android.app.Activity
import android.Manifest
import android.content.Intent
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
import android.bluetooth.BluetoothDevice

class MainActivity : Activity() {
    private lateinit var probe: ClassicAdapterProbe
    private lateinit var ble: BleExplorer
    private lateinit var sessionLog: SessionLog
    private lateinit var content: LinearLayout
    private val preferences by lazy { getSharedPreferences("dynamic_os_ocu", MODE_PRIVATE) }
    private var pendingExport = ""
    private val dark = Color.rgb(8, 14, 25)
    private val surface = Color.rgb(22, 34, 52)
    private val foreground = Color.rgb(239, 244, 255)
    private val muted = Color.rgb(164, 179, 201)
    private val cyan = Color.rgb(106, 213, 255)
    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); probe = ClassicAdapterProbe(this); sessionLog = SessionLog(this); ble = BleExplorer(this) { sessionLog.add(it) }; sessionLog.add("App gestartet; Schreiboperationen nicht aktiviert"); showHome() }
    override fun onDestroy() { ble.close(); probe.close(); super.onDestroy() }

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
        addPanel("◈  OCU-Serviceakte", "Modulkennungen, LED-Status und Herstellervorgaben zu B200049 mit Diagnoseprotokoll abgleichen.", "SERVICEAKTE ÖFFNEN" to { showServiceRecord() })
        addPanel("⚙  Werkzeuge & Konfiguration", "Adapterpräferenzen, Zeitlimits, Debug-Protokolle und Export.", "EINSTELLUNGEN" to { showSettings() })
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
                    val result = try { probe.connect(device) + "\n\nATI:\n" + probe.probeElm(preferences.getInt("timeout_ms", 2500).toLong()) } catch (e: Exception) { "SPP nicht erreichbar: ${e.javaClass.simpleName}: ${e.message}" }
                    sessionLog.add("SPP-Test $name: $result")
                    runOnUiThread { status.text = result }
                }.start()
            })
            addPanel("ELM-Adapter-Selbsttest · $name", "Liest ausschließlich Adapteridentität, Protokollmodus und Adapter-Versorgungsspannung (ATI, AT@1, ATDP, ATDPN, ATRV). Kein Steuergerätebefehl.", "SELBSTTEST STARTEN" to {
                begin("Adapter-Selbsttest")
                content.addView(button("← Adapter") { probe.close(); showAdapter() })
                val status = label("Verbinde und frage Adapterdaten ab …", 13f, muted)
                content.addView(status)
                Thread {
                    val result = try {
                        probe.connect(device) + "\n\n" + probe.adapterSelfTest(preferences.getInt("timeout_ms", 2500).toLong())
                    } catch (e: Exception) { "Selbsttest fehlgeschlagen: ${e.javaClass.simpleName}: ${e.message}" }
                    sessionLog.add("ELM-Selbsttest $name: $result")
                    runOnUiThread { status.text = result }
                }.start()
            })
            addPanel("UDS-Servicekonsole · $name", "Direkte ISO-TP-Diagnose mit manuell gewähltem CAN-Anfrageheader. Unterstützt Identifikation, DTC-Abfrage, Reset und bestätigtes DTC-Löschen.", "UDS-KONSOLE ÖFFNEN" to { showUdsConsole(device) })
            addPanel("OBD-II Basisdiagnose · $name", "Echte generische OBD-II-Leseabfragen: VIN, verfügbare PIDs, Drehzahl, Geschwindigkeit, Spannung. Keine DTC-Löschung.", "OBD-TEST STARTEN" to {
                begin("OBD-II Lesediagnose")
                val result = label("Verbinde …", 13f, muted)
                content.addView(result)
                Thread {
                    val output = try { probe.connect(device) + "\n\n" + probe.genericObdReadout(preferences.getInt("timeout_ms", 2500).toLong()) }
                        catch (e: Exception) { "OBD-Test fehlgeschlagen: ${e.javaClass.simpleName}: ${e.message}" }
                    sessionLog.add("OBD-II: $output")
                    runOnUiThread { result.text = output }
                }.start()
                content.addView(button("DEBUG-PROTOKOLL") { showDebug() })
            })
        }
        addPanel("BLE / GATT-Gerätesuche", "BLE-Adapter ohne vorherige Android-Kopplung finden und angebotene GATT-Dienste anzeigen. Keine Nutzdaten an das Fahrzeug senden.", "BLE-SUCHLAUF" to { showBleScan() })
        addPanel("Debug-Protokolle", "SPP- und BLE-Ergebnisse lokal protokollieren und exportieren.", "PROTOKOLL ÖFFNEN" to { showDebug() })
    }
    private fun showBleScan() {
        begin("BLE-Geräte")
        val resultPanel = panel()
        val info = label("Suche läuft …", 14f, muted)
        resultPanel.addView(info)
        content.addView(resultPanel)
        content.addView(button("SUCHE STOPPEN") { ble.stopScan(); info.text = "Suche beendet" })
        val seen = hashSetOf<String>()
        try {
            ble.start { device, name, rssi ->
                runOnUiThread {
                    if (seen.add(device.address)) {
                        val entry = panel()
                        entry.addView(label(name, 17f, foreground, true))
                        entry.addView(label("RSSI $rssi dBm  ·  ${device.address}", 12f, muted))
                        entry.addView(button("GATT-DIENSTE LESEN") {
                            begin("GATT-Analyse")
                            val output = label("Verbinde …", 13f, muted)
                            content.addView(output)
                            try { ble.inspect(device) { report -> runOnUiThread { output.text = report } } } catch (e: Exception) { output.text = e.message ?: "Verbindungsfehler" }
                            content.addView(button("ZURÜCK ZUR SUCHE") { ble.disconnect(); showBleScan() })
                        })
                        content.addView(entry)
                    }
                }
            }
        } catch (e: SecurityException) { info.text = "Bluetooth-Berechtigungen fehlen. Bitte im Adaptermenü freigeben." }
        catch (e: Exception) { info.text = "BLE nicht verfügbar: ${e.message}" }
    }
    private fun showDebug() {
        begin("Debug & Protokolle")
        val textView = label(sessionLog.text(), 12f, muted)
        val card = panel(); card.addView(textView); content.addView(card)
        content.addView(button("PROTOKOLL EXPORTIEREN") { exportLog() })
        content.addView(button("PROTOKOLL LEEREN") { sessionLog.clear(); showDebug() })
    }
    private fun showSettings() {
        begin("Konfiguration")
        addPanel("Sicherheitsmodus", "Nur lesende Operationen und Metadatenanalyse. Unbestätigte ECU-Schreibbefehle sind technisch nicht implementiert.")
        val p = panel()
        p.addView(label("Adapterpräferenz", 17f, foreground, true))
        val options = arrayOf("Auto (manuell auswählen)", "Bluetooth Classic SPP", "Bluetooth LE / GATT")
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, options); setSelection(preferences.getInt("transport", 0)) }
        p.addView(spinner)
        p.addView(label("Adapterantwort-Zeitlimit (ms)", 16f, foreground, true))
        val timeout = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(preferences.getInt("timeout_ms", 2500).toString()); setTextColor(this@MainActivity.foreground); hint = "500–10000" }
        p.addView(timeout)
        p.addView(button("EINSTELLUNGEN SPEICHERN") {
            val ms = timeout.text.toString().toIntOrNull()?.coerceIn(500, 10000) ?: 2500
            preferences.edit().putInt("transport", spinner.selectedItemPosition).putInt("timeout_ms", ms).apply()
            sessionLog.add("Konfiguration gespeichert: Adapter=${options[spinner.selectedItemPosition]}, Timeout=$ms ms")
            Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
        })
        content.addView(p)
        addPanel("Diagnose-Protokoll", "Alle erfassten Adapter- und GATT-Ereignisse ansehen und als TXT exportieren.", "DEBUG ÖFFNEN" to { showDebug() })
    }
    private fun exportLog() {
        pendingExport = sessionLog.text()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "text/plain"; putExtra(Intent.EXTRA_TITLE, "dynamicOS_OCU_Debug.txt") }
        startActivityForResult(intent, 200)
    }
    @Deprecated("Required by Android Activity document result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK && data?.data != null) {
            try { contentResolver.openOutputStream(data.data!!)?.use { it.write(pendingExport.toByteArray(Charsets.UTF_8)) }; Toast.makeText(this, "Protokoll gespeichert", Toast.LENGTH_SHORT).show() }
            catch (e: Exception) { Toast.makeText(this, "Export fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show() }
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
        addPanel("VW-Prüfvorgaben", "Bei statischem B200049 kann die Ursache je nach OCU-Variante Hardware, insbesondere die integrierte Backup-Antenne, sein. Ein bloßes Löschen der Ereignishistorie gilt nicht als Nachweis einer Reparatur.\n\nDie beschriebenen VW-TPI-Verfahren sind teils auf andere Teilefamilien beschränkt. Vor Anwendung muss die Zuordnung zum 2GA 035 284 A eindeutig bestätigt werden.", "SERVICEAKTE & TPI-ABGLEICH" to { showServiceRecord() })
        addPanel("Manueller Prüfablauf", "Hake einen Schritt nur ab, wenn du ihn tatsächlich durchgeführt und dokumentiert hast. Diese Checkliste sendet keine Befehle an das Fahrzeug.")
        val steps = listOf(
            "Diagnose sichern" to "Steuergerät 75, DTC-Status und Umgebungsbedingungen auslesen und dokumentieren.",
            "Hardware prüfen" to "Versorgung, Steckkontakte und Notrufbatterie kontrollieren; Antennenfehler nicht allein aus B200049 ableiten.",
            "OEM-Prüfplan" to "Herstellervorgaben für 2GA035284A und dokumentierte Neustart-/Inbetriebnahmefunktion prüfen.",
            "Fehlerstatus neu bewerten" to "Nach der Reparatur im autorisierten Diagnosepfad löschen und anschließend erneut auslesen.",
            "Notruffunktion validieren" to "SOS-LED, Netzstatus, Batterie und Selbsttest prüfen; keine echte Notrufauslösung zum Test."
        )
        steps.forEachIndexed { index, step ->
            val p = panel()
            val check = CheckBox(this).apply {
                text = "${index+1}. ${step.first}"; textSize = 17f; setTextColor(this@MainActivity.foreground)
                isChecked = preferences.getBoolean("recovery_$index", false)
                setOnCheckedChangeListener { _, checked -> preferences.edit().putBoolean("recovery_$index", checked).apply(); sessionLog.add("Recovery-Checkliste Schritt ${index+1}: ${if(checked) "als erledigt markiert" else "zurückgesetzt"}") }
            }
            p.addView(check); p.addView(label(step.second, 13f, muted)); content.addView(p)
        }
        content.addView(button("DEBUG-PROTOKOLL ÖFFNEN") { showDebug() })
    }

    private fun showServiceRecord() {
        begin("OCU-Serviceakte")
        addPanel("Modulidentifikation", "Fahrzeug: VW T-Cross R-Line MJ 2025\nDiagnoseadresse: 0075 · J949\nBekannt: 2GA 035 284 A · H11 · SW 0619\nBeanstandung: B200049 nach zeitweiliger Trennung der OCU bei eingeschalteter Fahrzeugdiagnose/Start.\n\nAngaben bitte anhand eines neuen Steuergeräte-Scans bestätigen.")
        addPanel("TPI-Abgleich · B200049", "VW TPI 2070694/6 und 2072798/2 beschreiben für bestimmte J949-Varianten eine Prüfung der integrierten Backup-Antenne und anschließend geführte Fehlersuche. Die Teilefamilie 5WA ist nicht automatisch identisch mit deinem 2GA-Modul.\n\nOriginalablauf: DTC-Umgebungsdaten sichern → ODIS Geführte Fehlersuche → Reparaturvorschlag nach Steuergerätekennung → Anpassung/Inbetriebnahme → erneutes Auslesen → SOS-Status prüfen.")
        addPanel("Wiederanlauf / Initialisierung", "Das abgeklemmt gestartete Modul ist inzwischen wieder eingebaut. Ein kontrollierter Spannungsreset, ODIS-Neustart oder die OCU-Aktivierung darf erst nach der zu diesem Steuergerät passenden VW-Anweisung erfolgen. Die App sendet hier bewusst keine vermuteten UDS-Routine-IDs, SFD-Entsperrungen oder Werkseinstellungsbefehle.")
        val p = panel()
        p.addView(label("Mess- und Sichtprüfung dokumentieren", 18f, foreground, true))
        val fields = listOf(
            "scan" to "DTC-Status / Fehlertext / Freeze Frame",
            "led" to "SOS-LED (rot, grün, aus, blinkend)",
            "battery" to "OCU-Backupbatterie / gemessener Zustand",
            "antenna" to "Antennen / Stecker / Deckel geprüft",
            "version" to "Abweichende Hardware-/Softwarekennung",
            "notes" to "Weitere Werkstatt- oder Diagnosenotizen"
        )
        val edits = mutableListOf<Pair<String, EditText>>()
        fields.forEach { (key, caption) ->
            p.addView(label(caption, 13f, muted, true))
            val field = EditText(this).apply {
                setSingleLine(false); minLines = if (key == "notes") 3 else 1
                setText(preferences.getString("ocu_$key", "") ?: "")
                setTextColor(this@MainActivity.foreground)
                setHintTextColor(this@MainActivity.muted)
                hint = "Hier dokumentieren"
            }
            edits.add(key to field); p.addView(field)
        }
        p.addView(button("SERVICEAKTE SPEICHERN") {
            val editor = preferences.edit()
            edits.forEach { (key, field) -> editor.putString("ocu_$key", field.text.toString()) }
            editor.apply()
            sessionLog.add("OCU-Serviceakte aktualisiert: " + edits.joinToString(" | ") { "${it.first}=${it.second.text}" })
            Toast.makeText(this, "Serviceakte gespeichert", Toast.LENGTH_SHORT).show()
        })
        content.addView(p)
        addPanel("Servicebericht exportieren", "Aus der ausgefüllten Serviceakte, den bekannten VW-TPI-Hinweisen und dem Debug-Protokoll entsteht eine lokale Textdatei für die weitere Fehlersuche.", "BERICHT EXPORTIEREN" to {
            val notes = fields.joinToString("\n") { (key, caption) -> "$caption: ${preferences.getString("ocu_$key", "")}" }
            pendingExport = "dynamicOS OCU – Servicebericht\nVW T-Cross 2025 / J949 / 0075\n2GA 035 284 A H11 SW 0619\nDTC B200049\n\n$notes\n\nDiagnoseprotokoll:\n${sessionLog.text()}"
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "text/plain"; putExtra(Intent.EXTRA_TITLE, "dynamicOS_OCU_Servicebericht.txt") }
            startActivityForResult(intent, 200)
        })
    }

    private fun showUdsConsole(device: BluetoothDevice) {
        begin("UDS-Servicekonsole")
        addPanel("Wichtige Voraussetzung", "Du musst die tatsächliche CAN-Anfrage-ID für Steuergerät 0075 aus einem verifizierten Diagnosemitschnitt eintragen. Die Adresse '75' ist die logische VAG-Steuergeräteadresse, kein CAN-Header. Ohne zutreffende ID darf kein Reset gesendet werden.\n\nELM327: 11-bit ISO 15765-4/500 kbit/s oder 29-bit CAN. SFD-/SecurityAccess wird nicht umgangen.")
        val box = panel()
        box.addView(label("CAN-Anfrage-ID in Hex (3 oder 8 Zeichen)", 16f, foreground, true))
        val header = EditText(this).apply {
            setText(preferences.getString("uds_can_header", "") ?: "")
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(this@MainActivity.muted)
            hint = "z. B. aus deinem Diagnosemitschnitt"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        box.addView(header)
        val status = label("Noch keine Anfrage ausgeführt.", 13f, muted)
        box.addView(status)
        fun executeUds(command: String, caption: String, changing: Boolean) {
            val target = header.text.toString().trim().uppercase()
            if (!target.matches(Regex("[0-9A-F]{3}|[0-9A-F]{8}"))) {
                status.text = "Ungültiger CAN-Header. Bitte erst die Ziel-ID eintragen."
                return
            }
            val work = {
                preferences.edit().putString("uds_can_header", target).apply()
                status.text = "Verbinde · $caption …"
                Thread {
                    val report = try {
                        probe.connect(device)
                        "$caption\nAnfrage-ID: $target\nUDS: $command\n\n${probe.udsRequest(target, command, preferences.getInt("timeout_ms", 3500).toLong())}"
                    } catch (e: Exception) { "$caption: ${e.javaClass.simpleName}: ${e.message}" }
                    sessionLog.add("UDS $report")
                    runOnUiThread { status.text = report }
                }.start()
            }
            if (changing) android.app.AlertDialog.Builder(this)
                .setTitle("Steuergerät wirklich verändern?")
                .setMessage("Befehl $command an CAN-ID $target senden? Dies kann die SOS-Funktion vorübergehend unterbrechen oder Fehlerspeicher löschen. Nur fortfahren, wenn ID und VW-Verfahren sicher bestätigt sind. Zündung und stabile Fahrzeugspannung sicherstellen.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("BEFEHL SENDEN") { _, _ -> work() }
                .show()
            else work()
        }
        box.addView(button("STEUERGERÄT / VIN LESEN (22 F190)") { executeUds("22F190", "Fahrzeugidentifikation", false) })
        box.addView(button("SOFTWAREKENNUNG LESEN (22 F188)") { executeUds("22F188", "Softwarekennung", false) })
        box.addView(button("FEHLERSTATUS LESEN (19 02 FF)") { executeUds("1902FF", "DTC lesen", false) })
        box.addView(button("DTC-ANZAHL LESEN (19 01 FF)") { executeUds("1901FF", "DTC-Status", false) })
        box.addView(button("STEUERGERÄT NEUSTARTEN (11 01)") { executeUds("1101", "ECU Hard Reset", true) })
        box.addView(button("STEUERGERÄT SOFT-RESET (11 03)") { executeUds("1103", "ECU Soft Reset", true) })
        box.addView(button("FEHLERSPEICHER LÖSCHEN (14 FF FF FF)") { executeUds("14FFFFFF", "DTC löschen", true) })
        content.addView(box)
        addPanel("Antworten interpretieren", "7F = negative UDS-Antwort; 50/51/54/59/62 = mögliche positive Antworten der zugehörigen Dienste. 'NO DATA', 'CAN ERROR' oder Timeout bedeuten nicht automatisch, dass die OCU defekt ist. Prüfe zuerst Adaptertransport, Anfrage-ID, Antwortadressierung und Sessionberechtigung.")
        content.addView(button("DEBUG-PROTOKOLL") { showDebug() })
    }
}
