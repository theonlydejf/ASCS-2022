import json
import argparse
import cv2 as cv
import numpy as np

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

    # Extract calibration settings
    rect_stg = settings["calib"]["rectangle"]
    tag_stg = settings["calib"]["tag"]

    rect = CalibrationRectangle((10, 10), (1010, 10), (1010, 510), (10, 510), 2340, 1120)

    robot_detector = RobotDetector(
        args.camera_idx, \
        graphics=args.graphics, \
        rscs_path=settings["detector-rscs-path"], \
        tag_family=tag_stg["family"], \
        tag_size=tag_stg["size"], \
        plane_size=(rect_stg["width"], rect_stg["height"]), \
        tag_margin=rect_stg["tag-margin"], \
        calib_rect_tags=(rect_stg["top-left-tag"], \
            rect_stg["top-right-tag"], \
            rect_stg["bottom-left-tag"], \
            rect_stg["bottom-right-tag"]), \
        id_blacklist=settings["id-blacklist"]
    )

    robot_detector.check_camera_wait()
    bridge.start()
    robot_detector.calibrate_plane()

    cv.imshow("img", np.zeros((100, 100)))
    cv.setWindowProperty("img", cv.WND_PROP_TOPMOST, 1)
    cv.waitKey(1)
    cv.setWindowProperty("img", cv.WND_PROP_TOPMOST, 0)
    cv.setWindowProperty("img", cv.WND_PROP_AUTOSIZE, 0)
    while bridge.isConnected():
        robots, img = robot_detector.detect()
        data = json.dumps({"robots": robots})
        bridge.sendLine(data)
        if args.graphics:
            cv.imshow("img", img)
            cv.waitKey(1)

def _resizeWithAspectRatio(image, width=None, height=None, inter=cv.INTER_AREA):
    dim = None
    (h, w) = image.shape[:2]

    if width is None and height is None:
        return image
    if width is None:
        r = height / float(h)
        dim = (int(w * r), height)
    else:
        r = width / float(w)
        dim = (width, int(h * r))

    return cv.resize(image, dim, interpolation=inter)

if __name__ == "__main__":
    main()