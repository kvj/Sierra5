package org.kvj.sierra5.common.root;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.theme.Theme;
interface Root {
	
	Node getNode(in byte[] id);
	RemoteViews render(in Node node, String left, String theme); 
	boolean update(in Node node, String raw);
	boolean expand(in Node node, boolean expand);
	boolean putFile(String to, String path, String text);
	Node append(in Node node, String raw);
	Theme[] getThemes();
}