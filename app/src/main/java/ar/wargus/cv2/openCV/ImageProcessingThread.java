package ar.wargus.cv2.openCV;

import android.util.SparseArray;

import org.opencv.core.Mat;
import org.opencv.core.Rect2d;

import java.lang.ref.WeakReference;

import ar.wargus.cv2.entity.ObjectData;
import ar.wargus.cv2.views.ARView;

// Osobny wątek na przetwarzanie obrazu aby nie opóźniać podglądu z kamery
public class ImageProcessingThread extends Thread {
	
	private WeakReference<ARView> arView;
	private InternalImageProcess imageProcess;
	
	private Mat awaitingMat;
	
	public ImageProcessingThread(InternalImageProcess imageProcess,
	                             WeakReference<ARView> arView){
		this.imageProcess = imageProcess;
		this.arView = arView;
	}
	
	private void preRun(){
		imageProcess.startTimerTask();
	}
	
	@Override
	public void run() {
		preRun();
		while(true) {
			if (awaitingMat     == null
			    || arView.get() == null) continue;
			Mat processingMat = awaitingMat;
			awaitingMat = null;
			
			SparseArray<Rect2d> objectsToDraw = imageProcess.process(processingMat);
			arView.get().setObjectsToDraw(objectsToDraw);
		}
	}
	
	public void addNewObjectDatas(SparseArray<ObjectData> objectData){
		imageProcess.addNewObjectDatas(objectData);
	}
	public void updateLastImage(Mat mat){ awaitingMat = mat; }
	
}
