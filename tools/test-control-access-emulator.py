"""Optional media disclosure and aerial catalog discovery, without grants or downloads."""
import argparse,json,re,time
from pathlib import Path
import emulator_ui as u

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True)
p.add_argument('--height',type=int,choices=[720,1080],required=True)
p.add_argument('--expected-sha256',required=True)
p.add_argument('--output',type=Path,required=True)
p.add_argument('--aerials',action='store_true')
a=p.parse_args();u.configure(a.serial,a.output)
installed=u.call('shell','pm','path','pe.tclhome.prototype').strip().removeprefix('package:')
assert installed.startswith('/data/app/') and re.fullmatch(r'[A-Za-z0-9/._+~=-]+',installed)
assert u.call('shell','sha256sum',installed).split()[0]==a.expected_sha256
listeners=u.call('shell','settings','get','secure','enabled_notification_listeners')
assert 'pe.tclhome.prototype' not in listeners
size=u.call('shell','wm','size');density=u.call('shell','wm','density')
results=[]
try:
    u.call('shell','wm','size',f'{a.height*16//9}x{a.height}');u.call('shell','wm','density','320' if a.height==1080 else '213');time.sleep(1)
    u.call('shell','am','force-stop','pe.tclhome.prototype')
    time.sleep(1)  # Let Android finish resuming the previous Home task before launch.
    u.call('shell','am','start','-W','-a','com.gathin.tvhome.action.LAUNCHER_SETTINGS','-n','pe.tclhome.prototype/com.gathin.tvhome.MainActivity');time.sleep(1)
    u.press('Sistema');u.press('Reproducción multimedia');nodes,f=u.dump('media-settings');u.shot('media-settings')
    assert f is not None and u.label(f).startswith('Acceso multimedia. Acceso opcional')
    u.keys('DPAD_CENTER');nodes,f=u.dump('access-disclosure');u.shot('access-disclosure')
    assert any('permiso es amplio' in n.get('text','') for n in nodes)
    u.keys('BACK');assert u.label(u.dump('disclosure-cancelled')[1]).startswith('Acceso multimedia')
    u.keys('BACK');assert u.label(u.dump('system-focus-restored')[1]).startswith('Reproducción multimedia')
    results.append({'media_disclosure':'PASS','permission_not_granted':True,'semantic_focus_restored':True})
    u.keys('BACK')
    if a.aerials:
        u.press('Fondos');time.sleep(1);u.press('Añadir imágenes o vídeo');u.press('Descargar vídeos aéreos')
        for attempt in range(10):
            nodes,f=u.dump('aerial-loading')
            if any('Descargar' in u.label(n) and n.get('focusable')=='true' for n in nodes):break
            time.sleep(1)
        else:raise AssertionError('Catalog did not expose downloadable rows')
        nodes,f=u.dump('aerial-catalog');u.shot('aerial-catalog')
        assert f is not None and f.get('class')=='android.widget.TextView'
        before=u.label(f);u.keys('DPAD_DOWN','DPAD_DOWN');nodes,f=u.dump('aerial-catalog-down');u.shot('aerial-catalog-down')
        assert u.label(f)!=before,'Catalog D-pad did not move'
        u.keys('BACK');u.dump('aerial-return');u.shot('aerial-return')
        results.append({'catalog_access':'PASS','D-pad_rows':'PASS','no_video_download_started':True})
    assert u.call('shell','settings','get','secure','enabled_notification_listeners')==listeners
    (u.OUT/'results.json').write_text(json.dumps({'height':a.height,'sha256':a.expected_sha256,'checks':results},indent=2),encoding='utf-8')
    print(json.dumps(results),flush=True)
finally:
    for command,original in [('size',size),('density',density)]:
        override=re.search(r'Override (?:size|density):\s*(\S+)',original)
        u.call('shell','wm',command,override[1] if override else 'reset')
