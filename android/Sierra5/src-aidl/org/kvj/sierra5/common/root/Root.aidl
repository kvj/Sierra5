package org.kvj.sierra5.common.root;

import org.kvj.sierra5.common.data.Node;
interface Root {
	
	String getRoot();
	Node getNode(in String file, in String[] path, boolean template);
	RemoteViews render(in Node node, String left, String theme); 
	boolean update(in Node node, String text, String raw);
}