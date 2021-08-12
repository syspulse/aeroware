#!/bin/bash                                                                                                                                                                                            
CWD=`echo $(dirname $(readlink -f $0))`
cd $CWD

JAR=`ls ./target/scala-2.13/adsb-ingest-assembly-*`
CP="`pwd`/conf/:$JAR"

echo "CP=$CP"| sed "s/\:/\n/g"

# command:                                                                                                                                                                                             
EXEC="$JAVA_HOME/bin/java -Xss512M -cp $CP $AGENT $OPT com.syspulse.avia.adsb.Ingest $@"
exec $EXEC