# adsb-tools

adsb-tool pipes ADS-B message through specified processors

```
run-flow.sh [flow]
```

__Supported Flow Blocks (pipes)__

* <file.csv> - read from CSV file
* <file.adsb> - read from ADSB file (dump1090 style)
* <ws://host:port> - Send to Websocket connected clients
* sleep(interval) - sleep for specified msec interval (between events)
* delay - delay events based on event timestamp (replay real time intervals)
* repeat(count) - repeat all flow *count* of times
* stdout - print events to stdout

### Examples

Decode from dump1090 format and print to stdout
```
./run-flow.sh ./data/flight-1.adsb stdout
```

Decode from CSV, print, broadcast to websockets, delay between events and repeat 10 times all flow
```
./run-flow.sh ./data/flight-1.csv stdout "ws://0.0.0.0:30000" delay "repeat(10)"
```

Read from multiple files, broadcast to websockets, sleep for 1 sec and repeat indefinitely
```
./run-flow.sh flight-1.csv flight-2.adsb "ws://0.0.0.0:30000" "sleep(1000) repeat"
```

