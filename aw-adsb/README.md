# ADS-B


## Components

1. [adsb-core](adsb-core) - ADSB parser
2. [adsb-ingest](adsb-ingest) - Ingestion from RP sensors


## Kafka Artchitecture Examples

### Cloud Ingestion with Private ADS-B feed sensors

Reference Architecture for small number of ADS-B Feeders (outbound feeds) based on Raspberry Pie dump1090

No customization is needed on RP side. All ingestion flow is handled by Kafka Cluster

<img src="doc/Aeroware-Architecture-adsb-kafka-small.png" width="850">

<br>

### Geo-distributed Decentralized Cloud Ingestion

Reference Architecture for Geodistributed, decentralized ingestion for a very large number of ADS-B producers.

Raspberry Pie uses __sig-proxy__ component is used for flow transformation (inbount to outbound) and private fields inclusion and signing

<img src="doc/Aeroware-Architecture-adsb-kafka-aws.png" width="850">

<br>


----

## Resources

ADS-B: [https://en.wikipedia.org/wiki/Automatic_Dependent_Surveillance%E2%80%93Broadcast](https://en.wikipedia.org/wiki/Automatic_Dependent_Surveillance%E2%80%93Broadcast)




