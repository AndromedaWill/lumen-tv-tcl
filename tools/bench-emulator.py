"""Repeatable development benchmark. Refuses physical devices and unknown packages."""
import argparse
import json
import os
from pathlib import Path
import re
import statistics
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--package', choices=['com.gathin.tvhome', 'pe.tclhome.prototype'], required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
if not re.fullmatch(r'emulator-\d+', args.serial):
    parser.error('Only an explicit emulator serial is allowed')
adb = str(Path(os.environ['LOCALAPPDATA']) / 'Android/Sdk/platform-tools/adb.exe')


def call(*cmd):
    return subprocess.run([adb, '-s', args.serial, *cmd], check=True,
                          capture_output=True, text=True, encoding='utf-8', errors='replace').stdout


if call('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
    raise SystemExit('Not an emulator')
args.output.mkdir(parents=True, exist_ok=True)
component = args.package + '/com.gathin.tvhome.MainActivity'
other = 'com.gathin.tvhome' if args.package == 'pe.tclhome.prototype' else 'pe.tclhome.prototype'
call('shell', 'am', 'force-stop', other)
samples = {'cold': [], 'warm': []}
waits = {'cold': [], 'warm': []}
launch_states = {'cold': [], 'warm': []}
for mode in samples:
    for i in range(5):
        if mode == 'cold':
            call('shell', 'am', 'force-stop', args.package)
        else:
            call('shell', 'am', 'start', '-W', '-n',
                 'pe.tclhome.fixture.files/pe.tclhome.fixture.FixtureActivity')
        time.sleep(1)
        out = call('shell', 'am', 'start', '-W', '-n', component)
        (args.output / f'{mode}-{i}.txt').write_text(out, encoding='utf-8')
        # A delivered warm intent may have TotalTime=0; keep it, don't call it frame latency.
        match = re.search(r'TotalTime:\s*(\d+)', out)
        samples[mode].append(int(match[1]) if match else None)
        wait = re.search(r'WaitTime:\s*(\d+)', out)
        waits[mode].append(int(wait[1]) if wait else None)
        state = re.search(r'LaunchState:\s*(.*)', out)
        launch_states[mode].append(state[1].strip() if state else 'not reported')
        time.sleep(2)
call('shell', 'am', 'start', '-W', '-a', 'android.intent.action.MAIN',
     '-c', 'android.intent.category.HOME', '-n', component)
time.sleep(2)
call('shell', 'dumpsys', 'gfxinfo', args.package, 'reset')
# Identical 48-event traversal: dock, grid, and back. Key repeats are separate events.
for _ in range(4):
    for key in ['RIGHT'] * 5 + ['DOWN'] + ['LEFT'] * 5 + ['UP']:
        call('shell', 'input', 'keyevent', 'KEYCODE_DPAD_' + key)
        time.sleep(.18)
time.sleep(3)
for service in ['gfxinfo', 'meminfo']:
    (args.output / f'{service}.txt').write_text(call('shell', 'dumpsys', service, args.package), encoding='utf-8')
(args.output / 'cpuinfo.txt').write_text(call('shell', 'dumpsys', 'cpuinfo'), encoding='utf-8')
pid_result = subprocess.run([adb, '-s', args.serial, 'shell', 'pidof', args.package],
                            capture_output=True, text=True, check=False)
pid = pid_result.stdout.strip()
if pid:
    (args.output / 'top.txt').write_text(call('shell', 'top', '-b', '-n', '3', '-d', '1', '-p', pid), encoding='utf-8')
result = {'package': args.package, 'serial': args.serial, 'valid_process_at_end': bool(pid),
          'samples_ms': samples, 'wait_ms': waits, 'launch_states': launch_states,
          'median_ms': {k: statistics.median([x for x in v if x is not None])
                        if any(x is not None for x in v) else None for k, v in samples.items()},
          'note': 'am start TotalTime, not first-interactive-frame latency; emulator only'}
(args.output / 'summary.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
print(json.dumps(result, indent=2))
