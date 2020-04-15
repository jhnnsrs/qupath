package qupath.lib.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.controlsfx.control.action.Action;

import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import qupath.lib.algorithms.IntensityFeaturesPlugin;
import qupath.lib.algorithms.TilerPlugin;
import qupath.lib.gui.ActionTools.ActionAccelerator;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionIcon;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.QuPathGUI.DefaultActions;
import qupath.lib.gui.commands.Commands;
import qupath.lib.gui.commands.MeasurementExportCommand;
import qupath.lib.gui.commands.ProjectCommands;
import qupath.lib.gui.commands.SparseImageServerCommand;
import qupath.lib.gui.commands.SpecifyAnnotationCommand;
import qupath.lib.gui.commands.TMACommands;
import qupath.lib.gui.commands.ZoomCommand;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.icons.IconFactory.PathIcons;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.gui.tools.CommandFinderTools;
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathCellObject;
import qupath.lib.objects.PathDetectionObject;
import qupath.lib.objects.PathTileObject;
import qupath.lib.objects.TMACoreObject;
import qupath.lib.plugins.objects.DilateAnnotationPlugin;
import qupath.lib.plugins.objects.FillAnnotationHolesPlugin;
import qupath.lib.plugins.objects.FindConvexHullDetectionsPlugin;
import qupath.lib.plugins.objects.RefineAnnotationsPlugin;
import qupath.lib.plugins.objects.ShapeFeaturesPlugin;
import qupath.lib.plugins.objects.SmoothFeaturesPlugin;
import qupath.lib.plugins.objects.SplitAnnotationsPlugin;

class Menus {
	
	private final static String URL_DOCS       = "https://qupath.readthedocs.io";
	private final static String URL_VIDEOS     = "https://www.youtube.com/c/QuPath";
	private final static String URL_CITATION   = "https://qupath.readthedocs.io/en/latest/docs/intro/citing.html";
	private final static String URL_BUGS       = "https://github.com/qupath/qupath/issues";
	private final static String URL_FORUM      = "https://forum.image.sc/tags/qupath";
	private final static String URL_SOURCE     = "https://github.com/qupath/qupath";

	
	private QuPathGUI qupath;
	private DefaultActions actionManager;
	
	private List<?> managers;
	
	Menus(QuPathGUI qupath) {
		this.qupath = qupath;
	}
	
	public synchronized Collection<Action> getActions() {
		if (managers == null) {
			this.actionManager = qupath.getDefaultActions();
			managers = Arrays.asList(
					new FileMenuManager(),
					new EditMenuManager(),
					new ObjectsMenuManager(),
					new ViewMenuManager(),
					new MeasureMenuManager(),
					new AutomateMenuManager(),
					new AnalyzeMenuManager(),
					new TMAMenuManager(),
					new ClassifyMenuManager(),
					new HelpMenuManager()
					);
		}
		return managers.stream().flatMap(m -> ActionTools.getAnnotatedActions(m).stream()).collect(Collectors.toList());
	}
	
	static Action createAction(Runnable runnable) {
		return new Action(e -> runnable.run());
	}
	
	Action createSelectableCommandAction(final ObservableValue<Boolean> observable) {
		return ActionTools.createSelectableAction(observable, null);
	}
	
	
	
	@ActionMenu("Edit")
	public class EditMenuManager {
		
		@ActionMenu("Undo")
		@ActionAccelerator("shortcut+z")
		public final Action UNDO;
		
		@ActionMenu("Redo")
		@ActionAccelerator("shortcut+shift+z")
		public final Action REDO;
		
		public final Action SEP_0 = ActionTools.createSeparator();
		
		// Copy actions
		@ActionMenu("Copy to clipboard...>Current viewer")
		@ActionAccelerator("shortcut+c")
		public final Action COPY_VIEW = createAction(() -> copyViewToClipboard(qupath, GuiTools.SnapshotType.VIEWER));
		
		@ActionMenu("Copy to clipboard...>Main window content")
		public final Action COPY_WINDOW = createAction(() -> copyViewToClipboard(qupath, GuiTools.SnapshotType.MAIN_SCENE));
		
		@ActionMenu("Copy to clipboard...>Main window screenshot")
		public final Action COPY_WINDOW_SCREENSHOT = createAction(() -> copyViewToClipboard(qupath, GuiTools.SnapshotType.MAIN_WINDOW_SCREENSHOT));			
		
