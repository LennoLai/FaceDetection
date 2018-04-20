package com.thit.ticwr;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.thit.ticwr.facedetection.FaceDetectionActivity;

/**
 * 也是启动界面
 * LocationManager在这里init
 * @author Mixiaoxiao
 *
 */
public class MainActivity extends BaseActivity {

	TextView tv;
	EditText mEditTextID, mEditTextPW;
	Button mButtonLogin;
	CheckBox mCheckBoxRememberPW;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initContentView();
	}

	private void initContentView() {
		setContentView(R.layout.activity_main);
		mButtonLogin = (Button) findViewById(R.id.login_button);
		mButtonLogin.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
//				AppConfig.DEBUG = !AppConfig.DEBUG;
//				toast("当前调试模式 DEBUG=" + AppConfig.DEBUG);
				FaceDetectionActivity.startWithProjectId(MainActivity.this);
			}
		});
	}
}
