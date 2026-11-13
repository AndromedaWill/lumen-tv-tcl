"""PC-side listener for the verified G08 remote. Read-only unless --apply."""
import argparse
import os
from pathlib import Path
import re
import subprocess

p = argparse.ArgumentParser(description=__doc__)
p.add_argument("--serial", required=True)
p.add_argument("--apply", action="store_true")
p.add_argument("--continuous", action="store_true",
               help="Listen until Ctrl+C or the ADB connection closes")
args = p.parse_args()
if not re.fullmatch(r"192\.168\.0\.107:[0-9]{1,5}", args.serial):
    p.error("Use the verified TCL endpoint")
adb = Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk/platform-tools/adb.exe"
target = [str(adb), "-s", args.serial, "shell"]

def shell(*command):
    return subprocess.check_output(target + list(command), text=True, timeout=8).strip()

fingerprint = "TCL/G08_4K_GB/G08:14/UTT2.250416.001/AU02:user/release-keys"
if shell("getprop", "ro.build.fingerprint") != fingerprint:
    p.error("Unexpected firmware")

home = "pe.tclhome.prototype"
keys = {
    (83, "000c0055"): ("Ajustes", None),
    (190, "000c0057"): ("YouTube", "com.google.android.youtube.tv"),
    (4062, "000c0051"): ("Netflix", "com.netflix.ninja"),
    (4099, "000c005f"): ("Prime Video", "com.amazon.amazonvideo.livingroom"),
}
event = re.compile(r"keyCode=(\d+), action=ACTION_UP, scanCode=240, usageCode=0x([0-9a-fA-F]{8})")
duration = [] if args.continuous else ["timeout", "90"]
print("Listening. Press Ctrl+C to stop.", flush=True)
with subprocess.Popen(
    target + duration + ["logcat", "-T", "1", "-s", "InputReader:D"],
    stdout=subprocess.PIPE, text=True, encoding="utf-8", errors="replace",
) as log:
    try:
        # logcat -T 1 first emits the most recent buffered record.
        skip = True
        for line in log.stdout:
            if line.startswith("---------"):
                continue
            if skip:
                skip = False
                continue
            match = event.search(line)
            if not match:
                continue
            key = keys.get((int(match[1]), match[2].lower()))
            if not key:
                continue
            label, package = key
            foreground = re.findall(
                r"topResumedActivity=.*? u0 ([\w.]+)/",
                shell("dumpsys", "activity", "activities"),
            )
            if len(foreground) != 1:
                print(f"{label}: foreground unclear", flush=True)
                continue
            if foreground[0] == home and package != "com.google.android.youtube.tv":
                print(f"{label}: handled by TCL Home", flush=True)
                continue
            if package is not None and foreground[0] == package:
                continue
            print(f"{label}: {'opening' if args.apply else 'preview'}", flush=True)
            if args.apply:
                if package is None:
                    print(shell("am", "start", "-W", "-n",
                                home + "/com.gathin.tvhome.QuickSettingsActivity"), flush=True)
                else:
                    print(shell("am", "start", "-a", "android.intent.action.MAIN",
                                "-c", "android.intent.category.LEANBACK_LAUNCHER",
                                "-p", package), flush=True)
    except KeyboardInterrupt:
        pass
    finally:
        if log.poll() is None:
            log.terminate()
