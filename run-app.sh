#!/bin/bash                                                                                                                                                                                            
#CWD=`echo $(dirname $(readlink -f $0))`
#cd $CWD

test -e server-cred.sh && source server-cred.sh

SITE=${SITE:-}
if [ "$SITE" != "" ]; then
   SITE="-"${SITE}
fi

APP=${1}
MAIN=${2}
APP_HOME=${APP_HOME:-`pwd`}

shift
shift 

JAR=`ls ${APP_HOME}/target/scala-2.13/*assembly*.jar`
CP="${APP_HOME}/conf/:$JAR"

CONFIG="application${SITE}.conf"

echo "=== Class Path ======================================="
echo "$CP"| sed "s/\:/\n/g"
echo "======================================================"
echo "APP: $APP"
echo "APP_HOME: $APP_HOME"
echo "MAIN: $MAIN"
echo "OPT: $OPT"
echo "ARGS: $@"
echo "Site: ${SITE}"
echo "Config: ${CONFIG}"

echo $CP

pwd

# command:
EXEC="$JAVA_HOME/bin/java -Xss512M -Dconfig.resource=$CONFIG -cp $CP $AGENT $OPT $MAIN $@"
exec $EXEC
