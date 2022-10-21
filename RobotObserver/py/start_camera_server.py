import json
import argparse
import apriltag
import cv2 as cv

from time import sleep
from calib_rect import CalibrationRectangle
from bridge import Bridge
from robot_detector import RobotDetector

parser = argparse.ArgumentParser()
parser.add_argument("settings_path", help="Path to the ascs settings file", type=str)
parser.add_argument("-v", "--verbose", help="Increase output verbosity", action="store_true")
parser.add_argument("-g", "--graphics", help="Show GUI", action="store_true")
parser.add_argument("--camera-idx", help="Index of the camera device to use", type=int, default=0)
parser.add_argument("--detector-rscs-path", help="Path to robot detectors resources", type=str)
args = parser.parse_args()

def main():
    # Load settings
    stg_file = open(args.settings_path, "r")
    settings = json.load(stg_file)["robot-observer"]
    stg_file.close()
    # Extract camera server settings
    server_stg = settings["camera-server"]
    bridge = Bridge(server_stg["host"], server_stg["port"], verbose=args.verbose)
    bridge.start()

    rect_stg = settings["calib"]["rectangle"]
    tag_stg = settings["calib"]["tag"]

    rect = CalibrationRectangle((10, 10), (1010, 10), (1010, 510), (10, 510), 2340, 1120)

    robot_detector = RobotDetector(args.camera_idx, \
        graphics=args.graphics, \
        rscs_path=settings["detector-rscs-path"], \
        tag_family=tag_stg["family"], \
        plane_size=(rect_stg["width"], rect_stg["height"]), \
        calib_rect_tags=(rect_stg["top-left-tag"], rect_stg["top-right-tag"], rect_stg["bottom-left-tag"], rect_stg["bottom-right-tag"]) \
    )
    robot_detector.calibrate_plane()

    while bridge.isConnected():
        robots, img = robot_detector.detect()
        data = json.dumps({"robots": robots})
        bridge.sendLine(data)
        if args.graphics:
            cv.imshow("img", img)
            cv.waitKey(1)

if __name__ == "__main__":
    main()