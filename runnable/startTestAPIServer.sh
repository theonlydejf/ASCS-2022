#!/bin/sh

root=$(dirname $0)
cd $root

java -jar TDNAPITestServer.jar 1111 MovementService