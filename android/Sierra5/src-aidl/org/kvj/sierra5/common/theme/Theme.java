package org.kvj.sierra5.common.theme;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

public class Theme implements Parcelable {

	public int colorBackground = Color.rgb(0x00, 0x00, 0x00);

	public int c1Red = Color.rgb(0xd0, 0x00, 0x00);
	public int c2Green = Color.rgb(0x00, 0xa0, 0x00);
	public int c3Yellow = Color.rgb(0xc0, 0x80, 0x00);
	public int c4Blue = Color.rgb(0x22, 0x22, 0xf0);
	public int c5Purple = Color.rgb(0xa0, 0x00, 0xa0);
	public int c6Cyan = Color.rgb(0x00, 0x80, 0x80);
	public int c7White = Color.rgb(0xc0, 0xc0, 0xc0);

	public int c9LRed = Color.rgb(0xff, 0x77, 0x77);
	public int caLGreen = Color.rgb(0x77, 0xff, 0x77);
	public int cbLYellow = Color.rgb(0xff, 0xff, 0x00);
	public int ccLBlue = Color.rgb(0x88, 0x88, 0xff);
	public int cdLPurple = Color.rgb(0xff, 0x00, 0xff);
	public int ceLCyan = Color.rgb(0x00, 0xff, 0xff);

	public int colorText = Color.rgb(0xff, 0xff, 0xff);

	public String name = null;

	public String code = null;
	public boolean dark = true;

	public Theme(String code, String name) {
		this.code = code;
		this.name = name;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(code);
		dest.writeString(name);
		dest.writeInt(colorBackground);
		dest.writeInt(c1Red);
		dest.writeInt(c2Green);
		dest.writeInt(c3Yellow);
		dest.writeInt(c4Blue);
		dest.writeInt(c5Purple);
		dest.writeInt(c6Cyan);
		dest.writeInt(c7White);
		dest.writeInt(c9LRed);
		dest.writeInt(caLGreen);
		dest.writeInt(cbLYellow);
		dest.writeInt(ccLBlue);
		dest.writeInt(cdLPurple);
		dest.writeInt(ceLCyan);
		dest.writeInt(colorText);
		dest.writeInt(dark ? 1 : 0);
	}

	public static final Parcelable.Creator<Theme> CREATOR = new Creator<Theme>() {

		@Override
		public Theme[] newArray(int size) {
			return new Theme[size];
		}

		@Override
		public Theme createFromParcel(Parcel source) {
			Theme theme = new Theme(source.readString(), source.readString());
			theme.colorBackground = source.readInt();
			theme.c1Red = source.readInt();
			theme.c2Green = source.readInt();
			theme.c3Yellow = source.readInt();
			theme.c4Blue = source.readInt();
			theme.c5Purple = source.readInt();
			theme.c6Cyan = source.readInt();
			theme.c7White = source.readInt();
			theme.c9LRed = source.readInt();
			theme.caLGreen = source.readInt();
			theme.cbLYellow = source.readInt();
			theme.ccLBlue = source.readInt();
			theme.cdLPurple = source.readInt();
			theme.ceLCyan = source.readInt();
			theme.colorText = source.readInt();
			theme.dark = source.readInt() == 1;
			return theme;
		}
	};

}
