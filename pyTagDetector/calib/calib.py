import numpy as np
import cv2 as cv
import glob
import os

chess_w = 7
chess_h = 6

# termination criteria
criteria = (cv.TERM_CRITERIA_EPS + cv.TERM_CRITERIA_MAX_ITER, 30, 0.001)
# prepare object points, like (0,0,0), (1,0,0), (2,0,0) ....,(6,5,0)
objp = np.zeros((chess_h*chess_w,3), np.float32)
objp[:,:2] = np.mgrid[0:chess_w,0:chess_h].T.reshape(-1,2)
# Arrays to store object points and image points from all the images.
objpoints = [] # 3d point in real world space
imgpoints = [] # 2d points in image plane.

os.chdir("/Users/david/Documents/MAP/pyTagDetector/calib/old")
images = glob.glob('*.jpg')
print("===============")
print(images)

cap = cv.VideoCapture(0)

good_cnt = 0
#for fname in images:
while good_cnt < 10:
    img_ret, img = cap.read()
    gray = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
    # Find the chess board corners
    ret, corners = cv.findChessboardCorners(gray, (chess_w,chess_h), None)
    # If found, add object points, image points (after refining them)
    if ret == True:
        objpoints.append(objp)
        corners2 = cv.cornerSubPix(gray,corners, (11,11), (-1,-1), criteria)
        imgpoints.append(corners)
        # Draw and display the corners
        cv.drawChessboardCorners(img, (chess_w,chess_h), corners2, ret)
        cv.imshow('img', img)
        cv.setWindowProperty("img", cv.WND_PROP_TOPMOST, 1)
        cv.waitKey(1000)
        good_cnt += 1

cv.destroyAllWindows()

print("-------------")
print(imgpoints[0].shape, objpoints[0].shape)
ret, mtx, dist, rvecs, tvecs = cv.calibrateCamera(objpoints, imgpoints, gray.shape[::-1], None, None)

ret_img, img = cap.read() #cv.imread(images[-1])
h,  w = img.shape[:2]
newcameramtx, roi = cv.getOptimalNewCameraMatrix(mtx, dist, (w,h), 1, (w,h))

print("camera mtx:")
print(newcameramtx)

# undistort
dst = cv.undistort(img, mtx, dist, None, newcameramtx)

cv.imshow("undistorted", dst)
cv.waitKey(0)