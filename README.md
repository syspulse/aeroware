# Aeroware

Aviation Platform Research

- [__aw-core__](aw-core)               - Core Entities and Objects
- [__aw-aircraft__](aw-aircraft)       - Aircraft (icao spec)
- [__aw-adsb__](aw-adsb)               - ADS-B mesh (to be moved to `aw`)
- [__aw-gpx__](aw-gpx)                 - GPX tools
- [__aw-nft__](aw-nft)                 - Experiments with NFT as Aeroware entity (Hardware/plane, License)
- [__aw-geo__](aw-geo)                 - Geo snippets
- [__aw-data__](aw-data)               - Datasets (ICAO Aircrafts DB,...)
- [__aw-gamet__](aw-gamet)             - GAMET tools
- [__aw-notam__](aw-notam)             - NOTAM tools
- [__aw-metar__](aw-metar)             - METAR tools
- [__aw-sigmet__](aw-sigmet)           - SIGMET tools

---

## Archticture

### Aeroware Mesh

High Level Overview

<img src="doc/Aeroware-Mesh-Overview.drawio.png" width="850">


---

### PoC

Simple Topology:

[__adsb-miner__] --[MQTT]--> [__adsb-validator__] --[file]--> [__adsb-radar__] <--[http]-- [__wscat__]

<img src="doc/Aeroware-Mesh-PoC.drawio.png" width="650">

<br>

See [aw-adsb/README.md]() for details

----

## Credits

I'm trying to credit all libs and inspiration projects (where I study code or architecture and reimplement). If something is missing, I will update immediately at request

- Fast Parser: [https://github.com/com-lihaoyi/fastparse](https://github.com/com-lihaoyi/fastparse)
- Bits Parser: [http://scodec.org/](http://scodec.org/) 
- XML Bindings (sbt plugin): [https://scalaxb.org/running-scalaxb](https://scalaxb.org/running-scalaxb)
- XML streaming: [https://www.scalawilliam.com/xml-streaming-for-scala/](https://www.scalawilliam.com/xml-streaming-for-scala/)
- dump1090: [https://github.com/antirez/dump1090](https://github.com/antirez/dump1090)
- Flightaware: Most inspiring project [https://github.com/flightaware/dump1090/blob/master/tools/README.aircraft-db.md](https://github.com/flightaware/dump1090/blob/master/tools/README.aircraft-db.md)
