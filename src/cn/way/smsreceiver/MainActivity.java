package cn.way.smsreceiver;

import java.util.ArrayList;
import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import cn.way.smsreceiver.SMSService.SMS;
import cn.way.smsreceiver.SMSService.SMSSendBroadcastReceiver;
import cn.way.smsreceiver.SMSService.SMSServiceConnection;
import cn.way.wandroid.utils.WLog;

public class MainActivity extends Activity {
	private GridView gv;
	private LinkedList<SMS> items = new LinkedList<SMS>();
	private ArrayAdapter<SMS> adapter;
	private SMSService mService;
	private EditText numberEditor;
	private EditText smsEditor;
	private Button sendBtn;
	private SMSServiceConnection serveiceConnection;
	private SMSSendBroadcastReceiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		numberEditor = (EditText) findViewById(R.id.numberEditor);
		smsEditor = (EditText) findViewById(R.id.smsEditor);
		sendBtn = (Button) findViewById(R.id.sendSMSBtn);
		sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (smsEditor.getText().length() == 0
						|| numberEditor.getText().length() == 0) {
					return;
				}
				SMS sms = new SMS();
				sms.number = numberEditor.getText().toString();
				sms.text = smsEditor.getText().toString();
				sms.isMine = true;
				items.addLast(sms);
				adapter.notifyDataSetChanged();
//				gv.smoothScrollToPosition(items.size() - 1);
				SMSService.sendSMS(getApplicationContext(), sms);
				sendBtn.setEnabled(false);
				sendBtn.setText("SENDING");
			}
		});
		serveiceConnection = new SMSServiceConnection() {
			@Override
			public void onServiceDisconnected(SMSService service) {
				WLog.d("########################onServiceDisconnected");
				mService = null;
			}

			@Override
			public void onServiceConnected(SMSService service) {
				WLog.d("########################onServiceConnected" + service);
				mService = service;
			}
		};
		receiver = new SMSSendBroadcastReceiver() {
			@Override
			public void onSMSSend(boolean success,int smsId, String message) {
				sendBtn.setEnabled(true);
				sendBtn.setText("SEND");
				Toast.makeText(getApplicationContext(), message,
						Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onSMSReceived(ArrayList<SMS> smss) {
				items.addAll(smss);
				adapter.notifyDataSetChanged();
				for (SMS sms : smss) {
					if (sms.number.startsWith("1065809912")) {
						Log.d("test1", sms.text.substring(52, 58));
					}
				}
			}
		};
		gv = (GridView) findViewById(R.id.gridView);
		adapter = new ArrayAdapter<SMS>(this, 0, items) {
			@SuppressLint("InflateParams")
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = convertView;
				ViewHolder vh = null;
				if (view == null) {
					view = getLayoutInflater().inflate(R.layout.sms_list_item,
							null);
					if (view != null) {
						final ViewHolder holder = new ViewHolder();
						view.setTag(holder);
						holder.numTv = (TextView) view
								.findViewById(R.id.smsNumTv);
						holder.textTv = (TextView) view
								.findViewById(R.id.smsTextTv);
						vh = holder;
					}
				} else {
					vh = (ViewHolder) view.getTag();
				}
				if (vh != null) {
					SMS sms = getItem(position);
					String fromto = "我发给" + sms.number;
					if (!sms.isMine) {
						fromto = sms.number + "发给我";
					}
					vh.numTv.setText(fromto);
					vh.textTv.setText(sms.text);
					view.setBackgroundColor(sms.isMine ? Color.rgb(255, 100,
							255) : Color.WHITE);
				}
				return view;
			}

			class ViewHolder {
				TextView textTv;
				TextView numTv;
			}
		};
		gv.setAdapter(adapter);
//		 Intent intent = new Intent(Intent.ACTION_MAIN);
//		 ComponentName cn = new ComponentName("com.android.settings",
//		 "com.android.settings.Settings");
//		 intent.setComponent(cn);
//		 intent.putExtra(":android:show_fragment",
//		 "com.android.settings.applications.AppOpsSummary");
//		 startActivity(intent);
//		final ContentObserver mObserver = new ContentObserver(new Handler()) {
//			@SuppressLint("NewApi")
//			@Override
//			public void onChange(boolean selfChange) {
//				super.onChange(selfChange);
//				ContentResolver resolver = getContentResolver();
//				Cursor cursor = resolver.query(
//						Uri.parse("content://sms/inbox"), new String[] { "_id",
//								"address", "body" }, null, null, "_id desc");
//				long id = -1;
//
//				if (cursor.getCount() > 0 && cursor.moveToFirst()) {
//					id = cursor.getLong(0);
//					String address = cursor.getString(1);
//					String body = cursor.getString(2);
//
//					Toast.makeText(
//							getApplicationContext(),
//							String.format("address: %s\n body: %s", address,
//									body), Toast.LENGTH_SHORT).show();
//				}
//				cursor.close();
//
//				if (id != -1) {
//					int count = resolver.delete(Sms.CONTENT_URI, "_id=" + id,
//							null);
//					Toast.makeText(getApplicationContext(),
//							count == 1 ? "删除成功" : "删除失败", Toast.LENGTH_SHORT)
//							.show();
//				}
//			}
//
//		};
//
//		getContentResolver().registerContentObserver(
//				Uri.parse("content://sms/"), true, mObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SMSService.bind(this, serveiceConnection);
		SMSService.registerReceiver(this, receiver);
		
//		InAppPurchaserSMS ipa = new InAppPurchaserSMS(this, "9");
//		Product product = new Product();
//		product.setPriceLocaleString("3");
//		ipa.purchaseProduct(product);
	}

	@Override
	protected void onStop() {
		super.onStop();
		SMSService.unbind(this, serveiceConnection);
		SMSService.unregisterReceiver(this, receiver);
	}
}
