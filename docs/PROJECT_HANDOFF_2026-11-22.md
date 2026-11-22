# dynamicOS / Ford Automotive — Übergabe- und Sicherungsstand
Stand: 2026-11-22 (Europe/Berlin)

## Zweck
Dieser Branch wurde als unveränderte Momentaufnahme von `dynamicOS-fiesta-unified` erzeugt. Die vorliegende Datei ergänzt nur eine Übergabe für neue ChatGPT-Unterhaltungen und zerstört keine bestehenden Quelldateien. Der Branchname ist keine Aussage darüber, ob APK oder Android Auto vollständig funktionieren.

## Plattform
- Fahrzeug: Ford Fiesta Mk7 (2015), ursprüngliches SYNC 1.
- Headunit: QS9008 Android, circa 1 GB RAM; Leistungs- und Speicherverbrauch besonders kritisch.
- Raise-CAN-Decoder und Lenkradtastenintegration als Projektanforderungen.
- Gemeinsames GitHub-Repository: kaivonderburg-create/MissionControl.
- Entwicklungszweig vor der Sicherung: dynamicOS-fiesta-unified.

## Verbindliche Produktziele
- Produktname exakt: **dynamicOS/ Ford Automotive** (nicht „SYNC / dynamicOS“).
- Modernes, hochwertiges, reduziertes Interface mit gestalterischer Orientierung an Ford SYNC, keine generische billig wirkende Oberfläche.
- Sehr einfache, ablenkungsarme Touch-Bedienung, klare Layout-Hierarchie und Ressourcenoptimierung für 1 GB RAM.
- Vollständig integriertes Android Auto als Bestandteil derselben eigenständigen Anwendung, nicht offensichtlich als separate APK.
- Kabelloses Android Auto mit Bluetooth-Pairing, WLAN/Hotspot und Kompatibilitätsbehandlung, soweit die konkrete Hardware und technische Protokollkompatibilität das erlauben.
- Medienbereich auf der Hauptseite mit Album-Cover und Umschaltung zwischen Medienkachel und miniaturisierter Android-Auto-Ansicht.
- Launcher als auswählbarer Standard-Launcher, damit nach Einrichtung die neue Oberfläche angezeigt wird.
- Deutschsprachige Bedienung; Fahrzeug- und CAN-Einstellungen einschließlich Lenkradtasten.
- Vor einer „fertig“-Meldung: tatsächliche Builds, Startfähigkeit, Touchbedienung insbesondere während laufendem Android Auto, Navigations- und Ressourcenprüfung verifizieren.

## Zuletzt vom Nutzer gemeldete Probleme
- Android Auto blieb zu deutlich als getrennte Anwendung sichtbar; fehlende echte Integration.
- Während Android Auto aktiv war, waren Steuerelemente nicht anklickbar.
- UI wirkte nicht ausreichend wie die beabsichtigte moderne Ford-SYNC-Designsprache.
- Abweichungen zwischen Mockup und echter Anwendung, fehlerhafte Kopfzeile und unstimmiges Fahrzeugfeld.
- Fehlende hochwertige Cover- und Android-Auto-Miniaturdarstellung im Medienbereich.
- Der Nutzer möchte keine wiederholten kosmetischen Experimente, sondern eine architektonisch saubere, kompilierbare, überprüfte Lösung.

## Arbeits- und Sicherheitshinweise
1. Vor Änderungen den aktuellen Branch, Commit-SHA und die Dateien verifizieren.
2. Sicherungsbranches niemals als Entwicklungsziel überschreiben.
3. Für Änderungen eigenen Entwicklungsbranch erstellen, nachvollziehbare Commits verwenden.
4. Keine Behauptung über funktionsfähige APK oder getestete Android-Auto-Implementierung ohne entsprechende Prüfergebnisse.
5. In neuen Chats immer diese Datei und den letzten erfolgreichen Commit lesen.
6. Grenzen benennen: Ein Git-Snapshot enthält nur bereits in GitHub committete Dateien; nicht gespeicherte Chat-Texte oder lokale Änderungen werden dadurch nicht gesichert.

## Neue Unterhaltung fortsetzen
„Öffne kaivonderburg-create/MissionControl, lies docs/PROJECT_HANDOFF_2026-11-22.md im Branch backup/fiesta-unified-2026-11-22, prüfe den aktuellen Entwicklungsbranch und den letzten Build. Setze dynamicOS/ Ford Automotive auf Basis des real vorhandenen Codes fort, ohne Backups zu überschreiben.“
