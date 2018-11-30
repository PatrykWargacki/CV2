package ar.wargus.cv2.openCV;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.TrackerKCF;

import java.util.Map;

import ar.wargus.cv2.entity.ObjectData;
import ar.wargus.cv2.utils.ImageProcess;

/*
Znajdz podane obiekty na pełnej scenie
Przypisz im miejsce rect
Oraz zainicjalizuj im Tracker
 */
public class ObjectFinderAsyncTask extends AsyncTask<Void,
		                                             Void,
		                                             Void> {
	
	private final Mat sceneImage;
	private final Map<ObjectData, Boolean> mapReference;
	private final ObjectData[] objectDatas;
	
	ObjectFinderAsyncTask(Mat sceneImage,
	                      ObjectData[] objectDatas,
	                      Map<ObjectData, Boolean> mapReference) {
		this.sceneImage     = sceneImage;
		this.objectDatas    = objectDatas;
		this.mapReference   = mapReference;
	}
	
	private MatOfKeyPoint   sceneKeyPoints      = new MatOfKeyPoint();
	private Mat             sceneDescriptors    = new Mat();
	private MatOfKeyPoint   objectKeyPoints     = new MatOfKeyPoint();
	private Mat             objectDescriptors   = new Mat();
	@Override
	protected Void doInBackground(Void... voids) {
		
		InternalImageProcess.detector.detect(sceneImage,
		                                     sceneKeyPoints,
		                                     InternalImageProcess.emptyMat);
		
		InternalImageProcess.extractor.compute(sceneImage,
		                                       sceneKeyPoints,
		                                       sceneDescriptors);
		
		for (int i = 0; i < objectDatas.length; i++) {
			ObjectData objectData = objectDatas[i];
			
			// GET object's KeyPoints
			InternalImageProcess.detector.detect(objectData.image,
			                                     objectKeyPoints,
				                                 InternalImageProcess.emptyMat);
			// GET object's Descriptor
			InternalImageProcess.extractor.compute(objectData.image,
			                                       objectKeyPoints,
			                                       objectDescriptors);
			
			DMatch[] matches = ImageProcess.findGoodMatches(sceneDescriptors,
			                                                objectDescriptors,
			                                                InternalImageProcess.matcher);
			
			Mat homography = ImageProcess.findHomography(sceneKeyPoints,
			                                             objectKeyPoints,
			                                             matches);
			
			// CHECK FCKING HOMO
			if(homography.empty()) {
				System.out.println("ObjectFinder: NO object homography: " + objectData.id);
				mapReference.remove(objectData);
				continue;
			}
			Mat sceneCorners = ImageProcess.findObjectCornersOnScene(objectData.image,
			                                                         homography);
			//TODO sprawdz czy ratio się zgadza od scene corners
			objectData.rect = ImageProcess.getObjectRectFromSceneCorners(sceneCorners);
			if(objectData.rect.empty()){
				System.out.println("ObjectFinder: NO object rect: " + objectData.id);
				mapReference.remove(objectData);
				continue;
			}

			mapReference.put(objectData, false);
			
			// sprawdź czy obiekt ma swój tracker
			// TAK, zaktualizuj o położenie
			// NIE, dodaj
			if(objectData.tracker == null) objectData.tracker = org.opencv.tracking.TrackerMOSSE.create();
			else                           objectData.tracker.clear();

			objectData.tracker.init(sceneImage,
			                        objectData.rect);
			
			System.out.println("ObjectFinder: Found object: " + objectData.id
			                  + " on scene, at position " + objectData.rect);
		}
		return null;
	}
}
