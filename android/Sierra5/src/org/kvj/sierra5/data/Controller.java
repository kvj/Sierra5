package org.kvj.sierra5.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.bravo7.ipc.RemoteServicesCollector;
import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.Plugin;
import org.kvj.sierra5.common.root.Root;

import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

/**
 * Holds all controller methods. All views and services have and access to
 * instance
 * 
 * @author Kostya
 * 
 */
public class Controller {

	private static final String TAG = "Controller";
	private Pattern left = Pattern.compile("^\\s*");
	private RemoteServicesCollector<Plugin> plugins = null;

	public Controller() {
		plugins = new RemoteServicesCollector<Plugin>(App.getInstance(),
				Constants.PLUGIN_NS) {

			@Override
			public Plugin castAIDL(IBinder binder) {
				return Plugin.Stub.asInterface(binder);
			}

			@Override
			public void onChange() {
				List<Plugin> list = getPlugins();
				Log.i(TAG, "Plugins: " + list.size());
				try {
					for (Plugin plugin : list) {
						Log.i(TAG, "Plugin: " + plugin.getName());
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		};
		parsePattern("/xx/?.xml/w${w+2d}/w${dd}");
	}

	public static interface LineEater {

		public void eat(int index, Node node, String left, String line);

		public boolean filter(int index, Node node);
	}

	private int writeOneNode(int index, int removeLeft, Node node,
			LineEater eater) {
		StringBuilder spaces = new StringBuilder();
		String[] lines = null;
		if (!eater.filter(index, node)) { // Filtered out
			return index;
		}
		boolean skipFirstLine = false;
		if (null != node.raw) { // Have raw data
			lines = node.raw.split("\n");
		} else { // Not changed
			if (node.type == Node.TYPE_FILE) {
				// Don't write first line - it's file name
				skipFirstLine = true;
			}
			lines = new String[] { node.text };
		}
		for (int i = 0; i < node.left.length() - removeLeft; i++) {
			// Add spaces
			spaces.append(' ');
		}
		int result = index;
		for (int i = 0; i < lines.length; i++) { // For every line
			if (i == 0 && skipFirstLine) { // Skip line
				continue;
			}
			String line = lines[i];
			if (TextUtils.isEmpty(line)) { // Empty line - write as empty
				eater.eat(result, node, "", "");
			} else {
				Matcher m = this.left.matcher(line);
				String leftSpaces = "";
				if (m.find()) { // Have spaces
					leftSpaces = m.group(0);
				}
				eater.eat(result, node, spaces.toString() + leftSpaces,
						line.trim());
			}
			result++;
		}
		if (null == node.raw && null != node.children) {
			// Process children
			for (int i = 0; i < node.children.size(); i++) {
				// Write every child
				result = writeOneNode(result, removeLeft, node.children.get(i),
						eater);
			}
		}
		if (null != node.raw && node.type == Node.TYPE_TEXT) {
			// Have raw data and it's text - update text
			node.raw = null;
			node.text = lines[0];
			if (null != node.textPath && node.textPath.size() > 0) {
				// Replace last textPath
				node.textPath.remove(node.textPath.size() - 1);
				node.textPath.add(node.text);
			}
		}
		return result;
	}

	private void writeNode(Node node, LineEater eater) {
		writeOneNode(0, node.left.length(), node, eater);
	}

	public String getEditableContents(final Node parent) {
		final StringBuilder buffer = new StringBuilder();
		writeNode(parent, new LineEater() {

			@Override
			public void eat(int index, Node node, String left, String line) {
				if (index > 0) { // Have data - CR
					buffer.append('\n');
				}
				buffer.append(left);
				buffer.append(line);
			}

			@Override
			public boolean filter(int index, Node node) {
				return true;
			}
		});
		return buffer.toString();
	}

	public boolean saveFile(Node node, final Node remove) {
		try { // Catch save errors
			final String crlf = App.getInstance().getBooleanPreference(
					R.string.useCRLF, false) ? "\r\n" : "\n";
			final boolean expandTab = App.getInstance().getBooleanPreference(
					R.string.expandTabs, false);
			final String tabRepl;
			if (!expandTab) { // Replace spaces with tab
				int tabSize = App.getInstance().getIntPreference(
						R.string.tabSize, R.string.tabSizeDefault);
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < tabSize; i++) {
					// Add spaces
					sb.append(' ');
				}
				tabRepl = sb.toString();
			} else {
				tabRepl = null;
			}
			File file = new File(node.file);
			final BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
			writeNode(node, new LineEater() {

				@Override
				public void eat(int index, Node node, String left, String line) {
					try {
						if (!expandTab) {
							// Need tabs
							left = left.replace(tabRepl, "\t");
						}
						writer.write(left + line + crlf);
					} catch (IOException e) {
						Log.e(TAG, "Error writing file:", e);
					}
				}

				@Override
				public boolean filter(int index, Node node) {
					if (node == remove) { // We need to remove it - skip
						return false;
					}
					return true;
				}
			});
			writer.close();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error saving file:", e);
		}
		return false; // Not implemented
	}

	private boolean parseFile(Node node) {
		try { // Catch all stream errors
			int spacesInTab = App.getInstance().getIntPreference(
					R.string.tabSize, R.string.tabSizeDefault);
			StringBuilder tabReplacement = new StringBuilder();
			for (int i = 0; i < spacesInTab; i++) { // Add spaces
				tabReplacement.append(' ');
			}
			// Log.i(TAG, "Parse file: " + node.file);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(node.file), "utf-8"));
			node.children = new ArrayList<Node>(); // Reset children
			Stack<Node> parents = new Stack<Node>();
			String line = null;
			while ((line = br.readLine()) != null) {
				if (TextUtils.isEmpty(line.trim())) {
					// Line is empty - no left chars
					line = "";
				}
				String leftChars = "";
				Matcher m = left.matcher(line);
				if (m.find()) { // Have space
					leftChars = m.group(0).replace("\t", tabReplacement);
				}
				Node parent = null;
				boolean fromParents = false;
				// Log.i(TAG, "Search parent: " + n.text + ", " +
				// parents.size());
				do { // Search for parent
					if (parents.isEmpty()) {
						// No more parents - parent is file
						parent = node;
						break;
					}
					parent = parents.pop();
					// Log.i(TAG, "Parse " + parent.text + " and " + n.text +
					// ", "
					// + parent.left.length() + ", " + n.left.length());
					if (parent.left.length() < leftChars.length()) {
						// Real parent
						fromParents = true;
						break;
					}
				} while (true);
				Node n = parent.createChild(Node.TYPE_TEXT, line.trim(), 0);
				n.collapsed = false; // All files are expanded by default
				n.left = leftChars;
				if (fromParents) { // Return parent back
					parents.push(parent);
				}
				parents.push(n);
				// Log.i(TAG, "Line: " + n.text + ", " + n.level + ", "
				// + parent.text);
			}
			br.close();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error reading file:", e);
		}
		return false; // Not implemented
	}

	class ItemPattern {
		boolean inverted = false;
		Pattern pattern = null;

		boolean matches(String text) {
			boolean matches = pattern.matcher(text).find();
			if ((matches && inverted) || (!matches && !inverted)) {
				return false;
			}
			return true;
		}
	}

	private static Pattern mask = Pattern
			.compile("\\*|\\?|(\\$\\{([a-zA-Z]+)((\\+|\\-)(\\d{1,3})(h|d|w|m|y))?\\})");

	// 2: dd 3: +2d 4: + 5: 2

	private ItemPattern parsePattern(String text) {
		StringBuffer pattern = new StringBuffer();
		ItemPattern res = new ItemPattern();
		if (text.charAt(0) == '!') {
			text = text.substring(1);
			res.inverted = true;
		}
		Matcher m = mask.matcher(text);
		while (m.find()) {
			String repl = "";
			String found = m.group();
			if ("*".equals(found)) {
				repl = ".*";
			} else if ("?".equals(found)) {
				repl = ".";
			} else {
				// Date replacement
				Calendar c = Calendar.getInstance();
				if (null != m.group(3)) { // Have date modifier
					int mul = "+".equals(m.group(4)) ? 1 : -1;
					int value = Integer.parseInt(m.group(5), 10);
					if ("h".equals(m.group(6))) { // Hour
						c.add(Calendar.HOUR, mul * value);
					} else if ("d".equals(m.group(6))) { // Day
						c.add(Calendar.DAY_OF_YEAR, mul * value);
					} else if ("w".equals(m.group(6))) { // Week
						c.add(Calendar.DAY_OF_YEAR, mul * value * 7);
					} else if ("m".equals(m.group(6))) { // Month
						c.add(Calendar.MONTH, mul * value);
					} else if ("y".equals(m.group(6))) { // Year
						c.add(Calendar.YEAR, mul * value);
					}
				}
				SimpleDateFormat dt = new SimpleDateFormat(m.group(2),
						Locale.ENGLISH);
				repl = dt.format(c.getTime());
			}
			m.appendReplacement(pattern, repl);
		}
		m.appendTail(pattern);
		pattern.append('$');
		res.pattern = Pattern.compile("^" + pattern.toString(),
				Pattern.CASE_INSENSITIVE);
		return res;
	}

	/**
	 * Expands or collapses Node
	 * 
	 * @param node
	 * @param forceExpand
	 * @param restoreExpand
	 */
	public boolean expand(Node node, Boolean forceExpand, Node restoreExpand) {
		boolean newStateCollapsed = null != forceExpand ? !forceExpand
				: !node.collapsed;
		// Log.i(TAG, "expand " + node.text + ", " + node.collapsed + ", "
		// + node.type);
		if (Node.TYPE_TEXT == node.type) { // Text - just change flag
			node.collapsed = newStateCollapsed;
			return true; // Done
		}
		if (Node.TYPE_FOLDER == node.type) {
			// Folder - use File methods to get folder and files
			File folder = new File(node.file);
			if (!folder.exists() || !folder.isDirectory()) {
				// Folder not accessible
				return false; // Error
			}
			node.collapsed = newStateCollapsed;
			if (newStateCollapsed) { // Just set children to NULL
				node.children = null;
				return true; // Done
			}
			ItemPattern filePattern = parsePattern(App.getInstance()
					.getStringPreference(R.string.filePattern,
							R.string.filePatternDefault));
			ItemPattern folderPattern = parsePattern(App.getInstance()
					.getStringPreference(R.string.folderPattern,
							R.string.folderPatternDefault));

			File[] files = folder.listFiles();
			List<Node> nodes = new ArrayList<Node>();
			// Log.i(TAG,
			// "Load dir: " + files.length + ", "
			// + folder.getAbsolutePath());
			for (File file : files) { // For every file/folder in folder
				ItemPattern patt = file.isDirectory() ? folderPattern
						: filePattern;
				boolean matches = patt.pattern.matcher(file.getName()).find();
				if ((matches && patt.inverted) || (!matches && !patt.inverted)) {
					continue;
				}
				Node child = node.createChild(
						file.isDirectory() ? Node.TYPE_FOLDER : Node.TYPE_FILE,
						file.getName(), 0);
				child.file = file.getAbsolutePath();
				nodes.add(child);
			}
			Collections.sort(nodes, new Comparator<Node>() {

				@Override
				public int compare(Node lhs, Node rhs) {
					if (lhs.type == Node.TYPE_FOLDER
							&& rhs.type == Node.TYPE_FILE) { // Folder first
						return -1;
					}
					if (lhs.type == Node.TYPE_FILE
							&& rhs.type == Node.TYPE_FOLDER) { // Folder first
						return 1;
					}
					return lhs.text.compareToIgnoreCase(rhs.text);
				}
			});
			node.children = nodes;
			return true;
		}
		if (Node.TYPE_FILE == node.type) { // File - not implemented yet
			node.collapsed = newStateCollapsed;
			if (newStateCollapsed) { // Collapsed
				node.children = null;
				return true; // Done
			}
			return parseFile(node);
		}
		return false;
	}

	/**
	 * Constructs Node by file path
	 */
	public Node nodeFromPath(String path) {
		// Log.i(TAG, "nodeFromPath: " + path);
		if (TextUtils.isEmpty(path)) { // Invalid path
			return null;
		}
		File file = new File(getRootFolder());
		if (!file.exists()) { // Not exists
			return null;
		}
		Node node = new Node();
		node.file = file.getAbsolutePath();
		node.text = file.getName();
		node.type = file.isDirectory() ? Node.TYPE_FOLDER : Node.TYPE_FILE;
		SearchNodeResult result = searchInNode(node, path, null);
		if (null == result || !result.found) { // Not found
			return null;
		}
		return result.node;
	}

	public static class SearchNodeResult {
		public boolean found = false;
		public Node node = null;
		public int index = 0;
	}

	private List<String> findPath(String root, String file) {
		if (!file.startsWith(root)) { // Not a same tree
			return null;
		}
		List<String> result = new ArrayList<String>();
		String[] parts = file.substring(root.length()).split(File.separator);
		for (String part : parts) { // Copy parts
			if (!TextUtils.isEmpty(part)) { // Not empty - add to result
				result.add(part);
			}
		}
		return result;
	}

	public int getNodeSize(Node n) {
		if (n.collapsed || null == n.children) { // Collapsed
			return 1;
		}
		int result = 1;
		for (int i = 0; i < n.children.size(); i++) { // Every children
			result += getNodeSize(n.children.get(i));
		}
		return result;
	}

	public SearchNodeResult searchInNode(Node root, String file, String[] path) {
		File rootFile = new File(root.file);
		if (!rootFile.exists()) { // Invalid file
			Log.w(TAG,
					"Root not exists: " + root.file + " " + rootFile.exists());
			return null;
		}
		List<String> pathToRoot = findPath(root.file, file);
		if (null == pathToRoot) { // Different trees
			Log.w(TAG, "Different trees: " + root.file + " - " + file);
			return null;
		}
		List<String> pathToSearch = new ArrayList<String>();
		// Copy path
		for (int i = 0; i < pathToRoot.size(); i++) { // Copy
			pathToSearch.add(pathToRoot.get(i));
		}
		if (null != path) { // Have path
			Log.i(TAG, "Path: " + path.length);
			for (String p : path) { // Copy path
				Log.i(TAG, "Add path: " + p + ", " + pathToSearch.size());
				pathToSearch.add(p);
			}
		}
		int index = 0; // Index of found item in tree
		Node n = root;
		Log.i(TAG, "Before search: " + pathToSearch + ", " + path);
		for (int depth = 0; depth < pathToSearch.size(); depth++) {
			// For every item
			if (!expand(n, true, null)) { // Expand failed
				Log.w(TAG, "Expand failed: " + n.file + ", " + n.textPath);
				return null;
			}
			boolean found = false;
			int siblingSize = 0;
			ItemPattern ip = parsePattern(pathToSearch.get(depth));
			// Log.i(TAG, "Pattern: " + ip.pattern.pattern());
			for (int i = 0; i < n.children.size(); i++) { // Search child
				Node ch = n.children.get(i);
				if (ip.matches(ch.text)) {
					// From end - found
					n = ch;
					index += siblingSize + 1;
					found = true;
					break;
				} else {
					siblingSize += getNodeSize(ch);
				}
			}
			if (!found) { // Item not found - return partial result
				SearchNodeResult result = new SearchNodeResult();
				result.index = index;
				result.node = n;
				return result;
			}
		}
		if (!expand(n, true, null)) { // Force expand last item
			Log.w(TAG, "Expand failed: " + n.file + ", " + n.textPath);
			return null;
		}
		SearchNodeResult result = new SearchNodeResult();
		result.index = index;
		result.node = n;
		result.found = true;
		return result;
	}

	public Node[] actualizeNode(Node node) {
		if (null == node || null == node.file) { // Invalid node
			return new Node[0];
		}
		Node parent = nodeFromPath(node.file);
		SearchNodeResult res = searchInNode(parent, node.file,
				node.textPath != null ? node.textPath.toArray(new String[0])
						: null);
		if (null == res || !res.found) { // Not found - parent only
			return new Node[] { parent };
		}
		return new Node[] { parent, res.node }; // All done
	}

	public boolean removeNode(Node file, Node removeMe) {
		return saveFile(file, removeMe);
	}

	public String getRootFolder() {
		String rootFolderParam = App.getInstance().getResources()
				.getString(R.string.rootFolder);
		String sdCardPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath();
		String rootFolder = App.getInstance().getStringPreference(
				rootFolderParam, sdCardPath);
		return rootFolder;

	}

	private Root.Stub stub = new Root.Stub() {

		@Override
		public String getRoot() throws RemoteException {
			return getRootFolder();
		}
	};

	public Root.Stub getRootService() {
		return stub;
	}
}
