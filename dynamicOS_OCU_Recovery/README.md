# dynamicOS OCU Recovery — Android-Projekt 0.1

## Ziel
Diagnose- und Recovery-Konsole speziell für VW T-Cross R-Line 2025, Telematik J949 / Adresse 75, LG 2GA 035 284 A, H11, SW 0619, Fehler B200049.

## Aktueller Funktionsumfang
- Native Android-Oberfläche mit Steuergeräteprofil, Diagnoseoperationskatalog, Risiken, Recovery-Prüfplan.
- Bluetooth Classic SPP-Adaptertest mit gekoppelten Geräten und adapterlokalem `ATI`-Identifikationsbefehl.
- Getrennte Beschreibung der UDS-Dienste 0x10, 0x11, 0x14, 0x19, 0x22, 0x31 und OEM/SFD2-Aufgaben.
- Keine simulierten erfolgreichen Reparaturen und keine unvalidierten Schreibbefehle.

**Stand:** Native App mit echtem Bluetooth-Classic-SPP-Verbindungstest. **Noch kein ECU-Zugriff implementiert.** Ein Zugriff auf den Carista EVO über BLE/GATT oder SPP ist nicht nachgewiesen. Der SPP-Test kann daher fehlschlagen. Diese Einschränkung ist bewusst sichtbar in der UI.

## Zum Build
In Android Studio (aktuelles Android-Gradle-Plugin 8.9.x, JDK 17, Android SDK 35) das Projekt öffnen und Gradle synchronisieren. `app` als Debug APK bauen. In der Ausführungsumgebung war kein Android SDK/Gradle installiert, daher wurde hier **kein APK-Build verifiziert**.

## Nächste Implementierungsstufe
1. EVO-Bluetooth-Service und benötigte UUIDs/Transportform autorisiert erfassen.
2. Fahrzeugseitige Diagnoseadressierung und funktionierende Leseabfrage protokollieren.
3. Echte lesende UDS-Daten und Exporte hinzufügen.
4. Für schreibende Operationen herstellerspezifische Unterstützungs- und Vorbedingungen ermitteln.
5. Bei SFD2-Aktionen offizielle Autorisierung einbinden, nicht durch lokale Berechtigungsänderungen ersetzen.

## Sicherheit
Die SOS-Funktion ist sicherheitskritisch. B200049 kann reale Hardwarefehler anzeigen. Ein gelöschter DTC beweist keine wiederhergestellte Notruffunktion. Das Projekt implementiert bewusst keinen Brute-Force für SecurityAccess, keine SFD2-Umgehung, keine ungeprüften ECU-Reset-/Flash-Routinen.

## Herkunft
Eigenständiger Quellcode. Keine kopierten proprietären Carista-Bibliotheken oder Firmwarebestandteile.
