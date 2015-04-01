package cn.way.smsreceiver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;
import cn.way.wandroid.utils.WLog;

/**
 * 短信服务
 * @author Wayne
 */
public class SMSService extends Service {
	@Override
	public void onCreate() {
		super.onCreate();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	/**
	 * 服务连接子类。会返回服务对象实例
	 * @author Wayne
	 */
	public static abstract class SMSServiceConnection implements
			ServiceConnection {
		private SMSService smsService;

		public abstract void onServiceConnected(SMSService service);

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			smsService = ((SMSService.LocalBinder) service)
					.getService();
			onServiceConnected(smsService);
		}

		public abstract void onServiceDisconnected(SMSService service);

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			onServiceConnected(smsService);
		}
	};

	public static boolean bind(Context context,
			SMSServiceConnection serviceConnection) {
		Intent intent = new Intent(context, SMSService.class);
		return context.bindService(intent, serviceConnection,
				BIND_AUTO_CREATE);
	}

	public static void unbind(Context context,
			SMSServiceConnection serviceConnection) {
		context.unbindService(serviceConnection);
	}
	public static void start(Context context) {
		context.startService(new Intent(context, SMSService.class));
	}
	public static void stop(Context context) {
		context.stopService(new Intent(context, SMSService.class));
	}

	/**
	 * 用于对外开发服务对象实例
	 * @author Wayne
	 */
	public class LocalBinder extends Binder {
		SMSService getService() {
			return SMSService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
	/**
	 * 发送短信
	 * @param context
	 * @param sms
	 */
	public static void sendSMS(Context context,SMS sms){
		if (sms==null||sms.isEmpty()) {
			broadcast(context, Action.SMS_SENT_FAIL, null);
			return;
		}
		SmsManager smsManager = SmsManager.getDefault();
		PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, new Intent(Action.SMS_SENT.toString()), 0); 
		PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0, new Intent(Action.SMS_DELIVERY.toString()), 0);
		List<String> messages = smsManager.divideMessage(sms.text);
        for (String message : messages) {
        	smsManager.sendTextMessage(sms.number, null, message, sentIntent, deliveryIntent);
        }
	}
	/**
	 * 短信接收广播接收者
	 * @author Wayne
	 */
	public static class SMSBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String actionName = intent.getAction();
			if (actionName.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
				abortBroadcast();
				SMSService.broadcast(context, Action.SMS_RECEIVED,intent);
			}
		}
	};
	
	public interface SMSReceiveListener {
		void onSMSReceived(ArrayList<SMS> smss);
	}
	public static class SMS {
		boolean isMine;//ture 是自己编写的。false是接收到的别人的
		Date date;
		String number;
		String text;
		public String getFormatedDate(){
			if (date==null) {
				return "";
			}
			SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());
            return formater.format(date);
		}
		public boolean isEmpty(){
			return text==null||number==null;
		}
	}
	
	public static abstract class SMSSendBroadcastReceiver extends BroadcastReceiver{
		public abstract void onSMSSend(boolean success,String message);
		public abstract void onSMSReceived(ArrayList<SMS> smss);
		@Override
		public void onReceive(Context context, Intent intent) {
			String actionName = intent.getAction();
			WLog.d("EEEEEEEEEEEEEEEEEEEE"+actionName);
			if (actionName.equals(Action.SMS_SENT.toString())) {
				String message = null;
	            boolean success = false;
	            switch (getResultCode()) {
	            case Activity.RESULT_OK:
	                message = "Message sent!";
	                success = true;
	                break;
	            case SmsManager.RESULT_ERROR_NO_SERVICE:
	                message = "Error: No service.";
	                break;
	            case SmsManager.RESULT_ERROR_NULL_PDU:
	                message = "Error: Null PDU.";
	                break;
	            case SmsManager.RESULT_ERROR_RADIO_OFF:
	                message = "Error: Radio off.";
	                break;
	            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
	            default:
	            	message = "Error.";
	            	break;
	            }
	            onSMSSend(success, message);
			}
			if (actionName.equals(Action.SMS_SENT_FAIL.toString())) {
				onSMSSend(false, "Error:Empty SMS.");
			}
			if (actionName.equals(Action.SMS_DELIVERY.toString())) {
				Toast.makeText(context, actionName, Toast.LENGTH_LONG).show();
			}
			if (actionName.equals(Action.SMS_RECEIVED.toString())) {
				doOnSMSReceived(intent);
			}
		}
		private void doOnSMSReceived(Intent intent){
			Bundle extras = intent.getExtras();
	        if (extras == null)
	            return;
	        WLog.d("doOnSMSReceived"+intent);
			Object[] pdus = (Object[]) extras.get("pdus");
			ArrayList<SMS> smss = new ArrayList<SMSService.SMS>();
			
			for (int i = 0; i < pdus.length; i++) {
				SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
				SMS sms = new SMS();
				sms.number = msg.getDisplayOriginatingAddress();
				sms.text = msg.getDisplayMessageBody();
				sms.date = new Date(msg.getTimestampMillis());
				smss.add(sms);
				onSMSReceived(smss);
			}
		}
	}
	/**
	 * 发送一个广播
	 * @param context
	 * @param action
	 * @param i 目前里面的信息是用来做收到短信广播
	 */
	private static void broadcast(Context context ,Action action,Intent i) {
		Intent intent = new Intent() ;
		if (i!=null) {//这里没有用new Intent(i)因为只想要里面的Extras属性。别的会影响广播
			intent.putExtras(i.getExtras());
		}
		intent.setAction(action.toString());
        context.sendBroadcast(intent);
    }
	private static IntentFilter createIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Action.SMS_SENT.toString());
        intentFilter.addAction(Action.SMS_SENT_FAIL.toString());
        intentFilter.addAction(Action.SMS_RECEIVED.toString());
        intentFilter.addAction(Action.SMS_DELIVERY.toString());
        return intentFilter;
    }
	public static enum Action{
		SMS_SENT,SMS_DELIVERY,SMS_RECEIVED,SMS_SENT_FAIL
		;
		@Override
		public String toString() {
			return "cn.way.wandroid."+super.toString();
		}
	}
	
	public static void registerReceiver(Context context,SMSSendBroadcastReceiver receiver){
		context.registerReceiver(receiver, createIntentFilter());
	}
	public static void unregisterReceiver(Context context,SMSSendBroadcastReceiver receiver){
		context.unregisterReceiver(receiver);
	}
	
}
