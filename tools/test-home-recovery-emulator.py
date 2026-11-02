"""API34 Home selection, failure and recovery on an explicitly verified disposable emulator.

Never accepts a physical serial; never disables packages, changes permissions or enables helpers.
Restores the original Home in finally. ADB restoration is independent of the launcher process.
"""
import argparse, json, re, subprocess, time
from pathlib import Path
import emulator_ui as u

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial',required=True)
p.add_argument('--output',type=Path,required=True)
p.add_argument('--expected-sha256',required=True)
a=p.parse_args();u.configure(a.serial,a.output)
PKG='pe.tclhome.prototype'
def shell(*args):return u.call('shell',*args).strip()
def resolve():
    raw=shell('cmd','package','resolve-activity','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
    candidates=[x for x in raw.splitlines() if re.fullmatch(r'[\w.]+/[\w.$]+',x)]
    assert len(candidates)==1,raw
    return candidates[0]
def role():return shell('cmd','role','get-role-holders','--user','0','android.app.role.HOME')
def component_set(raw):
    result=set()
    for component in raw.split(':'):
        if '/' not in component:continue
        package,cls=component.split('/',1)
        result.add(package+'/'+(package+cls if cls.startswith('.') else cls))
    return result
def front():
    return '\n'.join(x.strip() for x in shell('dumpsys','activity','activities').splitlines() if 'topResumedActivity=' in x)
def await_front(package,timeout=25):
    started=time.monotonic()
    while time.monotonic()-started<timeout:
        if package+'/' in front():return round(time.monotonic()-started,2)
        time.sleep(.5)
    u.shot('failed-foreground');raise AssertionError('Not foreground: '+package+'; '+front())
def record(name,**values):
    results.append({'test':name,**values});(u.OUT/'results.json').write_text(json.dumps(results,indent=2),encoding='utf8')
    print(json.dumps(results[-1]),flush=True)
def press_home(package):u.keys('HOME');await_front(package)
def launch_own():
    shell('am','start','-W','-n',PKG+'/com.gathin.tvhome.MainActivity');await_front(PKG)
def picker(label):
    shell('am','start','-W','-a','android.settings.HOME_SETTINGS');time.sleep(.5)
    # Official screen's selectable row owns child text; never tap guessed coordinates.
    for i in range(14):
        nodes,f=u.dump('picker')
        if f is not None and f.get('clickable')=='true' and any(label in (x.get('text') or '') for x in f.iter('node')):
            u.shot('picker-'+('custom' if label.startswith('TCL') else 'stock'))
            u.keys('DPAD_CENTER');time.sleep(1);return
        u.keys('DPAD_DOWN')
    raise AssertionError('Official Home row not reachable by D-pad: '+label)

assert shell('getprop','ro.build.version.sdk')=='34'
assert shell('am','get-current-user')=='0'
installed=shell('pm','path',PKG).removeprefix('package:')
assert re.fullmatch(r'/data/app/[\w./+~=-]+',installed)
assert shell('sha256sum',installed).split()[0]==a.expected_sha256
original=resolve();stock=original.split('/')[0]
assert stock=='com.google.android.tvlauncher','This fixture expects the Android TV AVD stock Home'
assert role()==stock,'Restore the original role before this suite'
baseline={'serial':a.serial,'original_component':original,'role':role(),
          'disabled_packages':shell('pm','list','packages','-d'),
          'accessibility':shell('settings','get','secure','enabled_accessibility_services'),
          'notification_listeners':shell('settings','get','secure','enabled_notification_listeners'),
          'apk_sha256':a.expected_sha256}
assert PKG not in baseline['accessibility']
(u.OUT/'baseline.json').write_text(json.dumps(baseline,indent=2),encoding='utf8')
(u.OUT/'RESTORE-EMULATOR.txt').write_text('adb -s '+a.serial+' shell cmd package set-home-activity --user 0 '+original+'\n',encoding='utf8')
results=[]
try:
    picker('TCL Home');assert role()==PKG
    effective=resolve().split('/')[0];assert effective in (PKG,stock)
    press_home(effective);u.dump('selected-home');u.shot('selected-home')
    record('official-selector',result='PASS' if effective==PKG else 'LIMITATION',role=role(),
           resolved_component=resolve(),actual_foreground=front(),replacement_home=effective==PKG)
    launch_own()
    for i in range(3):
        u.keys('SLEEP');time.sleep(1)
        power=shell('dumpsys','power');assert 'mWakefulness=Asleep' in power or 'mWakefulness=Dozing' in power
        u.keys('WAKEUP');await_front(PKG);assert resolve().split('/')[0]==effective;assert role()==PKG
        record('sleep-wake-'+str(i),result='PASS',scope='Emulated screen sleep, not TCL deep standby')
    for i in range(2):
        oldpid=shell('pidof',PKG)
        shell('am','crash','--user','0',PKG);time.sleep(2)
        u.shot('forced-crash-'+str(i))
        # Home is also the remote escape if Android displays a crash dialog.
        press_home(effective)
        if effective!=PKG:launch_own()
        newpid=shell('pidof',PKG);assert newpid and newpid!=oldpid
        nodes,f=u.dump('after-crash-'+str(i));assert any(n.get('resource-id','').endswith('/card') for n in nodes)
        u.shot('after-crash-'+str(i));record('forced-java-crash-'+str(i),result='PASS',new_process=True,
             home_escape=effective,custom_relaunch='Home' if effective==PKG else 'explicit launch')
    shell('am','force-stop',PKG);time.sleep(1);press_home(effective)
    if effective!=PKG:launch_own()
    record('force-stop-home',result='PASS',home_escape=effective)
    for i in range(2):
        before=shell('cat','/proc/sys/kernel/random/boot_id');u.call('reboot');deadline=time.monotonic()+150
        while time.monotonic()<deadline:
            try:
                boot=shell('getprop','sys.boot_completed')
                if boot=='1' and shell('cat','/proc/sys/kernel/random/boot_id')!=before:break
            except (subprocess.SubprocessError,OSError):pass
            time.sleep(2)
        else:raise AssertionError('Emulator boot timed out')
        assert shell('getprop','ro.kernel.qemu')=='1';assert resolve().split('/')[0]==effective;assert role()==PKG
        elapsed=await_front(effective,40);u.shot('boot-before-home-key-'+str(i))
        press_home(effective)
        if effective!=PKG:launch_own()
        nodes,f=u.dump('relaunch-after-boot-'+str(i));assert any(n.get('resource-id','').endswith('/card') for n in nodes)
        record('full-reboot-'+str(i),result='PASS' if effective==PKG else 'LIMITATION',role_preserved=True,
               automatic_custom_home=effective==PKG,foreground_before_key=effective,custom_relaunch_ok=True,after_boot_poll_seconds=elapsed)
    shell('am','force-stop',PKG);time.sleep(1)
    shell('am','start','-W','-a','android.settings.SETTINGS');await_front('com.android.tv.settings')
    u.shot('native-settings-independent');record('native-settings-while-stopped',result='PASS')
    picker('Android TV Home');assert resolve()==original;press_home(stock)
    u.shot('restored-official');record('restore-official-selector',result='PASS',launcher_not_required=True)
    picker('TCL Home');assert role()==PKG;launch_own()
    shell('am','force-stop',PKG);time.sleep(1)
    shell('cmd','package','set-home-activity','--user','0',original)
    assert resolve()==original;assert role()==baseline['role'];press_home(stock)
    u.shot('restored-adb');record('restore-adb-while-stopped',result='PASS')
finally:
    # Independent of app code, UI, media and accessibility. No package enable/disable operation.
    shell('cmd','package','set-home-activity','--user','0',original)
    assert resolve()==original;assert role()==baseline['role']
    assert shell('pm','list','packages','-d')==baseline['disabled_packages']
    assert component_set(shell('settings','get','secure','enabled_accessibility_services'))==component_set(baseline['accessibility'])
    assert component_set(shell('settings','get','secure','enabled_notification_listeners'))==component_set(baseline['notification_listeners'])
    press_home(stock);record('baseline-restored',result='PASS',packages_and_permissions_unchanged=True)
