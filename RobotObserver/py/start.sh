#!/bin/bash

python3 /Users/david/Documents/MAP/StorageObserver/py/start_camera_server.py /Users/david/Documents/MAP/ascs-settings.json -v -g --camera-idx 0 &
 { sleep 1; java -jar /Users/david/Documents/MAP/StorageObserver/py/observer.jar; }