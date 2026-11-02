"""Real MediaSession transport/UI test using own synthetic sessions and injected discovery.

Never grants notification access. Does not establish Home or use accessibility. Debug-only
instrumentation is a separate APK; the production discovery permission still needs a manual test.
"""
import argparse,hashlib,json,re,subprocess,time
from pathlib import Path
import emulator_ui as u

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True)
p.add_argument('--apk',type=Path,required=True)
p.add_argument('--test-apk',type=Path,required=True)
p.add_argument('--expected-sha256',required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args();u.configure(a.serial,a.output)
assert hashlib.sha256(a.apk.read_bytes()).hexdigest()==a.expected_sha256,'Wrong debug APK'
assert 'pe.tclhome.prototype' not in u.call('shell','settings','get','secure','enabled_notification_listeners'),'Keep emulator access off'
for apk in (a.apk,a.test_apk):assert 'Success' in u.call('install','-r',str(apk.resolve()))
size=u.call('shell','wm','size');density=u.call('shell','wm','density')
results=[]
try:
    for height in (1080,720):
        u.call('shell','wm','size',f'{height*16//9}x{height}');u.call('shell','wm','density','320' if height==1080 else '213');time.sleep(1)
        folder=a.output/str(height);folder.mkdir(parents=True,exist_ok=True)
        raw=u.call('shell','am','instrument','-w','pe.tclhome.prototype.test/com.gathin.tvhome.MediaPanelInstrumentation')
        (folder/'instrumentation.txt').write_text(raw,encoding='utf-8')
        assert 'INSTRUMENTATION_RESULT: result=PASS:' in raw,raw
        subprocess.run(u.TARGET+['pull','/sdcard/Android/data/pe.tclhome.prototype/files/media-review/.',str(folder.resolve())],check=True,capture_output=True)
        assert len(list(folder.glob('*.png')))>=10,'Missing actual screenshots'
        results.append({'height':height,'result':'PASS','debug_sha256':a.expected_sha256,'scope':'Own framework sessions, injected discovery, no notification grant'})
        print(json.dumps(results[-1]),flush=True)
finally:
    for command,original in [('size',size),('density',density)]:
        override=re.search(r'Override (?:size|density):\s*(\S+)',original)
        u.call('shell','wm',command,override[1] if override else 'reset')
(a.output/'results.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
