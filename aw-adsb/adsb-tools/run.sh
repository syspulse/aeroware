#!/bin/bash                                                                                                                                                                                            
CWD=`echo $(dirname $(readlink -f $0))`
cd $CWD
export APP_HOME=`pwd`

t=`pwd`;
APP=`basename "$t"`
CONF=`echo $APP | awk -F"-" '{print $2}'`

export SITE=${SITE:-$CONF}

MAIN=io.syspulse.aeroware.adsb.tools.AppFlow

echo "app: $APP"
echo "site: $SITE"
echo "main: $MAIN"

cd ..
exec ../run-app.sh $APP $MAIN $@
