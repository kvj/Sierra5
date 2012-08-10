package org.kvj.sierra5.common.data;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class Node implements Parcelable {

	public static final int TYPE_FOLDER = 0;
	public static final int TYPE_FILE = 1;
	public static final int TYPE_TEXT = 2;

	public String file = null;

	public List<String> textPath = null;

	public String left = "";
	public List<Node> children = null;
	public boolean collapsed = true;

	public String text = "";
	public String raw = null;

	public int type = TYPE_FOLDER;
	public int level = 0;
	public boolean visible = true;

	public static <T> T[] list2array(List<T> list, T[] zero) {
		if (null == list) { // Empty
			return zero;
		}
		return list.toArray(zero);
	}

	public Node createChild(int childType, String childText, int tabSize) {
		StringBuilder sb = new StringBuilder();
		if (type == Node.TYPE_TEXT) { // Padding only for text
			for (int i = 0; i < tabSize; i++) { // Add spaces
				sb.append(' ');
			}
		}
		Node child = new Node();
		child.file = file;
		child.children = new ArrayList<Node>();
		child.text = childText;
		child.type = childType;
		child.left = left + sb.toString();
		child.level = level + 1;
		if (TYPE_TEXT == childType) { // textPath is necessary
			child.textPath = new ArrayList<String>();
			if (TYPE_TEXT == type && null != textPath) { // Parent is TEXT
				child.textPath.addAll(textPath);
			}
			child.textPath.add(childText);
		}
		if (null == children) { // No children yet
			children = new ArrayList<Node>();
		}
		children.add(child);
		return child;
	}

	public void setText(String text) {
		this.text = text;
		if (TYPE_TEXT == type) { // Replace last text
			textPath.remove(textPath.size() - 1);
			textPath.add(text);
		}
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		p.writeString(file); // file
		p.writeStringList(textPath == null ? new ArrayList<String>() : textPath); // textPath
		p.writeString(left); // left
		p.writeList(children == null ? new ArrayList<Node>() : children);
		p.writeByte((byte) (collapsed ? 1 : 0));
		p.writeString(text);
		p.writeString(raw);
		p.writeInt(type);
		p.writeInt(level);
		p.writeByte((byte) (visible ? 1 : 0));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public void readFromParcel(Parcel p) {
		file = p.readString();
		textPath = new ArrayList<String>();
		p.readStringList(textPath);
		left = p.readString();
		children = new ArrayList<Node>();
		p.readList(children, Node.class.getClassLoader());
		collapsed = p.readByte() == 1;
		text = p.readString();
		raw = p.readString();
		type = p.readInt();
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

}
