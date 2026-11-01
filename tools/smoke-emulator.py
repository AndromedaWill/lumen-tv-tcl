"""D-pad smoke test and unedited screenshots; no physical-TV targets accepted."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial', required=True)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
if not re.fullmatch(r'emulator-\d+', a.serial):
    p.error('An explicit emulator serial is required')
adb = str(Path(os.environ['LOCALAPPDATA']) / 'Android/Sdk/platform-tools/adb.exe')
package = 'pe.tclhome.prototype'
component = package + '/com.gathin.tvhome.MainActivity'


def run(*cmd):
    return subprocess.run([adb, '-s', a.serial, *cmd], check=True, capture_output=True,
                          text=True, encoding='utf-8', errors='replace').stdout


if run('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
    raise SystemExit('Refusing non-emulator device')
a.output.mkdir(parents=True, exist_ok=True)
results = []


def keys(*codes):
    run('shell', 'input', 'keyevent', *['KEYCODE_' + c for c in codes])
    time.sleep(.8)


def wait_top(expected):
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        active = run('shell', 'dumpsys', 'activity', 'activities')
        if any(expected in line for line in active.splitlines() if 'topResumedActivity=' in line):
            return True
        time.sleep(.5)
    return False


def ui(name):
    for attempt in range(3):
        run('shell', 'uiautomator', 'dump', '/sdcard/tcl-smoke.xml')
        xml = run('shell', 'cat', '/sdcard/tcl-smoke.xml')
        (a.output / (name + '.xml')).write_text(xml, encoding='utf-8')
        root = ET.fromstring(xml)
        focused = [n.attrib for n in root.iter('node') if n.get('focused') == 'true']
        if focused:
            return focused[0]
        time.sleep(1)
    raise AssertionError('No focus: ' + name)


def shot(name):
    run('shell', 'screencap', '-p', '/sdcard/tcl-smoke.png')
    run('pull', '/sdcard/tcl-smoke.png', str(a.output / (name + '.png')))


def record(name, ok, detail):
    results.append({'scenario': name, 'passed': bool(ok), 'detail': detail})
    (a.output / 'results.json').write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding='utf-8')
    print(name, 'PASS' if ok else 'FAIL', detail, flush=True)
    assert ok, name


run('shell', 'am', 'force-stop', 'pe.tclhome.fixture.files')
run('shell', 'am', 'force-stop', package)
time.sleep(.8)
launch = run('shell', 'am', 'start', '-W', '-a', 'android.intent.action.MAIN',
             '-c', 'android.intent.category.LEANBACK_LAUNCHER', '-n', component)
(a.output / 'launch.txt').write_text(launch, encoding='utf-8')
assert wait_top(package), 'Launcher not in foreground at test start'
time.sleep(2)
keys('BACK', *(['DPAD_LEFT'] * 7))
first = ui('dock-first')
keys('DPAD_LEFT')
edge = ui('dock-left-edge')
record('dock-left-edge', first.get('resource-id', '').endswith('/card')
       and first.get('package') == package
       and first.get('content-desc') == edge.get('content-desc'), edge)
shot('home')
keys('DPAD_RIGHT')
chosen = ui('chosen-app')
record('named-focus', bool(chosen.get('content-desc')), chosen)
keys('DPAD_CENTER')
record('open-installed-fixture', wait_top('pe.tclhome.fixture.files'), 'Archivos fixture')
keys('BACK')
record('return-from-fixture', wait_top(package), 'Home resumed within 10 s')
returned = ui('return-from-app')
record('focus-after-back', returned.get('content-desc') == chosen.get('content-desc'), returned)
# Background first, so Android dispatches onPause, then simulate process loss.
run('shell', 'am', 'start', '-W', '-n', 'pe.tclhome.fixture.files/pe.tclhome.fixture.FixtureActivity')
run('shell', 'am', 'force-stop', package)
run('shell', 'am', 'start', '-W', '-a', 'android.intent.action.MAIN',
    '-c', 'android.intent.category.LEANBACK_LAUNCHER', '-n', component)
time.sleep(2)
restored = ui('process-restored')
record('focus-after-process-loss', restored.get('content-desc') == chosen.get('content-desc'), restored)
keys('DPAD_DOWN')
grid = ui('library-row')
record('dpad-down-to-grid', grid.get('resource-id', '').endswith('/card')
       and grid.get('content-desc') != chosen.get('content-desc'), grid)
shot('library')
keys('DPAD_DOWN', 'DPAD_DOWN', 'DPAD_DOWN', 'DPAD_DOWN')
catalog_button = ui('catalog-button')
record('catalog-button-reachable', catalog_button.get('resource-id', '').endswith('/btnLibrary'), catalog_button)
keys('DPAD_CENTER')
ui('catalog-open')
shot('catalog')
keys('BACK')
closed_catalog = ui('catalog-closed')
record('catalog-close-focus', closed_catalog.get('resource-id', '').endswith('/btnLibrary'), closed_catalog)
keys('BACK')
dock = ui('back-to-dock')
record('back-to-dock', dock.get('resource-id', '').endswith('/card'), dock)
keys('DPAD_UP', *(['DPAD_LEFT'] * 6))
top = ui('toolbar-left')
record('toolbar-reachable', top.get('resource-id', '').endswith('/clockBlock'), top)
keys('DPAD_RIGHT', 'DPAD_RIGHT')
gear = ui('settings-button')
record('settings-button', gear.get('resource-id', '').endswith('/btnSettings'), gear)
shot('controls')
keys('DPAD_CENTER')
ui('settings-menu')
shot('settings')
# Every control and edge must be reachable without diagonal jumps.
for label, codes, expected in [
    ('cc-right', ['DPAD_RIGHT'], 'Red.'),
    ('cc-down', ['DPAD_DOWN'], 'Inicio.'),
    ('cc-left', ['DPAD_LEFT'], 'Entradas.'),
    ('cc-bottom', ['DPAD_DOWN', 'DPAD_DOWN'], 'Sonido'),
    ('cc-compact-image', ['DPAD_RIGHT'], 'Imagen'),
    ('cc-compact-bluetooth', ['DPAD_RIGHT'], 'Bluetooth'),
    ('cc-compact-apps', ['DPAD_RIGHT'], 'Aplicaciones'),
    ('cc-right-edge', ['DPAD_RIGHT'], 'Aplicaciones'),
    ('cc-footer-down', ['DPAD_DOWN'], 'Notificaciones desactivadas'),
    ('cc-notifications', ['BACK'], 'Notificaciones.'),
    ('cc-home', ['DPAD_LEFT'], 'Google Home.'),
    ('cc-top', ['DPAD_UP', 'DPAD_UP', 'DPAD_LEFT'], 'Televisor.')]:
    keys(*codes)
    focused = ui(label)
    record(label, (focused.get('content-desc', '') or focused.get('text', '')).startswith(expected), focused)

keys('BACK')
closed = ui('settings-closed')
record('menu-returns-focus', closed.get('resource-id') == gear.get('resource-id'), closed)
# The optional viewer must work without notification access. Never grant/toggle that access here.
access = run('shell', 'settings', 'get', 'secure', 'enabled_notification_listeners')
record('notification-access-default-off', package not in access, 'Own listener is not approved')
keys('DPAD_CENTER')
ready_cc = ui('notification-controls-ready')
record('notification-controls-ready', ready_cc.get('content-desc', '').startswith('Televisor.'), ready_cc)
keys('DPAD_DOWN', 'DPAD_DOWN', 'DPAD_RIGHT')
entry = ui('notification-entry')
record('notification-entry', entry.get('content-desc', '').startswith('Notificaciones.'), entry)
keys('DPAD_CENTER')
configure = ui('notification-off')
record('notification-compact-focus', configure.get('text', '').startswith('Notificaciones desactivadas'), configure)
shot('notification-compact')
keys('DPAD_CENTER')
configure = ui('notification-disclosure')
record('notification-disclosure-focus', configure.get('text','').startswith('Desactivado.'), configure)
off_xml = (a.output / 'notification-disclosure.xml').read_text(encoding='utf-8')
record('notification-no-auto-enable', 'Desactivado.' in off_xml and 'Activar visor' not in off_xml,
       'Permission explanation visible; no active viewer')
shot('notification-off')
keys('DPAD_DOWN')
configure = ui('notification-configure-button')
record('notification-configure-focus', configure.get('text') == 'Configurar acceso en Android', configure)
services = run('shell', 'dumpsys', 'activity', 'services', package)
(a.output / 'services-notifications-off.txt').write_text(services, encoding='utf-8')
record('notification-service-not-running', 'HomeNotificationService' not in services, 'No listener service while access is off')
keys('BACK')
collapsed = ui('notification-collapsed')
record('notification-details-collapse', collapsed.get('text', '').startswith('Notificaciones desactivadas'), collapsed)
keys('BACK')
returned_cc = ui('notification-back')
record('notification-back-to-controls', returned_cc.get('content-desc', '').startswith('Notificaciones.'), returned_cc)
keys('BACK')
# The first menu item opens standard TV settings through the safe resolver.
keys('DPAD_CENTER')
keys('DPAD_CENTER')
active = run('shell', 'dumpsys', 'activity', 'activities')
record('native-settings', wait_top('com.android.tv.settings'), 'Official AOSP TV settings')
keys('BACK')
run('shell', 'am', 'start', '-W', '-n', component)
keys('BACK', *(['DPAD_LEFT'] * 7))
shot('home')
(a.output / 'crashes.txt').write_text(run('logcat', '-d', '-b', 'crash'), encoding='utf-8')
