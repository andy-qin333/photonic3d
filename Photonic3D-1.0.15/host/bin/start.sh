#!/bin/bash

if [[ $UID != 0 ]]; then
    echo "Please run this script with sudo:"
    echo "sudo $0 $*"
    exit 1
fi

cpu=`uname -m`

cd /opt/cwh

#This application will always need to have the display set to the following
export DISPLAY=:0.0
xinitProcess=`ps -ef | grep grep -v | grep xinit`
if [ -z "${xinitProcess}" ]; then
    echo No X server running, starting and configuring one
    startx &
    xhost +x
fi

if [ "$1" == "debug" ]; then
	pkill -9 -f "org.area515.resinprinter.server.Main"
	echo "Starting printer host server($2)"
	java -Xmx512m -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -Dlog4j.configurationFile=debuglog4j2.properties -Djava.library.path=/usr/lib/jni:os/Linux/${cpu} -cp lib/*:. org.area515.resinprinter.server.Main > log.out 2> log.err &
elif [ "$1" == "TestKit" ]; then
	pkill -9 -f "org.area515.resinprinter.test.HardwareCompatibilityTestSuite"
	echo Starting test kit
	java -Xmx512m -Dlog4j.configurationFile=testlog4j2.properties -Djava.library.path=/usr/lib/jni:os/Linux/${cpu} -cp lib/*:. org.junit.runner.JUnitCore org.area515.resinprinter.test.HardwareCompatibilityTestSuite &
else
	pkill -9 -f "org.area515.resinprinter.server.Main"
	echo Starting printer host server
	java -Xmx512m -Dlog4j.configurationFile=log4j2.properties -Djava.library.path=/usr/lib/jni:os/Linux/${cpu} -cp lib/*:. org.area515.resinprinter.server.Main > log.out 2> log.err &
fi
