#!/bin/bash                                                                                                                                                                                            
CWD=`echo $(dirname $(readlink -f $0))`

# use --file NONE 
$CWD/run.sh --dump1090.host=rp-1 --dump1090.port=30002 --aircraft='.*' #--file 'ADSB-{yyyy-MM-dd'T'HH:mm:ssZ}.log'