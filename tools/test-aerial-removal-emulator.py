"""Confirm deletion/cancel with an emulator-owned video, without downloading an Apple video."""
import argparse,json,re,time
from pathlib import Path
import emulator_ui as u
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True);p.add_argument('--expected-sha256',required=True);p.add_argument('--output',type=Path,required=True)
a=p.parse_args();u.configure(a.serial,a.output)
installed=u.call('shell','pm','path','pe.tclhome.prototype').strip().removeprefix('package:')
assert installed.startswith('/data/app/') and re.fullmatch(r'[A-Za-z0-9/._+~=-]+',installed)
assert u.call('shell','sha256sum',installed).split()[0]==a.expected_sha256
folder='/sdcard/Android/data/pe.tclhome.prototype/files/aerials'
name='africa_and_the_middle_east.mov';title='Africa and the Middle East'
fixture='/sdcard/Movies/TCLReview/test-video.mp4'
if name in u.call('shell','ls',folder).split():
    assert u.call('shell','sha256sum',fixture).split()[0]==u.call('shell','sha256sum',folder+'/'+name).split()[0],'Do not touch an existing non-fixture download'
else:u.call('shell','cp',fixture,folder+'/'+name)
u.call('shell','am','force-stop','pe.tclhome.prototype');time.sleep(1)
launch=u.call('shell','am','start','-W','-a','com.gathin.tvhome.action.LAUNCHER_SETTINGS','-n','pe.tclhome.prototype/com.gathin.tvhome.MainActivity')
(u.OUT/'launch.txt').write_text(launch,encoding='utf-8');time.sleep(2)
assert any('LauncherSettingsActivity' in line for line in u.call('shell','dumpsys','activity','activities').splitlines() if 'topResumedActivity=' in line),'Settings must be foreground before D-pad input'
u.press('Fondos');time.sleep(1)
u.press('Añadir imágenes o vídeo');u.press('Descargar vídeos aéreos')
for attempt in range(12):
    nodes,_=u.dump('loading')
    if any(u.label(n).startswith(title+'.') for n in nodes):break
    time.sleep(1)
else:raise AssertionError('Catalog unavailable')
u.focus(title);u.shot('fixture-saved')
u.call('shell','input','keyevent','--longpress','KEYCODE_DPAD_CENTER');time.sleep(.5)
u.dump('delete-confirmation');u.shot('delete-confirmation')
u.keys('BACK');assert name in u.call('shell','ls',folder).split(),'Cancel removed the fixture'
u.call('shell','input','keyevent','--longpress','KEYCODE_DPAD_CENTER');time.sleep(.5)
u.press('Eliminar descarga');time.sleep(.5)
assert name not in u.call('shell','ls',folder).split(),'Confirmed deletion did not remove the fixture'
u.dump('fixture-removed');u.shot('fixture-removed');u.keys('BACK')
(u.OUT/'results.json').write_text(json.dumps({'sha256':a.expected_sha256,'source':'Own six-second emulator fixture','cancel_preserves_file':True,'confirmed_delete_removes_file':True,'no_apple_video_downloaded':True},indent=2),encoding='utf-8')
print('PASS: cancelled and confirmed removal of own emulator fixture',flush=True)
