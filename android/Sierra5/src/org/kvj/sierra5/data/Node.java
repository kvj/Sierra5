package org.kvj.sierra5.data;

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

}
