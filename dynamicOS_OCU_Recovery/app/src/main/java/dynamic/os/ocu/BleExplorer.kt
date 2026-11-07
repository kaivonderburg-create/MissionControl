package dynamic.os.ocu

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context

/** BLE inspection only: scan, connect and enumerate GATT UUIDs. No writes. */
class BleExplorer(private val context: Context, private val log: (String) -> Unit) {
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var scanner = manager.adapter?.bluetoothLeScanner
    private var gatt: BluetoothGatt? = null
    private var callback: ScanCallback? = null
    @SuppressLint("MissingPermission")
    fun start(onDevice: (BluetoothDevice, String, Int) -> Unit) {
        stopScan()
        val s = manager.adapter?.bluetoothLeScanner ?: error("Bluetooth LE nicht verfügbar oder Bluetooth deaktiviert")
        scanner = s
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName ?: try { result.device.name } catch (_: SecurityException) { null } ?: "Unbenanntes BLE-Gerät"
                onDevice(result.device, name, result.rssi)
            }
            override fun onScanFailed(errorCode: Int) { log("BLE-Scan fehlgeschlagen: $errorCode") }
        }
        s.startScan(callback)
        log("BLE-Suche gestartet")
    }
    @SuppressLint("MissingPermission")
    fun stopScan() {
        try { callback?.let { scanner?.stopScan(it) } } catch (_: SecurityException) {}
        callback = null
    }
    @SuppressLint("MissingPermission")
    fun inspect(device: BluetoothDevice, result: (String) -> Unit) {
        stopScan(); disconnect()
        log("GATT-Verbindung angefordert (nur Service-Entdeckung)")
        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) { result("GATT-Verbindungsfehler: $status"); log("GATT-Fehler $status"); return }
                if (newState == BluetoothProfile.STATE_CONNECTED) { result("Verbunden. Lese GATT-Dienste …"); g.discoverServices() }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) result("BLE getrennt")
            }
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) { result("Service-Entdeckung fehlgeschlagen: $status"); return }
                val report = buildString {
                    appendLine("GATT-Dienste: ${g.services.size}")
                    g.services.forEach { service ->
                        appendLine("SERVICE ${service.uuid}")
                        service.characteristics.forEach { characteristic ->
                            appendLine("   CHAR ${characteristic.uuid}  properties=0x${characteristic.properties.toString(16)}")
                        }
                    }
                }
                log(report.trim()); result(report)
            }
        })
    }
    @SuppressLint("MissingPermission")
    fun disconnect() { try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}; gatt = null }
    fun close() { stopScan(); disconnect() }
}
