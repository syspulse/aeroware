# adsb-validator


## Validator

```
./run-validator.sh validator -f mqtt://localhost:1883

```


### MQTT Broker nodtes

__MQTT Broker__

```
docker run -d -p 1883:1883 --name nanomq nanomq/nanomq:0.5.9
```

__MQTT Subscriber__

```
docker exec -ti  nanomq nanomq sub start -t adsb-topic
```


