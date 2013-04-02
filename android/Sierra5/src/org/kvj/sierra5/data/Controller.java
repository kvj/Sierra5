package org.kvj.sierra5.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kvj.bravo7.ipc.RemoteServicesCollector;
import org.kvj.bravo7.ipc.RemoteServicesCollector.APIPluginFilter;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.data.provider.NodeID;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.common.root.Root;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.data.provider.DataProvider;
import org.kvj.sierra5.data.provider.DataProvider.EditType;
import org.kvj.sierra5.data.provider.impl.SDCardDataProvider;
import org.kvj.sierra5.ui.adapter.ListViewAdapter;
import org.kvj.sierra5.ui.adapter.theme.ThemeProvider;
import org.kvj.sierra5.ui.plugin.LocalPlugin;
import org.kvj.sierra5.ui.plugin.impl.ClipboardPlugin;

import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Holds all controller methods. All views and services have and access to
 * instance
 * 
 * @author Kostya
 * @param <E>
 * 
 */
public class Controller<E> {

	private static final String TAG = "Controller";
	private RemoteServicesCollector<Plugin> plugins = null;

	private List<LocalPlugin> localPlugins = new ArrayList<LocalPlugin>();

	private ClipboardPlugin clipboardPlugin = null;
	private App app = null;

	private Map<String, DataProvider<? extends NodeID>> providers = new HashMap<String, DataProvider<? extends NodeID>>();

	public Controller(App app) {
		this.app = app;
		SDCardDataProvider sdCardDataProvider = new SDCardDataProvider();
		providers.put(sdCardDataProvider.getId(), sdCardDataProvider);
		plugins = new RemoteServicesCollector<Plugin>(app, Constants.PLUGIN_NS, new APIPluginFilter()) {

			@Override
			public Plugin castAIDL(IBinder binder) {
				return Plugin.Stub.asInterface(binder);
			}

			@Override
			public void onChange() {
				// List<Plugin> list = getPlugins();
				// Log.i(TAG, "Plugins: " + list.size());
				// try {
				// for (Plugin plugin : list) {
				// Log.i(TAG, "Plugin: " + plugin.getName());
				// }
				// } catch (RemoteException e) {
				// e.printStackTrace();
				// }
			}
		};
		clipboardPlugin = new ClipboardPlugin(this);
		localPlugins.add(clipboardPlugin);
	}

	/**
	 * Expands or collapses Node
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Node> expand(Node node, int expandType) {
		DataProvider provider = getProvider((NodeID) node.id);
		if (null == provider) { // Not found
			Log.e(TAG, "Provider not found");
			return null;
		}
		return provider.expand(node, expandType);
	}

	private DataProvider<? extends NodeID> getProvider(NodeID id) {
		try { // Conversion errors
			return providers.get(id.getProviderID());
		} catch (Exception e) {
			Log.e(TAG, "Provider not found: " + id, e);
		}
		return null;
	}

	/**
	 * Constructs Node by file path
	 */
	@SuppressWarnings("rawtypes")
	public Node nodeFromParcelable(Parcelable p) {
		if (!(p instanceof NodeID)) { // Invalid type
			return null;
		}
		DataProvider provider = getProvider((NodeID) p);
		if (null == provider) { // Not found
			Log.e(TAG, "Provider not found");
			return null;
		}
		return provider.find((NodeID) p);
	}

	public int getNodeSize(Node n) {
		if (n.collapsed || null == n.children) { // Collapsed
			return 1;
		}
		int result = 1;
		for (int i = 0; i < n.children.size(); i++) { // Every children
			result += getNodeSize((Node) n.children.get(i));
		}
		return result;
	}

