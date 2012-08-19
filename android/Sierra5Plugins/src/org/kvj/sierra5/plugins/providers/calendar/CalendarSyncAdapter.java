package org.kvj.sierra5.plugins.providers.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.root.Root;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.WidgetController.ParserListener;
import org.kvj.sierra5.plugins.providers.contact.ContactSyncAdapter;
import org.kvj.sierra5.plugins.providers.contact.ContactSyncAdapter.ProviderEntryInfo;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.util.Log;

public class CalendarSyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "CalendarSync";

	public CalendarSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}

	class CalendarEntry {
		ProviderEntryInfo event = null;
		ProviderEntryInfo reminder = null;
		List<ProviderEntryInfo> attendees = new ArrayList<ProviderEntryInfo>();
	}

	List<CalendarEntry> entries = new ArrayList<CalendarEntry>();

	private int getCalendarCompareValue(Calendar c, String key) {
		if ("y".equals(key)) { // year
			return c.get(Calendar.YEAR);
		} else if ("m".equals(key)) { // month
			return c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH);
		} else if ("d".equals(key)) { // Day
			return c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH) * 31
					+ c.get(Calendar.DAY_OF_MONTH);
		} else if ("w".equals(key)) { // Week
			return c.get(Calendar.YEAR) * 60 + c.get(Calendar.WEEK_OF_YEAR);
		}
		return 0;
	}

	private Calendar createCalendar(Calendar from, Calendar to,
			Map<String, Object> values) {
		Calendar c = Calendar.getInstance();
		c.set(from.get(Calendar.YEAR), 0, 1);
		for (String key : values.keySet()) { // Check
			int calEntry = -1;
			int calValue = -1;
			if ("y".equals(key)) { // year
				calEntry = Calendar.YEAR;
				calValue = (Integer) values.get(key);
			} else if ("m".equals(key)) { // month
				calEntry = Calendar.MONTH;
				calValue = (Integer) values.get(key) - 1;
			} else if ("d".equals(key)) { // Day
				calEntry = Calendar.DAY_OF_MONTH;
				calValue = (Integer) values.get(key);
			} else if ("w".equals(key)) { // Week
				calEntry = Calendar.WEEK_OF_YEAR;
				calValue = (Integer) values.get(key);
			}
			if (-1 != calEntry) { // Found
				c.set(calEntry, calValue);
				int fromValue = getCalendarCompareValue(from, key);
				int toValue = getCalendarCompareValue(to, key);
				int cValue = getCalendarCompareValue(c, key);
				if (fromValue > cValue || toValue < cValue) { // Not in range
					// Log.i(TAG, "Not in range: " + cValue + ", " + fromValue
					// + " - " + toValue);
					return null; // Not in range - skip
				}
			}
		}
		return c;
	}

	private static Pattern entryPattern = Pattern
			.compile("^(\\[.\\]|(\\d\\d?):(\\d\\d)(\\s*\\-\\s*(\\d\\d?):(\\d\\d))?)?(.*)$");
	// 1: [X]; 2, 3: dtstart, 4: - dtend; 5, 6: dtend; 7: event

	private static Pattern entryPartsPattern = Pattern
			.compile("(\\s\\~((\\d{1,2})h)?((\\d{1,2})m)?)|(\\s\\/-)");

	// 1: hrmin value; 3: hr; 5: min

	public static void logMatcher(Matcher m, String title) {
		Log.i(TAG, "Matcher: " + title);
		for (int i = 0; i < m.groupCount() + 1; i++) {
			Log.i(TAG, "m " + i + " " + m.group(i));
		}
	}

	private boolean setEndTime(Calendar c, Calendar ce, Matcher m) {
		int minutes = 0;
		if (null != m.group(3)) { // Have h
			minutes = 60 * Integer.parseInt(m.group(3), 10);
		}
		if (null != m.group(5)) { // Have m
			minutes += Integer.parseInt(m.group(5), 10);
		}
		// ce.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
		// ce.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
		ce.add(Calendar.MINUTE, minutes);
		return true;
	}

	private void parseEntry(Node node, Calendar c, Map<String, Object> values,
			int reminder, String durationDefault) {
		Matcher m = entryPattern.matcher(node.text);
		if (!m.find()) { // Which is strange
			return;
		}
		if ("[X]".equalsIgnoreCase(m.group(1))) { // Done item
			return; // Skip done
		}
		boolean allDay = true;
		boolean endDateSet = false;
		if (null != m.group(2)) { // Have start date
			c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(2), 10));
			c.set(Calendar.MINUTE, Integer.parseInt(m.group(3), 10));
			allDay = false;
		}
		Calendar ce = (Calendar) c.clone();
		if (null != m.group(4)) { // Have end time
			ce.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(5), 10));
			ce.set(Calendar.MINUTE, Integer.parseInt(m.group(6), 10));
			endDateSet = true;
		}
		Matcher mp = entryPartsPattern.matcher(m.group(7));
		StringBuffer buffer = new StringBuffer();
		while (mp.find()) {
			mp.appendReplacement(buffer, "");
			// logMatcher(mp, "mp");
			if (null != m.group(1) && !endDateSet) {
				// Have hour minute
				if (setEndTime(c, ce, mp)) { // Set
					endDateSet = true;
				}
			}
		}
		mp.appendTail(buffer);
		if (!allDay && !endDateSet) { // Parse default duration
			mp = entryPartsPattern.matcher(durationDefault);
			if (mp.find()) { // Found
				setEndTime(c, ce, mp);
			}
		}
		CalendarEntry entry = new CalendarEntry();
		entry.event = new ProviderEntryInfo();
		// Log.i(TAG, "Adding event: " + buffer + ", " + m.group(7));
		entry.event.a(Events.TITLE, buffer.toString());
		entry.event.a(Events.DTSTART, c.getTimeInMillis());
		entry.event.a(Events.DTEND, ce.getTimeInMillis());
		entry.event.a(Events.ALL_DAY, allDay ? 1 : 0);
		entry.event.a(Events.EVENT_TIMEZONE, TimeZone.getDefault()
				.getDisplayName());
		StringBuffer description = new StringBuffer();
		if (null != node.children) { // parse every children
			for (Node ch : node.children) { // ch = child
				int colonPos = ch.text.indexOf(":");
				String type = "";
				String value = "";
				if (-1 != colonPos) {
					type = ch.text.substring(0, colonPos).trim().toLowerCase();
					value = ch.text.substring(colonPos + 1).trim();
					if (TextUtils.isEmpty(value)) {
						// No value - use children
						value = ContactSyncAdapter.getChildText(ch, "");
					}
				} else {
					value = ch.text + "\n"
							+ ContactSyncAdapter.getChildText(ch, "  ");
				}
				String[] types = type.split(",");
				for (String oneType : types) { // For every type
					if (!parseTypeValue(entry, oneType, value)) {
						description.append(value.trim());
						description.append('\n');
					}
				}
			}
		}
		if (description.length() > 0) { // Have description
			// Log.i(TAG, "Event desc: " + description);
			entry.event.a(Events.DESCRIPTION, description.toString().trim());
		}
		if (reminder > 0 && !allDay) {
			// Add reminder
			entry.reminder = new ProviderEntryInfo();
			entry.reminder.a(Reminders.MINUTES, reminder);
			entry.reminder.a(Reminders.METHOD, Reminders.METHOD_ALERT);
		}
		entries.add(entry);
	}

	private boolean parseTypeValue(CalendarEntry entry, String type,
			String value) {
		// Log.i(TAG, "Parse: " + type + " = " + value);
		if ("who".equals(type)) { // Parse attendees
			String[] att = value.split("\\s");
			for (String at : att) { // Create attendee
				ProviderEntryInfo a = new ProviderEntryInfo();
				// Log.i(TAG, "Found att: " + at);
				a.a(Attendees.ATTENDEE_NAME, at);
				a.a(Attendees.ATTENDEE_EMAIL, at);
				a.a(Attendees.ATTENDEE_STATUS,
						Attendees.ATTENDEE_STATUS_ACCEPTED);
				a.a(Attendees.ATTENDEE_RELATIONSHIP,
						Attendees.RELATIONSHIP_ATTENDEE);
				entry.attendees.add(a);
			}
			return true;
		}
		if ("where".equals(type)) { // Location
			entry.event.a(Events.EVENT_LOCATION, value);
			return true;
		}
		return false;
	}

	@Override
	public void onPerformSync(Account account, Bundle data, String authority,
			ContentProviderClient client, SyncResult result) {
		WidgetController controller = App.getInstance().getBean(
				WidgetController.class);
		Root root = controller.getRootService();
		if (null == root) { // No root service
			Log.w(TAG, "No root service, skipping sync");
			return;
		}
		try { // Remote exceptions
			String file = App.getInstance().getStringPreference(
					"calendar_file", root.getRoot());
			String path = App.getInstance().getStringPreference(
					"calendar_path", "");
			String exp = App.getInstance().getStringPreference(
					R.string.calendar_exp, R.string.calendar_expDefault);
			// if (TextUtils.isEmpty(exp)) { // FIXME: For debug
			// exp = "${0:y}/${0:m}_*/${0:d} */${*:e}";
			// }
			final String duration = App.getInstance().getStringPreference(
					R.string.calendar_duration,
					R.string.calendar_durationDefault);
			final int reminderMinutes = App.getInstance().getIntPreference(
					R.string.calendar_reminder,
					R.string.calendar_reminderDefault);
			int color = Color.BLACK;
			String colorString = App.getInstance().getStringPreference(
					R.string.calendar_color, R.string.calendar_colorDefault);
			if (!TextUtils.isEmpty(colorString)) { // Parse color
				try { // Parse error
					color = Color.parseColor(colorString);
				} catch (Exception e) {
				}
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
			entries.clear();
			final Calendar from = Calendar.getInstance();
			from.add(
					Calendar.DAY_OF_YEAR,
					-App.getInstance().getIntPreference(
							R.string.calendar_syncPast,
							R.string.calendar_syncPastDefault));
			final Calendar to = Calendar.getInstance();
			to.add(Calendar.DAY_OF_YEAR,
					App.getInstance().getIntPreference(
							R.string.calendar_syncFuture,
							R.string.calendar_syncFutureDefault));
			controller.parseNode(exp, rootNode, new ParserListener() {

				@Override
				public boolean onItem(boolean finalItem,
						Map<String, Object> values, Node node) {
					if (!node.visible) { // Node not visible
						return false;
					}
					Calendar c = createCalendar(from, to, values);
					if (null == c) { // Not in range
						// Log.w(TAG, "Skipping " + values + ", " +
						// from.getTime()
						// + " - " + to.getTime());
						return false;
					}
					if (finalItem) { // This is entry
						// Log.i(TAG, "Found event: " + node.text + ", " +
						// values
						// + ", " + c.getTime());
						parseEntry(node, c, values, reminderMinutes, " ~"
								+ duration);
						// parseContact((String) values.get("g"), node);
					}
					return true;
				}
			});
			Log.i(TAG, "Ready to add entries: " + entries.size());
			saveData(account, authority, client, color);
		} catch (Exception e) {
			// TODO: handle exception
		}
		Log.i(TAG, "Performing sync... " + account.name + ", " + authority);
	}

	private Uri addIsSync(Uri uri, Account account) {
		android.net.Uri.Builder uriBuilder = uri.buildUpon()
				.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER,
						"true");
		if (null != account) { // Append account data
			uriBuilder.appendQueryParameter(Calendars.ACCOUNT_NAME,
					account.name);
			uriBuilder.appendQueryParameter(Calendars.ACCOUNT_TYPE,
					account.type);
		}
		return uriBuilder.build();
	}

	private void saveData(Account account, String authority,
			ContentProviderClient client, int color) {
		try { // Save errors
			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			// Check if calendar exists or not
			long calID = -1;
			final Cursor cursor = client.query(
					addIsSync(Calendars.CONTENT_URI, null),
					new String[] { Calendars._ID }, Calendars.ACCOUNT_NAME
							+ "=? AND " + Calendars.ACCOUNT_TYPE + "=?",
					new String[] { account.name, account.type }, null);
			if (cursor != null) {
				try {
					if (cursor.moveToFirst()) {
						calID = cursor.getLong(0);
					}
				} finally {
					cursor.close();
				}
			}

			if (calID == -1) {
				// Calendar not exist
				final ContentValues contentValues = new ContentValues();
				contentValues.put(Calendars.ACCOUNT_NAME, account.name);
				contentValues.put(Calendars.ACCOUNT_TYPE, account.type);
				contentValues.put(Calendars.NAME, "Sierra5");
				contentValues.put(Calendars.CALENDAR_DISPLAY_NAME, "Sierra5");
				contentValues.put(Calendars.CALENDAR_COLOR, color);
				contentValues
						.put(Calendars.ALLOWED_REMINDERS,
								Reminders.METHOD_DEFAULT + ","
										+ Reminders.METHOD_ALERT);
				contentValues.put(Calendars.SYNC_EVENTS, 1);
				ops.add(ContentProviderOperation
						.newInsert(addIsSync(Calendars.CONTENT_URI, account))
						.withValues(contentValues).build());
			} else {
				// Remove old entries
				ops.add(ContentProviderOperation
						.newDelete(Events.CONTENT_URI)
						.withSelection(Events.CALENDAR_ID + "=?",
								new String[] { Long.toString(calID) }).build());
				ops.add(ContentProviderOperation
						.newUpdate(addIsSync(Calendars.CONTENT_URI, account))
						.withValue(Calendars.CALENDAR_COLOR, color)
						.withSelection(Calendars._ID + "=?",
								new String[] { Long.toString(calID) }).build());
			}
			for (CalendarEntry ce : entries) { // Insert entries
				Builder builder = ContentProviderOperation
						.newInsert(Events.CONTENT_URI);
				int eventID = ops.size();
				if (calID == -1) { // No calendar
					builder.withValueBackReference(Events.CALENDAR_ID, 0);
				} else {
					builder.withValue(Events.CALENDAR_ID, calID);
				}
				ops.add(builder.withValues(ce.event.getValues()).build());
				if (null != ce.reminder) { // Have reminder
					ops.add(ContentProviderOperation
							.newInsert(Reminders.CONTENT_URI)
							.withValueBackReference(Reminders.EVENT_ID, eventID)
							.withValues(ce.reminder.getValues()).build());
				}
				for (ProviderEntryInfo att : ce.attendees) { // Insert attendees
					ops.add(ContentProviderOperation
							.newInsert(Attendees.CONTENT_URI)
							.withValueBackReference(Attendees.EVENT_ID, eventID)
							.withValues(att.getValues()).build());
				}
			}
			client.applyBatch(ops);
		} catch (Exception e) {
			Log.e(TAG, "Save error", e);
		}
	}

}
