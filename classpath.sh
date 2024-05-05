#!/bin/bash                                                                                                                                                                                            
ROOT=`echo $(dirname $(readlink -f $0))`

if [ "$1" != "" ]; then
    NAME=$1
else
    NAME=`basename $PWD | sed 's/\-/_/g'`
fi

DIR=$PWD

>&2 echo "NAME=$NAME"

pushd $ROOT
#sbt -error ";project $NAME; export dependencyClasspath" 2>/dev/null >$DIR/CLASSPATH
sbt  -error ";project $NAME; export dependencyClasspath" >$DIR/CLASSPATH
popd
