package com.thit.ticwr.facedetection;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.thit.ticwr.BaseActivity;
import com.thit.ticwr.R;

/**
 * 考勤界面，人脸检测
 * @author Mixiaoxiao
 *
 */
@SuppressWarnings("deprecation")
public class FaceDetectionActivity extends BaseActivity implements FaceDetectionListener, Camera.PreviewCallback {
	/**定位，间隔**/
	public static final int LOCATION_SCAN_SPAN = 20 * 1000;
	/**摄像头ID，取前摄像头**/
	public static final int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT;
	/** Face.socre可信的最小值 **/
	public static int DECTECTION_MIN_SCORE = 60;// Integer.MIN_VALUE;

	/** 实际上传人脸的图像大小要比检测到的人脸rect扩大一定的倍数 **/
	public static final float FACE_RECT_SCALE = 0.4f;

	private TextView mInfoTextView;
	private CameraView mCameraView;
	private FaceDetectionOverlayView mFaceDetectionOverlayView;
	private LayoutRotateable mLayoutRotateable;

	/** Camera预览数据，长宽 **/
	private byte[] yuvData;
	private int dataWidth, dataHeight;

	/** 以下4项用于计算Face的框，具体计算见Face.rect的注释 **/
	private Rect mRect = new Rect();
	private Rect mRectForCrop = new Rect();
	private RectF mRectF_tmp = new RectF();
	private Matrix mMatrix = new Matrix();

	// 记录最后一个Face
	private Face mLastFace;

	// 标识当前是否可以捕获人脸，
	// 比如当裁剪图像和上传期间就停止捕获人脸
	private boolean canCapture = false;

	// 标记是否已经onDestroy，如果为true，异步任务结束之后什么都不做
	private boolean mDestroyed = false;

