package org.kvj.sierra5.ui.adapter.theme;

import java.util.LinkedHashMap;
import java.util.Map;

import org.kvj.sierra5.common.theme.Theme;

public class ThemeProvider {

	static Map<String, Theme> themes = null;

	public static Theme getTheme(String name) {
		if (null == themes) { // Create
			themes = new LinkedHashMap<String, Theme>();
			themes.put("black", new Theme("black", "Default"));
			themes.put("white", new LightTheme());
			themes.put("mono", new MonoTheme());
		}
		Theme theme = themes.get(name);
		if (null == theme) { // Not found
			return themes.get("black");
		}
		return theme;
	}

	public static Theme[] getThemes() {
		getTheme(null);// Make sure themes created
		return themes.values().toArray(new Theme[0]);
	}

}