	public Node getRoot() {
		DataProvider provider = providers.values().iterator().next();
		return provider.getRoot();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String getEditableContents(Node node) {
		DataProvider provider = getProvider((NodeID) node.id);
		if (null == provider) { // Not found
			Log.e(TAG, "Provider not found");
			return null;
		}
		return provider.getEditable(node);
	}

	@SuppressWarnings("unchecked")
	public Node editNode(EditType type, Node node, String raw) {
		DataProvider provider = getProvider((NodeID) node.id);
		if (null == provider) { // Not found
			Log.e(TAG, "Provider not found");
			return null;
		}
		return provider.edit(type, node, raw);
	}

	private Node expandTo(DataProvider provider, Node root, String[] selectPath, int expandType) {
		String[] rootPath = getPath(root);
		Node node = root; // Will use in cycle
		for (int i = 0; i < selectPath.length; i++) { // Check every item
			if (i < rootPath.length) { // Part of root check if root is same
				if (!rootPath[i].equals(selectPath[i])) { // Failed
					Log.e(TAG, "Path item is invalid: " + rootPath[i] + ", " + selectPath[i]);
					return null;
				}
			} else {
				// After root - expand
				List<Node> children = provider.expand(node, expandType);
				if (null == children) { // Failed to expand
					Log.e(TAG, "Expand failed: " + node.text);
					return null;
				}
				node.children = children;
				node.collapsed = false;
				boolean found = false;
				for (Node ch : children) { // Search by text
					if (ch.text.equals(selectPath[i])) { // Found
						// Log.i(TAG, "Found child: " + selectPath[i]);
						found = true;
						node = ch;
						break;
					}
				}
				if (!found) { // Not found
					Log.e(TAG, "Child not found: " + selectPath[i]);
					return null;
				}
			}
		}
		if (null == node.children) { // No children loaded
			node.children = provider.expand(node, expandType);
		}
		if (null != node.children) { // Have children - expand
			node.collapsed = false;
		}
		return node;
	}

	public boolean expandTo(Node root, Node select, int expandType) {
		DataProvider provider = getProvider((NodeID) root.id);
		if (null == provider) { // Not found
			Log.e(TAG, "Provider not found");
			return false;
		}
		// Log.i(TAG, "Expand to: " + root.id + " and " + select.id);
		String[] rootPath = getPath(root);
		String[] selectPath = getPath(select);
		if (null == rootPath || null == selectPath) { // Invalid path
			Log.e(TAG, "Path is invalid: " + rootPath + ", " + selectPath);
			return false;
		}
		return null != expandTo(provider, root, selectPath, expandType);
	}

	@SuppressWarnings("unchecked")
	public boolean removeNode(Node removeMe) {
		DataProvider provider = getProvider((NodeID) removeMe.id);
		if (null == provider) { // Not found
			Log.e(TAG, "Provider not found");
			return false;
		}
		return provider.remove(removeMe);
	}

	public Node getParent(Node node) {
		DataProvider provider = getProvider((NodeID) node.id);
		if (null == provider) { // Not found
			Log.e(TAG, "Provider not found");
			return null;
		}
		return provider.getParent(node);
	}

	@SuppressWarnings("unchecked")
	public String[] getPath(Node node) {
		DataProvider provider = getProvider((NodeID) node.id);
		if (null == provider) { // Not found
			Log.e(TAG, "Provider not found");
			return null;
		}
		List<Node> path = provider.getPath(node);
		if (null == path) { // Failed to build
			Log.e(TAG, "Failed to build path: " + node.text);
			return null;
		}
		String[] result = new String[path.size()];
		for (int i = 0; i < path.size(); i++) { // Copy text to result
			result[i] = path.get(i).text;
		}
		return result;
	}

	private Root.Stub stub = new Root.Stub() {

		@Override
		public RemoteViews render(Node node, String left, String theme) throws RemoteException {
			return ListViewAdapter.renderRemote(Controller.this, node, left, theme);
		}

		@Override
		public boolean putFile(String to, String path, String text) throws RemoteException {
			try { // IO errors
				Log.i(TAG, "putFile: " + to + ", " + path + ", " + text);
				InputStream reader = null;
				File toFile = new File(to);
				if (!toFile.getParentFile().exists()) { // Not exist yet
					if (!toFile.getParentFile().mkdirs()) { // Mkdirs failed
						Log.e(TAG, "mkdirs failed: " + to);
						return false;
					}
				}
				if (toFile.isDirectory()) { // No folders here
					Log.e(TAG, "File is folder: " + to);
					return false;
				}
				if (null != path) { // Copy files
					File fromFile = new File(path);
					if (!fromFile.exists() || !fromFile.isFile()) {
						// Invalid file
						Log.e(TAG, "Invalid file: " + path);
						return false;
					}
					reader = new FileInputStream(fromFile);
				} else { // Write string
					reader = new ByteArrayInputStream(text.getBytes("utf-8"));
				}
				int BUFFER_SIZE = 4096;
				BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(toFile, false), BUFFER_SIZE);
				BufferedInputStream breader = new BufferedInputStream(reader, BUFFER_SIZE);
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead = 0;
				while ((bytesRead = breader.read(buffer)) > 0) { // Have data
					writer.write(buffer, 0, bytesRead);
				}
				breader.close();
				writer.close();
				Log.i(TAG, "Copy done");
				return true;
			} catch (Exception e) {
				Log.e(TAG, "Error copying file: ", e);
			}
			return false;
		}

		@Override
		public Theme[] getThemes() throws RemoteException {
			return ThemeProvider.getThemes();
		}

		@SuppressWarnings({ "rawtypes" })
		@Override
		public Node getNode(String[] path) throws RemoteException {
			DataProvider provider = providers.values().iterator().next();
			Node root = provider.getRoot();
			return expandTo(provider, root, path, Node.EXPAND_ONE);
		}

		@Override
		public boolean update(Node node, String text, String raw) throws RemoteException {
			if (null != text) { // Update text
				node.text = text;
			}
			return editNode(EditType.Replace, node, raw) != null;
		}

		@Override
		public List<Node> expand(Node node, boolean expand) throws RemoteException {
			return Controller.this.expand(node, Node.EXPAND_FORCE);
		}

		@Override
		public Node append(Node node, String raw) throws RemoteException {
			return editNode(EditType.Append, node, raw);
		}
	};

