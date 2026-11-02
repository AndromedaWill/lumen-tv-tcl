"""Functional hierarchy/material checks. Explicit emulator only; never grants access or changes HOME."""
import argparse, json, re, time
from pathlib import Path
import emulator_ui as u

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial', required=True)
p.add_argument('--height', type=int, choices=[720, 1080], required=True)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
u.configure(a.serial, a.output)
size, density = u.call('shell', 'wm', 'size'), u.call('shell', 'wm', 'density')
results = []
installed = u.call('shell', 'pm', 'path', 'pe.tclhome.prototype').strip().removeprefix('package:')
assert installed.startswith('/data/app/') and re.fullmatch(r'[A-Za-z0-9/._+~=-]+', installed)
sha = u.call('shell', 'sha256sum', installed).split()[0]

def expect(name, prefix):
    nodes, focus = u.dump(name)
    label = '' if focus is None else u.label(focus)
    ok = label.startswith(prefix)
    results.append({'scenario': name, 'passed': ok, 'focus': label})
    print(name, 'PASS' if ok else 'FAIL', flush=True)
    assert ok, (name, prefix, label)
    return nodes

def capture(name):
    assert any('pe.tclhome.prototype' in s for s in u.call('shell', 'dumpsys', 'activity', 'activities').splitlines() if 'topResumedActivity=' in s)
    u.shot(name)

def restore(command, original):
    found = re.search(r'Override (?:size|density):\s*(\S+)', original)
    u.call('shell', 'wm', command, found[1] if found else 'reset')

try:
    u.call('shell', 'wm', 'size', '1920x1080' if a.height == 1080 else '1280x720')
    u.call('shell', 'wm', 'density', '320' if a.height == 1080 else '213'); time.sleep(1)
    u.call('shell', 'am', 'start', '-W', '-a', 'com.gathin.tvhome.action.LAUNCHER_SETTINGS', '-n', 'pe.tclhome.prototype/com.gathin.tvhome.MainActivity'); time.sleep(1)
    expect('settings-root', 'Inicio'); capture('settings-root')
    u.keys('DPAD_UP', 'DPAD_LEFT', 'DPAD_RIGHT'); expect('root-edges', 'Inicio')
    u.keys('DPAD_CENTER'); expect('settings-home', 'Aplicaciones por fila. 6'); capture('settings-home')
    u.keys('DPAD_CENTER'); expect('settings-columns', '6 aplicaciones'); capture('settings-columns')
    # A real configuration change reconstructs this Activity; the page depth and focus must survive.
    target_density = '321' if a.height == 1080 else '214'
    u.call('shell', 'wm', 'density', target_density); time.sleep(2)
    expect('recreated-choice', '6 aplicaciones')
    u.call('shell', 'wm', 'density', '320' if a.height == 1080 else '213'); time.sleep(2)
    expect('recreated-choice-restored', '6 aplicaciones')
    u.keys('DPAD_DOWN'); expect('choice-focus-independent', '7 aplicaciones'); capture('settings-columns-resting')
    u.keys('BACK'); expect('choice-back-parent', 'Aplicaciones por fila. 6')
    for value in [4, 5, 7, 6]:
        u.keys('DPAD_CENTER'); u.press(str(value) + ' aplicaciones')
        expect('columns-saved-' + str(value), 'Aplicaciones por fila. ' + str(value))
    u.press('Filas debajo del dock'); expect('settings-rows', 'Ninguna'); capture('settings-rows')
    u.press('1 fila'); expect('rows-saved', 'Filas debajo del dock. 1')
    u.keys('DPAD_CENTER'); u.press('Ninguna'); expect('rows-restored', 'Filas debajo del dock. 0')
    u.keys('BACK'); expect('home-parent-focus', 'Inicio')
    u.press('Apariencia'); expect('settings-appearance', 'Animaciones. Sí'); capture('settings-appearance')
    u.keys('DPAD_CENTER'); nodes = expect('animations-disabled', 'Animaciones. No')
    advanced = [n for n in nodes if n.get('content-desc', '').startswith('Efectos avanzados')]
    assert advanced and all(n.get('enabled') == 'false' and n.get('focusable') == 'false' for n in advanced)
    capture('settings-appearance-disabled')
    u.keys('DPAD_DOWN'); expect('disabled-row-skipped', 'Seguir el tema')
    u.keys('DPAD_UP', 'DPAD_CENTER'); expect('animations-restored', 'Animaciones. Sí')
    u.press('Efectos avanzados'); capture('advanced-confirmation'); u.keys('BACK')
    expect('advanced-cancelled', 'Efectos avanzados. No')
    u.press('Seguir el tema'); capture('theme-confirmation'); u.keys('BACK')
    expect('theme-cancelled', 'Seguir el tema')
    u.keys('BACK'); expect('appearance-parent-focus', 'Apariencia')
    u.press('Fondos'); time.sleep(1); u.press('Editar este fondo'); capture('background-actions'); u.dump('background-actions')
    u.keys('BACK'); u.shot('background-restored'); u.keys('BACK')
    expect('gallery-parent-focus', 'Fondos. Fijo')
    u.press('Sistema'); expect('settings-system', 'Ajustes'); capture('settings-system')
    u.press('Reproducción multimedia'); expect('settings-media', 'Acceso multimedia. Acceso opcional'); capture('settings-media')
    u.keys('DPAD_CENTER'); capture('media-access-disclosure')
    nodes,_=u.dump('media-access-disclosure')
    assert any('permiso es amplio' in n.get('text','') for n in nodes),'Full permission disclosure required'
    u.keys('BACK'); expect('media-disclosure-cancelled','Acceso multimedia. Acceso opcional')
    u.keys('BACK'); expect('media-parent-focus','Reproducción multimedia')
    u.press('Créditos y licencias'); expect('settings-credits', 'Inter'); capture('settings-credits')
    u.keys('DPAD_DOWN'); expect('credits-focus', 'Simple Icons')
    u.keys('BACK'); expect('credits-parent-focus', 'Créditos y licencias')
    u.focus('Ajustes'); u.keys('DPAD_CENTER'); time.sleep(1)
    top = u.call('shell', 'dumpsys', 'activity', 'activities')
    assert any('com.android.tv.settings' in line for line in top.splitlines() if 'topResumedActivity=' in line)
    u.keys('BACK'); expect('native-parent-focus', 'Ajustes')
    u.keys('BACK'); expect('system-parent-focus', 'Sistema')
    u.keys('BACK'); time.sleep(1); u.press('Ajustes'); u.focus('Sonido')
    nodes, focus = u.dump('control-sound')
    assert not any(n.get('text') == 'Sonido' for n in nodes), 'Unrequested visible sound caption'
    assert focus is not None and u.label(focus) == 'Sonido', 'Accessible icon name must remain'
    capture('control-sound'); u.keys('BACK')
    assert 'HomeNotificationService' not in u.call('shell', 'dumpsys', 'activity', 'services', 'pe.tclhome.prototype')
    (u.OUT / 'capture.json').write_text(json.dumps({'serial': a.serial, 'height': a.height, 'apk_sha256': sha,
        'checks': len(results), 'no_grants': True, 'columns_restored': 6, 'rows_restored': 0, 'advanced_effects': False}, indent=2))
finally:
    (u.OUT / 'results.json').write_text(json.dumps(results, indent=2, ensure_ascii=False), encoding='utf-8')
    restore('size', size); restore('density', density)
