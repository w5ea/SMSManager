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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import cn.way.wandroid.utils.WLog;

/**
 * 短信服务
 * 
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
	 * 
	 * @author Wayne
	 */
	public static abstract class SMSServiceConnection implements
			ServiceConnection {
		private SMSService smsService;

		public abstract void onServiceConnected(SMSService service);

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			smsService = ((SMSService.LocalBinder) service).getService();
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
		return context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
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
	 * 
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

	private static final String EXTRA_SMS_ID = "smsId";
	/**
	 * 发送短信
	 * 
	 * @param context
	 * @param sms
	 */
	public static void sendSMS(Context context, SMS sms) {
		if (sms == null || sms.isEmpty()) {
			broadcast(context, Action.SMS_SENT_FAIL, null);
			return;
		}
		SmsManager smsManager = SmsManager.getDefault();
		Intent sendSmsIntent = new Intent(Action.SMS_SENT.toString());
		sendSmsIntent.putExtra(EXTRA_SMS_ID, sms.id+"");
//		sendSmsIntent.getExtras().putString("smsId",sms.id+"");
		PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
				sendSmsIntent, 0);
		
		PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0,
				new Intent(Action.SMS_DELIVERY.toString()), 0);
		List<String> messages = smsManager.divideMessage(sms.text);
		for (String message : messages) {
			smsManager.sendTextMessage(sms.number, null, message, sentIntent,
					deliveryIntent);
		}
	}

	/**
	 * 短信接收广播接收者
	 * 
	 * @author Wayne
	 */
	public static class SMSBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String actionName = intent.getAction();
			WLog.d("SMSBroadcastReceiver:" + actionName);
			if (actionName.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
				abortBroadcast();
				SMSService.broadcast(context, Action.SMS_RECEIVED, intent);
			}
		}
	};

	public interface SMSReceiveListener {
		void onSMSReceived(ArrayList<SMS> smss);
	}

	public static class SMS {
		public int id;//短信ID(_id)
		public int tid;//会话ID(thread_id)
		public boolean isMine;// ture 是自己编写的。false是接收到的别人的
		public Date date;
		public String number;//收信人号码
		public String address;//发信人号码
		public String text;

		public String getFormatedDate() {
			if (date == null) {
				return "";
			}
			SimpleDateFormat formater = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			return formater.format(date);
		}

		public boolean isEmpty() {
			return text == null || number == null;
		}

		@Override
		public String toString() {
			return "SMS [id=" + id + ", tid=" + tid + ", isMine=" + isMine
					+ ", date=" + date + ", number=" + number + ", address="
					+ address + ", text=" + text + "]";
		}
		
	}

	public static abstract class SMSSendBroadcastReceiver extends
			BroadcastReceiver {
		public abstract void onSMSSend(boolean success,int smsId, String message);

		public abstract void onSMSReceived(ArrayList<SMS> smss);

		@Override
		public void onReceive(Context context, Intent intent) {
			String actionName = intent.getAction();
			WLog.d("EEEEEEEEEEEEEEEEEEEE" + actionName);
			if (actionName.equals(Action.SMS_SENT.toString())) {
//				int smsId = intent.getIntExtra(EXTRA_SMS_ID,0);
				String smsIdStr = intent.getStringExtra(EXTRA_SMS_ID);
				int smsId = 0;
				if (smsIdStr!=null) {
//					WLog.d("Action.SMS_SENT:::::" + smsIdStr);
					try {
						smsId = Integer.valueOf(smsIdStr);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
				WLog.d("Action.SMS_SENT:::::" + smsId);
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
				onSMSSend(success,smsId, message);
			}
			if (actionName.equals(Action.SMS_SENT_FAIL.toString())) {
				onSMSSend(false, 0,"Error:Empty SMS.");
			}
			if (actionName.equals(Action.SMS_DELIVERY.toString())) {
			}
			if (actionName.equals(Action.SMS_RECEIVED.toString())) {
				doOnSMSReceived(intent);
			}
		}

		private void doOnSMSReceived(Intent intent) {
			Bundle extras = intent.getExtras();
			if (extras == null)
				return;
			WLog.d("doOnSMSReceived" + intent);
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
	 * 
	 * @param context
	 * @param action
	 * @param i
	 *            目前里面的信息是用来做收到短信广播
	 */
	private static void broadcast(Context context, Action action, Intent i) {
		Intent intent = new Intent();
		if (i != null) {// 这里没有用new Intent(i)因为只想要里面的Extras属性。别的会影响广播
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

	public static enum Action {
		SMS_SENT, SMS_DELIVERY, SMS_RECEIVED, SMS_SENT_FAIL;
		@Override
		public String toString() {
			return "cn.way.wandroid." + super.toString();
		}
	}

	public static void registerReceiver(Context context,
			SMSSendBroadcastReceiver receiver) {
		context.registerReceiver(receiver, createIntentFilter());
	}

	public static void unregisterReceiver(Context context,
			SMSSendBroadcastReceiver receiver) {
		context.unregisterReceiver(receiver);
	}

	
	public class SMSObserver extends ContentObserver
	{
	    private Handler mHandle = null;
	    public SMSObserver(Handler handle) {
	        super(handle);
	        mHandle = handle;
	    }
	    public void onChange(boolean bSelfChange){
	        super.onChange(bSelfChange);
	        mHandle.sendEmptyMessage(0);
	        WLog.d("##########################SMSObserver onChange");
	    }
	}
	private Handler handler = new Handler();
	private ContentObserver mSMSObserver = new SMSObserver(handler);
	public void registerSMSObserver(Context context){
		ContentResolver contentResolver = context.getContentResolver();
		contentResolver.registerContentObserver(Uri.parse("content://sms"),true, mSMSObserver);
		WLog.d("##########################SMSObserver register");
	}
	public void unregisterSMSObserver(Context context){
		context.getContentResolver().unregisterContentObserver(mSMSObserver);
		WLog.d("##########################SMSObserver unregister");
	}
	public static final String SMS_ALL = "content://sms"; // 所有
	public static final String SMS_INBOX = "content://sms/inbox"; // 收件箱
	public static final String SMS_SENT = "content://sms/sent"; // 已发送
	public static final String SMS_DRAFT = "content://sms/draft"; // 草稿
	public static final String SMS_OUTBOX = "content://sms/outbox"; // 发件箱
	public static final String SMS_FAILED = "content://sms/failed"; // 发送失败
	public static final String SMS_QUEUED = "content://sms/queued"; // 待发送列表

	public static ArrayList<SMS> querySmsByPhoneNumber(Context context, String phoneNumber) {
		WLog.d("querySmsByPhoneNumber"+phoneNumber);
		Uri uri = Uri.parse(SMS_SENT);
		String[] projection = {"_id,thread_id,address,body,type,date"};
		String selection = null;//"address=?";
		String[] selectionArgs = null;//{phoneNumber};
		String sortOrder = "date desc";
		Cursor cur = context.getContentResolver()
				.query(uri, projection, selection, selectionArgs, sortOrder);
		if (cur.moveToFirst()) {
			int index_id = cur.getColumnIndex("_id");  
			int index_tid = cur.getColumnIndex("thread_id");  
			int index_address = cur.getColumnIndex("address");  
//            int index_person = cur.getColumnIndex("person");  
            int index_body = cur.getColumnIndex("body");  
            int index_date = cur.getColumnIndex("date");  
//            int index_type = cur.getColumnIndex("type");
			ArrayList<SMS> smss = new ArrayList<SMSService.SMS>();
			do {  
				SMS sms = new SMS();
				int id = cur.getInt(index_id);  
				int tid = cur.getInt(index_tid);  
				String address = cur.getString(index_address);  
	            String body = cur.getString(index_body);  
	            long date = cur.getLong(index_date);  
//	            int type = cur.getInt(index_type); 


                sms.id = id;sms.tid = tid;sms.address = address;
                sms.text = body;sms.date = new Date(date);
                WLog.d(sms.toString());
                
                smss.add(sms);
            } while (cur.moveToNext());  
			
            if (cur!=null&&!cur.isClosed()) {  
                cur.close();  
            } 
            cur = null;  
            return smss;
		}
		return null;
	}

	public static boolean deleteSmsByThreadId(Context context, String threadId) {
		int count = context.getContentResolver().delete(
				Uri.parse("content://sms/conversations/"+threadId),null,null);
//		Uri.parse("content://sms/conversations/"), "thread_id=?", new String[] { threadId });
		//should SMS_ALL be changed to be "content://sms/conversations/aThreadId"
		if (count > 0) {
			WLog.d("success deleteSmsByThreadId:"+threadId);
			return true;
		}
		WLog.d("false deleteSmsByThreadId:"+threadId);
		return false;
	}
	public static boolean deleteSmsById(Context context, String smsId) {
		int count = context.getContentResolver().delete(
				Uri.parse("content://sms"), "_id=?", new String[] { smsId });
		if (count > 0) {
			WLog.d("success deleteSmsById:"+smsId);
			return true;
		}
		WLog.d("false deleteSmsById:"+smsId);
		return false;
	}
	public static boolean updateSmsById(Context context, String smsId) {
		ContentValues cv = new ContentValues();    
		cv.put("address", "18888888888");    
		cv.put("person", "王力宏");    
		cv.put("body", "你就是我的唯一两个世界都变形");    
		int count = context.getContentResolver().update(Uri.parse("content://sms/inbox/"+smsId), cv, null, null);    
		if (count > 0) {
			WLog.d("success updateSmsById:"+smsId);
			return true;
		}
		WLog.d("false updateSmsById:"+smsId);
		return false;
	}
}