	public Root.Stub getRootService() {
		return stub;
	}

	private <T extends Plugin> boolean checkPlugin(Class<T> cl, Plugin plugin, int[] types) throws RemoteException {
		if (!cl.isAssignableFrom(plugin.getClass())) { // Invalid type
			return false;
		}
		for (int type : types) {
			if (PluginInfo.PLUGIN_ANY == type) { // Any plugin
				return true;
			}
		}
		int[] caps = plugin.getCapabilities();
		boolean found = false;
		for (int cap : caps) {
			// Search for capability
			for (int type : types) { //
				if (cap == type) { // Found
					found = true;
					break;
				}
			}
			if (found) { // Found - stop checking
				break;
			}
		}
		return found;
	}

	public List<Plugin> getPlugins(int... types) {
		return getPlugins(Plugin.class, types);
	}

	public <T extends Plugin> List<T> getPlugins(Class<T> cl, int... types) {
		List<T> result = new ArrayList<T>();
		boolean pluginsEnabled = app.getBooleanPreference(R.string.usePlugins, R.string.usePluginsDefault);
		if (!pluginsEnabled) { // Not enabled
			return result;
		}
		List<Plugin> pls = plugins.getPlugins();
		for (Plugin plugin : localPlugins) {
			// Local plugins
			try { // Remote errors
				if (checkPlugin(cl, plugin, types)) { // Plugin OK
					result.add((T) plugin);
				}
			} catch (Exception e) {
				// Ignore remote errors
			}
		}
		for (Plugin plugin : pls) {
			// Remote errors
			try { // Remote errors
				if (checkPlugin(cl, plugin, types)) { // Plugin OK
					result.add((T) plugin);
				}
			} catch (Exception e) {
				// Ignore remote errors
			}
		}
		return result;
	}

	public ClipboardPlugin getClipboard() {
		return clipboardPlugin;
	}

}
