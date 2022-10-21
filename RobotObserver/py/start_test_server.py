import json
from bridge import Bridge
import json

SETTINGS_PATH = "/Users/david/Documents/MAP/ascs-settings.json"

stg_file = open(SETTINGS_PATH, "r")
settings = json.load(stg_file)["storage-observer"]
stg_file.close()
# Extract camera server settings
server_stg = settings["camera-server"]

while True:
    bridge = Bridge(server_stg["host"], server_stg["port"], verbose=True)
    bridge.start()

    while bridge.isConnected():
        data = json.dumps({"robots": [{"heading":45.0,"x":100.0,"y":200.0,"id":10}, {"heading":75.0,"x":50.0,"y":100.0,"id":11}]})
        bridge.sendLine(data)
        print("SENT: " + data)

    bridge.close()