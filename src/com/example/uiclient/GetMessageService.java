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
	/* 参数设置 */
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
	private WakeLock mWakeLock = null; // 电源锁

	/** 申请电源锁 */
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

	/** 释放电源锁 */
	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			mWakeLock = null;
			Log.v("LocalService", "wakelockdown");
		}
	}

	/** 获取主界面的context */
	static void registerIntent(Context context) {
		mainActivity = (InfoBackUpActivity) context;
	}

	/** 开始计时任务 */
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

	/** 停止计时 */
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
					// 尝试连接服务器并获取信息
					mSocket = new Socket(PUSH_SERVER_IP, PUSH_SERVER_PORT);
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(mSocket.getOutputStream())),
							true);
					// out.println("getMessage" + "@%" + usrName); // 尝试获取信息
					out.println(usrName);
					// 接受服务器的信息
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

	/** 处理得到的信息，主要是解析出uri并通知用户有通知到来 */
	private void deelWithMessage(String msg) {
		//String start = "uri_start";
		//String end = "uri_end";
		if (!msg.equals("null")) {
			
			//String uriStr = msg.substring(msg.indexOf(start), msg.indexOf(end));
			setNotification(msg);
		}

	}

	/** 设置信息到来的通知 */
	public void setNotification(String uriString) {
		Log.v("getmsgService", "notification");
		Uri uri = Uri.parse(uriString);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		PendingIntent contentIntent = PendingIntent.getActivity(
				webView.getContext(), 0, intent, 0);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification.Builder(webView.getContext()).setAutoCancel(true).setSmallIcon(R.drawable.ic_launcher).setTicker("用户行为信息采集系统").setWhen(System.currentTimeMillis())
				.setContentText("用户行为信息采集系统").setContentTitle("Info Collext")
				.setContentIntent(contentIntent).getNotification();

		// notification = new Notification(R.drawable.ic_launcher, "用户行为信息采集系统",
		// System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;

		// notification.setLatestEventInfo(webView.getContext(),
		// "通知，点击获取详情......", "用户行为信息采集系统", contentIntent);
		notificationManager.notify(R.drawable.ic_launcher, notification);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return binder;
	}

	class ServiceBinder extends Binder implements ServiceInterface {
		public GetMessageService getService() { // 返回服务
			return GetMessageService.this;
		}

		@Override
		public void show() {
			// TODO Auto-generated method stub
			Log.v("LocalSerivce", "通过绑定调用");
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
		// TODO 接收启动时传递的信息
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
