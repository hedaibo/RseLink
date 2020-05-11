package com.nsv.rselink.utils;

import android.content.Context;

import java.lang.Thread.UncaughtExceptionHandler;


public class MyException implements UncaughtExceptionHandler {

	/**单列模式*/
	private static MyException myException = new MyException();
	private UncaughtExceptionHandler defaultUncaughtExceptionHandler;
	private MyException(){
		
	}
	
	public static MyException instance(){
		return myException;
	}
	
	public void initData(Context context) {
		defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		if (ex == null && defaultUncaughtExceptionHandler != null) {
			defaultUncaughtExceptionHandler.uncaughtException(thread, ex);
		//	System.out.println("---------defaultuncaught----------");
		}else {
		//	System.out.println("---------killProcess----------");
			android.os.Process.killProcess(android.os.Process.myPid());
			System.exit(1);
		}	
	}
	
	
	

}
