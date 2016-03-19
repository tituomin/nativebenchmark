#!/bin/bash

FOUND_RE='^Measuring started|^Measuring finished|^Interrupting|[eE]rror'

function send_push_email () {
    while read line; do
        if echo $line | egrep -q "$FOUND_RE"; then
            echo "$(date) - This email was sent from $HOSTNAME. Do not reply." | mail -s "$line" $NOTIFICATION_EMAIL
        fi
    done
}

set -e

adb push nativebenchmark_setup.json /sdcard/
adb shell am start -n fi.helsinki.cs.tituomin.nativebenchmark/.BenchmarkSelector
adb forward tcp:38300 tcp:38300
sleep 1
nc localhost 38300 | tee >(send_push_email)

#start :default_full
#end
#* stopping: adb shell am force-stop fi.helsinki.cs.tituomin.nativebenchmark
#* or: adb shell pm clear com.my.app.package ?
