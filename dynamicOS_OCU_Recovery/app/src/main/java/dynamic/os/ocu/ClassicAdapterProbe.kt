package dynamic.os.ocu

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.util.UUID

/** Passive Bluetooth Classic SPP probe. No OBD vehicle commands are sent. */
class ClassicAdapterProbe(private val context: Context) : Closeable {
    private var socket: android.bluetooth.BluetoothSocket? = null
    private var input: BufferedInputStream? = null
    private var output: BufferedOutputStream? = null
    private val spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<BluetoothDevice> {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter?.bondedDevices?.sortedBy { it.name ?: "" } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): String {
        close()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        // Optional optimization: Android 12+ requires BLUETOOTH_SCAN here.
        // Never abort the SPP connection just because scan permission is absent.
        try { manager.adapter?.cancelDiscovery() } catch (_: SecurityException) {}
        val next = device.createRfcommSocketToServiceRecord(spp)
        try {
            next.connect()
            socket = next
            input = BufferedInputStream(next.inputStream)
            output = BufferedOutputStream(next.outputStream)
            return "SPP-Verbindung hergestellt: ${device.name ?: device.address}. Noch keine ECU-Diagnose aktiviert."
        } catch (e: Exception) {
            try { next.close() } catch (_: Exception) {}
            throw e
        }
    }

    /** AT commands are adapter-local. Never issue UDS frames through this method. */
    @Synchronized fun probeElm(timeoutMs: Long = 2500): String = adapterCommand("ATI", timeoutMs)

    /** Whitelisted ELM interface metadata only; no OBD/CAN/UDS frames are transmitted. */
    @Synchronized fun adapterCommand(command: String, timeoutMs: Long = 2500): String {
        require(command in setOf("ATI", "AT@1", "ATDP", "ATDPN", "ATRV", "ATIGN", "ATIGN?", "ATE0", "ATL0", "ATS0", "ATH1", "ATSP0", "ATSP6", "ATCAF1", "0100", "010C", "010D", "0902")) { "Nicht freigegebener Diagnosebefehl" }
        val out = output ?: return "Keine SPP-Verbindung"
        // Old unread response bytes must not be misattributed to the next command.
        val stream = input ?: return "Keine Antwort"
        var discarded = 0
        while (stream.available() > 0 && discarded < 2048) { stream.read(); discarded++ }
        out.write("$command\r".toByteArray(Charsets.US_ASCII)); out.flush()
        val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(500, 10000)
        val response = StringBuilder()
        while (System.currentTimeMillis() < deadline && response.length < 1024) {
            val available = stream.available()
            if (available > 0) {
                repeat(minOf(available, 256)) { response.append(stream.read().toChar()) }
                if (response.contains('>')) break
            } else Thread.sleep(35)
        }
        return response.toString().ifBlank { "Keine ELM-Antwort erhalten. Der EVO nutzt möglicherweise BLE oder ein proprietäres Protokoll." }
    }

    /** Executable ELM327 generic OBD-II readout; requests only standard read-only PIDs. */
    @Synchronized fun genericObdReadout(timeoutMs: Long = 2500): String {
        return listOf("ATE0", "ATL0", "ATS0", "ATSP0", "ATI", "ATRV", "ATDP", "0100", "010C", "010D", "0902")
            .joinToString("\n\n") { cmd ->
                val reply = try { adapterCommand(cmd, timeoutMs) } catch (e: Exception) { "Fehler: ${e.message}" }
                "$cmd\n$reply"
            }
    }

    /** Explicit, user-configured ISO-TP/UDS session. No guessed VW CAN identifiers. */
    @Synchronized fun udsRequest(header: String, request: String, timeoutMs: Long = 3500, bitrate: Int = 500): String {
        val cleanHeader = header.trim().uppercase()
        require(cleanHeader.matches(Regex("[0-9A-F]{3}|[0-9A-F]{8}"))) { "CAN-Header muss 3 oder 8 Hex-Zeichen enthalten" }
        val cleanRequest = request.replace(" ", "").uppercase()
        require(cleanRequest.length in 2..128 && cleanRequest.length % 2 == 0 && cleanRequest.matches(Regex("[0-9A-F]+"))) {
            "UDS-Hexdaten ungültig (1–64 Bytes)"
        }
        require(output != null) { "Keine aktive SPP-Verbindung" }
        require(bitrate == 500 || bitrate == 250) { "Nur 250/500 kbit/s unterstützt" }
        // ELM automatic CAN formatting; the ECU request is ISO-TP payload (not CAN framing bytes).
        val protocol = when {
            cleanHeader.length == 8 && bitrate == 500 -> "ATSP7"
            cleanHeader.length == 8 -> "ATSP9"
            bitrate == 500 -> "ATSP6"
            else -> "ATSP8"
        }
        val setup = listOf("ATE0", "ATL0", "ATS0", "ATH1", protocol, "ATCAF1", "ATSH$cleanHeader")
        setup.forEach { cmd ->
            val reply = writeAndRead(cmd, timeoutMs)
            if (reply.contains("?")) error("Adapter lehnt $cmd ab: $reply")
        }
        return writeAndRead(cleanRequest, timeoutMs)
    }

    @Synchronized private fun writeAndRead(command: String, timeoutMs: Long): String {
        val out = output ?: error("Nicht verbunden")
        val stream = input ?: error("Kein Empfang")
        while (stream.available() > 0) stream.read()
        out.write("$command\r".toByteArray(Charsets.US_ASCII)); out.flush()
        val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(500, 15000)
        val response = StringBuilder()
        while (System.currentTimeMillis() < deadline && response.length < 4096) {
            if (stream.available() > 0) {
                response.append(stream.read().toChar())
                if (response.contains('>')) break
            } else Thread.sleep(20)
        }
        return response.toString().ifBlank { "TIMEOUT" }
    }

    @Synchronized fun adapterSelfTest(timeoutMs: Long = 2500): String {
        return listOf("ATI", "AT@1", "ATDP", "ATDPN", "ATRV").joinToString("\n\n") { command ->
            val result = try { adapterCommand(command, timeoutMs) } catch (e: Exception) { "Fehler: ${e.message}" }
            "$command\n$result"
        }
    }

    override fun close() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        input = null; output = null; socket = null
    }
}
