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

>&2 echo "=== Class Path ======================================="
>&2 echo "$CP"| sed "s/\:/\n/g"
>&2 echo "======================================================"
>&2 echo "APP: $APP"
>&2 echo "APP_HOME: $APP_HOME"
>&2 echo "MAIN: $MAIN"
>&2 echo "OPT: $OPT"
>&2 echo "ARGS: $@"
>&2 echo "Site: ${SITE}"
>&2 echo "Config: ${CONFIG}"

>&2 echo $CP
>&2 pwd

# command:
EXEC="$JAVA_HOME/bin/java -Xss512M -Dcolor -Dconfig.resource=$CONFIG -cp $CP $AGENT $OPT $MAIN $@"
exec $EXEC
