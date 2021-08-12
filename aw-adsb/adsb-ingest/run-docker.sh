#!/bin/bash                                                                                                                                                                                            
CWD=`echo $(dirname $(readlink -f $0))`
cd $CWD

t=`pwd`;
APP=`basename "$t"`
CONF=`echo $APP | awk -F"-" '{print $2}'`

export SITE=${SITE:-$CONF}

DOCKER="syspulse/${APP}:latest"

echo "app: $APP"
echo "site: $SITE"
echo "main: $MAIN"
echo "docker: $DOCKER"

docker run --rm --name $APP -p 8080:8080 -v `pwd`/conf:/app/conf -v /mnt/share/data/adsb/data:/data $DOCKER $@
