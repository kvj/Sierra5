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
	}

	private int writeOneNode(int index, int removeLeft, Node node,
			LineEater eater) {
		StringBuilder spaces = new StringBuilder();
		String[] lines = null;
		if (null != node.raw) { // Have raw data
			lines = node.raw.split("\n");
		} else { // Not changed
			lines = new String[] { node.text };
		}
		for (int i = 0; i < node.left.length() - removeLeft; i++) {
			// Add spaces
			spaces.append(' ');
		}
		int result = index;
		for (int i = 0; i < lines.length; i++) { // For every line
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
		if (null != node.raw) { //
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
				if (index == 0 && parent.type == Node.TYPE_FILE) {
					// Writing file and first line - skip
					return;
				}
				if (buffer.length() > 0) { // Have data - CR
					buffer.append('\n');
				}
				buffer.append(left);
				buffer.append(line);
			}
		});
		return buffer.toString();
	}

	public boolean saveFile(Node node) {
		try { // Catch save errors
			final String crlf = "\r\n";
			File file = new File(node.file);
			final BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
			writeNode(node, new LineEater() {

				@Override
				public void eat(int index, Node node, String left, String line) {
					if (index > 0) { // Skip first line - file
						try {
							writer.write(left + line + crlf);
						} catch (IOException e) {
							Log.e(TAG, "Error writing file:", e);
						}
					}
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
			int spacesInTab = 4;
			StringBuilder tabReplacement = new StringBuilder();
			for (int i = 0; i < spacesInTab; i++) { // Add spaces
				tabReplacement.append(' ');
			}
			// Log.i(TAG, "Parse file: " + node.file);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(node.file), "utf-8"));
			node.children = new ArrayList<Node>(); // Reset children
			Stack<Node> parents = new Stack<Node>();
			Node last = null;
			String line = null;
			while ((line = br.readLine()) != null) {
				if (TextUtils.isEmpty(line)) { // Empty line
					if (null != last) { // Have last - append to text
						last.text += '\n';
					} else { // First line - silently ignore
					}
					continue;
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
				do { // Search for parent
					if (parents.isEmpty()) {
						// No more parents - parent is file
						parent = node;
						break;
					}
					parent = parents.pop();
					if (parent.left.length() < n.left.length()) {
						// Real parent
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
				parents.push(n);
				// Log.i(TAG, "Line: " + n.text + ", " + n.level + ", "
				// + parent.text);
				last = n;
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
			for (int i = 0; i < n.children.size(); i++) { // Search child
				Node ch = n.children.get(i);
				if (ch.text.equals(pathToSearch.get(depth))) {
					// From end - found
					n = ch;
					index += i + 1;
					found = true;
					break;
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
}