		@ActionMenu("Copy to clipboard...>Full screenshot")
		public final Action COPY_FULL_SCREENSHOT = createAction(() -> copyViewToClipboard(qupath, GuiTools.SnapshotType.FULL_SCREENSHOT));

		public final Action SEP_1 = ActionTools.createSeparator();
		
		@ActionMenu("Preferences...")
		@ActionIcon(PathIcons.COG)
		@ActionAccelerator("shortcut+,")
		@ActionDescription("Set preferences to customize QuPath's appearance and behavior")
		public final Action PREFERENCES = createAction(() -> Commands.showPreferencesDialog(qupath));
		
		@ActionMenu("Reset preferences")
		@ActionDescription("Reset preferences to their default values - this can be useful if you are experiencing any newly-developed persistent problems with QuPath")
		public final Action RESET_PREFERENCES = createAction(() -> Commands.promptToResetPreferences());

		
		public EditMenuManager() {
			var undoRedo = qupath.getUndoRedoManager();
			UNDO = createUndoAction(undoRedo);
			REDO = createRedoAction(undoRedo);
		}
		
		private Action createUndoAction(UndoRedoManager undoRedoManager) {
			Action actionUndo = new Action("Undo", e -> undoRedoManager.undoOnce());
			actionUndo.disabledProperty().bind(undoRedoManager.canUndo().not());
			return actionUndo;
		}
		
		private Action createRedoAction(UndoRedoManager undoRedoManager) {
			Action actionRedo = new Action("Redo", e -> undoRedoManager.redoOnce());
			actionRedo.disabledProperty().bind(undoRedoManager.canRedo().not());
			return actionRedo;
		}
		
		private void copyViewToClipboard(final QuPathGUI qupath, final GuiTools.SnapshotType type) {
			Image img = GuiTools.makeSnapshotFX(qupath, qupath.getViewer(), type);
			Clipboard.getSystemClipboard().setContent(Collections.singletonMap(DataFormat.IMAGE, img));
		}
	}
	

	@ActionMenu("Automate")
	public class AutomateMenuManager {
		
		@ActionMenu("Show script editor")
		@ActionAccelerator("shortcut+[")
		public final Action SCRIPT_EDITOR = createAction(() -> Commands.showScriptEditor(qupath));

		@ActionMenu("Script interpreter")
		public final Action SCRIPT_INTERPRETER = createAction(() -> Commands.showScriptInterpreter(qupath));
		
		public final Action SEP_0 = ActionTools.createSeparator();
		
		@ActionMenu("Show workflow command history")
		@ActionAccelerator("shortcut+shift+w")
		public final Action HISTORY_SHOW = Commands.createSingleStageAction(() -> Commands.createWorkflowDisplayDialog(qupath));

		@ActionMenu("Create command history script")
		public final Action HISTORY_SCRIPT = qupath.createImageDataAction(imageData -> Commands.showWorkflowScript(qupath, imageData));

	}
	
	@ActionMenu("Analyze")
	public class AnalyzeMenuManager {
		
		@ActionMenu("Preprocessing>Estimate stain vectors")
		public final Action COLOR_DECONVOLUTION_REFINE = qupath.createImageDataAction(imageData -> Commands.promptToEstimateStainVectors(imageData));
		
		@ActionMenu("Region identification>Tiles & superpixels>Create tiles")
		public final Action CREATE_TILES = qupath.createPluginAction("Create tiles", TilerPlugin.class, null);

		@ActionMenu("Cell detection>")
		public final Action SEP_0 = ActionTools.createSeparator();

		@ActionMenu("Calculate features>Add smoothed features")
		public final Action SMOOTHED_FEATURES = qupath.createPluginAction("Add Smoothed features", SmoothFeaturesPlugin.class, null);
		@ActionMenu("Calculate features>Add intensity features")
		public final Action INTENSITY_FEATURES = qupath.createPluginAction("Add intensity features", IntensityFeaturesPlugin.class, null);
		@ActionMenu("Calculate features>Add shape features (deprecated)")
		public final Action SHAPE_FEATURES = qupath.createPluginAction("Add shape features", ShapeFeaturesPlugin.class, null);

		@ActionMenu("Spatial analysis>Distance to annotations 2D")
		public final Action DISTANCE_TO_ANNOTATIONS = qupath.createImageDataAction(imageData -> Commands.distanceToAnnotations2D(imageData));
		@ActionMenu("Spatial analysis>Detect centroid distances 2D")
		public final Action DISTANCE_CENTROIDS = qupath.createImageDataAction(imageData -> Commands.detectionCentroidDistances2D(imageData));

	}
	
