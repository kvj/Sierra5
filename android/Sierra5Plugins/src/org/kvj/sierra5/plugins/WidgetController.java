package org.kvj.sierra5.plugins;

import org.kvj.bravo7.ipc.RemoteServiceConnector;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.root.Root;

import android.content.Context;
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

	private Plugin.Stub widgetPlugin = new Plugin.Stub() {

		@Override
		public String getName() throws RemoteException {
			return "Widget plugin";
		}
	};

	private Plugin.Stub linkPlugin = new Plugin.Stub() {

		@Override
		public String getName() throws RemoteException {
			return "Link plugin";
		}
	};

	public Plugin.Stub getWidgetPlugin() {
		return widgetPlugin;
	}

	public Plugin.Stub getLinkPlugin() {
		return linkPlugin;
	}
}
