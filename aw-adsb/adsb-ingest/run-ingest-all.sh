#!/bin/bash                                                                                                                                                                                            
CWD=`echo $(dirname $(readlink -f $0))`

OUTPUT=${OUTPUT:-/mnt/share/data/adsb/raw}

# use --file NONE 
$CWD/run-ingest.sh -f dump1090://rp-1:30002 --aircraft='.*' -o "file://${OUTPUT}/adsb-{yyyy}-{MM}-{dd}.dump1090" --format=raw