package ar.wargus.cv2.utils;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_32FC2;

public class ImageProcess {
	private static final Mat emptyMat = new Mat();
	
	public static DMatch[] findGoodMatches(Mat sceneDescriptor,
	                                       Mat objectDescriptor,
	                                       DescriptorMatcher matcher){
		// TODO check if there is detector for many mat's
		List<MatOfDMatch> foundMatches = new ArrayList<>();
		
		matcher.knnMatch(sceneDescriptor,
		                 objectDescriptor,
		                 foundMatches,
		                 2);
		
		return foundMatches.stream()
		                   .map(MatOfDMatch::toArray)
		                   .filter(m -> m[0].distance < 0.7F * m[1].distance)
		                   .map(m -> m[0])
		                   .toArray(DMatch[]::new);
	}
	
	// LOCALS
	// TODO MAYBE use 3D surface from depth_point_cloud request instead?
	public static Mat findHomography(MatOfKeyPoint sceneKeyPoints,
	                                 MatOfKeyPoint objectKeyPoints,
	                                 DMatch[] matches){
		int arraySize = matches.length;
		if(arraySize < 4) return new Mat();
		
		Point[] obj = new Point[arraySize];
		Point[] scene = new Point[arraySize];
		
		KeyPoint[] listOfKeypointsScene = sceneKeyPoints.toArray();
		KeyPoint[] listOfKeypointsObject = objectKeyPoints.toArray();
		for (int i = 0; i < arraySize; i++) {
			//-- Get the keypoints from the good matches
			scene[i] = listOfKeypointsScene[matches[i].queryIdx].pt;
			obj[i] = listOfKeypointsObject[matches[i].trainIdx].pt;
		}
		MatOfPoint2f sceneMat = new MatOfPoint2f();
		sceneMat.fromArray(scene);
		MatOfPoint2f objMat = new MatOfPoint2f();
		objMat.fromArray(obj);
		
		double ransacReprojThreshold = 5.2;
		return Calib3d.findHomography(objMat,
		                              sceneMat,
		                              Calib3d.RANSAC,
		                              ransacReprojThreshold,
		                              emptyMat,
		                              500, // maxIters
		                              0.995); // confidence
	}
	
	public static Mat findObjectCornersOnScene(Mat objectMat,
	                                           Mat objectHomography){
		Mat objCorners = new Mat(4, 1, CV_32FC2);
		Mat sceneCorners = new Mat();
		
		objCorners.put(0, 0, 0, 0);                         // TOP LEFT
		objCorners.put(1, 0, objectMat.cols(), 0);          // TOP RIGHT
		objCorners.put(2, 0, objectMat.cols(), objectMat.rows()); // DOWN RIGHT
		objCorners.put(3, 0, 0, objectMat.rows());          // DOWN LEFT
		
		Core.perspectiveTransform(objCorners,
		                          sceneCorners,
		                          objectHomography);
		
		return sceneCorners;
	}
	
	public static Rect2d getObjectRectFromSceneCorners(Mat sceneCorners){
		float[] sceneCornersData = new float[8];
		sceneCorners.get(0, 0, sceneCornersData);
		
		// find most left, right, top and down points
		float left = sceneCornersData[0];
		float right = sceneCornersData[0];
		float top = sceneCornersData[1];
		float down = sceneCornersData[1];
		
		for (int i = 2; i < 8; i+=2) {
			// X
			left    = sceneCornersData[i] < left    ? sceneCornersData[i] : left;
			right   = sceneCornersData[i] > right   ? sceneCornersData[i] : right;
			
			// Y
			top     = sceneCornersData[i+1] < top   ? sceneCornersData[i+1] : top;
			down    = sceneCornersData[i+1] > down  ? sceneCornersData[i+1] : down;
		}

		return new Rect2d(left,
		                  top,
		                  right-left,
		                  down-top);
	}
	
	public static Mat convertToGray(Mat mYUV_Mat,
	                                int format){
		Mat mGRAY_Mat = new Mat(mYUV_Mat.height(),
		                        mYUV_Mat.width(),
		                        CvType.CV_8UC4);
		Imgproc.cvtColor(mYUV_Mat,
		                 mGRAY_Mat,
		                 format);
		return mGRAY_Mat;
	}
	
	public static Mat getMaskFromRect2D(Mat sceneMat,
	                                    Rect2d rect){
		Mat mask = Mat.zeros(sceneMat.rows(), sceneMat.cols(), sceneMat.type());
		Imgproc.rectangle(mask,
		                  new Point(rect.x,
		                            rect.y),
		                  new Point(rect.x + rect.width,
		                            rect.y + rect.height),
		                  new Scalar(255, 255, 255),
		                  -1);
				
		return mask;
	}
}
