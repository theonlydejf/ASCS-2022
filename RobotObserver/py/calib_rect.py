import numpy as np
import cv2 as cv

def _lineseg_dists(p, a, b):
    """Cartesian distance from point to line segment

    Edited to support arguments as series, from:
    https://stackoverflow.com/a/54442561/11208892

    Args:
        - p: np.array of single point, shape (2,) or 2D array, shape (x, 2)
        - a: np.array of shape (x, 2)
        - b: np.array of shape (x, 2)
    """
    # normalized tangent vectors
    d_ba = b - a
    d = np.divide(d_ba, (np.hypot(d_ba[:, 0], d_ba[:, 1])
                        .reshape(-1, 1)))

    # signed parallel distance components
    # rowwise dot products of 2D vectors
    s = np.multiply(a - p, d).sum(axis=1)
    t = np.multiply(p - b, d).sum(axis=1)

    # clamped parallel distance
    h = np.maximum.reduce([s, t, np.zeros(len(s))])

    # perpendicular distance component
    # rowwise cross products of 2D vectors  
    d_pa = p - a
    c = d_pa[:, 0] * d[:, 1] - d_pa[:, 1] * d[:, 0]

    return np.hypot(h, c)

class CalibrationRectangle:

    def __init__(self, topLeft, topRight, bottomRight, bottomLeft, width, height) -> None:
        self.a = topLeft
        self.b = topRight
        self.c = bottomRight
        self.d = bottomLeft
        self.w = width
        self.h = height

        self._corners1 = np.array([self.a, self.b, self.c, self.d])
        self._corners2 = np.array([self.b, self.c, self.d, self.a])

    def map_point(self, p):
        dists = _lineseg_dists(p, self._corners1, self._corners2)
        sx = (dists[3] / (dists[1] + dists[3])) * self.w 
        sy = (dists[0] / (dists[0] + dists[2])) * self.h
        return np.array([sx, sy])

    def draw(self, img, color):
        pts = self._corners1
        cv.polylines(img, np.int32([pts]), True, color, 1)