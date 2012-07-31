package org.kvj.sierra5.plugins;

import org.kvj.bravo7.ipc.RemoteServiceConnector;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.root.Root;
import org.kvj.sierra5.plugins.impl.check.CheckboxPlugin;
import org.kvj.sierra5.plugins.impl.link.LinkPlugin;
import org.kvj.sierra5.plugins.impl.widget.WidgetPlugin;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class WidgetController {

	private RemoteServiceConnector<Root> root = null;
	protected String TAG = "WidgetController";

	public WidgetController(Context ctx) {
		Log.i(TAG, "Starting controller");
		root = new RemoteServiceConnector<Root>(ctx, Constants.ROOT_NS, null) {

			@Override
			public Root castAIDL(IBinder binder) {
				return Root.Stub.asInterface(binder);
			}

			@Override
			public void onConnect() {
				super.onConnect();
				try {
					Log.i(TAG, "Root interface connected: "
							+ root.getRemote().getRoot());
					App.getInstance().updateWidgets(-1);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onDisconnect() {
				super.onDisconnect();
				Log.i(TAG, "Root interface disconnected");
			}
		};
	}

	private Plugin.Stub widgetPlugin = new WidgetPlugin();

	private Plugin.Stub linkPlugin = new LinkPlugin();

	private Plugin.Stub checkboxPlugin = new CheckboxPlugin(this);

	public Root getRootService() {
		return root.getRemote();
	}

	public Plugin.Stub getWidgetPlugin() {
		return widgetPlugin;
	}

	public Plugin.Stub getLinkPlugin() {
		return linkPlugin;
	}

	public Binder getCheckboxPlugin() {
		return checkboxPlugin;
	}
}
