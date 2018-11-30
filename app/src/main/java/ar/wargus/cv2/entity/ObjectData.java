package ar.wargus.cv2.entity;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect2d;
import org.opencv.tracking.Tracker;

public class ObjectData {
	public int id;
	public Mat image;
	public MatOfKeyPoint keyPoints;
	public Mat descriptors;
	public MatOfKeyPoint fastKeyPoints;
	public Mat fastDescriptors;
	public Rect2d rect;
	public Tracker tracker;
	public byte trackerFailure = 0;
	
	public ObjectData(int id,
	                  Mat image,
	                  MatOfKeyPoint keyPoints,
	                  Mat descriptors) {
		this.id = id;
		this.image = image;
		this.keyPoints = keyPoints;
		this.descriptors = descriptors;
	}
	
	public ObjectData(int id,
	                  Mat image,
	                  MatOfKeyPoint keyPoints,
	                  Mat descriptors,
	                  MatOfKeyPoint fastKeyPoints,
	                  Mat fastDescriptors) {
		this.id = id;
		this.image = image;
		this.keyPoints = keyPoints;
		this.descriptors = descriptors;
		this.fastKeyPoints = fastKeyPoints;
		this.fastDescriptors = fastDescriptors;
	}
}


// TODO trzymać obrazy od czasu stworzenia objectFinderAsyncTask
// do czasu stworzenia trackera
// wtedy z update'ować go o wszystkie obrazy