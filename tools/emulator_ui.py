import subprocess,os,time,re,json,xml.etree.ElementTree as ET
from pathlib import Path
ADB=str(Path(os.environ['LOCALAPPDATA'])/'Android/Sdk/platform-tools/adb.exe')
TARGET=[]
OUT=Path('artifacts/emulator-ui')
def call(*args):return subprocess.check_output(TARGET+list(args),text=True,encoding='utf-8',errors='replace',timeout=35)
def configure(serial,output):
    global TARGET,OUT
    assert re.fullmatch(r'emulator-\d+',serial),'Explicit emulator serial required'
    TARGET=[ADB,'-s',serial];assert call('shell','getprop','ro.kernel.qemu').strip()=='1','Emulator required'
    OUT=Path(output);OUT.mkdir(parents=True,exist_ok=True)
def keys(*codes,wait=.45):
    for c in codes:call('shell','input','keyevent','KEYCODE_'+c);time.sleep(wait)
def dump(name='state'):
    call('shell','uiautomator','dump','--compressed','/sdcard/review06.xml')
    xml=call('shell','cat','/sdcard/review06.xml');(OUT/(name+'.xml')).write_text(xml,encoding='utf-8')
    root=ET.fromstring(xml);nodes=list(root.iter('node'));focused=[n for n in nodes if n.get('focused')=='true']
    return nodes,focused[0] if focused else None
def label(n):return (n.get('content-desc') or n.get('text') or '').replace('\u25cf ','').strip()
def rect(n):return list(map(int,re.findall(r'\d+',n.get('bounds',''))))
def focus(text,steps=18):
    previous=None;stuck=0
    for i in range(steps):
        nodes,f=dump();assert f is not None,'No focus'
        candidates=[n for n in nodes if n.get('focusable')=='true' and label(n).casefold().startswith(text.casefold())]
        if not candidates:keys('DPAD_DOWN');continue
        target=candidates[0]
        if f is target or f.attrib==target.attrib:return f
        x1,y1,x2,y2=rect(f);tx1,ty1,tx2,ty2=rect(target)
        if ty1>=y2:direction='DOWN'
        elif ty2<=y1:direction='UP'
        elif tx1>=x2:direction='RIGHT'
        elif tx2<=x1:direction='LEFT'
        else:direction='DOWN' if ty1+ty2>y1+y2 else 'UP'
        marker=(label(f),f.get('bounds'),direction)
        stuck=stuck+1 if marker==previous else 0;previous=marker
        if stuck>=2:direction='RIGHT' if tx1+tx2>x1+x2 else 'LEFT'
        keys('DPAD_'+direction)
    nodes,f=dump('focus-failure');raise AssertionError((text,label(f) if f is not None else None))
def press(text):focus(text);keys('DPAD_CENTER')

def thumb(name):
    focus('Vista previa');keys('DPAD_DOWN',*(['DPAD_LEFT']*24),wait=.08)
    for i in range(24):
        _,f=dump()
        if f is not None and label(f).startswith(name+'.'):return
        keys('DPAD_RIGHT',wait=.12)
    raise AssertionError('Thumbnail unreachable: '+name)
def shot(name):
    (OUT/(name+'.png')).write_bytes(subprocess.check_output(TARGET+['exec-out','screencap','-p']))
def launch_settings():
    call('shell','am','start','-W','-a','com.gathin.tvhome.action.LAUNCHER_SETTINGS','-n','pe.tclhome.prototype/com.gathin.tvhome.MainActivity');time.sleep(1)
    press('Fondos');time.sleep(2)
def texts(name):return [n.get('text','') for n in dump(name)[0] if n.get('text')]
def report(name,**values):
    (OUT/(name+'.json')).write_text(json.dumps(values,indent=2,ensure_ascii=False),encoding='utf-8');print(json.dumps({'test':name,**values},ensure_ascii=True),flush=True)
