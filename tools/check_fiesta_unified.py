#!/usr/bin/env python3
"""Fail CI if unified SYNC build drifts from its real source / wiring."""
from pathlib import Path
import re

root = Path("receiver/app/src/main")
launcher = root / "java/de/vd3/fiesta/LauncherActivity.java"
manifest = root / "AndroidManifest.xml"
projection = root / "java/com/andrerinas/openheadunit/aap/AapProjectionActivity.kt"
service = root / "java/de/vd3/fiesta/MediaAccessService.java"
assert all(p.is_file() for p in (launcher, manifest, projection, service)), "Missing merged files"
ui = launcher.read_text()
xml = manifest.read_text()
aa = projection.read_text()
assert '{"Start","Audio","Telefon","Navigation","System"}' in ui, "Main tabs out of sync"
assert 'else if(page==1)music();' in ui and 'else if(page==3)navigation();' in ui, "Audio/navigation routes incorrect"
assert 'else if(page==4)system();' in ui, "System tab opens wrong view"
assert 'else vehicle();' in ui, "Vehicle advanced diagnostics not reachable"
assert 'getActiveSessions(' in ui and 'getTransportControls()' in ui, "Media session API missing"
assert 'MediaAccessService' in xml and 'BIND_NOTIFICATION_LISTENER_SERVICE' in xml, "Media permission component missing"
assert 'LauncherActivity' in xml, "Unified HOME target missing"
assert 'installFiestaDock()' in aa, "Native AA interface not wired"
assert 'applyFiestaSplitLayout()' in aa, "Projection touch boundary integration missing"
assert 'getInt("viewmode",0)' in aa, "Android Auto not full-screen by default"
assert 'if(split)(w*.73f)' in aa and 'touchOverlayView' in aa, "Touch boundary missing"
assert 'MediaAccessService.java' not in xml, "Manifest service name malformed"
print("Unified SYNC static integrity checks passed")
