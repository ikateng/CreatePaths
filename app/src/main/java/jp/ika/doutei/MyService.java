package jp.ika.doutei;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by ikeda on 15/07/07.
 */
public class MyService extends Service implements GoogleApiClient
		.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

	public static final String ACTION = "MyService Action";
	static final String TAG="LocalService";
	private GoogleApiClient googleApiClient;
	private LocationRequest locationRequest;
	private MainData data;
	private NotificationManager nm;

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind");
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate");

		googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();

		locationRequest = new LocationRequest()
				.setInterval(10000)
				.setFastestInterval(5000)
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		showNotification();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.i(TAG, "onStartCommand Received start id " + startId + ": " + intent);

		if(intent != null)
			data = (MainData)intent.getSerializableExtra("data");
		else if(data == null)
			data = MainData.newInstance(this);

		if (!googleApiClient.isConnected() || !googleApiClient.isConnecting())
			googleApiClient.connect();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		data.save(this);
		stopLocationUpdates();
		googleApiClient.disconnect();
		nm.cancel(1);
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.i(TAG, "onConnected");
		startLocationUpdates();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.i(TAG, "onConnectionSuspended");
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.i(TAG, "onConnectionFailed");
	}
	@Override
	public void onLocationChanged(Location location) {
		Log.i(TAG, "onLocationChanged");
		PositionUpdate(location);
	}

	private void startLocationUpdates() {
		LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
	}

	private void stopLocationUpdates() {
		LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
	}

	private void PositionUpdate(Location location){
		data.addPosition(location);
		data.save(this);
		Intent intent = new Intent(ACTION);
		intent.putExtra("location", new double[]{location.getLatitude(), location.getLongitude()});
		sendBroadcast(intent);
	}

	private void showNotification() {
		PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this,
				MapsActivity.class), 0);

		Notification notification= new Notification.Builder(this)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(getString(R.string.app_name))
				.setContentText("Now Tracing...")
				.setContentIntent(intent)
				.build();

		notification.flags = Notification.FLAG_ONGOING_EVENT;
		nm.notify(1, notification);
	}
}
