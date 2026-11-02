"""Own fixture only: wallpaper changes, foreground Settings key and warm video return. No grants/HOME."""
import argparse,json,re,subprocess,time,statistics
from pathlib import Path
import emulator_ui as u

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True)
p.add_argument('--output',type=Path,required=True)
p.add_argument('--expected-sha256',required=True)
a=p.parse_args();u.configure(a.serial,a.output)
installed=u.call('shell','pm','path','pe.tclhome.prototype').strip().removeprefix('package:')
assert installed.startswith('/data/app/') and re.fullmatch(r'[A-Za-z0-9/._+~=-]+',installed)
assert u.call('shell','sha256sum',installed).split()[0]==a.expected_sha256
results=[]
def top(package,timeout=15):
    deadline=time.monotonic()+timeout
    while time.monotonic()<deadline:
        raw=u.call('shell','dumpsys','activity','activities')
        if any(package in line for line in raw.splitlines() if 'topResumedActivity=' in line):return raw
        time.sleep(.3)
    (u.OUT/'failed-activity.txt').write_text(raw,encoding='utf-8');u.shot('failed-screen')
    raise AssertionError('Not foreground: '+package)
def record(name,**values):
    results.append(dict(test=name,**values));(u.OUT/'results.json').write_text(json.dumps(results,indent=2,ensure_ascii=False),encoding='utf-8');print(name,values,flush=True)
def home():
    u.call('shell','am','start','-W','-a','android.intent.action.MAIN','-c','android.intent.category.LEANBACK_LAUNCHER','-n','pe.tclhome.prototype/com.gathin.tvhome.MainActivity');top('pe.tclhome.prototype');time.sleep(1)
def dock(name):
    nodes,focus=u.dump(name)
    cards=[n for n in nodes if n.get('resource-id','').endswith('/card')]
    assert cards,'No dock'
    # A focused tile scales/lifts; the row itself must remain anchored.
    return statistics.median(u.rect(n)[1] for n in cards[:6])

rec=None
try:
    home();u.keys('BACK',*(['DPAD_LEFT']*7));original=dock('photo-before');u.shot('photo-before')
    # Only keys delivered to the foreground Activity are within this test's scope.
    for code in ['SETTINGS','NOTIFICATION']:
        u.keys(code);nodes,focus=u.dump('remote-'+code.lower())
        delivered=focus is not None and u.label(focus).startswith('Televisor.')
        record('foreground-key-'+code,control_center_open=delivered)
        u.keys('BACK')
        if not delivered:home()
    u.launch_settings();u.thumb('Noche');u.press('Fijo');u.press('Usar este fondo');u.keys('BACK','BACK');top('pe.tclhome.prototype')
    for index,delay in enumerate([0,.2,1]):
        time.sleep(delay);assert dock('photo-return-'+str(index))==original,'Dock moved after wallpaper change';u.shot('photo-return-'+str(index))
    record('photo-change',dock_bounds_unchanged=True)
    # Test the actual existing emulator-owned six-second video through the system picker.
    u.launch_settings();u.press('Añadir imágenes');u.press('Importar un vídeo');u.keys('TAB')
    nodes,_=u.dump('video-picker');items=[n for n in nodes if '00:06' in u.label(n)]
    assert items,'The emulator-owned 6 s video fixture is required'
    assert 'photopicker' in u.call('shell','dumpsys','activity','activities').lower()
    x1,y1,x2,y2=u.rect(items[0]);u.call('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(2)
    u.press('Usar este fondo');u.keys('BACK','BACK');top('pe.tclhome.prototype');time.sleep(2)
    assert dock('video-applied')==original;u.shot('video-applied')
    u.keys('BACK',*(['DPAD_LEFT']*7),'DPAD_RIGHT');assert u.label(u.dump('chosen')[1])=='Archivos'
    rec=subprocess.Popen(u.TARGET+['shell','screenrecord','--size','1280x720','--time-limit','20','--bit-rate','4000000','/sdcard/tcl-continuity-08.mp4'],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    time.sleep(1)
    for cycle in range(2):
        # Prior suites may have explicitly opened another instance of the fixture. Isolate
        # this task so Back tests the launcher return, rather than popping a second fixture.
        u.call('shell','am','force-stop','pe.tclhome.fixture.files')
        u.keys('DPAD_CENTER',wait=0);top('pe.tclhome.fixture.files');time.sleep(.3)
        away=u.call('shell','dumpsys','media.player');(u.OUT/f'player-away-{cycle}.txt').write_text(away,encoding='utf-8')
        assert 'packageName: pe.tclhome.prototype' not in away,'Our decoder stayed active outside Home'
        u.keys('BACK',wait=0);top('pe.tclhome.prototype');time.sleep(.1);u.shot(f'video-return-early-{cycle}');time.sleep(1)
        live=u.call('shell','dumpsys','media.player');(u.OUT/f'player-return-{cycle}.txt').write_text(live,encoding='utf-8')
        assert 'packageName: pe.tclhome.prototype' in live,'Video did not resume'
        assert u.label(u.dump(f'focus-return-{cycle}')[1])=='Archivos','App focus lost'
        record('video-cycle-'+str(cycle),decoder_released_away=True,resumed=True,focus_retained=True)
    rec.wait(timeout=25);assert rec.returncode==0
    u.call('pull','/sdcard/tcl-continuity-08.mp4',str(u.OUT/'transition.mp4'))
    record('recording',path=str(u.OUT/'transition.mp4'),apk_sha256=a.expected_sha256,not_a_performance_benchmark=True)
finally:
    if rec is not None and rec.poll() is None:rec.wait(timeout=25)
    # This suite is for the disposable fixture AVD. Restore its six defaults, never originals on a TV.
    home();u.launch_settings();u.press('Colección ·');u.press('Restablecer las seis');u.press('Confirmar');u.thumb('Día');u.press('Fijo');u.press('Usar este fondo');u.keys('BACK','BACK')
