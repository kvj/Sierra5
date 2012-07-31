package org.kvj.sierra5.ui.adapter.theme;

import org.kvj.sierra5.common.theme.Theme;

public class ThemeProvider {

	public static Theme getTheme(String theme) {
		if ("white".equals(theme)) { // Light theme
			return new LightTheme();
		}
		if ("mono".equals(theme)) { // Light theme
			return new MonoTheme();
		}
		return new Theme();
	}

}
