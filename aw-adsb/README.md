# ADS-B


## Components

1. [adsb-core](adsb-core) - ADSB parser
2. [adsb-ingest](adsb-ingest) - Ingestion from RP sensors
3. [adsb-tools](adsb-tools) - Tools to work with ADS-B data (Player)
4. [adsb-view](adsb-view) - Map viewer
5. [adsb-proxy](adsb-proxy) - simple dump1090 proxy
6. [adsb-mesh](adsb-mesh) - decentralized mesh network
7. [adsb-miner](adsb-miner) - Miner
8. [adsb-validator](adsb-validator) - Validator
9. [adsb-radar](adsb-radar) - Radar App Service


<br>

## Demo 

### Running with historical telemetry (from the past)

__adsb-radar__

It is import to specify `--past=-1` to simulate events from the past.

Otherwise API will not return __last__ events from the store (indexed by Timestamp of event for historical queries):

```
GOD=1 ./run-radar.sh server -f tail:///tmp/adsb --past=-1
```

__wscat__ (to see telemetry broadcast)

```
wscat --connect ws://localhost:8080/api/v1/radar/ws
```

__adsb-validator__

By default, validator checks for old data (`--validation` options),
So tolerance should be changed to large value to allow miner jitter:

```
./run-validator.sh -o json:// >>/tmp/adsb --tolerance.ts=10000
```

__adsb-miner-1__ (Miner-1)

It is nice to specify `block.size=1` to have only one update message

```
./run-miner.sh -f data/adsb/flight-100.raw --throttle=500 --keystore.file=keystore/miner-1.json --block.size=1
```

__adsb-miner-2__ (Miner-2)

`--jitter` allows to introduce delta to timestamp of the message to simulate lag or future:

```
./run-miner.sh -f data/adsb/flight-100.raw --throttle=500 --keystore.file=keystore/miner-2.json --block.size=1 --jitter=-150
```



----

## Resources and Credits

* ADS-B: [https://en.wikipedia.org/wiki/Automatic_Dependent_Surveillance%E2%80%93Broadcast](https://en.wikipedia.org/wiki/Automatic_Dependent_Surveillance%E2%80%93Broadcast)
* High Precision ToA: [https://github.com/openskynetwork/dump1090-hptoa](https://github.com/openskynetwork/dump1090-hptoa)
* [https://airplanes.live/opensource-intelligence]



