#!/bin/sh
rm lib/.DS_Store
scp -oKexAlgorithms=+diffie-hellman-group1-sha1 -c 3des-cbc -r lib/* root@$1:/home/lejos/lib/