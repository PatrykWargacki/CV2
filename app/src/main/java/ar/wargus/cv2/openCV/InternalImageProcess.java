package ar.wargus.cv2.openCV;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Size;
import android.util.SparseArray;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect2d;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.BRISK;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.FlannBasedMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.MultiTracker;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerKCF;
import org.opencv.xfeatures2d.SURF;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ar.wargus.cv2.entity.ObjectData;
import ar.wargus.cv2.utils.ImageProcess;

public class InternalImageProcess {
	static final Mat emptyMat = new Mat();
	
	// DETECTOR
//	public static final Feature2D detector = SURF.create();
//	public static final Feature2D fastDetector = ORB.create();
	public static final Feature2D detector = ORB.create(10000,
	                                                    1.4f,
	                                                    8,
	                                                    31,
	                                                    0,
	                                                    2,
	                                                    ORB.HARRIS_SCORE,
	                                                    31,
	                                                    20);
	public static final Feature2D fastDetector = ORB.create(1000,
	                                                        1.2f,
	                                                        8,
	                                                        31,
	                                                        0,
	                                                        2,
	                                                        ORB.FAST_SCORE,
	                                                        31,
	                                                        20);
	
	// DESCRIPTOR EXTRACTOR
//	public static final Feature2D extractor = SURF.create();
//	public static final Feature2D fastExtractor = BRISK.create();
	public static final Feature2D extractor = detector;
	public static final Feature2D fastExtractor = fastDetector;
	
	// MATCHER
	public static final DescriptorMatcher matcher = org.opencv.features2d.BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING,
	                                                                                       false);
//	public static final DescriptorMatcher matcher = FlannBasedMatcher.create(FlannBasedMatcher.FLANNBASED);
	public static final DescriptorMatcher fastMatcher = FlannBasedMatcher.create(FlannBasedMatcher.BRUTEFORCE_HAMMING);
	
	
