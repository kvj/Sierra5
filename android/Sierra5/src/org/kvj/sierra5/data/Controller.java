package org.kvj.sierra5.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.sierra5.App;
import org.kvj.sierra5.R;

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

	public Controller() {
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
				Node n = new Node();
				n.collapsed = false; // All files are expanded by default
				n.children = new ArrayList<Node>();
				n.file = node.file; // Copy from file node
				n.left = leftChars;
				n.text = line.trim();
				n.type = Node.TYPE_TEXT;
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
					if (parent.left.length() < n.left.length()) {
						// Real parent
						fromParents = true;
						break;
					}
				} while (true);
				parent.children.add(n);
				n.level = parent.level + 1; // Level found
				n.textPath = new ArrayList<String>();
				if (null != parent.textPath) { // Parent is text
					n.textPath.addAll(parent.textPath);
				}
				n.textPath.add(n.text);
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
		Log.i(TAG, "expand " + node.text + ", " + node.collapsed + ", "
				+ node.type);
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
			File[] files = folder.listFiles();
			List<Node> nodes = new ArrayList<Node>();
			Log.i(TAG,
					"Load dir: " + files.length + ", "
							+ folder.getAbsolutePath());
			for (File file : files) { // For every file/folder in folder
				Node child = new Node();
				child.level = node.level + 1;
				child.file = file.getAbsolutePath();
				child.text = file.getName();
				child.type = file.isDirectory() ? Node.TYPE_FOLDER
						: Node.TYPE_FILE;
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
		File file = new File(path);
		if (!file.exists()) { // Not exists
			return null;
		}
		Node node = new Node();
		node.file = path;
		node.text = file.getName();
		node.type = file.isDirectory() ? Node.TYPE_FOLDER : Node.TYPE_FILE;
		return node;
	}

	public static class SearchNodeResult {
		public boolean found = false;
		public Node node = null;
		public int index = 0;
	}

	private List<String> findPath(File root, File file) {
		File f = file;
		List<String> result = new ArrayList<String>();
		do {
			if (root.equals(f)) { // Files are same
				return result;
			}
			result.add(f.getName());
			f = f.getParentFile();
			if (null == f) { // FS root reached
				return null;
			}
		} while (f != null);
		return null;
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
		File thisFile = new File(file);
		File rootFile = new File(root.file);
		if (!thisFile.exists() || !rootFile.exists()) { // Invalid file
			Log.w(TAG, "File(s) not exists: " + thisFile.exists() + ", "
					+ rootFile.exists());
			return null;
		}
		List<String> pathToRoot = findPath(rootFile, thisFile);
		if (null == pathToRoot) { // Different trees
			Log.w(TAG, "Different trees: " + root.file + " - " + file);
			return null;
		}
		List<String> pathToSearch = new ArrayList<String>();
		// Copy path
		for (int i = pathToRoot.size() - 1; i >= 0; i--) { // Invert path
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
			for (int i = 0; i < n.children.size(); i++) { // Search child
				Node ch = n.children.get(i);
				if (ch.text.equals(pathToSearch.get(depth))) {
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
}
