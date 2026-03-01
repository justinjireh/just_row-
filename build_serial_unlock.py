#!/usr/bin/env python3
"""
Build a recovery-sideloadable OTA ZIP that unlocks /dev/ttyS1 (the Hydrow
rower's UART serial port) so that RowPlus can read live telemetry.

How it works:
  1. Installs an init .rc file into /system/etc/init/ that runs
     `chmod 0666 /dev/ttyS1` on every boot.
  2. Signs the ZIP with AOSP test keys (same as the ADB enabler).

Usage:
  python build_serial_unlock.py
  python sign_ota.py          # re-use existing signer
  adb reboot recovery         # then sideload from recovery menu
"""

import zipfile
import os

UPDATE_BINARY = r"""#!/sbin/sh

# update-binary shell script
# Args: 1=API version, 2=status fd, 3=zip path

OUTFD=$2

ui_print() {
  echo "ui_print $1" > /proc/self/fd/$OUTFD
  echo "ui_print" > /proc/self/fd/$OUTFD
}

ui_print "================================"
ui_print "  Hydrow Serial Unlock v1"
ui_print "  Unlocks /dev/ttyS1 for RowPlus"
ui_print "================================"
ui_print ""

# --- Mount /system read-write ---
ui_print "Mounting /system..."

mount /system 2>/dev/null
mount -o rw,remount /system 2>/dev/null

# Try alternative mount points (MediaTek MT8167 layout)
if [ ! -d /system/etc/init ]; then
  ui_print "Trying /dev/block/by-name/system..."
  mount /dev/block/by-name/system /system 2>/dev/null
  mount -o rw,remount /dev/block/by-name/system /system 2>/dev/null
fi

if [ ! -d /system/etc/init ]; then
  ui_print "Trying MediaTek partition layout..."
  mount /dev/block/platform/soc/11230000.mmc/by-name/system /system 2>/dev/null
  mount -o rw,remount /dev/block/platform/soc/11230000.mmc/by-name/system /system 2>/dev/null
fi

if [ ! -d /system/etc/init ]; then
  ui_print "ERROR: Cannot find /system/etc/init"
  ui_print "System mount may have failed."
  exit 1
fi

# --- Install the init RC file ---
ui_print "Installing serial unlock init script..."

INIT_RC="/system/etc/init/init.serial_unlock.rc"

cat > "$INIT_RC" << 'RCEOF'
# Hydrow Serial Unlock — allows RowPlus to read /dev/ttyS1
# The rower's UART serial port is normally owned by system:system (0660).
# This relaxes it to 0666 so any app can open it for telemetry.

on boot
    chmod 0666 /dev/ttyS1

on post-fs-data
    chmod 0666 /dev/ttyS1
RCEOF

chmod 0644 "$INIT_RC"
chown root:root "$INIT_RC"

if [ -f "$INIT_RC" ]; then
  ui_print "init.serial_unlock.rc installed!"
else
  ui_print "ERROR: Failed to write init RC file"
  exit 1
fi

# --- Also do an immediate chmod (takes effect this boot if run live) ---
chmod 0666 /dev/ttyS1 2>/dev/null

# --- Verify ---
ui_print ""
ui_print "Verifying..."
ls -la /dev/ttyS1 2>/dev/null | while read line; do
  ui_print "  $line"
done
ls -la "$INIT_RC" 2>/dev/null | while read line; do
  ui_print "  $line"
done

# --- Cleanup ---
ui_print ""
ui_print "Unmounting..."
umount /system 2>/dev/null
sync

ui_print ""
ui_print "================================"
ui_print "  DONE! Reboot now."
ui_print "  /dev/ttyS1 will be 0666"
ui_print "  on every boot."
ui_print "================================"
"""

UPDATER_SCRIPT = 'ui_print("Hydrow Serial Unlock v1");'

base_dir = r'e:\LISTING LAB DOWNLOADS\platform-tools-latest-windows'
output_path = os.path.join(base_dir, 'serial_unlock.zip')

with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    zf.writestr('META-INF/com/google/android/update-binary', UPDATE_BINARY)
    zf.writestr('META-INF/com/google/android/updater-script', UPDATER_SCRIPT)

print(f"Built: {output_path}")
print(f"Size: {os.path.getsize(output_path)} bytes")
print()
print("Next steps:")
print("  1. python sign_ota.py  (update input path to serial_unlock.zip)")
print("  2. adb reboot recovery")
print("  3. Select 'Apply update from ADB'")
print("  4. adb sideload serial_unlock_signed.zip")
print("  5. Reboot and test")
