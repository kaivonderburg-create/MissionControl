#!/usr/bin/env python3
"""Fail CI if unified SYNC build drifts from its real source / wiring."""
from pathlib import Path
import re

root = Path("receiver/app/src/main")
launcher = root / "java/de/vd3/fiesta/LauncherActivity.java"
manifest = root / "AndroidManifest.xml"
projection = root / "java/com/andrerinas/openheadunit/aap/AapProjectionActivity.kt"
service = root / "java/de/vd3/fiesta/MediaAccessService.java"
hardware = root / "java/de/vd3/fiesta/MediaHardwareActivity.java"
assert all(p.is_file() for p in (launcher, manifest, projection, service, hardware)), "Missing merged files"
ui = launcher.read_text()
xml = manifest.read_text()
aa = projection.read_text()
hw = hardware.read_text()
assert '{"Start","Audio","Telefon","Navigation","System"}' in ui, "Main tabs out of sync"
assert 'else if(page==1)music();' in ui and 'else if(page==3)navigation();' in ui, "Audio/navigation routes incorrect"
assert 'else if(page==4)system();' in ui, "System tab opens wrong view"
assert 'else vehicle();' in ui, "Vehicle advanced diagnostics not reachable"
assert 'getActiveSessions(' in ui and 'getTransportControls()' in ui, "Media session API missing"
assert 'MediaAccessService' in xml and 'BIND_NOTIFICATION_LISTENER_SERVICE' in xml, "Media permission component missing"
assert 'LauncherActivity' in xml, "Unified HOME target missing"
assert 'installFiestaDock()' in aa, "Native AA interface not wired"
assert 'installFiestaDock()' in aa and 'Zur SYNC-Startseite' in aa, "Compact SYNC home affordance missing"
assert 'applyFiestaSplitLayout()' not in aa, "Legacy AA split overlay must not intercept touch"
assert 'showFiestaMedia()' not in aa, "Legacy modal media dialog still installed"
assert 'MediaAccessService.java' not in xml, "Manifest service name malformed"
print("Unified SYNC static integrity checks passed")
