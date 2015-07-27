#!/bin/sh
#
#Shell Script to execute resource access tools
#

ant clean all
cd dist
chmod 777 run.sh
chmod 777 files/mgrep/mgrep3.0/mgrep

#./run.sh >resource_access_tools_all.log &
#tail -f resource_access_tools_all.log
./run.sh &
