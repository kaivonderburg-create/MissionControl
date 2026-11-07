package dynamic.os.ocu

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import java.util.UUID

/** ELM327 ASCII over common BLE UART profiles. Vendor-specific encrypted adapters may not work. */
class BleElmTransport(private val context: Context, private val log: (String) -> Unit) {
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private val response = StringBuilder()
    private val lock = Object()
    private var ready = false
    private var error: String? = null
    private val nusService = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val nusRx = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    private val nusTx = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, onReady: (String) -> Unit) {
        close(); ready = false; error = null
        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (status != 0) { synchronized(lock) { error = "GATT Fehler $status"; lock.notifyAll() }; onReady("GATT Fehler $status"); return }
                if (newState == BluetoothProfile.STATE_CONNECTED) g.discoverServices()
                if (newState == BluetoothProfile.STATE_DISCONNECTED) { synchronized(lock) { ready = false; lock.notifyAll() } }
            }
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != 0) { onReady("Service-Entdeckung fehlgeschlagen: $status"); return }
                val service = g.getService(nusService)
                var writable = service?.getCharacteristic(nusRx)
                var notifier = service?.getCharacteristic(nusTx)
                var profile = "Nordic UART"
                if (writable == null || notifier == null) {
                    // Generic transparent UART profile: select matching write + notify properties.
                    val candidate = g.services.asSequence().filterNot { it.uuid.toString().startsWith("000018") }.mapNotNull { svc ->
                        val tx = svc.characteristics.firstOrNull { it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 }
                        val rx = svc.characteristics.firstOrNull { it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 }
                        if (tx != null && rx != null) Triple(svc.uuid, tx, rx) else null
                    }.firstOrNull()
                    if (candidate != null) { writable = candidate.second; notifier = candidate.third; profile = "Generic UART ${candidate.first}" }
                }
                if (writable == null || notifier == null) {
                    val uuids = g.services.joinToString("; ") { it.uuid.toString() }
                    onReady("Kein geeignetes UART-GATT-Profil gefunden. GATT: $uuids")
                    return
                }
                writeChar = writable
                g.setCharacteristicNotification(notifier, true)
                notifier.getDescriptor(cccd)?.let { descriptor ->
                    if (Build.VERSION.SDK_INT >= 33) g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    else { @Suppress("DEPRECATION") descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; @Suppress("DEPRECATION") g.writeDescriptor(descriptor) }
                }
                synchronized(lock) { ready = true; lock.notifyAll() }
                onReady("BLE $profile verbunden: TX=${writable.uuid}, RX=${notifier.uuid}. ELM-ASCII-Test möglich.")
            }
            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                @Suppress("DEPRECATION") val bytes = characteristic.value ?: return
                received(bytes)
            }
            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) { received(value) }
        })
    }

    private fun received(bytes: ByteArray) = synchronized(lock) {
        response.append(bytes.toString(Charsets.US_ASCII)); lock.notifyAll()
    }

    @SuppressLint("MissingPermission")
    fun command(command: String, timeoutMs: Long = 3500): String {
        require(command.matches(Regex("AT[A-Z0-9@?]{1,20}")) || (command.length in 2..128 && command.length % 2 == 0 && command.matches(Regex("[0-9A-F]+")))) { "Ungültiger BLE-ELM-Befehl" }
        val active = gatt ?: return "BLE nicht verbunden"
        val characteristic = writeChar ?: return "Keine UART-Schreibcharakteristik"
        synchronized(lock) { response.clear() }
        val data = "$command\r".toByteArray(Charsets.US_ASCII)
        val submitted = if (Build.VERSION.SDK_INT >= 33) {
            active.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION") characteristic.value = data
            @Suppress("DEPRECATION") active.writeCharacteristic(characteristic)
        }
        if (!submitted) return "BLE-Schreibanforderung abgewiesen"
        val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(500, 15000)
        synchronized(lock) {
            while (!response.contains('>') && System.currentTimeMillis() < deadline) {
                lock.wait(minOf(120, deadline - System.currentTimeMillis()).coerceAtLeast(1))
            }
            val text = response.toString().ifEmpty { "TIMEOUT" }
            log("BLE ELM $command: $text")
            return text
        }
    }

    /** Read or write a configured UDS service through BLE ELM ASCII when UART is ELM-compatible. */
    fun udsRequest(header: String, payload: String, bitrate: Int, timeoutMs: Long): String {
        val normalizedHeader = header.trim().uppercase()
        require(normalizedHeader.matches(Regex("[0-9A-F]{3}|[0-9A-F]{8}"))) { "Ungültige CAN-Anfrage-ID" }
        val normalizedPayload = payload.replace(" ", "").uppercase()
        require(normalizedPayload.length in 2..128 && normalizedPayload.length % 2 == 0 && normalizedPayload.matches(Regex("[0-9A-F]+"))) { "Ungültige UDS-Nutzdaten" }
        val protocol = when { normalizedHeader.length == 8 && bitrate == 500 -> "ATSP7"; normalizedHeader.length == 8 -> "ATSP9"; bitrate == 500 -> "ATSP6"; else -> "ATSP8" }
        for (setup in listOf("ATE0", "ATL0", "ATS0", "ATH1", protocol, "ATCAF1", "ATSH$normalizedHeader")) {
            val result = command(setup, timeoutMs)
            if (result.contains("?") || result == "TIMEOUT") return "Adapter-Setup $setup fehlgeschlagen: $result"
        }
        return command(normalizedPayload, timeoutMs)
    }

    @SuppressLint("MissingPermission")
    fun close() { try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}; gatt = null; writeChar = null; ready = false }
}
