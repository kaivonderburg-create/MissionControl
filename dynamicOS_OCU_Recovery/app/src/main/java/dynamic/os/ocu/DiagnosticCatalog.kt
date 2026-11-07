package dynamic.os.ocu

data class DiagnosticOperation(val title: String, val service: String, val purpose: String, val risk: Risk, val availability: String)
enum class Risk { READ_ONLY, STATE_CHANGE, SECURITY_CRITICAL }

/** Catalog, not a list of commands guaranteed to be supported by this OCU. */
object DiagnosticCatalog {
    val operations = listOf(
        DiagnosticOperation("Steuergeräteidentifikation", "UDS 0x22", "Teilenummer, HW/SW, ASAM und Seriennummer ermitteln", Risk.READ_ONLY, "DID-Liste fahrzeugspezifisch"),
        DiagnosticOperation("Fehlerspeicher", "UDS 0x19", "DTC, Statusbytes, Häufigkeit und Freeze-Frame auswerten", Risk.READ_ONLY, "DTC-Subfunktionen fahrzeugspezifisch"),
        DiagnosticOperation("Messwerte", "UDS 0x22", "Batterie, Spannungen, GPS, Mobilfunk, Antennen und SOS-Status", Risk.READ_ONLY, "DIDs erst identifizieren"),
        DiagnosticOperation("Diagnosesitzung", "UDS 0x10", "Aktive Sitzung abfragen/Wechsel vorbereiten", Risk.STATE_CHANGE, "ECU-Autorisierung erforderlich"),
        DiagnosticOperation("Steuergerät-Neustart", "UDS 0x11", "Geprüften Soft-Reset durchführen", Risk.STATE_CHANGE, "Nicht für diese OCU verifiziert"),
        DiagnosticOperation("Fehler löschen", "UDS 0x14", "Nach Reparatur DTCs löschen und erneut lesen", Risk.STATE_CHANGE, "Nur nach Vorher-Protokoll"),
        DiagnosticOperation("Batteriehistorie", "UDS 0x31", "Hersteller-Grundeinstellung prüfen", Risk.STATE_CHANGE, "Routine-ID für 2GA nicht verifiziert"),
        DiagnosticOperation("OCU-Inbetriebnahme", "ODIS-Prüfplan", "Initialisierung und ggf. Backend-Synchronisation", Risk.SECURITY_CRITICAL, "Nicht lokal implementiert"),
        DiagnosticOperation("SFD2-Freigabe", "VW-Backend", "Offiziell autorisierte Anpassung", Risk.SECURITY_CRITICAL, "Signierte serverseitige Berechtigung erforderlich"),
        DiagnosticOperation("Firmware-Update", "OEM-Flash", "Versionsprüfung und signiertes Update", Risk.SECURITY_CRITICAL, "Nicht implementiert")
    )
}
