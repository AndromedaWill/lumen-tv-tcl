"""Controlled dock/grid transitions; refuses physical TVs. No input-latency claim."""
import argparse, json, os, re, subprocess, time
from pathlib import Path

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args()
if not re.fullmatch(r'emulator-\d+',a.serial):p.error('Explicit emulator serial required')
adb=str(Path(os.environ['LOCALAPPDATA'])/'Android/Sdk/platform-tools/adb.exe')
pkg='pe.tclhome.prototype'
def call(*args):return subprocess.check_output([adb,'-s',a.serial,*args],text=True,encoding='utf-8',errors='replace')
assert call('shell','getprop','ro.kernel.qemu').strip()=='1'
a.output.mkdir(parents=True,exist_ok=True)
device={name:call('shell','wm',name).strip() for name in ['size','density']}
(a.output/'catalog.txt').write_text(call('shell','pm','list','packages','pe.tclhome.fixture'),encoding='utf-8')
installed=call('shell','pm','path',pkg).strip().removeprefix('package:')
assert installed.startswith('/data/app/') and re.fullmatch(r'[A-Za-z0-9/._+~=-]+',installed)
apk_sha256=call('shell','sha256sum',installed).split()[0]
call('shell','am','force-stop',pkg)
time.sleep(.8)
launch=call('shell','am','start','-W','-a','android.intent.action.MAIN','-c','android.intent.category.LEANBACK_LAUNCHER','-n',pkg+'/com.gathin.tvhome.MainActivity')
(a.output/'launch.txt').write_text(launch,encoding='utf-8');time.sleep(4)
if not any(pkg in s for s in call('shell','dumpsys','activity','activities').splitlines() if 'topResumedActivity=' in s):
    launch+=call('shell','am','start','-W','-a','android.intent.action.MAIN','-c','android.intent.category.LEANBACK_LAUNCHER','-n',pkg+'/com.gathin.tvhome.MainActivity')
    (a.output/'launch.txt').write_text(launch,encoding='utf-8');time.sleep(3)
assert any(pkg in s for s in call('shell','dumpsys','activity','activities').splitlines() if 'topResumedActivity=' in s)
call('shell','input','keyevent','KEYCODE_BACK');time.sleep(.5)
for _ in range(6):call('shell','input','keyevent','KEYCODE_DPAD_LEFT')
time.sleep(1)
(a.output/'home.png').write_bytes(subprocess.check_output([adb,'-s',a.serial,'exec-out','screencap','-p']))
call('shell','dumpsys','gfxinfo',pkg,'reset')
for i in range(12):
    call('shell','input','keyevent','KEYCODE_DPAD_DOWN');time.sleep(.9)
    if i==0:(a.output/'first-down.txt').write_text(call('shell','dumpsys','gfxinfo',pkg,'framestats'),encoding='utf-8')
    call('shell','input','keyevent','KEYCODE_BACK');time.sleep(.9)
gfx=call('shell','dumpsys','gfxinfo',pkg,'framestats')
mem=call('shell','dumpsys','meminfo',pkg)
(a.output/'gfxinfo.txt').write_text(gfx,encoding='utf-8');(a.output/'meminfo.txt').write_text(mem,encoding='utf-8')
def metric(pattern,text,kind=int):
    m=re.search(pattern,text);return kind(m[1]) if m else None
def frames(text):
    return {'frames':metric(r'Total frames rendered:\s*(\d+)',text),'jank_pct':metric(r'Janky frames:.*?\(([\d.]+)%\)',text,float),
            **{f'p{n}_ms':metric(rf'{n}th percentile:\s*(\d+)ms',text) for n in [50,95,99]}}
summary={'serial':a.serial,'apk_sha256':apk_sha256,'device':device,'cycles':12,'route':'Down from dock; Back to dock; 900 ms waits; same fixture catalog',
         'first_down':frames((a.output/'first-down.txt').read_text()),'all_cycles':frames(gfx),'pss_kib':metric(r'TOTAL PSS:\s*(\d+)',mem),
         'note':'Emulator, histogram buckets; not physical remote latency or a TCL baseline'}
(a.output/'summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8');print(json.dumps(summary,indent=2))
