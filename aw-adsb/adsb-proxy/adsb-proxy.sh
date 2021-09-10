#!/bin/bash
# SK file will be cached in memory, so access should be pretty fast
# Notes: due to OpenSSL peculiarity, signature is r:s with 0 suffix to fix possible negative point

RP_HOST=${1:-rp-1}
RP_PORT=${2:-30002}

SK_HOME=${SK_HOME:-.}

export SK_FILE=${3:-$SK_FILE}

while read data
do
   #echo "raw: $data"
   ts=`echo $(($(date +%s%N)/1000000))`
   sig=`echo $data | ${SK_HOME}/sig-data-sign.sh | paste - - -|awk '{print $13,$20}' | awk -F':' '{print "0"substr($2,1,length($2)-1)":""0"$3}'`
   echo "$data $ts $sig"
done < <(nc -q -1 $RP_HOST $RP_PORT)
