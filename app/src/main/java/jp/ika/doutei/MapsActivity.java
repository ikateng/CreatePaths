package jp.ika.doutei;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity{
	private final static String TAG = "MainActivity";
	final MapsActivity mapsActivity = this;
	private final float DEFAULT_ZOOM_LEVEL = 14;
	private GoogleMap mMap; // Might be null if Google Play services APK is not available.
	private MainData data;
	private ArrayList<Polyline> previousPolylines;
	private float lineWidth;
	private Receiver receiver;
	private boolean markerAction;
	private ArrayList<Marker> markerList;
	private PopupWindow popupWindow;

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

		previousPolylines = new ArrayList<>();
		markerList = new ArrayList<>();
		markerAction = false;
		calcLineWidth(DEFAULT_ZOOM_LEVEL);
		setUpMapIfNeeded();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){

		menu.add(Menu.NONE, 0, Menu.NONE, "ON");
		menu.add(Menu.NONE, 1, Menu.NONE, "OFF");
		menu.add(Menu.NONE, 2, Menu.NONE, "CLEAR");
		menu.add(Menu.NONE, 3, Menu.NONE, "MARKER");

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case 0:
				if(isServiceRunning(MyService.class)) break;

				// positionListを更新して線を分断する
				if(data.positions.size() != 0)
					data.addPositionList();

				sendData();
				Toast.makeText(this, "ON", Toast.LENGTH_LONG).show();
				return true;
			case 1:
				if(!isServiceRunning(MyService.class)) break;

				stopService(new Intent(this, MyService.class));
				Toast.makeText(this, "OFF", Toast.LENGTH_LONG).show();
				return true;
			case 2:
				MainData.deleteFile(this);
				data = MainData.newInstance(this);
				clearMarkers();
				drawLines();
				if(isServiceRunning(MyService.class)) sendData();

				Toast.makeText(this, "CLEAR", Toast.LENGTH_LONG).show();
				return true;
			case 3:
				markerAction = !markerAction;
				if(markerAction){
					mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
						@Override
						public void onMapClick(LatLng latLng){
							MarkerOptions mo = new MarkerOptions().position(latLng)
									.title("New Marker")
									.snippet("");
							Marker marker = mMap.addMarker(mo);
							marker.setDraggable(true);
							markerList.add(marker);
							data.addMarkerOptions(mo, marker);
							data.save(mapsActivity);
							if(isServiceRunning(MyService.class)) sendData();

							markerAction = false;
							mMap.setOnMapClickListener(null);
						}
					});
				}else{
					mMap.setOnMapClickListener(null);
				}
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
		mMap.setMyLocationEnabled(true);

		CameraPosition cameraPos = new CameraPosition.Builder()
				.target(new LatLng(35.605123, 139.683530)) // 初期位置：大岡山キャンパス
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

		mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener(){
			@Override
			public void onInfoWindowClick(Marker marker){

				LayoutInflater li = (LayoutInflater)getBaseContext()
						.getSystemService(LAYOUT_INFLATER_SERVICE);
				View popupView = li.inflate(R.layout.popup_window, null);

				final EditText titleEdit = ((EditText)popupView.findViewById(R.id.titleEdit));
				final EditText snippetEdit = ((EditText)popupView.findViewById(R.id.snippetEdit));
				titleEdit.setText(marker.getTitle(), TextView.BufferType.EDITABLE);
				snippetEdit.setText(marker.getSnippet(), TextView.BufferType.EDITABLE);

				popupWindow = new PopupWindow(popupView,LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);

				final Marker m = marker;
				Button ok = (Button)popupView.findViewById(R.id.ok);
				ok.setOnClickListener(new Button.OnClickListener(){
					@Override
					public void onClick(View v){
						m.setTitle(titleEdit.getText().toString());
						m.setSnippet(snippetEdit.getText().toString());
						m.hideInfoWindow();
						m.showInfoWindow();

						data.updateMarkerOptions(m);
						data.save(mapsActivity);
						if(isServiceRunning(MyService.class)) sendData();

						popupWindow.dismiss();
					}
				});
				Button cancel = (Button)popupView.findViewById(R.id.cancel);
				cancel.setOnClickListener(new Button.OnClickListener(){
					@Override
					public void onClick(View v){
						popupWindow.dismiss();
					}
				});
				Button delete = (Button)popupView.findViewById(R.id.delete);
				delete.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View view){
						data.deleteMarkerOptions(m);
						data.save(mapsActivity);
						if(isServiceRunning(MyService.class)) sendData();

						clearMarkers();
						showMarkers();

						popupWindow.dismiss();
					}
				});

				popupWindow.setFocusable(true);
				popupWindow.setWidth(findViewById(R.id.map).getWidth() - 40);
				popupWindow.update();
				popupWindow.showAtLocation(findViewById(R.id.map), Gravity.CENTER, 0, 0);
			}
		});

		mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener(){
			@Override
			public void onMarkerDragStart(Marker marker){}

			@Override
			public void onMarkerDrag(Marker marker){}

			@Override
			public void onMarkerDragEnd(Marker marker){
				data.updateMarkerOptions(marker);
				data.save(mapsActivity);
				if(isServiceRunning(MyService.class))
					sendData();
			}
		});

		//mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
	}

	private void calcLineWidth(float zoom){
		lineWidth = (float) (20*Math.pow(2.0,zoom-18));
	}

	public void drawLines(){
		if(!previousPolylines.isEmpty())
			for(Polyline p : previousPolylines)
				p.remove();

		PolylineOptions geodesics;
		for(PositionList pl : data.positionLists){
			geodesics = new PolylineOptions()
					.geodesic(true)
					.color(Color.GREEN)
					.width(lineWidth);

			for(double[] pos : pl){
				geodesics.add(new LatLng(pos[0], pos[1]));
			}
			previousPolylines.add(mMap.addPolyline(geodesics));
		}
	}

	private void clearMarkers(){
		for(Marker m : markerList) m.remove();
	}

	private void showMarkers(){
		Log.i(TAG, "showMarkers");
		Marker marker;
		for(SerializableMarkerOptions smo : data.markerOptionsList){
			marker = mMap.addMarker(smo.toMarkerOptions());
			smo.setId(marker.getId());
			marker.setDraggable(true);
			markerList.add(marker);
		}
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
		showMarkers();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(popupWindow != null && popupWindow.isShowing())
			popupWindow.dismiss();
		try{
			unregisterReceiver(receiver);
		}catch(IllegalArgumentException e){}
		Log.i(TAG, "onStop");
	}

	void addNewPosition(double[] location){
		data.addPosition(location);
	}

	private boolean isServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void sendData(){
		Intent intent = new Intent(this, MyService.class);
		intent.putExtra("data", data);
		startService(intent);
	}
}
