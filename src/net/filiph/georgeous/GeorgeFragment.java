package net.filiph.georgeous;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GeorgeFragment extends Fragment {
	private static final String TAG = "GeorgeFragment";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.george_big_face, container, false);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.v(TAG, "onStart called");
	}
}
