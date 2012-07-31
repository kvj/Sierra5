package org.kvj.sierra5.common.plugin;

import android.os.Parcel;
import android.os.Parcelable;

public class MenuItemInfo implements Parcelable {

	public static final int MENU_ITEM_SUBMENU = 0;
	public static final int MENU_ITEM_ACTION = 1;

	private String text = null;
	private int type = MENU_ITEM_ACTION;
	private int id = 0;

	public MenuItemInfo() {
	}

	public MenuItemInfo(int id, int type, String text) {
		this();
		this.id = id;
		this.type = type;
		this.text = text;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		p.writeInt(id);
		p.writeInt(type);
		p.writeString(text);
	}

	public static final Parcelable.Creator<MenuItemInfo> CREATOR = new Creator<MenuItemInfo>() {

		@Override
		public MenuItemInfo[] newArray(int arg0) {
			return new MenuItemInfo[arg0];
		}

		@Override
		public MenuItemInfo createFromParcel(Parcel p) {
			return new MenuItemInfo(p.readInt(), p.readInt(), p.readString());
		}
	};

	public String getText() {
		return text;
	}

	public int getType() {
		return type;
	}

	public int getId() {
		return id;
	}

}
