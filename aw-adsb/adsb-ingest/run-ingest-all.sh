#!/bin/bash                                                                                                                                                                                            
CWD=`echo $(dirname $(readlink -f $0))`

$CWD/run.sh --dump1090-host=rp-1 --dump1090-port=30002 --aircraft='.*' --file NONE
