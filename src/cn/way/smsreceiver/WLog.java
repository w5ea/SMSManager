package cn.way.smsreceiver;

import android.util.Log;

public class WLog{
	public static void d(String msg){
		if (BuildConfig.DEBUG) {
			Log.d("test", msg);
		}
	}
}
