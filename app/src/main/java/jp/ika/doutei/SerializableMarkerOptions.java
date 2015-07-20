package jp.ika.doutei;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.Serializable;

/**
 * Created by ikeda on 15/07/20.
 */
public class SerializableMarkerOptions implements Serializable{
	String id;
	double[] position;
	String title;
	String snippet;

	public SerializableMarkerOptions(){
		position = new double[2];
		title = "";
		snippet = "";
	}

	public SerializableMarkerOptions setId(String id){
		this.id = id;
		return this;
	}

	public SerializableMarkerOptions setPosition(double[] pos){
		position[0] = pos[0];
		position[1] = pos[1];
		return this;
	}

	public SerializableMarkerOptions setPosition(LatLng lat){
		return setPosition(new double[]{lat.latitude, lat.longitude});
	}

	public SerializableMarkerOptions setTitle(String title){
		this.title = title;
		return this;
	}

	public SerializableMarkerOptions setSnippet(String snippet){
		this.snippet = snippet;
		return this;
	}

	public LatLng getPositionLatLng(){
		return new LatLng(position[0], position[1]);
	}

	public MarkerOptions toMarkerOptions(){
		return new MarkerOptions()
				.title(this.title)
				.snippet(this.snippet)
				.position(this.getPositionLatLng());
	}
}