	@ActionMenu("Classify")
	public class ClassifyMenuManager {
		
		
		@ActionMenu("Object classification>Legacy>Load detection classifier (legacy)")
		public final Action LEGACY_DETECTION = Commands.createSingleStageAction(() -> Commands.createLegacyLoadDetectionClassifierCommand(qupath));
		
		@ActionMenu("Object classification>")
		public final Action SEP_1 = ActionTools.createSeparator();

		@ActionMenu("Object classification>")
		public final Action SEP_2 = ActionTools.createSeparator();

		@ActionMenu("Object classification>Reset detection classifications")
		public final Action RESET_DETECTION_CLASSIFICATIONS = qupath.createImageDataAction(imageData -> Commands.resetClassifications(imageData, PathDetectionObject.class));

		@ActionMenu("Pixel classification>")
		public final Action SEP_3 = ActionTools.createSeparator();

		public final Action SEP_4 = ActionTools.createSeparator();

		@ActionMenu("Extras>Create combined training image")
		public final Action TRAINING_IMAGE = createAction(new SparseImageServerCommand(qupath));

	}
	
	@ActionMenu("File")
	public class FileMenuManager {
		
		@ActionMenu("Project...>Create project")
		public final Action PROJECT_NEW = createAction(() -> Commands.promptToCreateProject(qupath));
		
		@ActionMenu("Project...>Open project")
		public final Action PROJECT_OPEN = createAction(() -> Commands.promptToOpenProject(qupath));
		
		@ActionMenu("Project...>Close project")
		public final Action PROJECT_CLOSE = qupath.createProjectAction(project -> Commands.closeProject(qupath));
		
		@ActionMenu("Project...>")
		public final Action SEP_1 = ActionTools.createSeparator();

		@ActionMenu("Project...>Add images")
		public final Action IMPORT_IMAGES = qupath.createProjectAction(project -> ProjectCommands.promptToImportImages(qupath));
		@ActionMenu("Project...>Export image list")
		public final Action EXPORT_IMAGE_LIST = qupath.createProjectAction(project -> ProjectCommands.promptToExportImageList(project));	
		
		@ActionMenu("Project...>")
		public final Action SEP_2 = ActionTools.createSeparator();

		@ActionMenu("Project...>Edit project metadata")
		public final Action METADATA = qupath.createProjectAction(project -> ProjectCommands.showProjectMetadataEditor(qupath));
		
		@ActionMenu("Project...>Check project URIs")
		public final Action CHECK_URIS = qupath.createProjectAction(project -> {
			try {
				ProjectCommands.promptToCheckURIs(project, false);
			} catch (IOException e) {
				Dialogs.showErrorMessage("Check project URIs", e);
			}
		});

		public final Action SEP_3 = ActionTools.createSeparator();

		// TODO: ADD RECENT PROJECTS
		@ActionMenu("Open...")
		@ActionAccelerator("shortcut+o")
		public final Action OPEN_IMAGE = createAction(() -> qupath.openImage(null, true, false));
		@ActionMenu("Open URI...")
		@ActionAccelerator("shortcut+shift+o")
		public final Action OPEN_IMAGE_OR_URL = createAction(() -> qupath.openImage(null, true, true));
		
		@ActionMenu("Reload data")
		@ActionAccelerator("shortcut+r")
		public final Action RELOAD_DATA = qupath.createImageDataAction(imageData -> Commands.reloadImageData(qupath, imageData));

		public final Action SEP_4 = ActionTools.createSeparator();
		
		@ActionMenu("Save As")
		@ActionAccelerator("shortcut+s")
		public final Action SAVE_DATA_AS = qupath.createImageDataAction(imageData -> Commands.promptToSaveImageData(qupath, imageData, false));
		@ActionMenu("Save")
		@ActionAccelerator("shortcut+shift+s")
		public final Action SAVE_DATA = qupath.createImageDataAction(imageData -> Commands.promptToSaveImageData(qupath, imageData, true));
		
		public final Action SEP_5 = ActionTools.createSeparator();
		
