#!/bin/sh

set -e

adb push nativebenchmark_setup.json /sdcard/
adb shell am start -n fi.helsinki.cs.tituomin.nativebenchmark/.BenchmarkSelector
adb forward tcp:38300 tcp:38300
sleep 1
nc localhost 38300

#start :default_full
#end
#* stopping: adb shell am force-stop fi.helsinki.cs.tituomin.nativebenchmark
#* or: adb shell pm clear com.my.app.package ?
