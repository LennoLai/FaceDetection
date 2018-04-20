package com.thit.ticwr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class BaseActivity extends Activity {

	private ViewGroup mTitlebarLayout;
	private TextView mTitlebarTitle;
	private ImageView mTitlebarBack, mTitlebarMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		initTitlebar();
	}

	@Override
	public void setTitle(CharSequence title) {
		super.setTitle(title);
		if (mTitlebarTitle != null) {
			mTitlebarTitle.setText(title);
		}
	}

	private void initTitlebar() {
		mTitlebarLayout = (ViewGroup) findViewById(R.id.titlebar_layout);
		if (mTitlebarLayout == null) {
			return;
		}
		mTitlebarTitle = (TextView) mTitlebarLayout.findViewById(R.id.titlebar_title);
		mTitlebarBack = (ImageView) mTitlebarLayout.findViewById(R.id.titlebar_back);
		mTitlebarMenu = (ImageView) mTitlebarLayout.findViewById(R.id.titlebar_menu);
		if (mTitlebarTitle != null) {
			mTitlebarTitle.setText(getTitle());
		}
		if (mTitlebarBack != null) {
			mTitlebarBack.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onBackPressed();
				}
			});
		}
		if (mTitlebarMenu != null) {
			mTitlebarMenu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onTitlebarMenuPressed(v);
				}
			});
		}
		setTitle(getTitle());
	}

	public void setTitlebarShowBack(boolean show) {
		mTitlebarBack.setVisibility(show ? View.VISIBLE : View.GONE);
	}


	public ViewGroup getTitlebar() {
		return mTitlebarLayout;
	}

	protected void onTitlebarMenuPressed(View menuView) {

	}

}