		@ActionMenu("Export images...>Original pixels")
		public final Action EXPORT_ORIGINAL = qupath.createImageDataAction(imageData -> Commands.promptToExportImageRegion(qupath.getViewer(), false));
		@ActionMenu("Export images...>Rendered RGB (with overlays)")
		public final Action EXPORT_RENDERED = qupath.createImageDataAction(imageData -> Commands.promptToExportImageRegion(qupath.getViewer(), true));
		
		@ActionMenu("Export snapshot...>Main window screenshot")
		public final Action SNAPSHOT_WINDOW = createAction(() -> Commands.saveSnapshot(qupath, GuiTools.SnapshotType.MAIN_WINDOW_SCREENSHOT));
		@ActionMenu("Export snapshot...>Main window content")
		public final Action SNAPSHOT_WINDOW_CONTENT = createAction(() -> Commands.saveSnapshot(qupath, GuiTools.SnapshotType.MAIN_SCENE));
		@ActionMenu("Export snapshot...>Current viewer content")
		public final Action SNAPSHOT_VIEWER_CONTENT = createAction(() -> Commands.saveSnapshot(qupath, GuiTools.SnapshotType.VIEWER));
		
		public final Action SEP_6 = ActionTools.createSeparator();

		@ActionMenu("TMA data...>Import TMA map")
		public final Action TMA_IMPORT = qupath.createImageDataAction(imageData -> TMACommands.promptToImportTMAData(imageData));
		@ActionMenu("TMA data...>Launch Export TMA data")
		public final Action TMA_EXPORT = qupath.createImageDataAction(imageData -> TMACommands.promptToExportTMAData(qupath, imageData));
		@ActionMenu("TMA data...>Launch TMA data viewer")
		public final Action TMA_VIEWER = createAction(() -> Commands.launchTMADataViewer(qupath));

		public final Action SEP_7 = ActionTools.createSeparator();

		@ActionMenu("Quit")
		public final Action QUIT = new Action("Quit", e -> qupath.tryToQuit());

	}
	
	
	@ActionMenu("Objects")
	public class ObjectsMenuManager {
		
		@ActionMenu("Delete...>Delete selected objects")
		public final Action DELETE_SELECTED_OBJECTS = qupath.createImageDataAction(imageData -> GuiTools.promptToClearAllSelectedObjects(imageData));
		
		@ActionMenu("Delete...>")
		public final Action SEP_1 = ActionTools.createSeparator();
		
		@ActionMenu("Delete...>Delete all objects")
		public final Action CLEAR_HIERARCHY = qupath.createImageDataAction(imageData -> Commands.promptToDeleteObjects(imageData, null));
		@ActionMenu("Delete...>Delete all annotations")
		public final Action CLEAR_ANNOTATIONS = qupath.createImageDataAction(imageData -> Commands.promptToDeleteObjects(imageData, PathAnnotationObject.class));
		@ActionMenu("Delete...>Delete all detects")
		public final Action CLEAR_DETECTIONS = qupath.createImageDataAction(imageData -> Commands.promptToDeleteObjects(imageData, PathDetectionObject.class));

		@ActionMenu("Select...>Reset selection")
		@ActionAccelerator("shortcut+alt+r")
		public final Action RESET_SELECTION = qupath.createImageDataAction(imageData -> Commands.resetSelection(imageData));

		@ActionMenu("Select...>")
		public final Action SEP_2 = ActionTools.createSeparator();

		@ActionMenu("Select...>Select TMA cores")
		@ActionAccelerator("shortcut+alt+t")
		public final Action SELECT_TMA_CORES = qupath.createImageDataAction(imageData -> Commands.selectObjectsByClass(imageData, TMACoreObject.class));
		@ActionMenu("Select...>Select annotations")
		@ActionAccelerator("shortcut+alt+a")
		public final Action SELECT_ANNOTATIONS = qupath.createImageDataAction(imageData -> Commands.selectObjectsByClass(imageData, PathAnnotationObject.class));

		@ActionMenu("Select...>Select detections...>Select all detections")
		@ActionAccelerator("shortcut+alt+d")
		public final Action SELECT_ALL_DETECTIONS = qupath.createImageDataAction(imageData -> Commands.selectObjectsByClass(imageData, PathDetectionObject.class));
		@ActionMenu("Select...>Select detections...>Select cells")
		@ActionAccelerator("shortcut+alt+c")
		public final Action SELECT_CELLS = qupath.createImageDataAction(imageData -> Commands.selectObjectsByClass(imageData, PathCellObject.class));
		@ActionMenu("Select...>Select detections...>Select tiles")
		public final Action SELECT_TILES = qupath.createImageDataAction(imageData -> Commands.selectObjectsByClass(imageData, PathTileObject.class));

