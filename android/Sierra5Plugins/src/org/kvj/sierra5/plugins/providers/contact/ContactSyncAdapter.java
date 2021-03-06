package org.kvj.sierra5.plugins.providers.contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.root.Root;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.WidgetController.ParserListener;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

public class ContactSyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "ContactSync";

	public ContactSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}

	public static long ensureSampleGroupExists(Context context, Account account, String name) {
		final ContentResolver resolver = context.getContentResolver();

		// Lookup the sample group
		long groupId = 0;
		final Cursor cursor = resolver.query(Groups.CONTENT_URI, new String[] { Groups._ID }, Groups.ACCOUNT_NAME
				+ "=? AND " + Groups.ACCOUNT_TYPE + "=? AND " + Groups.TITLE + "=?", new String[] { account.name,
				account.type, name }, null);
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					groupId = cursor.getLong(0);
				}
			} finally {
				cursor.close();
			}
		}

		if (groupId == 0) {
			// Sample group doesn't exist yet, so create it
			final ContentValues contentValues = new ContentValues();
			contentValues.put(Groups.ACCOUNT_NAME, account.name);
			contentValues.put(Groups.ACCOUNT_TYPE, account.type);
			contentValues.put(Groups.TITLE, name);
			contentValues.put(Groups.GROUP_IS_READ_ONLY, true);

			final Uri newGroupUri = resolver.insert(Groups.CONTENT_URI, contentValues);
			groupId = ContentUris.parseId(newGroupUri);
		}
		return groupId;
	}

	public static class ProviderEntryInfo {

		ContentValues values = new ContentValues();

		public ProviderEntryInfo() {
		}

		public ProviderEntryInfo(String mime, String type) {
			a(mime, type);
		}

		public ProviderEntryInfo a(String type, String data) {
			values.put(type, data);
			return this;
		}

		public ProviderEntryInfo a(String type, Integer data) {
			values.put(type, data);
			return this;
		}

		public ProviderEntryInfo a(String type, Long data) {
			values.put(type, data);
			return this;
		}

		public ProviderEntryInfo a(String type, Boolean data) {
			values.put(type, data);
			return this;
		}

		public ContentValues getValues() {
			return values;
		}
	}

	class ContactEntry {
		String rawID = "";
		List<String> groups = new ArrayList<String>();
		List<ProviderEntryInfo> entries = new ArrayList<ProviderEntryInfo>();
	}

	private List<String> groups = new ArrayList<String>();
	private List<ContactEntry> contacts = new ArrayList<ContactEntry>();

	public static boolean in(String where, String... what) {
		for (String wh : what) {
			if (where.contains(wh)) {
				return true;
			}
		}
		return false;
	}

	public static String getChildText(Node node, String left) {
		StringBuffer sb = new StringBuffer();
		if (null != node.children) { // Have children
			for (int i = 0; i < node.children.size(); i++) { // Every child
				if (i > 0) { // Add \n
					sb.append('\n');
				}
				sb.append(left);
				sb.append(node.children.get(i).text);
				String childText = getChildText(node.children.get(i), left + left);
				if (!TextUtils.isEmpty(childText)) { // Have
					sb.append('\n');
					sb.append(childText);
				}
			}
		}
		return sb.toString();
	}

	private void parseTypeValue(ContactEntry ce, String type, String value) {
		if (in(type, "tel", "phone", "cell")) { // This is phone
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Phone.NUMBER, value);
			cei.a(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			if (in(type, "cell")) { // Mobile
				cei.a(Phone.TYPE, Phone.TYPE_MOBILE);
			} else if (in(type, "work", "office")) { // Work
				cei.a(Phone.TYPE, Phone.TYPE_WORK);
			} else if (in(type, "home")) { // Home
				cei.a(Phone.TYPE, Phone.TYPE_HOME);
			} else { // Custom
				cei.a(Phone.TYPE, Phone.TYPE_OTHER);
			}
			ce.entries.add(cei);
		} else if ("name".equals(type)) { // Name as nickname
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
			cei.a(Nickname.TYPE, Nickname.TYPE_OTHER_NAME);
			cei.a(Nickname.NAME, value);
			ce.entries.add(cei);
		} else if ("nick".equals(type)) { // Nick as nickname
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
			cei.a(Nickname.TYPE, Nickname.TYPE_DEFAULT);
			cei.a(Nickname.NAME, value);
			ce.entries.add(cei);
		} else if ("tag".equals(type)) { // Tag as short name
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
			cei.a(Nickname.TYPE, Nickname.TYPE_SHORT_NAME);
			cei.a(Nickname.NAME, value);
			ce.entries.add(cei);
		} else if ("home".equals(type) || "work".equals(type) || "office".equals(type)
				|| in(type, "address", "location")) {
			// Address
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
			if (in(type, "home")) { // Home address
				cei.a(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME);
			} else if (in(type, "work", "office")) { // Office
				cei.a(StructuredPostal.TYPE, StructuredPostal.TYPE_WORK);
			} else {
				cei.a(StructuredPostal.TYPE, StructuredPostal.TYPE_CUSTOM);
				cei.a(StructuredPostal.LABEL, type);
			}
			cei.a(StructuredPostal.FORMATTED_ADDRESS, value);
			ce.entries.add(cei);
		} else if ("bday".equals(type)) { // Birthday in YYYY-MM-DD
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE);
			cei.a(Event.TYPE, Event.TYPE_BIRTHDAY);
			cei.a(Event.START_DATE, value);
			ce.entries.add(cei);
		} else if ("job".equals(type)) { // Job info
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
			cei.a(Organization.TYPE, Organization.TYPE_WORK);
			String[] parts = value.split("\n", 3);
			cei.a(Organization.COMPANY, parts[parts.length - 1]);
			// Last item - company
			if (parts.length > 1) { // More strings
				cei.a(Organization.DEPARTMENT, parts[parts.length - 2]);
				if (parts.length > 2) { // More strings
					cei.a(Organization.TITLE, parts[parts.length - 3]);
				}
			}
			ce.entries.add(cei);
		} else if (in(type, "skype", "talk", "icq", "msn", "jabber")) { // IM
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Im.MIMETYPE, Im.CONTENT_ITEM_TYPE);
			cei.a(Im.DATA, value);
			cei.a(Im.TYPE, Im.TYPE_OTHER);
			if (in(type, "work", "office")) { // Work
				cei.a(Im.TYPE, Im.TYPE_WORK);
			} else if (in(type, "home")) { // Home
				cei.a(Im.TYPE, Im.TYPE_HOME);
			}
			if (in(type, "skype")) { // Skype
				cei.a(Im.PROTOCOL, Im.PROTOCOL_SKYPE);
			} else if (in(type, "talk")) { // Google talk
				cei.a(Im.PROTOCOL, Im.PROTOCOL_GOOGLE_TALK);
			} else if (in(type, "icq")) { // ICQ
				cei.a(Im.PROTOCOL, Im.PROTOCOL_ICQ);
			} else if (in(type, "msn")) { // MSN
				cei.a(Im.PROTOCOL, Im.PROTOCOL_MSN);
			} else if (in(type, "jabber")) { // Jabber
				cei.a(Im.PROTOCOL, Im.PROTOCOL_JABBER);
			} else {
				cei.a(Im.PROTOCOL, Im.PROTOCOL_CUSTOM);
				cei.a(Im.CUSTOM_PROTOCOL, type);
			}
			ce.entries.add(cei);
		} else if (in(type, "mail")) { // This is email
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Email.ADDRESS, value);
			cei.a(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
			if (in(type, "mobile")) { // Mobile
				cei.a(Email.TYPE, Email.TYPE_MOBILE);
			} else if (in(type, "work", "office")) { // Work
				cei.a(Email.TYPE, Email.TYPE_WORK);
			} else if (in(type, "home")) { // Home
				cei.a(Email.TYPE, Email.TYPE_HOME);
			} else { // Custom
				cei.a(Email.TYPE, Email.TYPE_OTHER);
			}
			ce.entries.add(cei);
		} else if ("groups".equals(type)) { // Additional groups
			String gg[] = value.split(",");
			for (String gr : gg) { // gr = group
				gr = gr.trim();
				if (TextUtils.isEmpty(gr)) { // Empty group
					continue;
				}
				if (!groups.contains(gr)) { // New group
					groups.add(gr);
				}
				if (!ce.groups.contains(gr)) { // New group
					ce.groups.add(gr);
				}
			}
		} else if (value.startsWith("http://") || value.startsWith("https://")) {
			// This is link
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Website.URL, value);
			cei.a(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE);
			cei.a(Website.TYPE, Website.TYPE_CUSTOM);
			cei.a(Website.LABEL, type);
			ce.entries.add(cei);
		} else { // All unknown fields - as note
			ProviderEntryInfo cei = new ProviderEntryInfo();
			cei.a(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE);
			cei.a(Note.NOTE, value);
			ce.entries.add(cei);
		}
	}

	private void parseContact(String group, Node node) {
		Log.i(TAG, "Parse contact: " + node.text + ", " + group);
		ContactEntry ce = new ContactEntry();
		ce.rawID = node.text + "_" + group;
		if (null != group) { // Have group
			ce.groups.add(group);
			if (!groups.contains(group)) { // New group
				groups.add(group);
			}
		}
		ce.entries.add(new ProviderEntryInfo().a(StructuredName.DISPLAY_NAME, node.text).a(StructuredName.MIMETYPE,
				StructuredName.CONTENT_ITEM_TYPE));
		contacts.add(ce);
		if (null != node.children) { // parse every children
			for (Node ch : node.children) { // ch = child
				int colonPos = ch.text.indexOf(":");
				if (-1 == colonPos) { // No colon
					continue;
				}
				String type = ch.text.substring(0, colonPos).trim().toLowerCase();
				String value = ch.text.substring(colonPos + 1).trim();
				if (TextUtils.isEmpty(value)) { // No value - use children
					value = getChildText(ch, "");
				}
				if (TextUtils.isEmpty(value) || TextUtils.isEmpty(type)) {
					// Still empty - skip
					continue;
				}
				String[] types = type.split(",");
				for (String oneType : types) { // For every type
					parseTypeValue(ce, oneType, value);
				}
			}
		}
	}

	private Uri addIsSync(Uri uri) {
		return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
	}

	private boolean saveData(Account account, String authority, ContentProviderClient client) {
		try {
			client.delete(addIsSync(Groups.CONTENT_URI), Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?",
					new String[] { account.name, account.type });
			client.delete(addIsSync(RawContacts.CONTENT_URI), RawContacts.ACCOUNT_NAME + "=? AND "
					+ RawContacts.ACCOUNT_TYPE + "=?", new String[] { account.name, account.type });
			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			Map<String, Integer> groupIDs = new HashMap<String, Integer>();
			for (String name : groups) { // Insert group
				int index = ops.size();
				ProviderEntryInfo group = new ProviderEntryInfo();
				group.a(Groups.ACCOUNT_NAME, account.name).a(Groups.ACCOUNT_TYPE, account.type)
						.a(Groups.GROUP_VISIBLE, true).a(Groups.TITLE, name);
				ops.add(ContentProviderOperation.newInsert(Groups.CONTENT_URI).withYieldAllowed(true)
						.withValues(group.values).build());
				groupIDs.put(name, index);
			}
			for (ContactEntry ce : contacts) {
				int index = ops.size();
				ProviderEntryInfo raw = new ProviderEntryInfo();
				raw.a(RawContacts.ACCOUNT_TYPE, account.type).a(RawContacts.ACCOUNT_NAME, account.name)
						.a(RawContacts.SOURCE_ID, ce.rawID);
				ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withYieldAllowed(true)
						.withValues(raw.values).build());
				for (ProviderEntryInfo cei : ce.entries) { // Add values
					ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID, index).withValues(cei.values)
							.withYieldAllowed(true).build());
				}
				for (String group : ce.groups) { // Insert group membership
					Integer groupID = groupIDs.get(group);
					if (null == groupID) { // That's strange
						Log.w(TAG, "No group ID for " + group);
						continue;
					}
					ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID, index)
							.withValueBackReference(GroupMembership.GROUP_ROW_ID, groupID.intValue())
							.withValue(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
							.withYieldAllowed(true).build());
				}
			}
			client.applyBatch(ops);
			return true;
		} catch (Exception e) {
			Log.e(TAG, "saveData error", e);
		}
		return false;

	}

	@Override
	public void onPerformSync(Account account, Bundle data, String authority, ContentProviderClient client,
			SyncResult result) {
		WidgetController controller = App.getInstance().getBean(WidgetController.class);
		Root root = controller.getRootService();
		if (null == root) { // No root service
			Log.w(TAG, "No root service, skipping sync");
			return;
		}
		try { // Remote exceptions
			String file = App.getInstance().getStringPreference("contacts_file", "");
			String path = App.getInstance().getStringPreference("contacts_path", "");
			String exp = App.getInstance().getStringPreference(R.string.contacts_exp, R.string.contacts_expDefault);
			if (TextUtils.isEmpty(exp)) {
				// TODO: For development only
				exp = "${*:g}.s5/${*:c}";
			}
			String[] pathArray = null;
			if (!TextUtils.isEmpty(path)) { // Have path
				pathArray = path.split("/");
			}
			Node rootNode = root.getNode(file, pathArray, false);
			if (null == rootNode) { // No root node for contacts
				Log.w(TAG, "No root node, skipping sync");
				return;
			}
			contacts.clear();
			groups.clear();
			controller.parseNode(exp, rootNode, new ParserListener() {

				@Override
				public boolean onItem(boolean finalItem, Map<String, Object> values, Node node) {
					if (!node.visible) { // Node not visible
						return false;
					}
					if (finalItem) { // This is contact
						parseContact((String) values.get("g"), node);
					}
					return true;
				}
			});
			Log.i(TAG, "Ready to add contacts: " + contacts.size() + ", groups: " + groups.size());
			saveData(account, authority, client);
		} catch (Exception e) {
			// TODO: handle exception
		}
		Log.i(TAG, "Performing sync... " + account.name + ", " + authority);
	}

}
