package com.thelogicmaster.switchgdx;

import com.agateau.pixelwheels.PwGame;
import com.agateau.utils.FileUtils;

public class SwitchLauncher {

	public static void main (String[] arg) {
		FileUtils.appName = "pixelwheels";
		new SwitchApplication(new PwGame());
	}
}
