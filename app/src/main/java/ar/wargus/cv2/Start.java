package ar.wargus.cv2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ar.wargus.cv2.openCV.ImageProcessingThread;
import ar.wargus.cv2.openCV.InternalImageProcess;
import ar.wargus.cv2.utils.ImageUtils;
import ar.wargus.cv2.views.ARView;

class Start {
	private final String LOG_TAG = this.getClass().getSimpleName();
	
	private AppCompatActivity appCompatActivity;
	
	private ImageProcessingThread imgProcThread;
	private ARView arView;
	
	private InternalImageProcess internalImageProcess;
	
	private Size prefPreviewSize;
	private TextureView textureView;
	private Surface surfaceTextureView;
	private CameraManager cameraManager;
	
	private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener(){
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
			try {
				setupCamera();
//				surface.setDefaultBufferSize(prefPreviewSize.getWidth(),
//				                             prefPreviewSize.getHeight());
				surfaceTextureView = new Surface(surface);
//				textureView.setTransform(transform());
				
				openCamera();
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return false; }
		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
	};
	private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(@NonNull CameraDevice camera) {
			try {
				createPreviewSession(camera);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onDisconnected(@NonNull CameraDevice camera) { camera.close(); }
		@Override
		public void onError(@NonNull CameraDevice camera, int error) { camera.close(); }
	};
	private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
		@Override
		public void onConfigured(@NonNull CameraCaptureSession session) {
			try {
				imgProcThread = new ImageProcessingThread(internalImageProcess,
				                                          new WeakReference<ARView>(arView));
				imgProcThread.addNewObjectDatas(internalImageProcess.loadDescriptors(appCompatActivity.getAssets()));
				imgProcThread.start();
				startPreview(session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
	};
	
	private Size yuvSize = new Size(2160, 1080);
	private ImageReader yuvImageReader;
	private ImageReader.OnImageAvailableListener yuvImageListener = reader -> {
		Image image = reader.acquireNextImage();
		if(image == null) { return; }
		
		try {
			imgProcThread.updateLastImage(ImageUtils.imageToMat(image));
		}catch(Exception e){
			e.printStackTrace();
		}
		image.close();
	};
	
	Start() {}

	void onResume(){
		if(! textureView.isAvailable()){
			textureView.setSurfaceTextureListener(surfaceTextureListener);
		}
	}
	
	void doStuff(AppCompatActivity appCompatActivity) {
		this.appCompatActivity = appCompatActivity;
		
		internalImageProcess = new InternalImageProcess(yuvSize);
		arView = appCompatActivity.findViewById(R.id.ar_view);
		
		cameraManager = (CameraManager) appCompatActivity.getSystemService(Context.CAMERA_SERVICE);
		textureView = appCompatActivity.findViewById(R.id.fullscreen_view);
		textureView.setSurfaceTextureListener(surfaceTextureListener);
		
		// TODO zmienić na największy rozmiar z minimalnym opóźnieniem (ImageFormat do uzgodnienia)
		yuvImageReader = ImageReader.newInstance(yuvSize.getWidth(),
		                                         yuvSize.getHeight(),
		                                         ImageFormat.YUV_420_888,
		                                         1);
		// Handler for yuvImage
		HandlerThread yuvHandler = new HandlerThread("MyHandlerThread");
		yuvHandler.start();
		Handler rawHandler = new Handler(yuvHandler.getLooper());
		yuvImageReader.setOnImageAvailableListener(yuvImageListener, rawHandler);
		
	}
	
	private void setupCamera() throws CameraAccessException {
		final CameraCharacteristics camChar = cameraManager.getCameraCharacteristics("0");
		prefPreviewSize = getPreviewOutputSize(camChar);
	}
	
	private void openCamera() throws CameraAccessException {
		if (ActivityCompat.checkSelfPermission(appCompatActivity,
		                                       Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			Log.e(LOG_TAG, "Illegal state, camera access was invoked without permissions");
			return;
		}
		cameraManager.openCamera("0", cameraStateCallback, null);
	}
	
	private void createPreviewSession(CameraDevice camera) throws CameraAccessException {
		camera.createCaptureSession(Arrays.asList(surfaceTextureView,
		                                          yuvImageReader.getSurface()),
		                            sessionStateCallback,
		                            null);
	}
	
	private void startPreview(CameraCaptureSession session) throws CameraAccessException {
		CaptureRequest.Builder builder = session.getDevice()
		                                        .createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
		builder.addTarget(surfaceTextureView);
		builder.addTarget(yuvImageReader.getSurface());
		builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_FAST);
		builder.set(CaptureRequest.HOT_PIXEL_MODE, CaptureRequest.HOT_PIXEL_MODE_FAST);
		builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
		builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
		builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
		builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
		builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_60HZ);
		
		session.setRepeatingRequest(builder.build(), null, null);
	}
	
////	private float widthTransform, heightTransform;
//	private void sendToProcess(Mat imageMat){
//
//		// TODO do it on different process/handler/thread?
//		Pair<Mat, Mat>[] objects = internalImageProcess.process(imageMat);
//		if(objects == null) return;
////		widthTransform = textureSize.getWidth()/yuvSize.getWidth();
////		heightTransform = textureSize.getHeight()/yuvSize.getHeight();
////		arView.setSizeTransforms(widthTransform, heightTransform);
//		arView.setObjectsToDraw(objects);
////		arView.invalidate();
//
//	}
	
	private class SmartSize{
		Size size;
		int longer;
		int shorter;
		SmartSize(int width, int height){
			this.size = new Size(width, height);
			this.longer = Math.max(size.getWidth(),
			                       size.getHeight());
			this.shorter = Math.min(size.getWidth(),
			                        size.getHeight());
		}
	}
	
	private SmartSize getDisplaySmartSize(){
		DisplayMetrics displayMetrics = new DisplayMetrics();
		((WindowManager) appCompatActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
		                                                                            .getMetrics(displayMetrics);

		return new SmartSize(displayMetrics.widthPixels, displayMetrics.heightPixels);
	}
	
	private Size getPreviewOutputSize(CameraCharacteristics camChar){
		SmartSize displayScreenSize = getDisplaySmartSize();

		StreamConfigurationMap config = camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		assert config != null;

		List<Size> allSizes = Arrays.asList(config.getOutputSizes(SurfaceTexture.class));

		// Find the smallest size of sizes bigger than screen
		return allSizes.stream()
		               .sorted(Comparator.comparingInt(size
		                                               -> (size.getHeight()
		                                                   * size.getWidth())))
		               .map(size
                            -> new SmartSize(size.getWidth(),
                                             size.getHeight()))
		               .filter(size
                               -> size.longer >= displayScreenSize.longer
		                          && size.shorter >= displayScreenSize.shorter)
		               .min(Comparator.comparingInt(size ->(int) Math.abs((size.shorter/(float)size.longer)-0.75F)*100))
                       .orElse(displayScreenSize)
				       .size;
	}

	private Matrix transform(){
//	private Matrix transform(Size input, Size output){
		Matrix matrix = new Matrix();
		matrix.setRotate(90);
//		matrix.setRectToRect(new RectF(0, 0, input.getWidth(), input.getHeight()),
//		                     new RectF(0, 0, output .getHeight(), output .getWidth()),
//		                     Matrix.ScaleToFit.START);
		return matrix;
	}
}
