import cv2 as cv
import apriltag
import json
import math

import numpy as np

from calib_rect import CalibrationRectangle

def _get_calib_dict(dx, dy, dheading):
    return {"dx": dx, "dy": dy, "dheading": dheading}
def _get_robot_dict(id, x, y, heading):
    return {"id": id, "x": x, "y": y, "heading": heading}

class RobotDetector:
    def __init__(self, camera_idx: int, graphics: bool = False, rscs_path: str = "robot_detector.rscs", tag_family: str = "tag25h9", plane_size: tuple = (1, 1), calib_rect_tags: tuple = (1, 2, 3, 4)):
        # Init fields
        self._camera_idx: int = camera_idx
        self._detector: apriltag.Detector = apriltag.Detector(apriltag.DetectorOptions(families=tag_family))
        self._capture: cv.VideoCapture = None
        self._graphics = graphics
        tagA, tagB, tagC, tagD = calib_rect_tags
        self._calib_rect_tags = calib_rect_tags
        self._calib_rect_tagId2CornerTag = {tagA: None, tagB: None, tagC: None, tagD: None}
        self._calib_rect: CalibrationRectangle = None
        self._plane_size = plane_size

        # Load resources
        from os.path import exists
        if(not exists(rscs_path)):
            with open(rscs_path, "w") as f:
                json.dump({"calib": {"robots": {}}}, f)
        
        self._rscs_path = rscs_path
        with open(rscs_path, "r") as rscs_file:
            self._rscs = json.load(rscs_file)

    def calibrate_plane(self, rect: CalibrationRectangle = None) -> bool:
        """
        Initiates a calibration rectangle the current instance of RobotDetector by finding tags, whos ids are defined in calib_rect_tags in __init__
        @return True, when all four calibration rectangle tags were found
        """

        if rect is not None:
            self._calib_rect = rect
            return True

        # Detect tags
        img = self._get_img()
        gray = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
        tags = self._detect_tags(gray)

        # Find corresponding tag to each corner
        for tag in tags:
            if(tag.tag_id in self._calib_rect_tagId2CornerTag):
                self._calib_rect_tagId2CornerTag[tag.tag_id] = tag

        # Get points from tags, if one or more tags weren't found, it will return False
        corners = []
        for id in self._calib_rect_tags:
            tag = self._calib_rect_tagId2CornerTag[id]
            if(tag is None):
                return False

            corners.append(tag.corners[0])
        
        # Create calibration rectangle
        self._calib_rect = CalibrationRectangle(corners[0], corners[1], corners[2], corners[3], \
            self._plane_size[0], self._plane_size[1])
        return True

    def detect(self):
        """
        Detects robots visible to the camera
        @return The following tuple: (array of robots, image from the camera)
        """
        # Detect tags
        img = self._get_img()

        # If calibration rectangle was not created -> do not detect
        if self._calib_rect is None:

            font = cv.FONT_HERSHEY_SIMPLEX
            text = "PLANE NOT CALIBRATED"

            textsize = cv.getTextSize(text, font, 1, 2)[0]
            textX = (img.shape[1] - textsize[0]) / 2
            textY = (img.shape[0] + textsize[1]) / 2

            cv.rectangle(img, (int(textX - 5), int(textY + 5)), \
                (int(textX + textsize[0] + 5), int(textY - textsize[1] - 5)), \
                (255, 255, 255), -1)

            cv.putText(img, text, (int(textX), int(textY)), \
                font, 1, (0, 0, 255), 2)
            
            return [], img

        # Detect AprilTags
        gray = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
        tags = self._detect_tags(gray)

        robots = []
        for tag in tags:
            # Convert tag points to tuples
            center = (int(tag.center[0]), int(tag.center[1]))
            (ptA, ptB, ptC, ptD) = tag.corners
            ptB = (int(ptB[0]), int(ptB[1]))
            ptA = (int(ptA[0]), int(ptA[1]))

            # Calculate heading of the tag (radians)
            top_center = ((ptA[0] + ptB[0]) / 2, (ptA[1] + ptB[1]) / 2)
            dx_ang = top_center[0] - center[0]
            dy_ang = top_center[1] - center[1]
            ang = math.atan2(dy_ang, dx_ang)

            # If robot is not present in the calibration dictionary, give him a default calibration values
            robot_id = str(tag.tag_id)
            if robot_id not in self._rscs["calib"]["robots"]:
                self.calibrate_robot(robot_id, 0, 0, 0)

            # Get calibration values for the current robot
            calib = self._rscs["calib"]["robots"][robot_id]

            # Old way of calibrating robot
            # ratio = math.dist(ptA, ptB) / self._tag_size
            # x = center[0] + math.cos(ang) * calib["dx"] * ratio - math.sin(ang) * calib["dy"] * ratio
            # y = center[1] + math.sin(ang) * calib["dx"] * ratio + math.cos(ang) * calib["dy"] * ratio

            # Calibrate robot using calibration rectangle and offset in rscs
            robot_pos = self._calib_rect.map_point(np.array(center))
            robot_pos += np.array([
                math.cos(ang) * calib["dx"] - math.sin(ang) * calib["dy"],
                math.sin(ang) * calib["dx"] + math.cos(ang) * calib["dy"]
                ])

            # TODO: 
            #   Add low pass filter for position & heading
            #   Fix blinking tags
            x = robot_pos[0]
            y = robot_pos[1]
            heading = math.degrees(ang) + calib["dheading"]

            robots.append(_get_robot_dict(int(tag.tag_id), x, y, heading))

            if(self._graphics):
                ptC = (int(ptC[0]), int(ptC[1]))
                ptD = (int(ptD[0]), int(ptD[1]))

                # Draw the bounding box of the AprilTag detection
                cv.line(img, ptA, ptB, (0, 255, 0), 2)
                cv.line(img, ptB, ptC, (0, 255, 0), 2)
                cv.line(img, ptC, ptD, (0, 255, 0), 2)
                cv.line(img, ptD, ptA, (0, 255, 0), 2)

                # Draw the heading of the AprilTag
                cv.line(img, center, (int(center[0] + 20 * math.cos(ang)), int(center[1] + 20 * math.sin(ang))), (0, 0, 255), 2)

                # Draw the center of the AprilTag
                (cX, cY) = (int(center[0]), int(center[1]))
                cv.circle(img, (cX, cY), 5, (0, 255, 0), -1)
                
                # Draw th ID of the AprilTag
                id = str(tag.tag_id)
                cv.putText(img, id, (int(tag.center[0])-15, int(tag.center[1])), \
                    cv.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

                self._calib_rect.draw(img, (0, 0, 255))

                # Draw calibrated center of a robot
                #cv.circle(img, (int(x), int(y)), 5, (0, 128, 255), -1)
                #heading_rad = math.radians(heading) + math.pi/2
                #cv.line(img, (int(x + math.cos(heading_rad) * 20), int(y + math.sin(heading_rad) * 20)), (int(x - math.cos(heading_rad) * 20), int(y - math.sin(heading_rad) * 20)), (0, 255, 0), 2)
                #cv.line(img, (int(x), int(y)), (int(x + math.cos(heading_rad - math.pi/2) * 20), int(y + math.sin(heading_rad - math.pi/2) * 20)), (0, 255, 0), 2)
                

        return robots, img

    def calibrate_robot(self, robot_id: int, dx: float, dy: float, dheading: float):
        self._rscs["calib"]["robots"][robot_id] = _get_calib_dict(dx, dy, dheading)
        with open(self._rscs_path, "w") as rscs_file:
            json.dump(self._rscs, rscs_file, indent=4)

    def _detect_tags(self, gray) -> list:
        return self._detector.detect(gray)

    def _get_img(self):
        if(self._capture is None):
            self._capture = cv.VideoCapture(self._camera_idx)
        
        ret, img = self._capture.read()

        if not ret:
            return None

        return img
