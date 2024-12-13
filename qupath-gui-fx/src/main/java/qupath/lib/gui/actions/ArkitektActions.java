/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2023 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */


package qupath.lib.gui.actions;

import org.controlsfx.control.action.Action;

import qupath.lib.gui.actions.annotations.ActionConfig;
import qupath.lib.gui.actions.annotations.ActionIcon;
import qupath.lib.gui.tools.IconFactory.PathIcons;
import qupath.lib.gui.viewer.ViewerManager;

/**
 * Actions that interact with one or more viewers.
 * These can be used as a basis for creating UI controls that operate on the same options.
 * 
 * @author Pete Bankhead
 * @since v0.5.0
 */
public class ArkitektActions {
	
	@ActionIcon(PathIcons.OVERVIEW)
	@ActionConfig("ViewerActions.overview")
	public final Action LOGIN;

	private final ViewerManager viewerManager;
    private final Arkitekt arkitekt = new Arkitekt();
	
	public ArkitektActions(ViewerManager viewerManager) {
		this.viewerManager = viewerManager;
	
		LOGIN = ActionTools.createAction(() -> {
			this.arkitekt.login("http://127.0.0.1/lok/f/start/");
			
		});

        // Register actions
		ActionTools.getAnnotatedActions(this);
	}
	
	public ViewerManager getViewerManager() {
		return viewerManager;
	}



	


	


	
}