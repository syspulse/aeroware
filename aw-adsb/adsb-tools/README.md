# adsb-tools

1. adsb-player - plays ADS-B message through specified processors pipeline


## adsb-player

```
run-player.sh [flow]
```

__Supported Flow Blocks (pipes)__

* <file.csv> - read from CSV file (use '/dev/stdin' for shell pipes)
* <file.adsb> - read from ADSB file (dump1090 style)
* <ws://host:port> - Send to Websocket connected clients
* sleep(interval) - sleep for specified msec interval (between events)
* delay - delay events based on event timestamp (replay real time intervals)
* repeat(count) - repeat all flow *count* of times
* stdout - print events to stdout
* radar - collect events in Radar 
* position - show positions (for investigation)
* <tcp://host:port> - Emulate dump1090 (Tcp listener sending feed to connected clients)

### Examples

Decode from dump1090 format and print to stdout
```
./run-player.sh ./data/flight-1.adsb stdout
```

Decode from dump1090 format and emulate dump1090 by accepting tcp clients and sending feed to them with 1 msg/sec
```
./run-player.sh ./data/flight-1.adsb "sleep(1000)" tcp://0.0.0.0:30002 repeat
```

Play recorded flight, print decoded, broadcast to websocket clients with delay between events and repeat 10 times all flow
```
./run-player.sh ./data/flight-1.csv stdout "ws://0.0.0.0:30000" delay "repeat(10)"
```

Read from stdin any data and try to parse it
```
cat flight-2.adsb | ./run-player.sh '/dev/stdin' print"
```

Play from recorded flight and track telemetry every 1 second
```
./run-player.sh ./data/flight-1.adsb radar(1000)"
```

Play directly from dump1090, track all and broadcast to websocket clients
```
nc rp-1 30002 | ./run-player.sh '/dev/stdin' radar "ws://0.0.0.0:30000"
```
Note: ws:// is also __radar__ internally - it is just nice to see the same telemetry pushed to Websocket printed


