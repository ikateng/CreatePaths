package jp.ika.doutei;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by ikeda on 15/07/18.
 */
public class Receiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("Receiver", "onReceive");
		double[] location = intent.getDoubleArrayExtra("location");
		MapsActivity activity = (MapsActivity) context;
		activity.addNewPosition(location);
		activity.drawLines();
	}
}
