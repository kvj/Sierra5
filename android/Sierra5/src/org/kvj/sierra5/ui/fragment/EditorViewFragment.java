package org.kvj.sierra5.ui.fragment;

import org.kvj.sierra5.R;
import org.kvj.sierra5.data.Controller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class EditorViewFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.editorview_fragment, container, false);
	}

	/**
	 * Loads data
	 */
	public void setController(Bundle data, Controller controller) {
	}
}
