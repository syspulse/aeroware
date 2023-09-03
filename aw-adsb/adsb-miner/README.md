# adsb-miner

Miner prototype

1. Create Blocks of ADSB messages within configurable number/time window
2. Signs (can multisign) block
3. Verifies block signature internally


## Miner

```
./run-miner.sh -f data/flight-1000.raw -o mqtt://localhost:1883

```

Debug:

```
./run-miner.sh -f data/flight-1000.raw -o stdout://

```

### File snapshots

It is possible to save `miner` stream to file with two options:

- `raw://` - saves as raw data (option v1 will save as text, v2 will save as binary stream)
- `hex://` - saves always as hex text (option v1 and option v2 will save as text)

ATTENTION: it is impossible to feed `raw://` (v2) to `validator` because binary stream is not delimited

ATTENTION: __--proto.version__ must be identical both on `miner` and `validator`

Example:

```
./run-miner.sh -f data/flight-1000.raw  --proto.option=0x1 -o raw:// >file-v1.txt
./run-validator.sh -f ../adsb-miner/file-v1.txt --proto.option=0x1
```

```
./run-miner.sh -f data/flight-1000.raw  --proto.option=0x1 -o hex:// >file-v1.txt
./run-validator.sh -f ../adsb-miner/file-v1.txt --proto.option=0x1
```

```
./run-miner.sh -f data/flight-1000.raw  --proto.option=0x2 -o hex:// >file-v2.txt
./run-validator.sh -f ../adsb-miner/file-v2.txt --proto.option=0x2
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


