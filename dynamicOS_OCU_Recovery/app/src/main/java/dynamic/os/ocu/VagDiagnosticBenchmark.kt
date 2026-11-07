package dynamic.os.ocu

import android.bluetooth.BluetoothDevice

/** Read-only VAG UDS verification run against an explicitly configured ECU CAN address. */
class VagDiagnosticBenchmark(private val adapter: ClassicAdapterProbe) {
    data class Step(val name: String, val request: String)
    private val steps = listOf(
        Step("Standardsitzung", "1001"),
        Step("Fahrzeugidentifikation", "22F190"),
        Step("Ersatzteilnummer", "22F187"),
        Step("Softwarestand", "22F188"),
        Step("ECU-Seriennummer", "22F18C"),
        Step("DTC-Status", "1901FF"),
        Step("DTC-Datensätze", "1902FF")
    )

    fun run(device: BluetoothDevice, requestId: String, bitrate: Int, timeoutMs: Long, output: (String) -> Unit): String {
        val report = StringBuilder("dynamicOS VAG – ECU-Protokolltest\nCAN-ID $requestId / $bitrate kbit/s\n")
        try {
            report.append(adapter.connect(device)).append("\n")
            adapter.configureUds(requestId, timeoutMs, bitrate)
            for ((index, step) in steps.withIndex()) {
                val start = System.nanoTime()
                val response = try { adapter.sendUdsPayload(step.request, timeoutMs) }
                    catch (e: Exception) { "FEHLER: ${e.message}" }
                val millis = (System.nanoTime() - start) / 1_000_000
                val state = classify(step.request, response)
                val line = "${index + 1}/${steps.size} ${step.name} [${step.request}] – $state (${millis}ms)\n$response\n"
                report.append(line).append("\n")
                output(line)
            }
        } catch (e: Exception) {
            val message = "Test abgebrochen: ${e.javaClass.simpleName}: ${e.message}"
            report.append(message); output(message)
        } finally { adapter.close() }
        return report.toString()
    }

    private fun classify(request: String, answer: String): String {
        val normalized = answer.uppercase().replace(Regex("[^0-9A-F]"), "")
        if (answer.contains("TIMEOUT") || answer.contains("NO DATA", true)) return "KEINE ANTWORT"
        if (answer.contains("CAN ERROR", true) || answer.startsWith("FEHLER")) return "TRANSPORTFEHLER"
        val expected = when (request.take(2)) { "10" -> "50"; "22" -> "62"; "19" -> "59"; else -> "" }
        if (normalized.contains("7F${request.take(2)}33")) return "SICHERHEITSZUGRIFF VERWEIGERT"
        if (normalized.contains("7F${request.take(2)}31")) return "ANFRAGE NICHT UNTERSTÜTZT"
        if (normalized.contains("7F${request.take(2)}")) return "NEGATIVE UDS-ANTWORT"
        if (expected.isNotBlank() && normalized.contains(expected + request.drop(2).take(if (request.startsWith("22")) 4 else 2))) return "POSITIV"
        return "UNKLAR – ROHDATEN PRÜFEN"
    }
}
