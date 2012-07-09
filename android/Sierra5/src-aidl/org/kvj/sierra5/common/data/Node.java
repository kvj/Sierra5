package org.kvj.sierra5.common.data;

import java.util.ArrayList;
import java.util.List;

public class Node {

	public static final int TYPE_FOLDER = 0;
	public static final int TYPE_FILE = 1;
	public static final int TYPE_TEXT = 2;

	public String file = null;

	public List<String> textPath = null;

	public String left = "";
	public List<Node> children = null;
	public boolean collapsed = true;

	public String text = "";
	public String raw = null;

	public int type = TYPE_FOLDER;
	public int level = 0;

	public Node createChild(int childType, String childText, int tabSize) {
		StringBuilder sb = new StringBuilder();
		if (type == Node.TYPE_TEXT) { // Padding only for text
			for (int i = 0; i < tabSize; i++) { // Add spaces
				sb.append(' ');
			}
		}
		Node child = new Node();
		child.file = file;
		child.children = new ArrayList<Node>();
		child.text = childText;
		child.type = childType;
		child.left = left + sb.toString();
		child.level = level + 1;
		if (TYPE_TEXT == childType) { // textPath is necessary
			child.textPath = new ArrayList<String>();
			if (TYPE_TEXT == type && null != textPath) { // Parent is TEXT
				child.textPath.addAll(textPath);
			}
			child.textPath.add(childText);
		}
		if (null == children) { // No children yet
			children = new ArrayList<Node>();
		}
		children.add(child);
		return child;
	}

}
