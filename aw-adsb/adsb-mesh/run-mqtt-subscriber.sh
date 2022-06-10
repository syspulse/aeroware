#!/bin/bash
# Run only after running broker
docker exec -ti  nanomq nanomq sub start -t adsb-topic
