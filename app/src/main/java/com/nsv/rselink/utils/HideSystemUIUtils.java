package com.nsv.rselink.utils;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by Tianluhua on 2017/8/17 0017.
 */
public class HideSystemUIUtils {
	/**
	 * Hide the status and navigation bars
	 */
	public static void hideSystemUI(Activity activity) {
		activity.getWindow()
				.getDecorView()
				.setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide
																		// nav
																		// bar
								| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status
																	// bar
								| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		enterFullScreenDisplayP(activity);
	}

	public static void enterFullScreenDisplayWh(Activity activity) {
		if (activity != null) {
			WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
			try {
				Class cls = Class.forName("com.huawei.android.view.LayoutParamsEx");
				Object newInstance = cls.getConstructor(new Class[]{WindowManager.LayoutParams.class}).newInstance(new Object[]{attributes});
				cls.getMethod("addHwFlags", new Class[]{Integer.TYPE}).invoke(newInstance, new Object[]{Integer.valueOf(65536)});
			} catch (Exception e) {
				//ExceptionUtils.printStackTrace(e);
				Log.e("","hdb--addHwFlags---");
			}
		}
	}

	public static void enterFullScreenDisplayP(Activity activity) {
		if (Build.VERSION.SDK_INT <= 27){
			return;
		}
		if (activity != null) {
			Window window = activity.getWindow();
			WindowManager.LayoutParams attributes = window.getAttributes();
			attributes.layoutInDisplayCutoutMode = 1;
			window.setAttributes(attributes);
		}
	}

	public void enterFullScreenDisplayXm(Activity activity) {
		if (activity != null) {
			Window window = activity.getWindow();
			try {
				Window.class.getMethod("addExtraFlags", new Class[]{Integer.TYPE}).invoke(window, new Object[]{Integer.valueOf(1792)});
			} catch (NoSuchMethodException e) {
//				ExceptionUtils.printStackTrace(e);
			} catch (IllegalAccessException e2) {
//				ExceptionUtils.printStackTrace(e2);
			} catch (InvocationTargetException e22) {
//				ExceptionUtils.printStackTrace(e22);
			}
		}
	}




}
