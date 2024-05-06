HEX=${1:-43C208}
OUTPUT=${2:-}
SLEEP=${SLEEP:-5}

if [ "$OUTPUT" == "" ]; then
   OUTPUT="/tmp/${HEX}.json"
fi

while [ true ]; do
   r=`curl -s https://api.airplanes.live/v2/hex/$HEX`
   code=`echo $r| jq -r .ac[0].hex`
   echo "code=${code}"
   if [ "$code" != "null" ]; then
      echo $r >>$OUTPUT
   else
      break      
   fi
   lines=`wc -l $OUTPUT`
   echo "${lines}"
   sleep $SLEEP
done
