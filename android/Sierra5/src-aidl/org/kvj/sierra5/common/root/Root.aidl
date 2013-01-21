package org.kvj.sierra5.common.root;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.theme.Theme;
interface Root {
	
	String getRoot();
	Node getNode(in String file, in String[] path, boolean template);
	RemoteViews render(in Node node, String left, String theme); 
	boolean update(in Node node, String text, String raw);
	boolean expand(inout Node node, boolean expand);
	boolean putFile(String to, String path, String text);
	Node append(in Node node, String raw);
	Theme[] getThemes();
}