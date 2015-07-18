package jp.ika.doutei;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsActivity extends FragmentActivity{
	private final static String TAG = "MainActivity";
	private final float DEFAULT_ZOOM_LEVEL = 14;
	private GoogleMap mMap; // Might be null if Google Play services APK is not available.
	private MainData data;
	private Polyline previousPolyline;
	private float lineWidth;
	private Receiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		receiver = new Receiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(MyService.ACTION);
		registerReceiver(receiver, filter);
		//MainData.deleteFile(this);
		data = MainData.newInstance(this);
		calcLineWidth(DEFAULT_ZOOM_LEVEL);
		setUpMapIfNeeded();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){

		menu.add(Menu.NONE, 0, Menu.NONE, "ON");
		menu.add(Menu.NONE, 1, Menu.NONE, "OFF");
		menu.add(Menu.NONE, 2, Menu.NONE, "CLEAR");

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case 0:
				startService(new Intent(this, MyService.class));
				Toast.makeText(this, "ON", Toast.LENGTH_LONG).show();
				return true;
			case 1:
				stopService(new Intent(this, MyService.class));
				Toast.makeText(this, "OFF", Toast.LENGTH_LONG).show();
				return true;
			case 2:
				Toast.makeText(this, "CLEAR", Toast.LENGTH_LONG).show();
				return true;
		}
		return false;
	}

	private void setUpMapIfNeeded(){
		// Do a null check to confirm that we have not already instantiated the map.
		if(mMap == null){
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if(mMap != null){
				setUpMap();
			}
		}
	}

	private void setUpMap(){
		mMap.getUiSettings().setZoomControlsEnabled(true);
		mMap.getUiSettings().setCompassEnabled(true);
		mMap.getUiSettings().setMyLocationButtonEnabled(true);

		CameraPosition cameraPos = new CameraPosition.Builder()
				.target(new LatLng(35.605123, 139.683530))
				.zoom(DEFAULT_ZOOM_LEVEL)
				.build();
		mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos));

		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener(){
			@Override
			public void onCameraChange(CameraPosition cameraPosition){
				calcLineWidth(cameraPosition.zoom);
				drawLines();
			}
		});

		//mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
	}

	private void calcLineWidth(float zoom){
		lineWidth = (float) (20*Math.pow(2.0,zoom-18));
	}

	public void drawLines(){
		Log.i(TAG, "drawLines");
		if(previousPolyline != null)
			previousPolyline.remove();

		PolylineOptions geodesics = new PolylineOptions()
				.geodesic(true)
				.color(Color.GREEN)
				.width(lineWidth);

		for(int i = 0; i < data.positions.size() - 1; i++){
			geodesics.add(data.getLatLng(i), data.getLatLng(i+1));
		}
		previousPolyline = mMap.addPolyline(geodesics);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart");
	}

	@Override
	protected void onResume(){
		super.onResume();
		Log.i(TAG, "onResume");
		setUpMapIfNeeded();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
		Log.i(TAG, "onStop");
	}

	void addNewPosition(double[] location){
		data.addPosition(location);
	}
}