		@ActionMenu("Select...>")
		public final Action SEP_3 = ActionTools.createSeparator();

		@ActionMenu("Select...>Select objects by classification")
		public final Action SELECT_BY_CLASSIFICATION = qupath.createImageDataAction(imageData -> Commands.promptToSelectObjectsByClassification(qupath, imageData));
		
		public final Action SEP_4 = ActionTools.createSeparator();
		
		@ActionMenu("Annotations...>Specify annotation")
		public final Action SPECIFY_ANNOTATION = createAction(new SpecifyAnnotationCommand(qupath));
		@ActionMenu("Annotations...>Create full image annotation")
		@ActionAccelerator("shortcut+shift+a")
		public final Action SELECT_ALL_ANNOTATION = qupath.createImageDataAction(imageData -> Commands.createFullImageAnnotation(qupath.getViewer()));

		@ActionMenu("Annotations...>")
		public final Action SEP_5 = ActionTools.createSeparator();
		
		@ActionMenu("Annotations...>Insert into hierarchy")
		@ActionAccelerator("shortcut+shift+i")
		public final Action INSERT_INTO_HIERARCHY = qupath.createImageDataAction(imageData -> Commands.insertSelectedObjectsInHierarchy(imageData));
		@ActionMenu("Annotations...>Resolve hierarchy")
		@ActionAccelerator("shortcut+shift+r")
		public final Action RESOLVE_HIERARCHY = qupath.createImageDataAction(imageData -> Commands.promptToResolveHierarchy(imageData));
		

		@ActionMenu("Annotations...>")
		public final Action SEP_6 = ActionTools.createSeparator();

		@ActionMenu("Annotations...>Rotate annotation")
		@ActionAccelerator("shortcut+shift+alt+r")
		public final Action RIGID_OBJECT_EDITOR = qupath.createImageDataAction(imageData -> Commands.editSelectedAnnotation(qupath));
		@ActionMenu("Annotations...>Duplicate annotations")
		@ActionAccelerator("shift+d")
		public final Action ANNOTATION_DUPLICATE = qupath.createImageDataAction(imageData -> Commands.duplicateSelectedAnnotations(imageData));
		@ActionMenu("Annotations...>Transfer last annotation")
		@ActionAccelerator("shift+e")
		public final Action TRANSFER_ANNOTATION = qupath.createImageDataAction(imageData -> qupath.viewerManager.applyLastAnnotationToActiveViewer());

		@ActionMenu("Annotations...>")
		public final Action SEP_7 = ActionTools.createSeparator();

		@ActionMenu("Annotations...>Expand annotations")
		public final Action EXPAND_ANNOTATIONS = qupath.createPluginAction("Expand annotations", DilateAnnotationPlugin.class, null);
		@ActionMenu("Annotations...>Split annotations")
		public final Action SPLIT_ANNOTATIONS = qupath.createPluginAction("Split annotations", SplitAnnotationsPlugin.class, null);
		@ActionMenu("Annotations...>Remove fragments")
		public final Action REMOVE_FRAGMENTS = qupath.createPluginAction("Remove annotations", RefineAnnotationsPlugin.class, null);
		@ActionMenu("Annotations...>Fill holes")
		public final Action FILL_HOLES = qupath.createPluginAction("Fill holes", FillAnnotationHolesPlugin.class, null);

		@ActionMenu("Annotations...>")
		public final Action SEP_8 = ActionTools.createSeparator();
		
		@ActionMenu("Annotations...>Make inverse")
		public final Action MAKE_INVERSE = qupath.createImageDataAction(imageData -> Commands.makeInverseAnnotation(imageData));
		@ActionMenu("Annotations...>Merge selected")
		public final Action MERGE_SELECTED = qupath.createImageDataAction(imageData -> Commands.mergeSelectedAnnotations(imageData));
		@ActionMenu("Annotations...>Simplify shape")
		public final Action SIMPLIFY_SHAPE = qupath.createImageDataAction(imageData -> Commands.promptToSimplifySelectedAnnotations(imageData, 1.0));

	}
	
	
	@ActionMenu("TMA")
	public class TMAMenuManager {
		
