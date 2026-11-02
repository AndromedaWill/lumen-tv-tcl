"""Counterbalanced A-B-B-A of two APKs. Installs only on a verified explicit emulator; no HOME changes."""
import argparse, hashlib, json, math, re, subprocess, sys
from pathlib import Path
import emulator_ui as u

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial', required=True)
p.add_argument('--baseline', required=True, type=Path)
p.add_argument('--candidate', required=True, type=Path)
p.add_argument('--output', required=True, type=Path)
a = p.parse_args()
assert not a.output.exists(), 'Use a new output directory to preserve prior samples'
u.configure(a.serial, a.output)
for apk in [a.baseline, a.candidate]:
    assert apk.is_file() and apk.suffix == '.apk'

def stages(path):
    header = None; rows = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('Flags,'):
            header = line.rstrip(',').split(','); continue
        if header and re.match(r'^\d+,', line):
            values = list(map(int, line.rstrip(',').split(',')))
            if len(values) == len(header):
                frame = dict(zip(header, values))
                if frame['Flags'] == 0: rows[frame['IntendedVsync']] = frame
        else: header = None
    result = {'frames_in_ring': len(rows), 'note': 'Last framestats ring, not all aggregate histogram frames. CPU elapsed stages, not pure GPU duration.'}
    for label, end, start in [('ui', 'SyncQueued', 'HandleInputStart'), ('record', 'SyncQueued', 'DrawStart'),
            ('sync', 'IssueDrawCommandsStart', 'SyncStart'), ('issue', 'SwapBuffers', 'IssueDrawCommandsStart'),
            ('swap', 'SwapBuffersCompleted', 'SwapBuffers'), ('total', 'FrameCompleted', 'IntendedVsync')]:
        values = sorted((r[end] - r[start]) / 1e6 for r in rows.values() if 0 < r[end] - r[start] < 1e9)
        result[label] = {f'p{q}_ms': round(values[math.ceil(len(values) * q / 100) - 1], 3) for q in [50, 95, 99]} if values else {}
        result[label]['valid_samples'] = len(values)
    return result

results = []
try:
    for i, (label, apk) in enumerate([('baseline', a.baseline), ('candidate', a.candidate),
            ('candidate', a.candidate), ('baseline', a.baseline)], 1):
        print(u.call('install', '-r', '--user', '0', str(apk)).strip(), flush=True)
        out = a.output / (str(i) + '-' + label)
        subprocess.run([sys.executable, str(Path(__file__).with_name('bench-grid-transition.py')),
            '--serial', a.serial, '--output', str(out)], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        sample = json.loads((out / 'summary.json').read_text())
        assert sample['apk_sha256'] == hashlib.sha256(apk.read_bytes()).hexdigest()
        sample.update(run=i, label=label, frame_stages=stages(out / 'gfxinfo.txt'))
        results.append(sample); print(json.dumps(sample), flush=True)
        (a.output / 'comparison.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
finally:
    # Leaves the candidate installed; never clears data or chooses a Home app.
    print(u.call('install', '-r', '--user', '0', str(a.candidate)).strip(), flush=True)
