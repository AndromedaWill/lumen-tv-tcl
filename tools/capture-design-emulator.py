"""Capture existing screens and two focus states. Explicit emulator only, never a TV."""
import argparse,os,re,sys,time,subprocess,json
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--height',type=int,choices=[720,1080],default=1080);a=p.parse_args()
if not re.fullmatch(r'emulator-\d+',a.serial):p.error('Explicit emulator serial required')
import emulator_ui as u
u.configure(a.serial,a.output)
original_size=u.call('shell','wm','size');original_density=u.call('shell','wm','density')
installed=u.call('shell','pm','path','pe.tclhome.prototype').strip().removeprefix('package:')
assert installed.startswith('/data/app/') and re.fullmatch(r'[A-Za-z0-9/._+~=-]+',installed)
apk_sha256=u.call('shell','sha256sum',installed).split()[0]
def restore(command,original):
 m=re.search(r'Override (?:size|density):\s*(\S+)',original);u.call('shell','wm',command,m[1] if m else 'reset')
def shot(name):
 top=u.call('shell','dumpsys','activity','activities').split('topResumedActivity=')[-1][:250]
 assert 'pe.tclhome.prototype' in top,(name,top)
 u.shot(name);u.dump(name);print(name,flush=True)
try:
 u.call('shell','wm','size','1920x1080' if a.height==1080 else '1280x720');u.call('shell','wm','density','320' if a.height==1080 else '213');time.sleep(2)
 u.call('shell','am','start','-W','-a','android.intent.action.MAIN','-c','android.intent.category.LEANBACK_LAUNCHER','-n','pe.tclhome.prototype/com.gathin.tvhome.MainActivity');time.sleep(2)
 u.keys('BACK',*(['DPAD_LEFT']*7));shot('home-focused');u.keys('DPAD_RIGHT');shot('home-resting')
 u.press('Reloj y calendario');shot('calendar-next');u.keys('DPAD_LEFT');shot('calendar-previous');u.keys('BACK')
 u.press('Ajustes');shot('control-focused');u.keys('DPAD_RIGHT');shot('control-resting');u.keys('DPAD_DOWN','DPAD_DOWN','DPAD_DOWN','DPAD_DOWN');shot('notifications-off');u.keys('BACK','BACK')
 u.call('shell','am','start','-W','-a','com.gathin.tvhome.action.LAUNCHER_SETTINGS','-n','pe.tclhome.prototype/com.gathin.tvhome.MainActivity');time.sleep(1)
 shot('customize-focused');u.keys('DPAD_DOWN');shot('customize-resting');u.press('Fondos');time.sleep(1);shot('backgrounds-controls');u.focus('Vista previa');shot('backgrounds-preview');u.keys('BACK','BACK')
 (u.OUT/'capture.json').write_text(json.dumps({'serial':a.serial,'height':a.height,'apk_sha256':apk_sha256,'method':'Real screencap; D-pad only; no permission grants','original_size':original_size,'original_density':original_density},indent=2))
finally:restore('size',original_size);restore('density',original_density)