		@ActionMenu("Add...>Add TMA row before")
		public final Action ADD_ROW_BEFORE = qupath.createImageDataAction(imageData -> TMACommands.promptToAddRowBeforeSelected(imageData));
		@ActionMenu("Add...>Add TMA row after")
		public final Action ADD_ROW_AFTER = qupath.createImageDataAction(imageData -> TMACommands.promptToAddRowAfterSelected(imageData));
		@ActionMenu("Add...>Add TMA column before")
		public final Action ADD_COLUMN_BEFORE = qupath.createImageDataAction(imageData -> TMACommands.promptToAddColumnBeforeSelected(imageData));
		@ActionMenu("Add...>Add TMA column after")
		public final Action ADD_COLUMN_AFTER = qupath.createImageDataAction(imageData -> TMACommands.promptToAddColumnAfterSelected(imageData));
		
		@ActionMenu("Remove...>Remove TMA row")
		public final Action REMOVE_ROW = qupath.createImageDataAction(imageData -> TMACommands.promptToDeleteTMAGridRow(imageData));
		@ActionMenu("Remove...>Remove TMA column")
		public final Action REMOVE_COLUMN = qupath.createImageDataAction(imageData -> TMACommands.promptToDeleteTMAGridColumn(imageData));

		@ActionMenu("Relabel TMA grid")
		public final Action RELABEL = qupath.createImageDataAction(imageData -> TMACommands.promptToRelabelTMAGrid(imageData));
		@ActionMenu("Reset TMA metadata")
		public final Action RESET_METADATA = qupath.createImageDataAction(imageData -> Commands.resetTMAMetadata(imageData));
		@ActionMenu("Delete TMA grid")
		public final Action CLEAR_CORES = qupath.createImageDataAction(imageData -> Commands.promptToDeleteObjects(imageData, TMACoreObject.class));
		@ActionMenu("TMA grid summary view")
		public final Action SUMMARY_GRID = qupath.createImageDataAction(imageData -> TMACommands.showTMAGridView(qupath));

		public final Action SEP_0 = ActionTools.createSeparator();
		
		@ActionMenu("Find convex hull detections (TMA)")
		public final Action CONVEX_HULL = qupath.createPluginAction("Find convex hull detections (TMA)", FindConvexHullDetectionsPlugin.class, null);

	}
	
	@ActionMenu("View")
	public class ViewMenuManager {
		
		@ActionMenu("Show analysis pane")
		@ActionAccelerator("shift+a")
		public final Action SHOW_ANALYSIS_PANEL = actionManager.SHOW_ANALYSIS_PANE;
		
		@ActionMenu("Show command list")
		@ActionAccelerator("shortcut+l")
		public final Action COMMAND_LIST = Commands.createSingleStageAction(() -> CommandFinderTools.createCommandFinderDialog(qupath));

		public final Action SEP_0 = ActionTools.createSeparator();
		public final Action BRIGHTNESS_CONTRAST = actionManager.BRIGHTNESS_CONTRAST;
		public final Action SEP_1 = ActionTools.createSeparator();
		public final Action TOGGLE_SYNCHRONIZE_VIEWERS = actionManager.TOGGLE_SYNCHRONIZE_VIEWERS;
		public final Action MATCH_VIEWER_RESOLUTIONS = actionManager.MATCH_VIEWER_RESOLUTIONS;
		
		@ActionMenu("Mini viewers...>Show channel viewer")
		public final Action CHANNEL_VIEWER = qupath.createViewerAction(viewer -> Commands.showChannelViewer(viewer));
		@ActionMenu("Mini viewers...>Show mini viewer")
		public final Action MINI_VIEWER = qupath.createViewerAction(viewer -> Commands.showMiniViewer(viewer));
		
		public final Action SEP_2 = ActionTools.createSeparator();
		
		@ActionMenu("Zoom...>400%")
		public final Action ZOOM_400 = qupath.createViewerAction(viewer -> Commands.setViewerDownsample(viewer, 0.25));
		@ActionMenu("Zoom...>100%")
		public final Action ZOOM_100 = qupath.createViewerAction(viewer -> Commands.setViewerDownsample(viewer, 1));
		@ActionMenu("Zoom...>10%")
		public final Action ZOOM_10 = qupath.createViewerAction(viewer -> Commands.setViewerDownsample(viewer, 10));
		@ActionMenu("Zoom...>1%")
		public final Action ZOOM_1 = qupath.createViewerAction(viewer -> Commands.setViewerDownsample(viewer, 100));
		
