# adsb-miner

Miner prototype

1. Create Blocks of ADSB messages within configurable number/time window
2. Signs (can multisign) block
3. Verifies block signature internally


## Validator

```
./run-validator.sh validator -f mqtt://localhost:1883

```

## Miner

```
./run-miner.sh -f data/flight-1000.dump1090 -o mqtt://localhost:1883

```

### MQTT Broker nodes

__MQTT Broker__

```
docker run -d -p 1883:1883 --name nanomq nanomq/nanomq:0.5.9
```

__MQTT Subscriber__

```
docker exec -ti  nanomq nanomq sub start -t adsb-topic
```


