package ar.wargus.cv2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	
	private Start start;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i("OpenCV", "OpenCV loaded successfully");
				} break;
				default:
				{
					super.onManagerConnected(status);
				} break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OpenCVLoader.initDebug();
		//Remove title bar
//		requestWindowFeature(Window.FEATURE_NO_TITLE);

		//Remove notification bar
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//		                     WindowManager.LayoutParams.FLAG_FULLSCREEN);

		//set content view AFTER ABOVE sequence (to avoid crash)
		setContentView(R.layout.activity_main);
		//Init actions after activity view is created
		findViewById(R.id.fullscreen_view).setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
				                                                         | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				                                                         | View.SYSTEM_UI_FLAG_VISIBLE
				                                                         | View.SYSTEM_UI_FLAG_IMMERSIVE);
		findViewById(R.id.ar_view).setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                                                         | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                         | View.SYSTEM_UI_FLAG_VISIBLE
                                                         | View.SYSTEM_UI_FLAG_IMMERSIVE);
		initAfterCreateView();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (!OpenCVLoader.initDebug()) {
			Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
		} else {
			Log.d("OpenCV", "OpenCV library found inside package. Using it!");
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
		if(start != null) start.onResume();
	}
	
	private void doStuff(){
		if(start == null)
			start = new Start();
		try {
			start.doStuff(this);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull
			                               String[] permissions,
	                                       @NonNull
			                               int[] grantResults) {
		doStuff();
	}
	
	private boolean permissions(String... permissions){
		List<String> requestPermissions = new ArrayList<>();
		for (String permission : permissions) {
			if (ContextCompat.checkSelfPermission(this,
			                                      permission) != PackageManager.PERMISSION_GRANTED) {
				if (! ActivityCompat.shouldShowRequestPermissionRationale(this,
				                                                          permission)) {
					requestPermissions.add(permission);
				}
			}
		}
		if(!requestPermissions.isEmpty()){
			ActivityCompat.requestPermissions(this,
//			                                  permissions,
                                              requestPermissions.toArray(new String[requestPermissions.size()]),
                                              1);
			return false;
		}
		return true;
	}
	
	//Initialize actions to be performed after activity view is created
	private void initAfterCreateView(){
		//Get activity root layout
		final View activityView = findViewById(android.R.id.content);
		//Add listener to
		activityView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				//Perform actions after layout is created
				if(permissions(Manifest.permission.CAMERA))
					doStuff();
				//Remove listener after view is created
				activityView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
	}
}
