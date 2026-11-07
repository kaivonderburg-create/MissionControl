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
        manager.adapter?.cancelDiscovery()
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
    @Synchronized fun probeElm(): String {
        val out = output ?: return "Keine SPP-Verbindung"
        out.write("ATI\r".toByteArray(Charsets.US_ASCII)); out.flush()
        val stream = input ?: return "Keine Antwort"
        val deadline = System.currentTimeMillis() + 2500
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

    override fun close() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        input = null; output = null; socket = null
    }
}
