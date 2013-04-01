package org.kvj.sierra5.data.provider.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.sierra5.App;
import org.kvj.sierra5.R;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.data.provider.DataProvider;
import org.kvj.sierra5.data.provider.NodeID;
import org.kvj.sierra5.data.provider.impl.SDCardDataProvider.SDCardProviderID;

import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

public class SDCardDataProvider implements DataProvider<SDCardProviderID> {

	private static final int TYPE_FOLDER = 0;
	private static final int TYPE_FILE = 1;
	private static final int TYPE_TEXT = 2;
	private static final String TAG = "SDCard";

	class Node_ extends Node<SDCardProviderID> {
		public Node_(SDCardProviderID id) {
			super();
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (null == o) { // One is null - false
				return false;
			}
			Node_ other = (Node_) o;
			if (other.id.path.length != id.path.length) {
				// Size is wrong - false
				return false;
			}
			for (int i = 0; i < id.path.length; i++) { // Compare path items
				if (!id.path[i].equals(other.id.path[i])) {
					// Not same - stop and return false
					return false;
				}
			}
			return true; // Everything is same
		}

		public Node_ createChild(int type, String text) {
			SDCardProviderID newID = new SDCardProviderID();
			newID.path = new String[id.path.length + 1];
			newID.path[id.path.length] = text;
			System.arraycopy(id.path, 0, newID.path, 0, id.path.length);
			Node_ child = new Node_(newID);
			child.id.type = type;
			child.level = level + 1;
			child.text = text;
			return child;
		}
	}

	static class SDCardProviderID implements NodeID {

		public String[] path = new String[0];
		public int type = TYPE_FOLDER;
		public String raw = null;

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("ID: " + type);
			for (String item : path) { // Add path
				sb.append(", " + item);
			}
			return sb.toString();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeStringArray(path);
		}

		public static final Parcelable.Creator<SDCardProviderID> CREATOR = new Creator<SDCardProviderID>() {

			@Override
			public SDCardProviderID createFromParcel(Parcel source) {
				SDCardProviderID id = new SDCardProviderID();
				id.path = source.createStringArray();
				return id;
			}

			@Override
			public SDCardProviderID[] newArray(int size) {
				return new SDCardProviderID[size];
			}

		};

