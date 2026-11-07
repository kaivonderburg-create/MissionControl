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
                val writable = service?.getCharacteristic(nusRx)
                val notifier = service?.getCharacteristic(nusTx)
                if (writable == null || notifier == null) {
                    val uuids = g.services.joinToString("; ") { it.uuid.toString() }
                    onReady("Kein Nordic-UART-Profil (NUS) gefunden. GATT: $uuids")
                    return
                }
                writeChar = writable
                g.setCharacteristicNotification(notifier, true)
                notifier.getDescriptor(cccd)?.let { descriptor ->
                    if (Build.VERSION.SDK_INT >= 33) g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    else { @Suppress("DEPRECATION") descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; @Suppress("DEPRECATION") g.writeDescriptor(descriptor) }
                }
                synchronized(lock) { ready = true; lock.notifyAll() }
                onReady("BLE-NUS UART verbunden. ELM-Befehle möglich.")
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
        require(command in setOf("ATI", "AT@1", "ATRV", "ATDP", "ATDPN", "0100", "0902")) { "Nicht freigegebener BLE-Befehl" }
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

    @SuppressLint("MissingPermission")
    fun close() { try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}; gatt = null; writeChar = null; ready = false }
}
