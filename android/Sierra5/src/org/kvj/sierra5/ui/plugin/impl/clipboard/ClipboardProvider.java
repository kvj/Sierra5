package org.kvj.sierra5.ui.plugin.impl.clipboard;

import java.util.List;

import org.kvj.sierra5.common.data.Node;

public interface ClipboardProvider {

	public boolean cutCopy(List<Node> items, boolean cut);

	public boolean wasCut();

	public List<Node> paste();

	public List<String> pasteText();

	public int getNodeCount();

	public void clearCut();
}
