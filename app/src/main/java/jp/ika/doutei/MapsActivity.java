package jp.ika.doutei;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements GoogleApiClient
		.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{
	private final static String TAG = "MainActivity";
	private final float DEFAULT_ZOOM_LEVEL = 14;
	private GoogleMap mMap; // Might be null if Google Play services APK is not available.
	private ArrayList<LatLng> positions;
	private Polyline previousPolyline;
	private float lineWidth;

	private GoogleApiClient googleApiClient;
	private LocationRequest locationRequest;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();

		locationRequest = new LocationRequest()
				.setInterval(10000)
				.setFastestInterval(5000)
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		positions = new ArrayList<>();
		calcLineWidth(DEFAULT_ZOOM_LEVEL);
		setUpMapIfNeeded();
	}

	/**
	 * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
	 * installed) and the map has not already been instantiated.. This will ensure that we only ever
	 * call {@link #setUpMap()} once when {@link #mMap} is not null.
	 * <p/>
	 * If it isn't installed {@link SupportMapFragment} (and
	 * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
	 * install/update the Google Play services APK on their device.
	 * <p/>
	 * A user can return to this FragmentActivity after following the prompt and correctly
	 * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
	 * have been completely destroyed during this process (it is likely that it would only be
	 * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
	 * method in {@link #onResume()} to guarantee that it will be called.
	 */
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

	/**
	 * This is where we can add markers or lines, add listeners or move the camera. In this case, we
	 * just add a marker near Africa.
	 * <p/>
	 * This should only be called once and when we are sure that {@link #mMap} is not null.
	 */
	private void setUpMap(){
		mMap.getUiSettings().setZoomControlsEnabled(true);
		mMap.getUiSettings().setCompassEnabled(true);

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

	private void drawLines(){
		if(previousPolyline != null)
			previousPolyline.remove();

		PolylineOptions geodesics = new PolylineOptions()
				.geodesic(true)
				.color(Color.RED)
				.width(lineWidth);

		for(int i = 0; i < positions.size() - 1; i++){
			geodesics.add(positions.get(i), positions.get(i + 1));
		}
		previousPolyline = mMap.addPolyline(geodesics);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart");
		googleApiClient.connect();
	}

	@Override
	protected void onResume(){
		super.onResume();
		Log.i(TAG, "onResume");
		if (googleApiClient.isConnected())
			startLocationUpdates();
		setUpMapIfNeeded();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
		stopLocationUpdates();
	}

	@Override
	protected void onStop() {
		super.onStop();
		googleApiClient.disconnect();
		Log.i(TAG, "onStop");
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
		addNewPosition(location);
		drawLines();
	}

	private void startLocationUpdates() {
		LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
	}

	private void stopLocationUpdates() {
		LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
	}

	private void addNewPosition(Location location){
		positions.add(new LatLng(location.getLatitude(), location.getLongitude()));
	}
}
