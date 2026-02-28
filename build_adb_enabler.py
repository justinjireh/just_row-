#!/usr/bin/env python3
"""Build the adb_enabler.zip with improved update-binary that also injects ADB keys."""

import zipfile
import os

ADB_PUBKEY = "QAAAAHkSZz03SLFsN/Bvyqt2TgNL2lepnoMhuNXQZeOQbmFTMIGNqn+fsaWzNz0/ertR3yXGk1BbXbtwwy/xLmuAVvSxm6jovSjHPcW4+rubTh5QWJztzsKZp9b768nWsU4T5DqAOvQ5AmJfMA3nJ22TQsZKJ26nHVJvDPEdX8APRRMhIofHFTa6yuYo9G/wrWCQWPa2xTpiDlZzWkT4fy1Op/upVYLEDm7OKix24demt/bN3PCCg7SM8xvcePSESlkfICpCz5OA+nmCnE7vp9a5LyK9AQcxbMFA014YU+lYtYwfTSWcbaxC7dafAjQW5dN7zPEPba/0ZGnyg/ctyRC8HSc+CHK7CC3Q/7Cp9dY+1Xgr0ib0AKxcAcDgMFyl9+yocXImjoLlveLbUr/xis/SIxR7vfTdYTW0cbIh/B7lVDpt2/yN9b07D5ODI0DA90Mf/G/R/lRw4N5LrxDh93QoriCm74tZV+WeUU0hVYh95q5W/EQex+riUHjqrDZL2zKOdtlLFaUTm7yzLW30h/ju5n4fEFOUZAxA3o5JImdPS474y5H9HdYNHXT/VdAiZ1mv+ZnZg9HsVrEGgeFn4gWTs/MBAMn6FNXmXfXOxh8FkqM1WC9D6MPdBrIYaU0JWBQ3fLom/qC8hvMCjjpSk74sG6dvaaJHQTTyT3rdUX5rJghc8+eCFwEAAQA= justinjirehjohnson@PUGET"

UPDATE_BINARY = r"""#!/sbin/sh

# update-binary shell script
# Args: 1=API version, 2=status fd, 3=zip path

OUTFD=$2

ui_print() {
  echo "ui_print $1" > /proc/self/fd/$OUTFD
  echo "ui_print" > /proc/self/fd/$OUTFD
}

ui_print "================================"
ui_print "  Hydrow ADB Enabler v2"
ui_print "================================"
ui_print ""

# --- Step 1: Enable ADB in build.prop ---
ui_print "Mounting /system..."

mount /system 2>/dev/null
mount -o rw,remount /system 2>/dev/null

# Try alternative mount points
if [ ! -f /system/build.prop ]; then
  ui_print "Trying /dev/block/by-name/system..."
  mount /dev/block/by-name/system /system 2>/dev/null
  mount -o rw,remount /dev/block/by-name/system /system 2>/dev/null
fi

# Try platform path for MediaTek MT8167
if [ ! -f /system/build.prop ]; then
  ui_print "Trying MediaTek partition layout..."
  mount /dev/block/platform/soc/11230000.mmc/by-name/system /system 2>/dev/null
  mount -o rw,remount /dev/block/platform/soc/11230000.mmc/by-name/system /system 2>/dev/null
fi

if [ -f /system/build.prop ]; then
  ui_print "Found build.prop!"

  # Backup original
  cp /system/build.prop /system/build.prop.bak
  ui_print "Backup created: build.prop.bak"

  # Remove existing ADB-related properties
  sed -i '/^persist.sys.usb.config/d' /system/build.prop
  sed -i '/^ro.adb.secure/d' /system/build.prop
  sed -i '/^ro.debuggable/d' /system/build.prop
  sed -i '/^ro.secure/d' /system/build.prop
  sed -i '/^persist.service.adb.enable/d' /system/build.prop
  sed -i '/^persist.service.debuggable/d' /system/build.prop

  # Add ADB-enabling properties
  echo "" >> /system/build.prop
  echo "# ADB Enabler v2" >> /system/build.prop
  echo "persist.sys.usb.config=mtp,adb" >> /system/build.prop
  echo "ro.adb.secure=0" >> /system/build.prop
  echo "ro.debuggable=1" >> /system/build.prop
  echo "ro.secure=0" >> /system/build.prop
  echo "persist.service.adb.enable=1" >> /system/build.prop
  echo "persist.service.debuggable=1" >> /system/build.prop

  ui_print "ADB properties set in build.prop!"
else
  ui_print "WARNING: Could not find /system/build.prop"
  ui_print "Will still try to inject ADB keys..."
fi

# --- Step 2: Inject ADB public key ---
ui_print ""
ui_print "Injecting ADB authorization key..."

mount /data 2>/dev/null
mount -o rw,remount /data 2>/dev/null

# Create the adb directory if needed
mkdir -p /data/misc/adb 2>/dev/null

# Write the ADB public key
ADB_KEY="""" + ADB_PUBKEY + r""""
echo "$ADB_KEY" > /data/misc/adb/adb_keys
chmod 640 /data/misc/adb/adb_keys
chown 1000:1000 /data/misc/adb/adb_keys 2>/dev/null

if [ -f /data/misc/adb/adb_keys ]; then
  ui_print "ADB key injected successfully!"
else
  ui_print "WARNING: Could not write ADB key"
fi

# Also set the persist property for USB config
mkdir -p /data/property 2>/dev/null
echo "mtp,adb" > /data/property/persist.sys.usb.config 2>/dev/null

# --- Step 3: Try to modify default.prop in root ---
ui_print ""
ui_print "Attempting to patch default.prop..."

if [ -f /default.prop ]; then
  mount -o rw,remount / 2>/dev/null
  sed -i 's/ro.adb.secure=1/ro.adb.secure=0/g' /default.prop
  sed -i 's/ro.secure=1/ro.secure=0/g' /default.prop
  sed -i 's/ro.debuggable=0/ro.debuggable=1/g' /default.prop
  ui_print "default.prop patched!"
else
  ui_print "default.prop not accessible (normal in recovery)"
fi

# --- Cleanup ---
ui_print ""
ui_print "Unmounting..."
umount /system 2>/dev/null
sync

ui_print ""
ui_print "================================"
ui_print "  DONE! Reboot now."
ui_print "  ADB should be enabled and"
ui_print "  your PC pre-authorized."
ui_print "================================"
"""

UPDATER_SCRIPT = 'ui_print("Hydrow ADB Enabler v2");'

base_dir = r'e:\LISTING LAB DOWNLOADS\platform-tools-latest-windows'
output_path = os.path.join(base_dir, 'adb_enabler.zip')

with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    zf.writestr('META-INF/com/google/android/update-binary', UPDATE_BINARY)
    zf.writestr('META-INF/com/google/android/updater-script', UPDATER_SCRIPT)

print(f"Built: {output_path}")
print(f"Size: {os.path.getsize(output_path)} bytes")
