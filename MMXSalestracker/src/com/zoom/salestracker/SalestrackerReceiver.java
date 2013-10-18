package com.zoom.salestracker;

// SPRD_Owen.Chen 20120803
// salestracker application for micromax

import java.util.Calendar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.android.internal.telephony.PhoneFactory;

public class SalestrackerReceiver extends BroadcastReceiver {
	private static String TAG = "SalestrackerReceiver";
	private final int MAX_SIT_MSG_LEN = 140;
	private static String SIT_SMS_ID = "REG";// 3 characters
	private static String SIT_SMS_VER = "01";// 2 characters
	private static String SIT_MODEL_NO = "MOBHIG0001";
	private static String SIT_SW_NO = "SWNO";
	private static String SIT_HW_NO = "HWNO";
	private boolean send_sales_tracker = false;
	private final int FIRST_BOOT_SECS = 10; // 10 mins
	private final int WAIT_RETRY_SECS = 10; // 10 mins
	private final int SIT_MSG_RETRIED_TIMES = 2; // Originally it was 3
	private boolean send_first_time_failed = false;
	private int sit_msg_retried_times = 0;
	private Context mContext = null;
	public static final String ACTION_SEND_SIT = "com.zoom.salestracker.SEND_SIT";
	public static final String ACTION_SMS_SENT = "com.zoom.salestracker.SMS_SENT_ACTION";
	public static final String ACTION_ALARM_TIMER = "com.zoom.salestracker.ALARM_TIME_UP";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG, "action: " + intent.getAction());
		mContext = context;
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			Log.i(TAG, "Boot up Receiver OK!");
			// get send_sales_tracker from sharepreference
			SharedPreferences sitdata = context.getSharedPreferences(
					"salestracker", 0);
			send_sales_tracker = sitdata.getBoolean("set", false);
			Log.i(TAG, "SIT set = " + send_sales_tracker);
			// start timer
			if (send_sales_tracker == false) {
				setAlarmTimer(context, FIRST_BOOT_SECS);
			}

		} else if (intent.getAction().equals(ACTION_SMS_SENT)) {
			// sent message callback
			Log.i(TAG, "Receive SMS call back!" + getResultCode());

			switch (getResultCode()) {
			case Activity.RESULT_OK:
				send_sales_tracker = true;
				SharedPreferences third = context.getSharedPreferences(
						"salestracker", 0);
				SharedPreferences.Editor siteditor = third.edit();
				siteditor.putBoolean("set", send_sales_tracker);
				siteditor.commit();

				Intent dialogintent = new Intent();
				dialogintent.setComponent(new ComponentName(
						"com.zoom.salestracker",
						"com.zoom.salestracker.SalestrackerDialog"));
				dialogintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(dialogintent);

				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			case SmsManager.RESULT_ERROR_NO_SERVICE:
			case SmsManager.RESULT_ERROR_NULL_PDU:
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				send_first_time_failed = true;
				sit_msg_retried_times++;

				setAlarmTimer(context, WAIT_RETRY_SECS);
				break;
			}
		} else if (intent.getAction().equals(ACTION_ALARM_TIMER)) {
			Log.i(TAG, "Alarm Timer up");
			SharedPreferences sitdata = context.getSharedPreferences(
					"salestracker", 0);
			send_sales_tracker = sitdata.getBoolean("set", false);
			Log.i(TAG, "SIT set = " + send_sales_tracker);
			checkSendSITmsg();
		} else if (intent.getAction().equals(ACTION_SEND_SIT)) {
			String sim0stat = SystemProperties.get("gsm.sim.state0");
			String sim1stat = SystemProperties.get("gsm.sim.state1");
			Log.d(TAG, "sim0_standby:" + sim0stat);
			Log.d(TAG, "sim1_standby:" + sim1stat);
			// Send message

			if (sim0stat.matches("READY") || sim1stat.matches("READY")) {
				if (sim0stat.matches("READY")) {
					sendSITmsg(context, 0);
				} else {
					sendSITmsg(context, 1);
				}

			}

		} else {
		}

	}

	private void checkSendSITmsg() {
		Log.i(TAG, "checkSendSITmsg: send_sales_tracker=" + send_sales_tracker);
		if (send_sales_tracker)
			return;
		Log.i(TAG, "checkSendSITmsg: send_first_time_failed="
				+ send_first_time_failed);
		if (!send_first_time_failed) {
			String sim0stat = SystemProperties.get("gsm.sim.state0");
			String sim1stat = SystemProperties.get("gsm.sim.state1");
			Log.d(TAG, "sim0_standby:" + sim0stat);
			Log.d(TAG, "sim1_standby:" + sim1stat);

			if (sim0stat.matches("READY") || sim1stat.matches("READY")) {// send
																			// broadcast
																			// to
																			// send
																			// message
				Intent mybroadcastintent = new Intent(ACTION_SEND_SIT);
				mContext.sendStickyBroadcast(mybroadcastintent);
			} else {// start retry timer
				send_first_time_failed = true;
				sit_msg_retried_times++;

				setAlarmTimer(mContext, WAIT_RETRY_SECS);
			}
		} else {
			if (SIT_MSG_RETRIED_TIMES >= sit_msg_retried_times) {
				String sim0stat = SystemProperties.get("gsm.sim.state0");
				String sim1stat = SystemProperties.get("gsm.sim.state1");
				Log.d(TAG, "sim0_standby:" + sim0stat);
				Log.d(TAG, "sim1_standby:" + sim1stat);

				if (sim0stat.matches("READY") || sim1stat.matches("READY")) {// send
																				// broadcast
																				// to
																				// send
																				// message
					Intent mybroadcastintent = new Intent(ACTION_SEND_SIT);
					mContext.sendStickyBroadcast(mybroadcastintent);
				} else {// start retry timer
					send_first_time_failed = true;
					sit_msg_retried_times++;

					setAlarmTimer(mContext, WAIT_RETRY_SECS);
				}
			}

		}
	}

	private void sendSITmsg(Context context, int nSIM) {
		Log.i(TAG, "sendSITmsg by sim" + nSIM);

		String strDestnum = context.getString(R.string.salestrackerNUM);
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(PhoneFactory.getServiceName(
						Context.TELEPHONY_SERVICE, nSIM));
		if (tm != null)
		{
			GsmCellLocation location = (((GsmCellLocation) tm.getCellLocation()));
			if (location != null)
			{
				String strMessage = composeSITmessage(context, nSIM);
				Log.i(TAG, "sendSITmsg to strDestnum: " + strDestnum);
				Log.i(TAG, "sendSITmsg with strDestnum: " + strMessage);

				SmsManager smsManager = SmsManager.getDefault();
				PendingIntent mPI = PendingIntent.getBroadcast(context, 0, new Intent(
						ACTION_SMS_SENT), 0);
				smsManager.sendTextMessage(strDestnum, null, strMessage, mPI, null);			
			}
			else
			{
				Log.i(TAG, "sendSITmsg: no network");			
			}

		}
		else
		{
			Log.i(TAG, "sendSITmsg: no network");			
		}

	}

	private void setAlarmTimer(Context context, int min) {
		Log.i(TAG, "setAlarmTimer by min: " + min);

		AlarmManager alarmMgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(ACTION_ALARM_TIMER);
		int requestCode = 0;
		PendingIntent pendIntent = PendingIntent.getBroadcast(context,
				requestCode, intent, 0);

		Calendar calendar = Calendar.getInstance();
	    calendar.setTimeInMillis(SystemClock.elapsedRealtime());
	    calendar.add(Calendar.MINUTE, min);
	        
//		long triggerAtTime = System.currentTimeMillis() + sec * 1000;
//		Log.i(TAG, "current time: " + System.currentTimeMillis());
//		Log.i(TAG, "alarm time: " + triggerAtTime);

		alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, calendar.getTimeInMillis(), pendIntent);
	}

	private static int getNum(char c) {
		return (int) c;
	}

	private int checkSum(String msg_txt) {
		char[] charArr = msg_txt.toCharArray();
		int sum = 0;

		for (int i = 0; i < charArr.length; i++) {
			sum = sum + getNum(charArr[i]);
		}
		Log.d(TAG, "checkSum:" + sum);

		return (sum %= 255);
	}

	private String composeSITmessage(Context context, int nSIM) {
		String msg_txt = "";
		int checksum = 0;
		// To get maximum valid SIM cards no. No starts from 0 for SIM1

		// SIT title and version
		msg_txt = msg_txt + SIT_SMS_ID;
		msg_txt = msg_txt + ":";
		msg_txt = msg_txt + SIT_SMS_VER;
		msg_txt = msg_txt + ":";

		// 01 MCC-MNC
		msg_txt = msg_txt + "01";
		TelephonyManager tm = null;
		tm = (TelephonyManager) context
				.getSystemService(PhoneFactory.getServiceName(
						Context.TELEPHONY_SERVICE, nSIM));
		String networkOperator = null;
		if (tm != null)
		{
			networkOperator = tm.getNetworkOperator();			
		}

		if (networkOperator != null)
		{
			Log.d(TAG, "mnc-mcc:" + networkOperator);
			msg_txt = msg_txt + networkOperator;
			msg_txt = msg_txt + ":";			
		}
		else
		{
			Log.d(TAG, "mnc-mcc: NULL");
			msg_txt = msg_txt + "NULL";
			msg_txt = msg_txt + ":";				
		}


		// 02 CELL ID
		msg_txt = msg_txt + "02";
		GsmCellLocation location = null;
		if (tm != null)
		{
			location = (((GsmCellLocation) tm.getCellLocation()));			
		}

		if (location != null)
		{
			String cellid = Integer.toHexString(location.getCid());		
			Log.d(TAG, "cell_ID:" + cellid);
			msg_txt = msg_txt + cellid;
			msg_txt = msg_txt + ":";
		}
		else
		{
			Log.d(TAG, "cell_ID: NULL");
			msg_txt = msg_txt + "NULL";
			msg_txt = msg_txt + ":";				
		}
		
		// 03 LAC ID
		msg_txt = msg_txt + "03";
		if (location != null)
		{
			String lac = Integer.toHexString(location.getLac());
			Log.d(TAG, "Lac:" + lac);
			msg_txt = msg_txt + lac;
			msg_txt = msg_txt + ":";			
		}
		else
		{
			Log.d(TAG, "Lac: NULL");
			msg_txt = msg_txt + "NULL";
			msg_txt = msg_txt + ":";				
		}



		// 04 MODEL NO
		msg_txt = msg_txt + "04";
		msg_txt = msg_txt + SIT_MODEL_NO;
		msg_txt = msg_txt + ":";

		// 05 IMEI NO
		msg_txt = msg_txt + "05";
		String sim0stat = SystemProperties.get("gsm.sim.state0");
		String sim1stat = SystemProperties.get("gsm.sim.state1");
		if (sim0stat.matches("READY")) {
			tm = null;
			tm = (TelephonyManager) context.getSystemService(PhoneFactory
					.getServiceName(Context.TELEPHONY_SERVICE, 0));
			String imei1 = null;
			if (tm != null)
			{
				imei1 = tm.getDeviceId();			
			}
			if (imei1 != null)
			{
				Log.d(TAG, "IMEI_1:" + imei1);
				msg_txt = msg_txt + imei1;
			}
			else
			{
				Log.d(TAG, "IMEI_1: NULL");
				msg_txt = msg_txt + "NULL";				
			}
		}
		msg_txt = msg_txt + ",";
		if (sim1stat.matches("READY")) {
			tm = null;
			tm = (TelephonyManager) context.getSystemService(PhoneFactory
					.getServiceName(Context.TELEPHONY_SERVICE, 1));
			String imei2 = null;
			if (tm != null)
			{
				imei2 = tm.getDeviceId();			
			}
			if (imei2 != null)
			{
				Log.d(TAG, "IMEI_2:" + imei2);
				msg_txt = msg_txt + imei2;
			}
			else
			{
				Log.d(TAG, "IMEI_2: NULL");
				msg_txt = msg_txt + "NULL";		
			}

			msg_txt = msg_txt + ":";
		}

		// 06 Hardware No.
		msg_txt = msg_txt + "06";
		msg_txt = msg_txt + SIT_SW_NO;
		msg_txt = msg_txt + ":";

		// 07 Software No.
		msg_txt = msg_txt + "07";
		msg_txt = msg_txt + SIT_HW_NO;
		msg_txt = msg_txt + ":";

		Log.d(TAG, "All msg:" + msg_txt);

		// check sum
		checksum = checkSum(msg_txt);
		String hex_checksum = Integer.toHexString(checksum);
		msg_txt = msg_txt + hex_checksum;
		msg_txt = msg_txt + ":";
		Log.d(TAG, "All msg with checksum:" + msg_txt);

		return msg_txt;
	}

}
