//
//  MUStatus.java
//  Thud
//
//  Copyright (c) 2001-2007 Anthony Parker & the THUD team.
//  All rights reserved. See LICENSE.TXT for more information.
//
package net.sourceforge.btthud.ui;

import net.sourceforge.btthud.ui.status.MUStatusComponent;

import net.sourceforge.btthud.data.MUPrefs;

/**
 * Implements a status report window that displays heading, speed, heat, and
 * weapon information very similar to the MUX's 'status'.
 * @author tkrajcar
 */
public class MUStatus extends ChildWindow implements Runnable {
	private final MUStatusComponent status;

	private final Thud thud;

	private Thread thread = null;
	private boolean go = true;

	public MUStatus (final Thud thud) {
		super (thud, "Status Report");

		this.thud = thud;

		status = new MUStatusComponent (thud.prefs);
		window.add(status);

		window.setSize(thud.prefs.statusSizeX, thud.prefs.statusSizeY);
		window.setLocation(thud.prefs.statusLoc);

		window.setAlwaysOnTop(thud.prefs.statusAlwaysOnTop);

		// Show the window now
		window.setVisible(true);

		start();
	}

	public void newPreferences (final MUPrefs prefs) {
		super.newPreferences(prefs);
		status.newPreferences(prefs);
		window.setAlwaysOnTop(prefs.statusAlwaysOnTop);
	}

	private void start () {
		if (thread == null) {
			thread = new Thread (this, "MUStatusReport");
			thread.start();
		}
	}

	public void run () {
		while (go) {
			try {
				synchronized (thud.data) {
					status.refresh(thud.data);
				}

				// TODO: Refresh only after we get new data.
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// no big deal
			}
		}
	}

	public void pleaseStop () {
		go = false;
		window.dispose();
	}
}
