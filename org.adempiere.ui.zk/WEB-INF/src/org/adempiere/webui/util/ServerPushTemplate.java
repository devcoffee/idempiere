/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
 * Copyright (C) 2008 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.adempiere.webui.util;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.session.SessionContextListener;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.DesktopUnavailableException;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * Zk UI update must be done in UI (event listener) thread. This class help to implement
 * that base on spring's jdbc template pattern.
 * @author hengsin
 *
 */
public class ServerPushTemplate {

	private Desktop desktop;

	/**
	 * @param desktop
	 */
	public ServerPushTemplate(Desktop desktop) {
		this.desktop = desktop;
	}

	/**
	 * Execute asynchronous task in UI (event listener) thread. This is implemented
	 * using Executions.schedule and will return immediately.
	 * @param callback
	 */
	public void executeAsync(final IServerPushCallback callback) {
		if (!desktop.isAlive())
			return;
		
		try {
    		EventListener<Event> task = new EventListener<Event>() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!SessionContextListener.isContextValid()) {
						SessionContextListener.setupExecutionContextFromSession(desktop.getExecution());
					}
					callback.updateUI();
				}
			};
    		Executions.schedule(desktop, task, new Event("onExecute"));
		} catch (DesktopUnavailableException de) {
			throw de;
    	} catch (Exception e) {
    		throw new AdempiereException("Failed to update client in server push worker thread.", e);
		}
	}

	/**
	 * Execute synchronous task in UI (event listener) thread. This is implemented
	 * using Executions.activate/deactivate and will only return after the
	 * invoked task have ended.
	 * <p> 
	 * For better scalability, if possible, you should use {@link #executeAsync(IServerPushCallback)} instead. 
	 * @param callback
	 */
	public void execute(IServerPushCallback callback) {
		boolean inUIThread = Executions.getCurrent() != null;
		boolean desktopActivated = false;

		try {
	    	if (!inUIThread) {
	    		//10 minutes timeout
	    		if (Executions.activate(desktop, 10 * 60 * 1000)) {
	    			desktopActivated = true;
	    		} else {
	    			throw new DesktopUnavailableException("Timeout activating desktop.");
	    		}
	    	}
			callback.updateUI();
		} catch (DesktopUnavailableException de) {
			throw de;
    	} catch (Exception e) {
    		throw new AdempiereException("Failed to update client in server push worker thread.", e);
    	} finally {
    		if (!inUIThread && desktopActivated) {
    			Executions.deactivate(desktop);
    		}
    	}
	}
	
	/**
	 * Get desktop
	 * @return desktop
	 */
	public Desktop getDesktop() {
		return desktop;
	}
}
