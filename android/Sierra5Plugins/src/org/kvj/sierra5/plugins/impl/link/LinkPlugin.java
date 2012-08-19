package org.kvj.sierra5.plugins.impl.link;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kvj.sierra5.common.data.Node;
import org.kvj.sierra5.common.plugin.FormatSpan;
import org.kvj.sierra5.common.plugin.MenuItemInfo;
import org.kvj.sierra5.common.plugin.PluginInfo;
import org.kvj.sierra5.common.theme.Theme;
import org.kvj.sierra5.plugins.App;
import org.kvj.sierra5.plugins.R;
import org.kvj.sierra5.plugins.WidgetController;
import org.kvj.sierra5.plugins.impl.DefaultPlugin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.RemoteViews;

public class LinkPlugin extends DefaultPlugin {

	class ParseInfo {
		Map<String, String> params = new HashMap<String, String>();
		int level = 0;
		String file = null;
	}

	private WidgetController controller = null;

	Map<String, ParseInfo> parseInfos = new HashMap<String, ParseInfo>();

	public LinkPlugin(WidgetController controller) {
		super();
		this.controller = controller;
	}

	private static final String TAG = "LinkPlugin";
	Pattern linkPattern = Pattern.compile("\\[\\[(.+)\\]\\]");

	@Override
	public String getName() throws RemoteException {
		return "Link plugin";
	}

	@Override
	public int[] getCapabilities() throws RemoteException {
		return new int[] { PluginInfo.PLUGIN_CAN_FORMAT,
				PluginInfo.PLUGIN_CAN_PARSE, PluginInfo.PLUGIN_CAN_RENDER,
				PluginInfo.PLUGIN_HAVE_EDIT_MENU, PluginInfo.PLUGIN_HAVE_MENU };
	}

	private String getLinkCaption(String link) {
		link = link.substring(2, link.length() - 2);
		Log.i(TAG, "Format:" + link);
		if (link.startsWith("geo:")) { // geo
			return "GEO";
		}
		if (link.endsWith(".jpg")) { // image
			return "Image";
		}
		if (link.endsWith(".kml")) { // KML file
			return "KML";
		}
		return "Attachment";
	}

	@Override
	public FormatSpan[] format(int index, Theme theme, Node node, String text,
			boolean selected) throws RemoteException {
		switch (index) {
		case 0: // Name
			return new FormatSpan[] { new FormatSpan(text,
					new ForegroundColorSpan(theme.caLGreen)) };
		case 1: // Activity
			return new FormatSpan[] {
					new FormatSpan(text.substring(0, text.length() - 1),
							new ForegroundColorSpan(theme.ceLCyan)),
					new FormatSpan(":",
							new ForegroundColorSpan(theme.colorText)) };
		case 2: // Project
			String project = text.substring(0, text.indexOf(','));
			return new FormatSpan[] {
					new FormatSpan(project, new ForegroundColorSpan(
							theme.c9LRed)),
					new FormatSpan(", ", new ForegroundColorSpan(
							theme.colorText)) };
		case 3: // Tag
			return new FormatSpan[] { new FormatSpan(text,
					new ForegroundColorSpan(theme.ccLBlue)) };
		case 4: // Link
			return new FormatSpan[] { new FormatSpan(getLinkCaption(text),
					new ForegroundColorSpan(theme.cbLYellow)) };
		}
		return null;
	}

	@Override
	public MenuItemInfo[] getMenu(int id, Node node) throws RemoteException {
		Matcher m = linkPattern.matcher(node.text);
		if (m.find()) { // Open link
			return new MenuItemInfo[] { new MenuItemInfo(0,
					MenuItemInfo.MENU_ITEM_ACTION, "Open attachment...") };
		}
		return null;
	}

	private Map<String, String> parseGeo(String geo) {
		String[] parts = geo.split(",");
		Map<String, String> result = new HashMap<String, String>();
		if (parts.length > 1) { // Have lat lon
			result.put("lat", parts[0]);
			result.put("lon", parts[1]);
		}
		for (int i = 2; i < parts.length; i++) { // Other parts
			String[] arr = parts[i].split("=");
			if (arr.length == 2) { // both
				result.put(arr[0], arr[1]);
			}
		}
		return result;
	}

	@Override
	public boolean executeAction(int id, Node node) throws RemoteException {
		Matcher m = linkPattern.matcher(node.text);
		if (m.find()) { // Open link
			String link = m.group(1);
			Intent i = new Intent(android.content.Intent.ACTION_VIEW);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (link.startsWith("geo:")) { // Geo
				Map<String, String> geo = parseGeo(link.substring(4));
				if (geo.containsKey("lat") && geo.containsKey("lon")) {
					// Have coords
					String uri = String.format("geo:%s,%s", geo.get("lat"),
							geo.get("lon"));
					i.setData(Uri.parse(uri));
					App.getInstance().startActivity(i);
					return true;
				}
			} else {
				File file = findFile(node, link);
				if (null == file) { // File not found
					return false;
				}
				Uri uri = Uri.fromFile(file);
				String extension = MimeTypeMap.getFileExtensionFromUrl(uri
						.toString());
				String mimetype = MimeTypeMap.getSingleton()
						.getMimeTypeFromExtension(extension);
				i.setDataAndType(uri, mimetype);
				App.getInstance().startActivity(i);
				return true;
			}
		}
		return false;
	}

