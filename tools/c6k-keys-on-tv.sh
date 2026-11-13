#!/system/bin/sh
# Trial only: an evdev reader on the TV, with no input grab or boot registration.
set -eu
[ "$(getprop ro.build.fingerprint)" = 'TCL/G08_4K_GB/G08:14/UTT2.250416.001/AU02:user/release-keys' ] || exit 2
enabled=$(settings --user 0 get secure enabled_accessibility_services)
case "$enabled" in null|'') ;; *) echo 'Turn off accessibility services before this trial'; exit 3 ;; esac
node=$(getevent -p 2>/dev/null | awk '
    /^add device/ { node=$NF }
    /name:[[:space:]]*"BT_RC813A_B1"/ { print node }
')
case "$node" in
    /dev/input/event*) ;;
    *) echo 'RC813A input node not found'; exit 4 ;;
esac
case "${node#/dev/input/event}" in ''|*[!0-9]*) exit 4 ;; esac
[ -r "$node" ] || exit 4

[ "${1:-}" = --session ] || exit 2
run_dir=/data/local/tmp/tcl-remote-session
boot_id=$(cat /proc/sys/kernel/random/boot_id)
if [ -r "$run_dir/boot-id" ] && [ "$(cat "$run_dir/boot-id")" != "$boot_id" ]; then
    rm -f "$run_dir/owner.pid" "$run_dir/boot-id"
    rmdir "$run_dir"
fi
mkdir "$run_dir" || { echo 'Session already running or stale state'; exit 5; }
trap 'rm -f "$run_dir/owner.pid" "$run_dir/boot-id"; rmdir "$run_dir"' EXIT
printf "%s\n" "$boot_id" > "$run_dir/boot-id"
echo "$$" > "$run_dir/owner.pid"
getevent -lt "$node" | while read -r left seconds type code value rest; do
    case "$type:$code" in
        EV_MSC:MSC_SCAN) usage="$value" ;;
        EV_KEY:*)
            if [ "$value" = UP ]; then
                case "${usage:-}" in
                    000c0057)
                        am start -a android.intent.action.MAIN \
                            -c android.intent.category.LEANBACK_LAUNCHER \
                            -p com.google.android.youtube.tv
                        ;;
                    000c0055)
                        front=$(dumpsys activity activities | grep topResumedActivity)
                        case "$front" in
                            *pe.tclhome.prototype/*|'') ;;
                            *) am start -n pe.tclhome.prototype/com.gathin.tvhome.QuickSettingsActivity ;;
                        esac
                        ;;
                esac
            fi
            usage=''
            ;;
    esac
done
