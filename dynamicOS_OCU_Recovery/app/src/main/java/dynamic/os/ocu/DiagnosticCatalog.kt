package dynamic.os.ocu

data class DiagnosticOperation(val title: String, val service: String, val purpose: String, val risk: Risk, val availability: String)
enum class Risk { READ_ONLY, STATE_CHANGE, SECURITY_CRITICAL }

/** Catalog, not a list of commands guaranteed to be supported by this OCU. */
object DiagnosticCatalog {
    val operations = listOf(
        DiagnosticOperation("Steuergeräteidentifikation", "UDS 0x22", "Teilenummer, HW/SW, ASAM und Seriennummer ermitteln", Risk.READ_ONLY, "DID-Liste fahrzeugspezifisch"),
        DiagnosticOperation("Fehlerspeicher", "UDS 0x19", "DTC, Statusbytes, Häufigkeit und Freeze-Frame auswerten", Risk.READ_ONLY, "DTC-Subfunktionen fahrzeugspezifisch"),
        DiagnosticOperation("Messwerte", "UDS 0x22", "Batterie, Spannungen, GPS, Mobilfunk, Antennen und SOS-Status", Risk.READ_ONLY, "DIDs erst identifizieren"),
        DiagnosticOperation("Bordnetz und Klemme 30", "UDS 0x22", "Spannung, Wake/Sleep und Reset-Ursachen protokollieren", Risk.READ_ONLY, "Messwertkennung aus ECU-Datensatz erforderlich"),
        DiagnosticOperation("Notrufbatterie", "UDS 0x22", "Ladestand, SoH, Temperatur, Historie und Notstromstatus", Risk.READ_ONLY, "Messwertkennung aus ECU-Datensatz erforderlich"),
        DiagnosticOperation("GNSS & Position", "UDS 0x22", "Antennenstatus, GNSS-Fix und Empfangsqualität", Risk.READ_ONLY, "Messwertkennung aus ECU-Datensatz erforderlich"),
        DiagnosticOperation("LTE/eSIM & Netz", "UDS 0x22", "Mobilfunkregistrierung, Signal, Modemstatus und Backend-Konnektivität", Risk.READ_ONLY, "Messwertkennung aus ECU-Datensatz erforderlich"),
        DiagnosticOperation("Notruf-Bereitschaft", "UDS 0x22", "SOS-LED, Aktivierung, Notrufverfügbarkeit und Selbsttests", Risk.READ_ONLY, "Messwertkennung aus ECU-Datensatz erforderlich"),
        DiagnosticOperation("Gatewaykommunikation", "UDS 0x19 / 0x22", "Kommunikationsstörungen zur OCU und Bus-Status", Risk.READ_ONLY, "Diagnoseadresse 19 zusätzlich erforderlich"),
        DiagnosticOperation("Fehler-Snapshot", "UDS 0x19", "Status, Zeitstempel, Häufigkeit, Priorität und Umgebung", Risk.READ_ONLY, "UDS-Subfunktionen fahrzeugspezifisch"),
        DiagnosticOperation("Diagnosesitzung", "UDS 0x10", "Aktive Sitzung abfragen/Wechsel vorbereiten", Risk.STATE_CHANGE, "ECU-Autorisierung erforderlich"),
        DiagnosticOperation("Sicherheitszugang", "UDS 0x27 / 0x29", "Vorbedingungen und Autorisierungsstatus auswerten", Risk.SECURITY_CRITICAL, "Keine Brute-Force-/Bypass-Implementierung"),
        DiagnosticOperation("Steuergerät-Neustart", "UDS 0x11", "Geprüften Soft-Reset durchführen", Risk.STATE_CHANGE, "Nicht für diese OCU verifiziert"),
        DiagnosticOperation("Fehler löschen", "UDS 0x14", "Nach Reparatur DTCs löschen und erneut lesen", Risk.STATE_CHANGE, "Nur nach Vorher-Protokoll"),
        DiagnosticOperation("Batteriehistorie", "UDS 0x31", "Hersteller-Grundeinstellung prüfen", Risk.STATE_CHANGE, "Routine-ID für 2GA nicht verifiziert"),
        DiagnosticOperation("Routineninventar", "UDS 0x31", "Herstellerseitig unterstützte Grundeinstellungen dokumentieren", Risk.STATE_CHANGE, "Keine pauschale Routine-Enumeration erlaubt"),
        DiagnosticOperation("Coding & Anpassung", "UDS 0x2E / OEM", "Parameter nach Freigabe dokumentieren und sichern", Risk.SECURITY_CRITICAL, "Nur bei bestätigter SFD2-Freigabe"),
        DiagnosticOperation("OCU-Inbetriebnahme", "ODIS-Prüfplan", "Initialisierung und ggf. Backend-Synchronisation", Risk.SECURITY_CRITICAL, "Nicht lokal implementiert"),
        DiagnosticOperation("Backend-Registrierung", "VW Service", "Fahrzeugbindung, Dienstaktivierung und Synchronisation", Risk.SECURITY_CRITICAL, "Nur offizieller Servicepfad"),
        DiagnosticOperation("SFD2-Freigabe", "VW-Backend", "Offiziell autorisierte Anpassung", Risk.SECURITY_CRITICAL, "Signierte serverseitige Berechtigung erforderlich"),
        DiagnosticOperation("Firmware-Update", "OEM-Flash", "Versionsprüfung und signiertes Update", Risk.SECURITY_CRITICAL, "Nicht implementiert")
    )
}
