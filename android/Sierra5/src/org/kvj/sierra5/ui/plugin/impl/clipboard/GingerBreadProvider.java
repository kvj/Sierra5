package org.kvj.sierra5.ui.plugin.impl.clipboard;

import java.util.ArrayList;
import java.util.List;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.data.Controller;

public class GingerBreadProvider implements ClipboardProvider {

	private Controller controller = null;

	public GingerBreadProvider(Controller controller) {
		this.controller = controller;
	}

	private List<Node> buffer = new ArrayList<Node>();
	private boolean cut = false;

	@Override
	public boolean cutCopy(List<Node> items, boolean cut) {
		buffer.clear();
		buffer.addAll(items);
		this.cut = cut;
		return true;
	}

	@Override
	public List<Node> paste() {
		return buffer;
	}

	@Override
	public List<String> pasteText() {
		List<String> result = new ArrayList<String>();
		for (Node node : buffer) { // Get texts
			String text = controller.getEditableContents(node);
			String[] lines = text.split("\\n");
			for (String line : lines) { // Add line
				result.add(line);
			}
		}
		return result;
	}

	@Override
	public boolean wasCut() {
		return cut;
	}

	@Override
	public int getNodeCount() {
		return buffer.size();
	}

	@Override
	public void clearCut() {
		cut = false;
	}

}