//	public static final MultiTracker multiTracker = MultiTracker.create();
//	// tracker
//	public static final Tracker tracker = TrackerKCF.create();
	
	public InternalImageProcess(Size bufferSize){
//		this.bufferSize = bufferSize;

//		detector = ORB.create();
//		extractor = BRISK.create();
//		matcher = FlannBasedMatcher.create(FlannBasedMatcher.BRUTEFORCE_HAMMINGLUT);
//		multiTracker = MultiTracker.create();
//		tracker = TrackerKCF.create();
		
	}
	
	/*
	Processed Mats to Descriptors for user area
	 */
	// LOCALS
	private static final String globalDescriptorsPath = "descriptors"+File.separator+"global";
	// RETURNS
	private final SparseArray<ObjectData> ObjectDatas = new SparseArray<>();
	public SparseArray<ObjectData> loadDescriptors(AssetManager manager){
		try {
			String[] filenames = manager.list(globalDescriptorsPath);
			
			for (int i = 0; i < filenames.length; i++) {
				InputStream inputStream = manager.open(globalDescriptorsPath
						                                       + File.separator
						                                       + filenames[i]);
				Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
				Mat mat = new Mat(bitmap.getHeight(),
				                  bitmap.getWidth(),
				                  CvType.CV_8UC1);
				Utils.bitmapToMat(bitmap, mat);
				
				bitmap.recycle();
				
				mat = ImageProcess.convertToGray(mat, Imgproc.COLOR_RGB2GRAY);
				
				ObjectData objectData = new ObjectData(Integer.parseInt(filenames[i].substring(0,
				                                                                               filenames[i].indexOf('.'))),
										               mat,
										               new MatOfKeyPoint(),
										               new Mat(),
										               new MatOfKeyPoint(),
										               new Mat());
				
				detector.detect(objectData.image,
				                objectData.keyPoints,
					            emptyMat);
				extractor.compute(mat,
				                  objectData.keyPoints,
				                  objectData.descriptors);
				
				fastDetector.detect(objectData.image,
				                    objectData.fastKeyPoints,
				                    emptyMat);
				fastExtractor.compute(objectData.image,
				                      objectData.fastKeyPoints,
				                      objectData.fastDescriptors);
				
				ObjectDatas.append(objectData.id,
				                   objectData);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			return ObjectDatas;
		}
	}
	
	// LOCALS
	private static final ObjectData sceneData = new ObjectData(0,
	                                                           new Mat(),
			                                                   new MatOfKeyPoint(),
			                                                   new Mat(),
			                                                   new MatOfKeyPoint(),
			                                                   new Mat());
	private final Map<ObjectData, Boolean> objectFinderHealthCheck = new HashMap<>();
	private final List<ObjectData> objectsToFind = new ArrayList<>();
	private final SparseArray<ObjectData> newObjectDatas = new SparseArray<>();
	// RETURNS
	private final SparseArray<Rect2d> objectsToDraw = new SparseArray<>();
	public SparseArray<Rect2d> process(Mat sceneMat){
		updateSceneData(sceneMat);
		updateObjectTrackers(sceneMat, objectsToDraw);
		
		// TYLKO NOWE OBIEKTY
		for (int i = 0; i < newObjectDatas.size(); i++) {
			ObjectData object = newObjectDatas.valueAt(i);
			
			Boolean finderIsAlive = objectFinderHealthCheck.get(object);
			// jeśli null; stwórz
			if(finderIsAlive == null) {
				System.out.println("FIND object: " + object.id);
				// wykryj obiekt na całej scenie
				objectsToFind.add(object);
				// dodaj obiekt do healthcheck'a
				objectFinderHealthCheck.put(object, true);
				
				continue;
			// jeśli żywy; pomiń
			}else if(finderIsAlive) {
				continue;
				
			// jeśli martwy; usuń i dodaj do śledzenia
			}else {
				System.out.println("TRACK object: " + object.id);
				newObjectDatas.removeAt(i);
				i--;
				objectFinderHealthCheck.remove(object);

				if(!trackedObjects.contains(object))
					trackedObjects.add(object);
				continue;
			}
		}
		
		if(!objectsToFind.isEmpty()){
			// nowy AsyncTask do znalezeinia na scenie obiektu i jego lokazlizacji jako maska
			new ObjectFinderAsyncTask(sceneData.image,
			                          objectsToFind.toArray(new ObjectData[0]),
			                          objectFinderHealthCheck).execute();
			objectsToFind.clear();
		}
		
		return objectsToDraw;
	}
	
	public void addNewObjectDatas(SparseArray<ObjectData> newObjectDatas){
		for (int i = 0; i < newObjectDatas.size(); i++) {
			ObjectData objectData = newObjectDatas.valueAt(i);
			this.newObjectDatas.put(objectData.id,
			                        objectData);
		}
	}
	
	private ObjectData updateSceneData(Mat sceneMat){
		sceneMat = ImageProcess.convertToGray(sceneMat,
		                                      Imgproc.COLOR_YUV2GRAY_420);
		Core.flip(sceneMat.t(), sceneData.image, 90);
		return sceneData;
	}
	
	// TODO osobny wątek na śledzenie?
	private final List<ObjectData> trackedObjects = new ArrayList<>();
	private final List<ObjectData> trackedObjectsToRemove = new ArrayList<>();
	private synchronized void updateObjectTrackers(Mat sceneMat,
	                                               SparseArray<Rect2d> objectsToDraw){
		actualizeTrackedObjects();
		
		for (int i = 0; i < trackedObjects.size(); i++) {
			ObjectData objectData = trackedObjects.get(i);
			objectData.tracker.update(sceneMat, objectData.rect);
			
			objectsToDraw.put(objectData.id, objectData.rect);
			System.out.println("TRACKED object: " + objectData.id + "  RECT: " + objectData.rect);
		}
	}
	private synchronized void actualizeTrackedObjects(){
		trackedObjects.removeAll(trackedObjectsToRemove);
		trackedObjectsToRemove.clear();
	}
	
	/*
	Sprawdzaj co jakiś czas czy tracker nie zgubił obiektu
	Jeśli na teoretycznym miejscu położenia obiektu nie można go znaleźć X razy
	usuń tracker
	usuń rect
	 */
	private final SparseArray<ObjectData> objectsToReFind = new SparseArray<>();
	public void startTimerTask() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
			
			for (int i = 0; i < trackedObjects.size(); i++) {
				ObjectData objectData = trackedObjects.get(i);
				System.out.println("Find tracked object: " + objectData.id);
				if(objectData.rect == null
				   || objectData.rect.empty()) {
					sendToFind(objectData);
					continue;
				}
				
				fastDetector.detect(sceneData.image,
				                    sceneData.fastKeyPoints,
				                    ImageProcess.getMaskFromRect2D(sceneData.image,
				                                                   objectData.rect));
				
				fastExtractor.compute(sceneData.image,
				                      sceneData.fastKeyPoints,
				                      sceneData.fastDescriptors);
				
				DMatch[] matches = ImageProcess.findGoodMatches(sceneData.fastDescriptors,
				                                                objectData.fastDescriptors,
				                                                fastMatcher);
				
				Mat homography = ImageProcess.findHomography(sceneData.fastKeyPoints,
				                                             objectData.fastKeyPoints,
				                                             matches);
				
				// nie znaleziono położenia obiektu
				if(homography.empty()) {
					if(objectData.trackerFailure < 3) {
						System.out.println("TimerTask: Could not find tracked object " + objectData.id
								           + " tried: " + objectData.trackerFailure + " times");
						objectData.trackerFailure++;
					}else{
						objectData.trackerFailure = 0;
						sendToFind(objectData);
					}
					continue;
				}
			}
			
			addNewObjectDatas(objectsToReFind);
			objectsToReFind.clear();
			}
			
			private synchronized void sendToFind(ObjectData objectData){
				System.out.println("TimerTask: Could not find object: " + objectData.id);
				objectData.tracker.clear();
				objectData.rect = null;
				
				objectsToReFind.put(objectData.id, objectData);
				
				trackedObjectsToRemove.add(objectData);
			}
		},
		0,
		1000);//Update every second
	}
}
