package org.kvj.sierra5.common.data.provider;

import android.os.Parcel;
import android.os.Parcelable;

public class SDCardProviderID implements NodeID {

	public String[] path = new String[0];
	public int type = 0;
	public String raw = null;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (null == o) { // Invalid
			return false;
		}
		SDCardProviderID other = (SDCardProviderID) o;
		if (path.length != other.path.length) { // Size is different
			return false;
		}
		for (int i = 0; i < path.length; i++) { // Compare items
			String item = path[i];
			if (!item.equals(other.path[i])) { // Failed
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID: " + type);
		for (String item : path) { // Add path
			sb.append(", " + item);
		}
		return sb.toString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringArray(path);
		dest.writeInt(type);
	}

	public static final Parcelable.Creator<SDCardProviderID> CREATOR = new Creator<SDCardProviderID>() {

		@Override
		public SDCardProviderID createFromParcel(Parcel source) {
			SDCardProviderID id = new SDCardProviderID();
			id.path = source.createStringArray();
			id.type = source.readInt();
			return id;
		}

		@Override
		public SDCardProviderID[] newArray(int size) {
			return new SDCardProviderID[size];
		}

	};

	@Override
	public String getProviderID() {
		return "sdcard";
	}

}
