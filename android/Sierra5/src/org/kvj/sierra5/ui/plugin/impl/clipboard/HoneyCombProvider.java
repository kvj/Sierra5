package org.kvj.sierra5.ui.plugin.impl.clipboard;

import java.util.ArrayList;
import java.util.List;

import org.kvj.sierra5.App;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.data.Controller;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HoneyCombProvider implements ClipboardProvider {

	class NodeClipDataItem extends ClipData.Item {

		public NodeClipDataItem(Intent intent, Node node, boolean cut) {
			super(intent);
			intent.setAction(Constants.ITEM_ACTION);
			intent.putExtra(Constants.LIST_INTENT_ID, node.id);
			intent.putExtra(Constants.LIST_INTENT_CUT, cut);
		}

		@Override
		public CharSequence getText() {
			Log.i(TAG, "getText");
			return coerceToText(null);
		}

		@Override
		public CharSequence coerceToText(Context context) {
			Log.i(TAG, "coerceToText");
			Intent intent = fromItem(this);
			if (null == intent) { // Return error
				return "Invalid data!";
			}
			Node node = fromIntent(intent);
			if (null == node) { // Invalid node
				return "Invalid data!";
			}
			return controller.getEditableContents(node);
		}
	}

	private static final String TAG = "HoneyCombClipboard";

	private Intent fromItem(ClipData.Item item) {
		if (null == item) {
			Log.w(TAG, "fromItem, is null");
			return null;
		}
		Intent intent = item.getIntent();
		if (null == intent || !Constants.ITEM_ACTION.equals(intent.getAction())) {
			// No intent or action is wrong
			Log.w(TAG, "fromItem, intent problem: " + intent);
			return null;
		}
		return intent;
	}

	private Node fromIntent(Intent intent) {
		return controller.nodeFromParcelable(intent.getParcelableExtra(Constants.LIST_INTENT_ID));
	}

	private Controller controller = null;
	private ClipboardManager manager = null;

	public HoneyCombProvider(Controller controller) {
		this.controller = controller;
		this.manager = (ClipboardManager) App.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
	}

	@Override
	public boolean cutCopy(List<Node> items, boolean cut) {
		if (items.size() == 0) { // No items
			Log.w(TAG, "cutCopy, no data");
			return false;
		}
		try { //
			ClipData data = new ClipData("Sierra5 nodes", new String[] { Constants.ITEM_MIME_TYPE },
					new NodeClipDataItem(new Intent(), items.get(0), cut));
			for (int i = 1; i < items.size(); i++) { // Add all other items
				data.addItem(new NodeClipDataItem(new Intent(), items.get(i), false));
			}
			manager.setPrimaryClip(data);
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error copying to clipboard", e);
		}
		return false;
	}

	@Override
	public boolean wasCut() {
		ClipData data = getNodeClipData();
		if (null == data) {
			return false;
		}
		return data.getItemAt(0).getIntent().getBooleanExtra(Constants.LIST_INTENT_CUT, false);
	}

	@Override
	public List<Node> paste() {
		Log.i(TAG, "Paste: " + wasCut());
		List<Node> result = new ArrayList<Node>();
		ClipData data = getNodeClipData();
		if (null == data) { // No data
			return result;
		}
		for (int i = 0; i < data.getItemCount(); i++) {
			Intent intent = fromItem(data.getItemAt(i));
			if (null == intent) { // Not a Node
				continue;
			}
			Node node = fromIntent(intent);
			if (null == node) { // Not found node
				continue;
			}
			result.add(node);
		}
		return result;
	}

	@Override
	public List<String> pasteText() {
		List<String> result = new ArrayList<String>();
		for (Node node : paste()) { // Get texts
			String text = controller.getEditableContents(node);
			String[] lines = text.split("\\n");
			for (String line : lines) { // Add line
				result.add(line);
			}
		}
		return result;
	}

	private ClipData getNodeClipData() {
		ClipDescription desc = manager.getPrimaryClipDescription();
		if (desc == null || !desc.hasMimeType(Constants.ITEM_MIME_TYPE)) {
			// No description or no nodes
			Log.w(TAG, "No Node type in mime type: " + desc);
			return null;
		}
		ClipData data = manager.getPrimaryClip();
		if (data.getItemCount() == 0 || null == fromItem(data.getItemAt(0))) {
			Log.w(TAG, "No item or item not found: " + data.getItemCount());
			// No items or item is not Node
			return null;
		}
		return data;
	}

	@Override
	public int getNodeCount() {
		ClipData data = getNodeClipData();
		if (null == data) {
			// No description or no nodes
			return 0;
		}
		return data.getItemCount();
	}

	@Override
	public void clearCut() {
		Log.i(TAG, "Clear cut");
		cutCopy(paste(), false);
	}

}