	// c处理设备方向
	private SimpleOrientationEventListener mOrientationEventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_facedetection);
		setTitlebarShowBack(true);
		getTitlebar().setBackgroundColor(0x33000000);
		
		mCameraView = (CameraView) findViewById(R.id.facedection_cameraview);
		mCameraView.setPreviewCallback(this);
		mCameraView.setFaceDetectionListener(this);
		/** 保持屏幕常亮 **/
		mCameraView.setKeepScreenOn(true);

		mInfoTextView = (TextView) findViewById(R.id.facedection_textview);


		mFaceDetectionOverlayView = (FaceDetectionOverlayView) findViewById(R.id.facedection_faceoverlayview);
		mLayoutRotateable = (LayoutRotateable) findViewById(R.id.facedection_layoutrotateable);
		mOrientationEventListener = new SimpleOrientationEventListener(this);
		setTitle("考勤");
		startCapture();


	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		this.yuvData = data;
		final Camera.Parameters parameters = camera.getParameters();
		this.dataWidth = parameters.getPreviewSize().width;
		this.dataHeight = parameters.getPreviewSize().height;
		// log("onPreviewFrame PreviewSize w=" + dataWidth + " h=" +
		// dataHeight);
	}

	@Override
	public void onFaceDetection(Face[] faces, Camera camera) {
		// log("onFaceDetection faces.length->" + faces.length);
		if (mDestroyed) {
			return;
		}
		mFaceDetectionOverlayView.onFaceDetection(faces, camera);
		if (!canCapture) {
			return;
		}
		if (faces == null || faces.length == 0) {
			return;
		}
		final Face newFace = faces[0];
		if (newFace.score <= 0) {
			DECTECTION_MIN_SCORE = Integer.MIN_VALUE;
			// 此处为无可奈何的办法，
			// 我们认为socre为负数的时候
			// 就不判断score了
		}
		if (newFace.score < DECTECTION_MIN_SCORE) {
			resetLastFace();
			return;
		}
		// newFace.score已经OK
		if (mLastFace == null) {
			mLastFace = newFace;
			return;
		} else {
			if (newFace.id == mLastFace.id) {// 同一张脸
				// newFace和mLastFace的score都OK了
				final Face betterFace = newFace;
				mapFaceRect(betterFace);
				buildFaceDetectionSource();
			}
		}
	}

	private void resetLastFace() {
		mLastFace = null;
	}

	public void startCapture() {
		if (mDestroyed) {
			return;
		}
		canCapture = true;
		showLog("正在捕获人脸");

		System.gc();
	}

	public void stopCapture() {
		canCapture = false;
		resetLastFace();
	}

	/**
	 * 第1步 计算Face所在的Rect
	 */
	private boolean mapFaceRect(final Face face) {
		mMatrix.reset();
		final boolean mirror = (CAMERA_ID == CameraInfo.CAMERA_FACING_FRONT);
		mMatrix.setScale(mirror ? -1 : 1, 1);
		// This is the value for android.hardware.Camera.setDisplayOrientation.
		// matrix.postRotate(displayOrientation);
		// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
		// UI coordinates range from (0, 0) to (width, height).
		mMatrix.postScale(dataWidth / 2000f, dataHeight / 2000f);
		mMatrix.postTranslate(dataWidth / 2f, dataHeight / 2f);
		mRectF_tmp.set(face.rect);
		mMatrix.mapRect(mRectF_tmp);
		mRect.left = Math.round(mRectF_tmp.left);
		mRect.top = Math.round(mRectF_tmp.top);
		mRect.right = Math.round(mRectF_tmp.right);
		mRect.bottom = Math.round(mRectF_tmp.bottom);
		if (mRect.isEmpty()) {
			mRect.setEmpty();
			return false;
		}
		return true;
	}

	/**
	 * 第2步 构造FaceDetectionSource 传入第1步中计算得出的人脸Rect
	 * 调用FaceDetectionSource.doHardWork() 来裁剪yuv数据，得到仅包含人脸的区域 生成byte和bitmap
	 */
	public void buildFaceDetectionSource() {
		if (this.mRect.isEmpty() || yuvData == null) {
			return;
		}
		mRectForCrop.set(mRect);
		// 对mRect进行扩大，inset一个负数即是扩大
		mRectForCrop.inset(-Math.round(mRectForCrop.width() / 2f * FACE_RECT_SCALE),
				-Math.round(mRectForCrop.height() / 2f * FACE_RECT_SCALE));
		final FaceDetectionSource source = new FaceDetectionSource(dataWidth, dataHeight, yuvData, mRectForCrop,
				mOrientationEventListener.getCurrentOrientation());
		stopCapture();
		showLog("已经捕获人脸，正在处理数据");
		Observable.just(source).map(new Func1<FaceDetectionSource, Bitmap>() {
			@Override
			public Bitmap call(FaceDetectionSource source) {
				final long start = AnimationUtils.currentAnimationTimeMillis();
				source.doHardWork();
				final long time = AnimationUtils.currentAnimationTimeMillis() - start;
				return source.getCroppedBitmap();
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Bitmap>() {
			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
				if (mDestroyed) {
					return;
				}
				showLog("Error->" + e.toString());
				startCapture();
			}

			@Override
			public void onNext(Bitmap bitmap) {
				if (mDestroyed) {
					return;
				}
				showLog("数据处理完毕，正在上传识别");
				//在这里实现上传识别的函数
			}
		});

	}

	private void showLog(String text) {
		mInfoTextView.setText(text);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCameraView.onResume();
		mOrientationEventListener.enable();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCameraView.onPause();
		mOrientationEventListener.disable();
	}

	@Override
	protected void onDestroy() {
		mDestroyed = true;
		super.onDestroy();
		mCameraView.onDestory();
		stopCapture();
		mOrientationEventListener.destroy();
		mOrientationEventListener = null;
	}

	public void onOrientationChanged(int orientation) {
		mFaceDetectionOverlayView.setOrientation(orientation);
		mLayoutRotateable.setOrientation(orientation);
	}

	public static void startWithProjectId(Context context) {
		Intent intent = new Intent(context, FaceDetectionActivity.class);
		context.startActivity(intent);
	}

	// ==========================================================================
	// OrientationEventListener
	// ==========================================================================
	private static class SimpleOrientationEventListener extends OrientationEventListener {

		FaceDetectionActivity faceDetectionActivity;
		int currentOrientation;

		public SimpleOrientationEventListener(FaceDetectionActivity faceDetectionActivity) {
			super(faceDetectionActivity, SensorManager.SENSOR_DELAY_NORMAL);
			this.faceDetectionActivity = faceDetectionActivity;
		}

		@Override
		public void onOrientationChanged(int orientation) {
			// We keep the last known orientation. So if the user first orient
			// the camera then point the camera to floor or sky, we still have
			// the correct orientation.
			if(faceDetectionActivity == null){
				return;
			}
			int newOrientation;
			if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
				newOrientation = Util.getDisplayRotation(faceDetectionActivity);
			} else {
				newOrientation = Util.roundOrientation(orientation, currentOrientation)
						+ Util.getDisplayRotation(faceDetectionActivity);
			}
			newOrientation = (newOrientation + 360) % 360;
			if (newOrientation != currentOrientation) {
				currentOrientation = newOrientation;
				faceDetectionActivity.onOrientationChanged(currentOrientation);
			}
		}
		
		public int getCurrentOrientation() {
			return currentOrientation;
		}

		public void destroy(){
			this.faceDetectionActivity = null;
		}
	}

}
