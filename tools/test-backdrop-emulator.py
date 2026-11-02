"""Real D-pad, same-video blur and lifecycle checks; disposable explicit emulator only."""
import argparse,json,time
from pathlib import Path
import emulator_ui as u
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();u.configure(a.serial,a.output)
for apk in ('launcher/app/build/outputs/apk/debug/app-debug.apk','launcher/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
    assert 'Success' in u.call('install','-r',str(Path(apk).resolve()))
u.call('push','artifacts/tcl-review-06/test-video.mp4','/sdcard/Android/data/pe.tclhome.prototype/files/fixture.mp4')
results=[]
try:
    for h in (1080,720):
        u.call('shell','wm','size',f'{h*16//9}x{h}');u.call('shell','wm','density','320' if h==1080 else '213');u.call('shell','am','force-stop','pe.tclhome.fixture.files');time.sleep(1)
        # Instrumentation includes bounded waits for real Activity transitions.
        import subprocess
        raw=subprocess.check_output(u.TARGET+['shell','am','instrument','-w','pe.tclhome.prototype.test/com.gathin.tvhome.BackdropInstrumentation'],text=True,encoding='utf-8',errors='replace',timeout=100)
        folder=a.output/str(h);folder.mkdir(exist_ok=True);(folder/'instrumentation.txt').write_text(raw,encoding='utf-8');print(raw[-600:],flush=True)
        u.call('pull','/sdcard/Android/data/pe.tclhome.prototype/files/backdrop-review/.',str(folder.resolve()))
        assert 'INSTRUMENTATION_RESULT: result=PASS:' in raw,raw[-600:]
        results.append({'height':h,'result':'PASS','scope':'Own video and fixture; no physical Africa Night test'})
finally:
    u.call('shell','wm','size','reset');u.call('shell','wm','density','reset')
(a.output/'results.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
