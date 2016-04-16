#!/bin/bash

FINISHED_RE='^Measuring finished|^Interrupting|[eE]rror'
FOUND_RE="^Measuring started|$FINISHED_RE"

if [[ $1 == "shutdown" ]]; then
   echo "Got shutdown request."
   SHUTDOWN_ON_END=true
fi


function send_push_email () {
    while read line; do
        if echo $line | egrep -q "$FOUND_RE"; then
            echo "$(date) - This email was sent from $HOSTNAME. Do not reply." | mail -s "$line" $NOTIFICATION_EMAIL
        fi
    done
}

function potentially_shutdown () {
    if [ "$SHUTDOWN_ON_END" = true ]; then
        while read line; do
            if echo $line | egrep -q "$FINISHED_RE"; then
                sudo poweroff
            fi
        done
    fi
}

set -e

echo "Pushing nativebenchmark_setup.json to device."
adb push nativebenchmark_setup.json /sdcard/
echo
adb shell am start -n fi.helsinki.cs.tituomin.nativebenchmark/.BenchmarkSelector
adb forward tcp:38300 tcp:38300
sleep 1
nc localhost 38300 | tee >(send_push_email) >(potentially_shutdown)

#start :default_full
#end
#* stopping: adb shell am force-stop fi.helsinki.cs.tituomin.nativebenchmark
#* or: adb shell pm clear com.my.app.package ?
