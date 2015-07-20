package jp.ika.doutei;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ikeda on 15/07/19.
 */
public class MainData implements Serializable{
	public static final String dataFileName = "doutei_data";
	public ArrayList<PositionList> positionLists;
	public PositionList positions;
	public ArrayList<SerializableMarkerOptions> markerOptionsList;

	MainData(){
		positionLists = new ArrayList<>();
		markerOptionsList = new ArrayList<>();
		addPositionList();
	}

	static public MainData newInstance(Context context){
		MainData instance = null;

		if(context.getFileStreamPath(dataFileName).exists()){
			InputStream in = null;
			ObjectInputStream ois = null;
			try{
				in = context.openFileInput(dataFileName);
				ois = new ObjectInputStream(in);
				instance = (MainData) ois.readObject();
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				try{
					if(ois != null) ois.close();
					if(in != null) in.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}

			return instance;
		}else{
			return new MainData();
		}
	}

	static public void deleteFile(Context context){
		if(context.getFileStreamPath(dataFileName).exists()){
			context.deleteFile(dataFileName);
		}
	}

	public boolean save(Context context){
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = context.openFileOutput(dataFileName, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally{
			try {
				if (oos != null) oos.close();
				if (fos != null) fos.close();
			}catch(IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public void addPositionList(){
		positions = new PositionList();
		positionLists.add(positions);
	}

	public void addPosition(double[] location){
		positions.add(location);
	}

	public void addPosition(Location location){
		positions.add(new double[]{location.getLatitude(), location.getLongitude()});
	}

	public void addMarkerOptions(MarkerOptions mo, Marker m){
		markerOptionsList.add(new SerializableMarkerOptions()
				.setPosition(mo.getPosition())
				.setTitle(mo.getTitle())
				.setSnippet(mo.getSnippet())
				.setId(m.getId()));
	}

	public void updateMarkerOptions(Marker m){
		for(SerializableMarkerOptions smo : markerOptionsList){
			if(smo.id.equals(m.getId())){
				smo.setPosition(m.getPosition())
						.setTitle(m.getTitle())
						.setSnippet(m.getSnippet());
				return;
			}
		}
	}

	public void deleteMarkerOptions(Marker m){
		for(SerializableMarkerOptions smo : markerOptionsList){
			if(smo.id.equals(m.getId())){
				markerOptionsList.remove(markerOptionsList.indexOf(smo));
				return;
			}
		}
	}
}