		public final Action SEP_3 = ActionTools.createSeparator();
		@ActionMenu("Zoom...>Zoom in")
		@ActionIcon(PathIcons.ZOOM_IN)
		@ActionAccelerator("ignore shift+plus")
		public final Action ZOOM_IN = createAction(ZoomCommand.createZoomInCommand(qupath.viewerProperty()));
		@ActionMenu("Zoom...>Zoom out")
		@ActionIcon(PathIcons.ZOOM_OUT)
		@ActionAccelerator("-")
		public final Action ZOOM_OUT = createAction(ZoomCommand.createZoomOutCommand(qupath.viewerProperty()));
		@ActionMenu("Zoom...>Zoom to fit")
		public final Action ZOOM_TO_FIT = actionManager.ZOOM_TO_FIT;
				
		@ActionMenu("Rotate image")
		public final Action ROTATE_IMAGE = Commands.createSingleStageAction(() -> Commands.createRotateImageDialog(qupath));

		public final Action SEP_4 = ActionTools.createSeparator();
		
		@ActionMenu("Cell display>")
		public final Action SHOW_CELL_BOUNDARIES = actionManager.SHOW_CELL_BOUNDARIES;
		public final Action SHOW_CELL_NUCLEI = actionManager.SHOW_CELL_NUCLEI;
		public final Action SHOW_CELL_BOUNDARIES_AND_NUCLEI = actionManager.SHOW_CELL_BOUNDARIES_AND_NUCLEI;
		public final Action SHOW_CELL_CENTROIDS = actionManager.SHOW_CELL_CENTROIDS;
		
		public final Action SHOW_ANNOTATIONS = actionManager.SHOW_ANNOTATIONS;
		public final Action FILL_ANNOTATIONS = actionManager.FILL_ANNOTATIONS;
		public final Action SHOW_NAMES = actionManager.SHOW_NAMES;
		public final Action SHOW_TMA_GRID = actionManager.SHOW_TMA_GRID;
		public final Action SHOW_TMA_GRID_LABELS = actionManager.SHOW_TMA_GRID_LABELS;
		public final Action SHOW_DETECTIONS = actionManager.SHOW_DETECTIONS;
		public final Action FILL_DETECTIONS = actionManager.FILL_DETECTIONS;

		@ActionMenu("Show object connections")
		public final Action SHOW_CONNECTIONS = createSelectableCommandAction(qupath.getOverlayOptions().showConnectionsProperty());

		public final Action SHOW_PIXEL_CLASSIFICATION = actionManager.SHOW_PIXEL_CLASSIFICATION;
		
		public final Action SEP_5 = ActionTools.createSeparator();
		
		public final Action SHOW_OVERVIEW = actionManager.SHOW_OVERVIEW;
		public final Action SHOW_LOCATION = actionManager.SHOW_LOCATION;
		public final Action SHOW_SCALEBAR = actionManager.SHOW_SCALEBAR;
		public final Action SHOW_GRID = actionManager.SHOW_GRID;
		public final Action GRID_SPACING = actionManager.GRID_SPACING;
		
		public final Action SEP_6 = ActionTools.createSeparator();
		
		@ActionMenu("Show viewer tracking panel")
		public final Action VIEW_TRACKER = qupath.createImageDataAction(imageData -> Commands.showViewTracker(qupath));
		@ActionMenu("Show slide label")
		public final Action SLIDE_LABEL = createSelectableCommandAction(qupath.slideLabelView.showingProperty());

		public final Action SEP_7 = ActionTools.createSeparator();
		
		@ActionMenu("Show input display")
		public final Action INPUT_DISPLAY = createAction(() -> Commands.showInputDisplay(qupath));

		@ActionMenu("Show memory monitor")
		public final Action MEMORY_MONITORY = Commands.createSingleStageAction(() -> Commands.createMemoryMonitorDialog(qupath));
		
		@ActionMenu("Show log")
		public final Action SHOW_LOG = actionManager.SHOW_LOG;
		
		
		public final Action SEP_8 = ActionTools.createSeparator();

		@ActionMenu("Multi-touch gestures>Turn on all gestures")
		public final Action GESTURES_ALL = createAction(() -> {
			PathPrefs.useScrollGesturesProperty().set(true);
			PathPrefs.useZoomGesturesProperty().set(true);
			PathPrefs.useRotateGesturesProperty().set(true);
		});
		
