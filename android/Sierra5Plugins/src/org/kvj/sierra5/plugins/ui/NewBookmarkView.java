package org.kvj.sierra5.plugins.ui;

import org.kvj.bravo7.SuperActivity;
import org.kvj.sierra5.common.Constants;
import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.service.UIService;

import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class NewBookmarkView extends SuperActivity<App, WidgetController, UIService> {

	public NewBookmarkView() {
		super(UIService.class);
	}

	TextView fileEdit = null, nameEdit = null, templateEdit = null;
	Button selectButton = null, saveButton = null;
	RadioGroup typeGroup = null;
	RadioButton typeShow = null, typeEdit = null, typeAdd = null;
	int type = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newbmark);
		fileEdit = (TextView) findViewById(R.id.bmark_file);
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
		setNodeType(Node.CAPABILITY_ROOT);
	}

	protected void saveBookmark() {
		Intent intent = new Intent();
		ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.file);
		Intent launchIntent = new Intent(Constants.SHOW_EDIT_ITEM_NS);
		int selectedType = typeGroup.getCheckedRadioButtonId();
		String file = fileEdit.getText().toString().trim();
		switch (selectedType) {
		case R.id.bmark_type_show: // Show
			launchIntent.putExtra(Constants.LIST_INTENT_ID, file);
			break;
		case R.id.bmark_type_add: // Add
			launchIntent.putExtra(Constants.LIST_FORCE_EDITOR, true);
			launchIntent.putExtra(Constants.EDITOR_INTENT_ID, file);
			launchIntent.putExtra(Constants.EDITOR_INTENT_ADD, true);
			icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.file_add);
			break;
		case R.id.bmark_type_edit: // Edit
			launchIntent.putExtra(Constants.LIST_FORCE_EDITOR, true);
			launchIntent.putExtra(Constants.EDITOR_INTENT_ID, file);
			icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.file_edit);
			break;
		}
		launchIntent.putExtra(Constants.INTENT_TEMPLATE, true);
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, nameEdit.getText().toString().trim());
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
			String path = controller.pathFromIntent(data.getExtras(), Constants.SELECT_ITEM_PATH);
			fileEdit.setText(path);
			setNodeType(Node.CAPABILITY_EDIT);
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
		case Node.CAPABILITY_EDIT: // Show, Save, Add, Edit
			showEnabled = true;
			saveEnabled = true;
			addEnabled = true;
			editEnabled = true;
			break;
		case Node.CAPABILITY_REMOVE: // Show, Save
			showEnabled = true;
			saveEnabled = true;
			break;
		case Node.CAPABILITY_ADD: // Show, Save, Add, Edit
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