		@Override
		public String getProviderID() {
			return "sdcard";
		}

	}

	private class ItemPattern {
		public boolean inverted = false;
		public String converted = null;
		public Pattern pattern = null;

		boolean matches(String text) {
			boolean matches = pattern.matcher(text).find();
			if ((matches && inverted) || (!matches && !inverted)) {
				return false;
			}
			return true;
		}
	}

	private static Pattern mask = Pattern.compile("\\*|\\?");

	// 2: dd 3: +2d 4: + 5: 2

	public ItemPattern parsePattern(String text, boolean regexp) {
		StringBuffer pattern = new StringBuffer();
		if (regexp) { // Add begin
			pattern.append('^');
		}
		ItemPattern res = new ItemPattern();
		if (!TextUtils.isEmpty(text) && text.charAt(0) == '!' && regexp) {
			text = text.substring(1);
			res.inverted = true;
		}
		Matcher m = mask.matcher(text);
		while (m.find()) {
			String repl = "";
			String found = m.group();
			if ("*".equals(found)) {
				repl = ".*";
				if (!regexp) { // Revert
					repl = "*";
				}
			} else if ("?".equals(found)) {
				repl = ".";
				if (!regexp) { // Revert
					repl = "?";
				}
			} else { // Unknown?
				repl = found;
			}
			m.appendReplacement(pattern, repl);
		}
		m.appendTail(pattern);
		if (regexp) { // Add end
			pattern.append('$');
		}
		res.converted = pattern.toString();
		if (regexp) { // Compile pattern
			res.pattern = Pattern.compile(res.converted, Pattern.CASE_INSENSITIVE);
		}
		return res;
	}

	@Override
	public String getId() {
		return "sdcard";
	}

	@Override
	public String getDescription() {
		return "Local files on SDCard";
	}

	private String getRootFolder() {
		String rootFolderParam = App.getInstance().getResources().getString(R.string.rootFolder);
		String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		String rootFolder = App.getInstance().getStringPreference(rootFolderParam, sdCardPath);
		return rootFolder;
	}

	@Override
	public Node_ getRoot() {
		Node_ root = new Node_(new SDCardProviderID());
		root.capabilities = new int[] { Node.CAPABILITY_ROOT };
		root.id.type = TYPE_FOLDER;
		root.text = "Root";
		return root;
	}

	class SearchResult {
		Node_ fileNode = null;
		Node_ node = null;
	}

	private SearchResult searchNode(SDCardProviderID id) {
		SearchResult result = new SearchResult();
		Node_ node = getRoot();
		int index = 0; // Index in path
		while (id.path.length > node.id.path.length) {
			// Search until path will be same
			Collection<Node<SDCardProviderID>> children = expand(node, Node.EXPAND_ONE);
			if (null == children) { // Expand failed
				return result;
			}
			boolean found = false;
			for (Node<SDCardProviderID> child : children) { // Search by name
				if (child.text.equals(id.path[index])) { // Found
					if (child.id.type == TYPE_FILE) { // Found file
						result.fileNode = (Node_) child;
					}
					index++;
					found = true;
					node = (Node_) child;
					break;
				}
			}
			if (!found) { // Not found - error
				return result;
			}
		}
		if (node.id.type == TYPE_FILE || node.id.type == TYPE_FOLDER) {
			// Search goal was file or folder
			result.fileNode = node;
		}
		result.node = node; // Found
		return result;
	}

	@Override
	public Node_ find(NodeID id) {
		SearchResult result = searchNode((SDCardProviderID) id);
		return result.node;
	}

	private List<File> listFiles(File folder) {
		try { // IO errors
			ItemPattern filePattern = parsePattern(
					App.getInstance().getStringPreference(R.string.filePattern, R.string.filePatternDefault), true);
			ItemPattern folderPattern = parsePattern(
					App.getInstance().getStringPreference(R.string.folderPattern, R.string.folderPatternDefault), true);
			File[] files = folder.listFiles();
			if (null == files) { // Empty dir
				files = new File[0];
			}
			List<File> result = new ArrayList<File>();
			for (File file : files) { // Check every file
				ItemPattern patt = file.isDirectory() ? folderPattern : filePattern;
				if (!patt.matches(file.getName())) { // Not matches - skip
					continue;
				}
				result.add(file);
			}
			Collections.sort(result, new Comparator<File>() {
				@Override
				public int compare(File lhs, File rhs) {
					if (lhs.isDirectory() && !rhs.isDirectory()) {
						// Folder first
						return -1;
					}
					if (!lhs.isDirectory() && rhs.isDirectory()) {
						// Folder first
						return 1;
					}
					return lhs.getName().compareToIgnoreCase(rhs.getName());
				}
			});
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Error listing files", e);
			return null;
		}
	}

	@Override
	public List<Node<SDCardProviderID>> expand(Node<SDCardProviderID> node, int type) {
		Node_ node_ = (Node_) node;
		List<Node<SDCardProviderID>> result = new ArrayList<Node<SDCardProviderID>>();
		if (null != node.children) { // Have children
			return node.children;
		}
		if (node.id.type == TYPE_FILE) {
			// File - parse only when children are null
			File file = getFile(node.id);
			if (null == file) { // Not found
				Log.e(TAG, "File not found: " + node.text);
				return null;
			}
			try {
				boolean parseResult = parseFile(new FileInputStream(file), (Node_) node, result);
				if (!parseResult) { // Not parsed
					Log.e(TAG, "Parse failed: " + node.text);
					return null;
				}
				return result;
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found: " + node.text, e);
				return null;
			}
		}
		if (node.id.type == TYPE_TEXT) { // Text file
			SearchResult searchResult = searchNode(node.id);
			if (null == searchResult.node) { // Not found
				Log.e(TAG, "Text not found: " + node.text);
				return null;
			}
			return searchResult.node.children;
		}
		if (node.id.type == TYPE_FOLDER) { // Expand folder
			File file = getFile(node.id);
			if (null == file) { // Not found
				Log.e(TAG, "Folder not found: " + node.text);
				return null;
			}
			if (!file.isDirectory()) { // Not folder
				Log.e(TAG, "Folder not folder: " + node.text);
				return null;
			}
			List<File> files = listFiles(file);
			if (null == files) { // Not listed
				return null;
			}
			for (File f : files) { // Convert files to Nodes
				Node_ n = node_.createChild(f.isDirectory() ? TYPE_FOLDER : TYPE_FILE, f.getName());
				if (f.isDirectory()) { // Folder
					n.capabilities = new int[] { Node.CAPABILITY_ROOT };
					n.style = Node.STYLE_1; // Folder
					if (type == Node.EXPAND_GROUP) { // Also expand
						List<Node<SDCardProviderID>> children = expand(n, type);
						if (null != children) { // Expanded
							n.children = children;
							n.collapsed = false;
						}
					}
				} else {
					// File
					n.capabilities = new int[] { Node.CAPABILITY_ROOT, Node.CAPABILITY_ADD, Node.CAPABILITY_EDIT };
					n.style = Node.STYLE_2; // File
				}
				result.add(n);
			}
		}
		return result;
	}

	private File getFile(SDCardProviderID id) {
		Log.i(TAG, "Get file: " + id.type + ", " + id.path.length);
		File file = new File(getRootFolder());
		int index = 0; // In path
		while (file.exists()) { // Break when file not exists - error
			if (index == id.path.length) { // End reached
				return file; // Done
			}
			file = new File(file, id.path[index]); // Next piece
			index++;
		}
		return null;
	}

	@Override
	public boolean remove(Node<SDCardProviderID> node) {
		SearchResult result = searchNode(node.id);
		if (null == result.node) { // Not found
			Log.e(TAG, "Node not found for remove: " + node.text);
			return false;
		}
		File file = getFile(result.fileNode.id);
		if (null == file) { // Not found
			Log.e(TAG, "File not found: " + file);
			return false;
		}
		try {
			boolean saveResult = saveFile(new FileOutputStream(file), result.fileNode, result.node);
			if (!saveResult) { // Save failed
				Log.e(TAG, "Error saving");
				return false;
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found: " + file, e);
			return false;
		}
		return true;
	}

	@Override
	public Node<SDCardProviderID> edit(EditType type, Node<SDCardProviderID> node, String text) {
		SearchResult result = searchNode(node.id);
		if (null == result.node) { // Not found
			Log.e(TAG, "Node not found for remove: " + node.text);
			return null;
		}
		File file = getFile(result.fileNode.id);
		if (null == file) { // Not found
			Log.e(TAG, "File not found: " + file);
			return null;
		}
		Log.i(TAG, "edit: " + type + ", " + node.text + ", " + file.getAbsolutePath() + ", " + result.fileNode.text);
		try {
			Node_ n = null;
			switch (type) {
			case Append:
				if (null == result.node.children) { // Not expanded
					List<Node<SDCardProviderID>> children = expand(result.node, Node.EXPAND_ONE);
					if (null == children) { // Failed
						Log.e(TAG, "Error loading current contents");
						return null;
					}
					result.node.children = children;
				}
				n = result.node.createChild(TYPE_TEXT, "");
				n.children = new ArrayList<Node<SDCardProviderID>>();
				n.id.raw = text;
				result.node.children.add(n);
				break;
			case Replace:
				result.node.id.raw = text;
				break;
			}
			boolean saveResult = saveFile(new FileOutputStream(file), result.fileNode, null);
			if (!saveResult) { // Save failed
				Log.e(TAG, "Error saving");
				return null;
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found: " + file, e);
			return null;
		}
		return result.node;
	}

	@Override
	public boolean upload(Node<SDCardProviderID> node, String resource, String location) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Node<SDCardProviderID>> getPath(Node<SDCardProviderID> node) {
		List<Node<SDCardProviderID>> result = new ArrayList<Node<SDCardProviderID>>();
		if (0 == node.id.path.length) { // Root
			return result;
		}
		result.add(node);
		for (int i = node.id.path.length - 1; i > 0; i--) {
			Node_ parent = new Node_(new SDCardProviderID());
			parent.id.path = new String[i];
			System.arraycopy(node.id.path, 0, parent.id.path, 0, i);
			parent.level = i;
			if (i > 0) { // Have text
				parent.text = node.id.path[i - 1];
			}
			result.add(0, parent);
		}
		return result;
	}

	@Override
	public String download(Node<SDCardProviderID> node, String resource) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEditable(Node<SDCardProviderID> node) {
		return getEditableContents((Node_) node);
	}

	public static interface LineEater {

		public void eat(int index, Node_ node, String left, String line);

		public boolean filter(int index, Node_ node);
	}

	private Pattern left = Pattern.compile("^\\s*");

	private int writeOneNode(int index, int startLevel, Node_ node, LineEater eater) {
		Log.i(TAG, "WriteOne: " + node + ", " + node.id.raw);
		StringBuilder spaces = new StringBuilder();
		String[] lines = null;
		if (!eater.filter(index, node)) { // Filtered out
			return index;
		}
		boolean skipFirstLine = false;
		if (null != node.id.raw) { // Have raw data
			lines = node.id.raw.split("\n");
			if (1 == lines.length && TextUtils.isEmpty(lines[0]) && node.id.type == TYPE_FILE) {
				// File + empty text = nothing to write
				lines = new String[0];
			}
		} else { // Not changed
			if (node.id.type == TYPE_FILE) {
				// Don't write first line - it's file name
				skipFirstLine = true;
			}
			lines = new String[] { node.text };
		}
		for (int i = 0; i < (node.level - startLevel) * getTabSize(); i++) {
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
				Log.i(TAG, "WriteOne eat: " + line);
				eater.eat(result, node, spaces.toString() + leftSpaces, line.trim());
			}
			result++;
		}
		if (null == node.children) { // No children - try to load
			node.children = expand(node, Node.EXPAND_ONE);
		}
		if (null == node.id.raw && null != node.children) {
			// Process children
			for (int i = 0; i < node.children.size(); i++) {
				// Write every child
				result = writeOneNode(result, node.id.type == TYPE_FILE ? startLevel + 1 : startLevel,
						(Node_) node.children.get(i), eater);
			}
		}
		if (null != node.id.raw && node.id.type == TYPE_TEXT) {
			// Have raw data and it's text - update text
			node.id.raw = null;
			node.text = lines[0];
			node.id.path[node.id.path.length - 1] = node.text;
		}
		return result;
	}

	private void writeNode(Node_ node, LineEater eater) {
		writeOneNode(0, node.level, node, eater);
	}

	private String getEditableContents(final Node_ parent) {
		final StringBuilder buffer = new StringBuilder();
		writeNode(parent, new LineEater() {

			@Override
			public void eat(int index, Node_ node, String left, String line) {
				if (index > 0) { // Have data - CR
					buffer.append('\n');
				}
				buffer.append(left);
				buffer.append(line);
			}

			@Override
			public boolean filter(int index, Node_ node) {
				return true;
			}
		});
		return buffer.toString();
	}

	public int getTabSize() {
		return App.getInstance().getIntPreference(R.string.tabSize, R.string.tabSizeDefault);
	}

	private boolean saveFile(OutputStream stream, Node_ node, final Node_ remove) {
		try { // Catch save errors
			final String crlf = App.getInstance().getBooleanPreference(R.string.useCRLF, false) ? "\r\n" : "\n";
			final boolean expandTab = App.getInstance().getBooleanPreference(R.string.expandTabs, false);
			final String tabRepl;
			if (!expandTab) { // Replace spaces with tab
				int tabSize = App.getInstance().getIntPreference(R.string.tabSize, R.string.tabSizeDefault);
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < tabSize; i++) {
					// Add spaces
					sb.append(' ');
				}
				tabRepl = sb.toString();
			} else {
				tabRepl = null;
			}
			final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "utf-8"));
			writeNode(node, new LineEater() {

				@Override
				public void eat(int index, Node_ node, String left, String line) {
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
				public boolean filter(int index, Node_ node) {
					if (node.equals(remove)) { // We need to remove it - skip
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

	private boolean parseFile(InputStream stream, Node_ node, List<Node<SDCardProviderID>> children) {
		try { // Catch all stream errors
			int spacesInTab = getTabSize();
			StringBuilder tabReplacement = new StringBuilder();
			for (int i = 0; i < spacesInTab; i++) { // Add spaces
				tabReplacement.append(' ');
			}
			// Log.i(TAG, "Parse file: " + node.file);
			BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"));
			// Reset children
			Stack<Node_> parents = new Stack<Node_>();
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
				int level = leftChars.length() / spacesInTab;
				Node_ parent = null;
				List<Node<SDCardProviderID>> parentChildren = new ArrayList<Node<SDCardProviderID>>();
				boolean fromParents = false;
				// Log.i(TAG, "Search parent: " + n.text + ", " +
				// parents.size());
				do { // Search for parent
					if (parents.isEmpty()) {
						// No more parents - parent is file
						parent = node;
						parentChildren = children;
						break;
					}
					parent = parents.pop();
					parentChildren = parent.children;
					// Log.i(TAG, "Parse " + parent.text + " and " + n.text +
					// ", "
					// + parent.left.length() + ", " + n.left.length());
					if (parent.level <= level + node.level) {
						// Real parent
						fromParents = true;
						break;
					}
				} while (true);
				Node_ n = parent.createChild(TYPE_TEXT, line.trim());
				n.capabilities = new int[] { Node.CAPABILITY_ADD, Node.CAPABILITY_EDIT, Node.CAPABILITY_REMOVE };
				n.collapsed = false; // All files are expanded by default
				n.children = new ArrayList<Node<SDCardProviderID>>();
				parentChildren.add(n);
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
}