		@ActionMenu("Multi-touch gestures>Turn off all gestures")
		public final Action GESTURES_NONE = createAction(() -> {
			PathPrefs.useScrollGesturesProperty().set(false);
			PathPrefs.useZoomGesturesProperty().set(false);
			PathPrefs.useRotateGesturesProperty().set(false);
		});
		
		@ActionMenu("Multi-touch gestures>")
		public final Action SEP_9 = ActionTools.createSeparator();
		
		@ActionMenu("Multi-touch gestures>Use scroll gestures")
		public final Action GESTURES_SCROLL = createSelectableCommandAction(PathPrefs.useScrollGesturesProperty());
		@ActionMenu("Multi-touch gestures>Use zoom gestures")
		public final Action GESTURES_ZOOM = createSelectableCommandAction(PathPrefs.useZoomGesturesProperty());
		@ActionMenu("Multi-touch gestures>Use rotate gestures")
		public final Action GESTURES_ROTATE = createSelectableCommandAction(PathPrefs.useRotateGesturesProperty());

		
	}
	
	
	@ActionMenu("Measure")
	public class MeasureMenuManager {
		
		@ActionMenu("Show measurement maps")
		@ActionAccelerator("shortcut+shift+m")
		@ActionDescription("View detection measurements in context using interactive, color-coded maps")
		public final Action MAPS = Commands.createSingleStageAction(() -> Commands.createMeasurementMapDialog(qupath));
		
		@ActionMenu("Show measurement manager")
		@ActionDescription("View and optionally delete detection measurements")
		public final Action MANAGER = qupath.createImageDataAction(imageData -> Commands.showDetectionMeasurementManager(qupath, imageData));
		
		@ActionMenu("")
		public final Action SEP_1 = ActionTools.createSeparator();
		
		public final Action TMA = qupath.getDefaultActions().MEASURE_TMA;
		
		public final Action ANNOTATIONS = qupath.getDefaultActions().MEASURE_ANNOTATIONS;
		
		public final Action DETECTIONS = qupath.getDefaultActions().MEASURE_DETECTIONS;
		
		public final Action SEP_2 = ActionTools.createSeparator();

		@ActionMenu("Export measurements")		
		@ActionDescription("Export summary measurements for multiple images within a project")
		public final Action EXPORT = createAction(new MeasurementExportCommand(qupath));
		
	}
	
	
	@ActionMenu("Help")
	public class HelpMenuManager {

		@ActionMenu("Show setup options")
		public final Action QUPATH_SETUP = createAction(() -> qupath.showSetupDialog());

		@ActionMenu("")
		public final Action SEP_1 = ActionTools.createSeparator();

		@ActionMenu("Documentation (web)")
		public final Action DOCS = createAction(() -> QuPathGUI.launchBrowserWindow(URL_DOCS));
		
		@ActionMenu("Demo videos (web)")
		public final Action DEMOS = createAction(() -> QuPathGUI.launchBrowserWindow(URL_VIDEOS));

		@ActionMenu("Check for updates (web)")
		public final Action UPDATE = createAction(() -> qupath.checkForUpdate(false));

		public final Action SEP_2 = ActionTools.createSeparator();
		
		@ActionMenu("Cite QuPath (web)")
		public final Action CITE = createAction(() -> QuPathGUI.launchBrowserWindow(URL_CITATION));
		
		@ActionMenu("Report bug (web)")
		public final Action BUGS = createAction(() -> QuPathGUI.launchBrowserWindow(URL_BUGS));
		
		@ActionMenu("View user forum (web)")
		public final Action FORUM = createAction(() -> QuPathGUI.launchBrowserWindow(URL_FORUM));
		
		@ActionMenu("View source code (web)")
		public final Action SOURCE = createAction(() -> QuPathGUI.launchBrowserWindow(URL_SOURCE));

		public final Action SEP_3 = ActionTools.createSeparator();

		@ActionMenu("License")
		public final Action LICENSE = Commands.createSingleStageAction(() -> Commands.createLicensesWindow(qupath));
		
		@ActionMenu("System info")
		public final Action INFO = Commands.createSingleStageAction(() -> Commands.createShowSystemInfoDialog(qupath));
		
		@ActionMenu("Installed extensions")
		public final Action EXTENSIONS = createAction(() -> Commands.showInstalledExtensions(qupath));
		
	}

}
