package org.kvj.sierra5.common.plugin;

import java.lang.reflect.Constructor;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.ParcelableSpan;
import android.util.Log;

public class FormatSpan implements Parcelable {

	protected static final String TAG = "FormatSpan";
	private ParcelableSpan[] spans = null;
	private String text = null;

	public FormatSpan() {
	}

	public FormatSpan(String text, ParcelableSpan... spans) {
		this();
		this.text = text;
		this.spans = spans;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(text);
		if (null == spans) { // No spans
			dest.writeInt(0);
			return;
		}
		dest.writeInt(spans.length);
		for (ParcelableSpan span : spans) { // Write array
			dest.writeString(span.getClass().getName());
			span.writeToParcel(dest, flags);
		}
	}

	public static final Parcelable.Creator<FormatSpan> CREATOR = new Creator<FormatSpan>() {

		@Override
		public FormatSpan[] newArray(int size) {
			return new FormatSpan[size];
		}

		@Override
		public FormatSpan createFromParcel(Parcel source) {
			FormatSpan sp = new FormatSpan();
			sp.text = source.readString();
			int spanCount = source.readInt();
			sp.spans = new ParcelableSpan[spanCount];
			// Log.i(TAG, "read items: " + spanCount);
			for (int i = 0; i < spanCount; i++) { // Create
				String className = source.readString();
				try {
					Class<?> cl = ParcelableSpan.class.getClassLoader()
							.loadClass(className);
					Constructor<?> cc = cl.getConstructor(Parcel.class);
					sp.spans[i] = (ParcelableSpan) cc.newInstance(source);
				} catch (Exception e) {
					Log.i(TAG, "createFromParcel: " + spanCount + ", " + i
							+ ", " + className, e);
					return sp;
				}
			}
			return sp;
		}
	};

	public String getText() {
		return text;
	}

	public ParcelableSpan[] getSpans() {
		return spans;
	}

}
