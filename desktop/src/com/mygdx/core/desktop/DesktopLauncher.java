package com.mygdx.core.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.core.Constants;
import com.mygdx.core.Main;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.width = Constants.VIEWPORT_WIDTH;
		config.height = Constants.VIEWPORT_HEIGHT;
		config.title = "Prickler";
		config.resizable = false;
		config.fullscreen = false;
		config.samples = 4;

		new LwjglApplication(new Main(), config);
	}
}
