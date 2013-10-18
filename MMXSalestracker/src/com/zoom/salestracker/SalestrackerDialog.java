package com.zoom.salestracker;

//SPRD_Owen.Chen 20120803
//salestracker application for micromax

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.Window;

public class SalestrackerDialog extends Activity {

	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(arg0);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		showDialog(0);
		super.onResume();
	}

	@Override
	protected Dialog onCreateDialog(int arg0) {
		// TODO Auto-generated method stub

		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage("Congratulations on purchasing Micromax handset. Press Continue to register your phone with us for warranty services, updates and more. *Standard SMS charges applicable");
		builder.setTitle("Micromax Registration");
		builder.setPositiveButton("Continue", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		});

		return builder.create();
	}

}
