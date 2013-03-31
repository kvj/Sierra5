package org.kvj.sierra5.common.data;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class Node<E extends Parcelable> implements Parcelable {

	public static final int CAPABILITY_ADD = 0;
	public static final int CAPABILITY_EDIT = 1;
	public static final int CAPABILITY_REMOVE = 2;
	public static final int CAPABILITY_ROOT = 3;

	public static final int STYLE_0 = 0;
	public static final int STYLE_1 = 1;
	public static final int STYLE_2 = 2;
	public static final int STYLE_3 = 3;
	public static final int STYLE_4 = 4;
	public static final int STYLE_5 = 5;
	public static final int STYLE_6 = 6;

	public static final int EXPAND_ONE = 1;
	public static final int EXPAND_GROUP = 2;

	public E id = null;
	public List<Node<E>> children = null;
	public boolean collapsed = true;

	public String text = "";

	public int level = 0;
	public boolean visible = true;
	public int style = STYLE_0;
	public int[] capabilities = {};

	public static <T> T[] list2array(List<T> list, T[] zero) {
		if (null == list) { // Empty
			return zero;
		}
		return list.toArray(zero);
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		p.writeParcelable(id, flags);
		p.writeInt(style);
		p.writeIntArray(capabilities);
		p.writeList(children == null ? new ArrayList<Node>() : children);
		p.writeByte((byte) (collapsed ? 1 : 0));
		p.writeString(text);
		p.writeInt(level);
		p.writeByte((byte) (visible ? 1 : 0));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public void readFromParcel(Parcel p) {
		id = p.readParcelable(Node.class.getClassLoader());
		style = p.readInt();
		capabilities = p.createIntArray();
		children = new ArrayList<Node<E>>();
		p.readList(children, Node.class.getClassLoader());
		collapsed = p.readByte() == 1;
		text = p.readString();
		level = p.readInt();
		visible = p.readByte() == 1;

	}

	public static final Parcelable.Creator<Node> CREATOR = new Creator<Node>() {

		@Override
		public Node[] newArray(int size) {
			return new Node[size];
		}

		@Override
		public Node createFromParcel(Parcel p) {
			Node node = new Node();
			node.readFromParcel(p);
			return node;
		}
	};

	public boolean can(int capability) {
		for (int c : capabilities) { // Search
			if (c == capability) { // Found
				return true;
			}
		}
		return false; // Not found
	}

}
