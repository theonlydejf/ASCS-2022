#!/bin/sh

root=$(dirname $0)
cd $root

java -jar SimulationWindow.jar 2360 1140 2111,2222,2333,970,570,90 3111,3222,3333,1390,570,90 cell,10,2250,570,180 cell,11,1180,1030,270
