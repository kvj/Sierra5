package org.kvj.sierra5.plugins.ui;

import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.plugins.R;

import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class NewBookmarkView extends Activity {

	TextView fileEdit = null, itemEdit = null, nameEdit = null,
			templateEdit = null;
	Button selectButton = null, saveButton = null;
	RadioGroup typeGroup = null;
	RadioButton typeShow = null, typeEdit = null, typeAdd = null;
	int type = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newbmark);
		fileEdit = (TextView) findViewById(R.id.bmark_file);
		itemEdit = (TextView) findViewById(R.id.bmark_item);
		nameEdit = (TextView) findViewById(R.id.bmark_name);
		selectButton = (Button) findViewById(R.id.bmark_select);
		saveButton = (Button) findViewById(R.id.bmark_save);
		typeGroup = (RadioGroup) findViewById(R.id.bmark_type);
		typeShow = (RadioButton) findViewById(R.id.bmark_type_show);
		typeEdit = (RadioButton) findViewById(R.id.bmark_type_edit);
		typeAdd = (RadioButton) findViewById(R.id.bmark_type_add);
		templateEdit = (TextView) findViewById(R.id.bmark_template);
		selectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				selectItem();
			}
		});
		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				saveBookmark();
			}
		});
		typeGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				boolean templateEnabled = false;
				if (checkedId == R.id.bmark_type_add) { // Only for add
					templateEnabled = true;
				}
				templateEdit.setEnabled(templateEnabled);
			}
		});
		setNodeType(-1);
	}

	protected void saveBookmark() {
		Intent intent = new Intent();
		ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(
				this, R.drawable.file);
		Intent launchIntent = new Intent(Constants.SHOW_EDIT_ITEM_NS);
		int selectedType = typeGroup.getCheckedRadioButtonId();
		String file = fileEdit.getText().toString().trim();
		String[] items = itemEdit.getText().toString().trim().split("/");
		String template = templateEdit.getText().toString().trim();
		if (items.length == 1 && TextUtils.isEmpty(items[0])) {
			// arr with empty str
			items = new String[0];
		}
		switch (selectedType) {
		case R.id.bmark_type_show: // Show
			launchIntent.putExtra(Constants.LIST_INTENT_ROOT, file);
			launchIntent.putExtra(Constants.LIST_INTENT_FILE, file);
			if (items.length > 0) { // Have items
				launchIntent.putExtra(Constants.LIST_INTENT_ITEM, items);
			}
			break;
		case R.id.bmark_type_add: // Add
			launchIntent.putExtra(Constants.LIST_FORCE_EDITOR, true);
			launchIntent.putExtra(Constants.EDITOR_INTENT_FILE, file);
			if (items.length > 0) { // Have items
				launchIntent.putExtra(Constants.EDITOR_INTENT_ITEM, items);
			}
			launchIntent.putExtra(Constants.EDITOR_INTENT_ADD, true);
			if (!TextUtils.isEmpty(template)) { // Have template
				launchIntent.putExtra(Constants.EDITOR_INTENT_ADD_TEMPLATE,
						template);
			}
			icon = Intent.ShortcutIconResource.fromContext(this,
					R.drawable.file_add);
			break;
		case R.id.bmark_type_edit: // Edit
			launchIntent.putExtra(Constants.LIST_FORCE_EDITOR, true);
			launchIntent.putExtra(Constants.EDITOR_INTENT_FILE, file);
			if (items.length > 0) { // Have items
				launchIntent.putExtra(Constants.EDITOR_INTENT_ITEM, items);
			}
			icon = Intent.ShortcutIconResource.fromContext(this,
					R.drawable.file_edit);
			break;
		}
		launchIntent.putExtra(Constants.INTENT_TEMPLATE, true);
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, nameEdit.getText()
				.toString().trim());
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

		setResult(RESULT_OK, intent);
		finish();
	}

	protected void selectItem() {
		Intent intent = new Intent(Constants.SELECT_ITEM_NS);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK && requestCode == 0) { // Selected
			fileEdit.setText(data.getStringExtra(Constants.SELECT_ITEM_FILE));
			String[] path = data
					.getStringArrayExtra(Constants.SELECT_ITEM_ITEM);
			if (null != path) { // Create string
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < path.length; i++) { // Add with /
					if (i > 0) { // Not first
						sb.append('/');
					}
					sb.append(path[i]);
				}
				itemEdit.setText(sb);
			} else { // Empty path
				itemEdit.setText("");
			}
			int nodeType = data.getIntExtra(Constants.SELECT_ITEM_TYPE, -1);
			setNodeType(nodeType);
		}
	}

	private void setNodeType(int nodeType) {
		boolean addEnabled = false;
		boolean editEnabled = false;
		boolean showEnabled = false;
		boolean saveEnabled = false;
		this.type = nodeType;
		int selected = R.id.bmark_type_show;
		switch (nodeType) {
		case Node.TYPE_FILE: // Show, Save, Add, Edit
			showEnabled = true;
			saveEnabled = true;
			addEnabled = true;
			editEnabled = true;
			break;
		case Node.TYPE_FOLDER: // Show, Save
			showEnabled = true;
			saveEnabled = true;
			break;
		case Node.TYPE_TEXT: // Show, Save, Add, Edit
			showEnabled = true;
			addEnabled = true;
			editEnabled = true;
			saveEnabled = true;
			selected = R.id.bmark_type_edit;
			break;
		}
		typeGroup.check(selected);
		typeShow.setEnabled(showEnabled);
		typeEdit.setEnabled(editEnabled);
		typeAdd.setEnabled(addEnabled);
		saveButton.setEnabled(saveEnabled);
	}
}