	@Override
	public int getFormatterCount() throws RemoteException {
		return 5;
	}

	@Override
	public String getPattern(int index, Node node, boolean selected)
			throws RemoteException {
		if (Node.TYPE_TEXT != node.type) { // Only for texts
			return null;
		}
		switch (index) {
		case 0: // Name
			return "@[A-Z][A-Za-z0-9\\-]+";
		case 1: // Activity
			return "#[A-Za-z0-9\\-]+\\:";
		case 2: // Project
			return "(\\ |^)[A-Z][A-Za-z0-9\\-]+,(\\ |$)";
		case 3: // Tag
			return "\\ -[a-z0-9\\_]+";
		case 4: // Link
			return linkPattern.pattern();
		}
		return null;
	}

	@Override
	public void parse(Node node, Node parent) throws RemoteException {
		if (node.type == Node.TYPE_TEXT) { // Text
			if (node.text.endsWith(" /-")) { // Collapse
				node.collapsed = true;
			}
			ParseInfo info = parseInfos.get(node.file);
			if (null != info) { // Have info
				String collapse = info.params.get("collapse");
				if (null != collapse) { // Have collapse
					if ("all".equals(collapse)) { // All levels
						node.collapsed = true;
					} else {
						try { // Number errors
							int level = Integer.parseInt(collapse, 10);
							if (node.level - info.level + 1 == level) {
								// This is level we are looking
								node.collapsed = true;
							}
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
				}
			}
		}
		if (node.type == Node.TYPE_TEXT && node.text.startsWith("#")) { // Comment
			node.visible = false;
			if (node.text.startsWith("##")) { // Special config
				String[] pairs = node.text.substring(2).split(",");
				ParseInfo info = new ParseInfo();
				info.level = node.level;
				for (String pair : pairs) { // split by =
					String[] nameValue = pair.split("=");
					if (nameValue.length == 2) { // Have nameValue
						info.params.put(nameValue[0].trim(),
								nameValue[1].trim());
					}
				}
				parseInfos.put(node.file, info);
			}
		}
	}

	private File findFile(Node node, String link) {
		File file = null;
		if (link.startsWith("/")) { // From root
			try {
				String root = controller.getRootService().getRoot();
				File rootFile = new File(root);
				if (!rootFile.exists()) { // Invalid root
					return null;
				}
				file = new File(rootFile, link.substring(1));
			} catch (Exception e) {
				return null;
			}
		} else {
			// Relative path
			File rootFile = new File(node.file);
			if (!rootFile.exists()) { // Invalid root
				return null;
			}
			file = new File(rootFile.getParent(), link);
		}
		if (null != file && file.exists() && file.isFile()) { // File is OK
			return file;
		}
		return null;
	}

	private File getLink(Node node) {
		Matcher m = linkPattern.matcher(node.text);
		if (m.find()) { // Have link
			String link = m.group(1);
			Log.i(TAG, "Found link: " + link);
			return findFile(node, link);
		}
		return null;
	}

	private Bitmap decodeFile(File f, int width) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			o.inPurgeable = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			// Find the correct scale value. It should be the power of 2.
			if (o.outWidth <= 0 || width <= 0) { // Invalid width
				return null;
			}
			int scale = 1;
			while (o.outWidth / scale / 2 >= width) {
				scale *= 2;
			}
			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	@Override
	public RemoteViews render(Node node, Theme theme, int width)
			throws RemoteException {
		File file = getLink(node);
		// Log.i(TAG, "render: " + node.text + ", " + file);
		if (null == file || !file.getName().toLowerCase().endsWith(".jpg")) {
			// Not an image
			return null;
		}
		Bitmap image = decodeFile(file, width);
		if (null == image) { // Not a bitmap
			return null;
		}
		RemoteViews rv = new RemoteViews(App.getInstance().getPackageName(),
				R.layout.link_image);
		rv.setImageViewBitmap(R.id.link_image_image, image);
		return rv;
	}

	@Override
	public MenuItemInfo[] getEditorMenu(int id, Node node)
			throws RemoteException {
		return new MenuItemInfo[] {
				new MenuItemInfo(0, MenuItemInfo.MENU_ITEM_INSERT_TEXT,
						"Insert date and time"),
				new MenuItemInfo(1, MenuItemInfo.MENU_ITEM_INSERT_TEXT,
						"Insert time") };
	}

	@Override
	public String executeEditAction(int id, String text, Node node)
			throws RemoteException {
		String format = "";
		switch (id) {
		case 0: // Date time
			format = App.getInstance().getStringPreference(
					R.string.template_insertDateTime,
					R.string.template_insertDateTimeDefault);
			break;
		case 1: // Time
			format = App.getInstance().getStringPreference(
					R.string.template_insertTime,
					R.string.template_insertTimeDefault);
			break;
		}
		SimpleDateFormat dt = new SimpleDateFormat(format, Locale.ENGLISH);
		return dt.format(new Date()) + " ";
	}

}
