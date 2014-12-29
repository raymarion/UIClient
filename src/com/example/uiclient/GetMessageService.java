package com.example.uiclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.webkit.WebView;

public class GetMessageService extends Service {
	/* �������� */
	private final long cycMilliseconds = 60000;
	private final static String PUSH_SERVER_IP = "202.38.95.146";
	private final static int PUSH_SERVER_PORT = 2525;
	private String usrName = "";
	private Timer timerGetMessage;
	private boolean isGetting = false;
	private final IBinder binder = new ServiceBinder();
	NotificationManager notificationManager;
	Notification notification;
	WebView webView;

	private static InfoBackUpActivity mainActivity;
	private WakeLock mWakeLock = null; // ��Դ��

	/** �����Դ�� */
	private void acquireWakeLock() {
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE, "LocalServiceWakeLock");
			if (null != mWakeLock) {
				mWakeLock.acquire();
			}
			Log.v("LocalService", "wakelockon");
		}
	}

	/** �ͷŵ�Դ�� */
	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			mWakeLock = null;
			Log.v("LocalService", "wakelockdown");
		}
	}

	/** ��ȡ�������context */
	static void registerIntent(Context context) {
		mainActivity = (InfoBackUpActivity) context;
	}

	/** ��ʼ��ʱ���� */
	private void startTimer() {
		Log.v("LocalService", "start timer");
		timerGetMessage = new Timer();
		TimerTask getMessageTimerTask = new TimerTask() {
			@Override
			public void run() {
				while (isGetting) {
				}
				;
				getMessage();
				// setNotification("www.baidu.com");
			}
		};
		timerGetMessage.schedule(getMessageTimerTask, 0, cycMilliseconds);
	}

	/** ֹͣ��ʱ */
	private void stopTimer() {
		timerGetMessage.cancel();
	}

	private void getMessage() {
		new Thread() {
			@Override
			public void run() {
				isGetting = true;
				Log.v("getMessageService", "in new Thread");
				Socket mSocket = new Socket();
				Log.v("getMessageService", "try to get message");
				try {
					// �������ӷ���������ȡ��Ϣ
					mSocket = new Socket(PUSH_SERVER_IP, PUSH_SERVER_PORT);
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(mSocket.getOutputStream())),
							true);
					// out.println("getMessage" + "@%" + usrName); // ���Ի�ȡ��Ϣ
					out.println(usrName);
					// ���ܷ���������Ϣ
					BufferedReader br = new BufferedReader(
							new InputStreamReader(mSocket.getInputStream()));
					String mstr = br.readLine();
					Log.v("LocalService", "getMsg " + mstr);
					deelWithMessage(mstr);
					out.close();
					br.close();
					mSocket.close();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					Log.v("LocalServiceSendError", e.toString());
				}
				isGetting = false;

			}
		}.start();

	}

	/** ����õ�����Ϣ����Ҫ�ǽ�����uri��֪ͨ�û���֪ͨ���� */
	private void deelWithMessage(String msg) {
		//String start = "uri_start";
		//String end = "uri_end";
		if (!msg.equals("null")) {
			
			//String uriStr = msg.substring(msg.indexOf(start), msg.indexOf(end));
			setNotification(msg);
		}

	}

	/** ������Ϣ������֪ͨ */
	public void setNotification(String uriString) {
		Log.v("getmsgService", "notification");
		Uri uri = Uri.parse(uriString);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		PendingIntent contentIntent = PendingIntent.getActivity(
				webView.getContext(), 0, intent, 0);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification.Builder(webView.getContext()).setAutoCancel(true).setSmallIcon(R.drawable.ic_launcher).setTicker("�û���Ϊ��Ϣ�ɼ�ϵͳ").setWhen(System.currentTimeMillis())
				.setContentText("�û���Ϊ��Ϣ�ɼ�ϵͳ").setContentTitle("Info Collext")
				.setContentIntent(contentIntent).getNotification();

		// notification = new Notification(R.drawable.ic_launcher, "�û���Ϊ��Ϣ�ɼ�ϵͳ",
		// System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;

		// notification.setLatestEventInfo(webView.getContext(),
		// "֪ͨ�������ȡ����......", "�û���Ϊ��Ϣ�ɼ�ϵͳ", contentIntent);
		notificationManager.notify(R.drawable.ic_launcher, notification);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return binder;
	}

	class ServiceBinder extends Binder implements ServiceInterface {
		public GetMessageService getService() { // ���ط���
			return GetMessageService.this;
		}

		@Override
		public void show() {
			// TODO Auto-generated method stub
			Log.v("LocalSerivce", "ͨ���󶨵���");
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		acquireWakeLock();
		webView = new WebView(mainActivity);

	}

	@Override
	public void onDestroy() {
		stopTimer();
		releaseWakeLock();
		notificationManager.cancel(R.drawable.ic_launcher);
		super.onDestroy();
	}

	// @Override
	// public void onStart(Intent intent, int startId) {
	// super.onStartCommand(intent,0, startId);
	// //super.onStart(intent, startId);
	// Log.v("GetMessageService", "Service onStart");
	// }

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v("GetMessageService", "Service onUnbind");
		return super.onUnbind(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("LocalService", "Recevice start id " + startId + ": " + intent);
		// TODO ��������ʱ���ݵ���Ϣ
		getInfoFromMain(intent);

		startTimer();

		return super.onStartCommand(intent, flags, startId);
	}

	private void getInfoFromMain(Intent intent) {
		Bundle bundle;
		bundle = intent.getExtras();
		usrName = bundle.getString("usrNameValue");
	}

}
