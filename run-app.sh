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

shift
shift

#ARGS="$@"
ARGS=$@

APP_HOME=${APP_HOME:-`pwd`}

JAR=`ls ${APP_HOME}/target/scala-2.13/*assembly*.jar`
JAR_UNFAT=`ls ${APP_HOME}/lib/*.jar`
CP="${APP_HOME}/conf/:$JAR:$JAR_UNFAT"

CONFIG="application${SITE}.conf"

MEM=${MEM:-1G}
STACK=${STACK:-512M}

>&2 echo "=== Class Path ======================================="
>&2 echo "$CP"| sed "s/\:/\n/g"
>&2 echo "======================================================"
>&2 echo "APP: $APP"
>&2 echo "APP_HOME: $APP_HOME"
>&2 echo "MAIN: $MAIN"
# to be compatibble with old scripts (to be deprecated)
>&2 echo "OPT: $OPT"
>&2 echo "JAVA_OPTS: $JAVA_OPTS"
>&2 echo "ARGS: $ARGS"
>&2 echo "SITE: ${SITE}"
>&2 echo "CONFIG: ${CONFIG}"
>&2 echo "MEM: ${MEM}"
>&2 echo "STACK: ${STACK}"

>&2 echo $CP
>&2 pwd

# command:
# JAVA_OPTS should be overriden by old script parameters like $OPT

# WARNING: Experiment no-globbing !
# WARNING: $ARGS must not be quoted or it will squash multiple arguments from the launch script.
# NOTE: I think the only thing which is extremely stupid in Linux/Unix is SH/BASH 
set -o noglob
exec $JAVA_HOME/bin/java -Xss${STACK} -Xms${MEM} -Xmx${MEM} $JAVA_OPTS -Dcolor -Dconfig.resource=$CONFIG -cp $CP $AGENT $OPT $MAIN $ARGS
set +o noglob
