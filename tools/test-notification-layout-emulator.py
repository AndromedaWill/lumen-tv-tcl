"""Archived revision-08 fixed-viewport regression (054/4B7); no notification access grant.

Revision 09 intentionally scrolls the whole column, including controls. Its replacement
coverage is test-media-panel-emulator.py (stacked notices, empty section and unified scroll).
"""
import argparse, json, re, time
from pathlib import Path
from PIL import Image, ImageChops
import emulator_ui as u

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True)
p.add_argument('--height',type=int,choices=[720,1080],required=True)
p.add_argument('--output',type=Path,required=True)
p.add_argument('--expected-sha256',required=True)
a=p.parse_args();u.configure(a.serial,a.output)
installed=u.call('shell','pm','path','pe.tclhome.prototype').strip().removeprefix('package:')
assert installed.startswith('/data/app/') and re.fullmatch(r'[A-Za-z0-9/._+~=-]+',installed)
assert u.call('shell','sha256sum',installed).split()[0]==a.expected_sha256,'Wrong APK installed'
size=u.call('shell','wm','size');density=u.call('shell','wm','density')
try:
    assert 'pe.tclhome.prototype' not in u.call('shell','settings','get','secure','enabled_notification_listeners')
    u.call('shell','wm','size',f'{a.height*16//9}x{a.height}')
    u.call('shell','wm','density','320' if a.height==1080 else '213');time.sleep(1)
    u.call('shell','am','force-stop','pe.tclhome.prototype')
    time.sleep(1)
    launch=u.call('shell','am','start','-W','-a','android.intent.action.MAIN','-c','android.intent.category.LEANBACK_LAUNCHER','-n','pe.tclhome.prototype/com.gathin.tvhome.MainActivity')
    (u.OUT/'launch.txt').write_text(launch,encoding='utf-8');time.sleep(2)
    u.call('shell','am','start','-W','-a','com.gathin.tvhome.action.NOTIFICATIONS','-n','pe.tclhome.prototype/com.gathin.tvhome.MainActivity');time.sleep(1)
    results=[];reference=None;bounds=None
    for name,keys in [('collapsed',[]),('explanation',['DPAD_CENTER']),('configure',['DPAD_DOWN']),('returned',['BACK'])]:
        u.keys(*keys);nodes,focus=u.dump(name);u.shot(name)
        assert any('pe.tclhome.prototype' in line for line in u.call('shell','dumpsys','activity','activities').splitlines() if 'topResumedActivity=' in line),'Our app must be foreground'
        controls=[n for n in nodes if n.get('focusable')=='true' and n.get('content-desc','').startswith(('Televisor.','Red.','Entradas.','Inicio.','Google Home.','Notificaciones.','Sonido','Imagen','Bluetooth','Aplicaciones'))]
        assert len(controls)==10,(name,len(controls))
        rects={u.label(n):u.rect(n) for n in controls}
        if bounds is None:bounds=rects
        assert rects==bounds,(name,'Controls moved')
        # Compare the central glyphs, away from antialiased rims/focus elevation. The old
        # scrolled panel painted over all four glyphs, despite unchanged accessibility bounds.
        half=round(8*a.height/540)
        compact=[rect for label,rect in rects.items() if label in ['Sonido','Imagen','Bluetooth','Aplicaciones']]
        patches=[]
        with Image.open(u.OUT/(name+'.png')) as screenshot:
            for x1,y1,x2,y2 in compact:
                x=(x1+x2)//2;y=(y1+y2)//2
                patches.append(screenshot.convert('RGB').crop((x-half,y-half,x+half,y+half)))
        if reference is None:reference=patches
        assert all(ImageChops.difference(old,new).getbbox() is None for old,new in zip(reference,patches)),(name,'Notification content painted over a compact glyph')
        results.append({'state':name,'control_bounds_unchanged':True,'compact_glyph_pixels_identical':True,'focus':u.label(focus) if focus is not None else None})
    u.keys('BACK','BACK')
    assert 'HomeNotificationService' not in u.call('shell','dumpsys','activity','services','pe.tclhome.prototype')
    (u.OUT/'results.json').write_text(json.dumps(results,indent=2,ensure_ascii=False),encoding='utf-8')
    (u.OUT/'capture.json').write_text(json.dumps({'apk_sha256':a.expected_sha256,'serial':a.serial,'height':a.height,'notification_access_granted':False},indent=2),encoding='utf-8')
    print(json.dumps(results,ensure_ascii=True),flush=True)
finally:
    for command,original in [('size',size),('density',density)]:
        override=re.search(r'Override (?:size|density):\s*(\S+)',original)
        u.call('shell','wm',command,override[1] if override else 'reset')
