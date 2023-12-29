# adsb-validator


## Validator

Listen for MQTT Miners and output to stdout:

```
./run-validator.sh validator -f mqtt://localhost:1883

```

Read from saved file (only protocol __v1__) and write (append) to file as json

This is convenient for testing with `aw-radar` via simeple file and not messsage broker with some reasonable delay

`tolerance.ts` allows for timestamps to be in the past by milliseconds.

```
./run-validator.sh -f feed/payload-10.txt -o json:// >>/tmp/adsb --tolerance.ts=10000 --throttle=1000
```

Run without validation:

```
./run-validator.sh --validation=
```

Run without verifying data payload (`payload`) (decoding ADS-B or NOTAM)

```
./run-validator.sh --validation=ts,sig,data,blacklist,blacklist.ip
```

## Miner data

Validated miner data is save to `--datastore` (csv file or parquet) for rewards calculation:

```
./run-validator.sh --datastore=parq://./lake/{yyyy}/{MM}/{dd}/{addr}/data-{HH}-{mm}.parq
```


----

### MQTT Broker nodes

__MQTT Broker__

```
docker run -d -p 1883:1883 --name nanomq nanomq/nanomq:0.5.9
```

__MQTT Subscriber__

```
docker exec -ti  nanomq nanomq sub start -t adsb-topic
```


