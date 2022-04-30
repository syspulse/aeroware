#!/bin/bash

docker run --rm -d -p 1883:1883 --name nanomq nanomq/nanomq:0.5.9
