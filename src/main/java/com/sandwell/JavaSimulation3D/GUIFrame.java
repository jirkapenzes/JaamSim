/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.sandwell.JavaSimulation3D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.vecmath.Vector3d;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.ui.AboutBox;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.Tester;
import com.sandwell.JavaSimulation.Util;

/**
 * The main window for a Graphical Simulation.  It provides the controls for managing then
 * EventManager (run, pause, ...) and the graphics (zoom, pan, ...)
 */
public class GUIFrame extends JFrame {
	private static GUIFrame instance;

	// global shutdown flag
	static private AtomicBoolean shuttingDown;

	private JMenu fileMenu;
	private JMenu viewMenu;
	private JMenu windowList;
	private JMenu optionMenu;
	private JCheckBoxMenuItem showPosition;
	private JCheckBoxMenuItem alwaysTop;
	private JCheckBoxMenuItem tooltip;
	private JMenuItem showEventViewer;
	private JMenuItem showEventTracker;
	private JMenuItem printInputItem;
	private JLabel clockDisplay;
	private JLabel speedUpDisplay;
	private JLabel remainingDisplay;

	private JToggleButton controlRealTime;
	private JSpinner spinner;

	private JToggleButton controlStartResume;
	private JToggleButton controlStop;
	private JTextField pauseTime;

	private JLabel locatorPos;
	private JLabel locatorLabel;

	JButton toolButtonIsometric;
	JButton toolButtonXYPlane;
	JButton toolButtonUndo;
	JButton toolButtonRedo;

	private int lastValue = -1;
	private JProgressBar progressBar;
	private static Image iconImage;

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.err.println("Unable to change look and feel.");
		}

		try {
			URL file = GUIFrame.class.getResource("/resources/images/icon.png");
			iconImage = Toolkit.getDefaultToolkit().getImage(file);
		}
		catch (Exception e) {
			System.err.println("Unable to load icon file.");
			iconImage = null;
		}

		shuttingDown = new AtomicBoolean(false);
	}

	private GUIFrame() {
		super();

		getContentPane().setLayout( new BorderLayout() );
		setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
		this.addWindowListener(new CloseListener());

		// Initialize the working environment
		initializeMenus();
		initializeMainToolBars();
		initializeStatusBar();

		this.setIconImage(GUIFrame.getWindowIcon());

		//Set window size and make visible
		pack();
		setResizable( false );

		controlStartResume.setSelected( false );
		controlStartResume.setEnabled( false );
		controlStop.setSelected( false );
		controlStop.setEnabled( false );
		clockDisplay.setText( "------.--" );
		setProgress( 0 );
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled( false );
		JPopupMenu.setDefaultLightWeightPopupEnabled( false );
	}

	public static synchronized GUIFrame instance() {
		if (instance == null)
			instance = new GUIFrame();

		return instance;
	}

	private class CloseListener extends WindowAdapter implements ActionListener {
		public void windowClosing(WindowEvent e) {
			GUIFrame.this.close();
		}

		public void actionPerformed( ActionEvent event ) {
			GUIFrame.this.close();
		}
	}

	/**
	 * Perform exit window duties
	 */
	void close() {

		// close warning/error trace file
		InputAgent.closeLogFile();

		// check for unsaved changes
		if (InputAgent.isSessionEdited()) {

			int userOption = JOptionPane.showConfirmDialog( null,
					"Do you want to save the changes?",
					"Confirm Exit Without Saving",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE );

			if (userOption == JOptionPane.YES_OPTION) {
				InputAgent.save(this);
				GUIFrame.shutdown(0);
			} else if (userOption == JOptionPane.NO_OPTION) {
				GUIFrame.shutdown(0);
			}

		} else {
			GUIFrame.shutdown(0);
		}
	}

	/**
	 * Clears the simulation and user interface for a new run
	 */
	public void clear() {

		// Clear the simulation
		DisplayEntity.simulation.clear();

		// Clear the title bar
		setTitle(DisplayEntity.simulation.getModelName());

		// Clear the status bar
		clockDisplay.setText( "------.--" );
		setProgress( 0 );
		speedUpDisplay.setText("------");
		remainingDisplay.setText("------");
		locatorPos.setText( "(-, -, -)" );

		// Read the autoload configuration file
		InputAgent.clear();
		InputAgent.readURL(InputAgent.class.getResource("/resources/inputs/autoload.cfg"));
	}

	public void initializeMenus() {

		// Initialize main menus
		JMenuBar mainMenuBar = new JMenuBar();

		// File menu creation
		fileMenu = new JMenu( "File" );
		fileMenu.setMnemonic( 'F' );
		fileMenu.setEnabled( false );

		JMenuItem newMenuItem = new JMenuItem( "New" );
		newMenuItem.setMnemonic( 'N' );
		newMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				DisplayEntity.simulation.pause();

				// check for unsaved changes
				if (InputAgent.isSessionEdited()) {

					int userOption = JOptionPane.showConfirmDialog( null,
					"A new simulation will overwrite the existing simulation without saving changes.  Do you wish to continue with a new simulation?",
					"Confirm New Simulation",
					JOptionPane.YES_OPTION,
					JOptionPane.WARNING_MESSAGE );

					if(userOption == JOptionPane.NO_OPTION) {
						return;
					}
				}
				clear();
				InputAgent.loadDefault();
				displayWindows(false);
			}
		} );
		fileMenu.add( newMenuItem );

		JMenuItem configMenuItem = new JMenuItem( "Open..." );
		configMenuItem.setMnemonic( 'O' );
		configMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				DisplayEntity.simulation.pause();

				// check for unsaved changes
				if (InputAgent.isSessionEdited()) {

					int userOption = JOptionPane.showConfirmDialog( null,
							"Opening a simulation will overwrite the existing simulation without saving changes.  Do you wish to continue opening a simulation?",
							"Confirm Open",
							JOptionPane.YES_OPTION,
							JOptionPane.WARNING_MESSAGE );

					if (userOption == JOptionPane.NO_OPTION) {
						return;
					}
				}
				InputAgent.load(GUIFrame.this);
			}
		} );
		fileMenu.add( configMenuItem );

		JMenuItem saveConfigurationMenuItem = new JMenuItem( "Save" );
		saveConfigurationMenuItem.setMnemonic( 'S' );
		saveConfigurationMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				InputAgent.save(GUIFrame.this);
			}
		} );
		fileMenu.add( saveConfigurationMenuItem );

		JMenuItem saveConfigurationAsMenuItem = new JMenuItem( "Save As..." );
		saveConfigurationAsMenuItem.setMnemonic( 'V' );
		saveConfigurationAsMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				InputAgent.saveAs(GUIFrame.this);

			}
		} );
		fileMenu.add( saveConfigurationAsMenuItem );

		printInputItem = new JMenuItem( "Print Input Report" );
		printInputItem.setMnemonic( 'I' );
		printInputItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				InputAgent.printInputFileKeywords();
			}
		} );
		fileMenu.add( printInputItem );

		JMenuItem exitMenuItem = new JMenuItem( "Exit" );
		exitMenuItem.setMnemonic( 'x' );
		exitMenuItem.addActionListener(new CloseListener());
		fileMenu.add( exitMenuItem );

		mainMenuBar.add( fileMenu );
		// End File menu creation

		// View menu creation
		viewMenu = new JMenu( "Tools" );
		viewMenu.setMnemonic( 'T' );

		JMenuItem objectPalletMenuItem = new JMenuItem( "Model Builder" );
		objectPalletMenuItem.setMnemonic( 'O' );
		objectPalletMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				EntityPallet.getInstance().setVisible(true);
			}
		} );
		viewMenu.add( objectPalletMenuItem );

		JMenuItem objectSelectorMenuItem = new JMenuItem( "Object Selector" );
		objectSelectorMenuItem.setMnemonic( 'S' );
		objectSelectorMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				ObjectSelector.getInstance().setVisible(true);
			}
		} );
		viewMenu.add( objectSelectorMenuItem );

		JMenuItem inputEditorMenuItem = new JMenuItem( "Input Editor" );
		inputEditorMenuItem.setMnemonic( 'I' );
		inputEditorMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				EditBox.getInstance().setVisible(true);
				if(ObjectSelector.getInstance().isVisible())
					FrameBox.setSelectedEntity(ObjectSelector.currentEntity);
			}
		} );
		viewMenu.add( inputEditorMenuItem );

		JMenuItem propertiesMenuItem = new JMenuItem( "Property Viewer" );
		propertiesMenuItem.setMnemonic( 'P' );
		propertiesMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				PropertyBox.getInstance().setVisible(true);
				if(ObjectSelector.getInstance().isVisible())
					FrameBox.setSelectedEntity(ObjectSelector.currentEntity);
			}
		} );
		viewMenu.add( propertiesMenuItem );

		JMenuItem outputsMenuItem = new JMenuItem( "Output Viewer" );
		outputsMenuItem.setMnemonic( 'U' );
		outputsMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				InfoBox.getInstance().setVisible(true);
				if(ObjectSelector.getInstance().isVisible())
					FrameBox.setSelectedEntity(ObjectSelector.currentEntity);
			}
		} );
		viewMenu.add( outputsMenuItem );

//		JMenuItem eventListMenuItem = new JMenuItem( "Event Viewer" );
//		eventListMenuItem.setMnemonic( 'E' );
//		eventListMenuItem.addActionListener( new ActionListener() {
//
//			public void actionPerformed( ActionEvent e ) {
//				new EventViewer( DisplayEntity.simulation.getEventManager() );
//			}
//		} );
//		viewMenu.add( eventListMenuItem );

		mainMenuBar.add( viewMenu );
		// End File menu creation

		// Window menu creation
		JMenu windowMenu = new NewRenderWindowMenu("Views");
		windowMenu.setMnemonic( 'V' );

		// Initialize list of windows
		windowList = new WindowMenu("Select Window");
		windowList.setMnemonic( 'S' );
		//windowMenu.add( windowList );

		mainMenuBar.add( windowMenu );
		// End window menu creation

		optionMenu = new JMenu( "Options" );
		optionMenu.setMnemonic( 'O' );
		mainMenuBar.add( optionMenu );

		showPosition = new JCheckBoxMenuItem( "Show Position", true );
		showPosition.setMnemonic( 'P' );
		optionMenu.add( showPosition );

		alwaysTop = new JCheckBoxMenuItem( "Always on top", false );
		alwaysTop.setMnemonic( 'A' );
		optionMenu.add( alwaysTop );
		alwaysTop.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if( GUIFrame.this.isAlwaysOnTop() ) {
					GUIFrame.this.setAlwaysOnTop( false );
				}
				else {
					GUIFrame.this.setAlwaysOnTop( true );
				}
			}
		} );

		tooltip = new JCheckBoxMenuItem( "Tooltip", true );
		tooltip.setMnemonic( 'L' );
		optionMenu.add( tooltip );
		tooltip.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				// TODO Needs to be implemented for the new Renderer
			}
		} );

		showPosition.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent e ) {
				setShowPositionXY();
			}
		} );

		// Help menu creation
		JMenu helpMenu = new JMenu( "Help" );
		helpMenu.setMnemonic( 'H' );

		JMenuItem aboutMenu = new JMenuItem( "About" );
		aboutMenu.setMnemonic( 'A' );
		aboutMenu.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				AboutBox.instance().setVisible(true);
			}
		} );
		helpMenu.add( aboutMenu );

		mainMenuBar.add( helpMenu );
		// End help menu creation

		// Add main menu to the window
		setJMenuBar( mainMenuBar );
	}

	public void initializeMainToolBars() {


		// Insets used in setting the toolbar components
		Insets noMargin = new Insets( 0, 0, 0, 0 );
		Insets smallMargin = new Insets( 1, 1, 1, 1 );

		// Initilize the main toolbar
		JToolBar mainToolBar = new JToolBar();
		mainToolBar.setMargin( smallMargin );
		mainToolBar.setFloatable(false);

		// Create Run Label and run control buttons
		controlStartResume = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/run.png")));
		controlStartResume.setSelectedIcon(
				new ImageIcon(GUIFrame.class.getResource("/resources/images/pause.png")));
		controlStartResume.setToolTipText( "Run" );
		controlStartResume.setMargin( noMargin );
		controlStartResume.setEnabled( false );
		controlStartResume.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				JToggleButton startResume = (JToggleButton)event.getSource();
				if(startResume.isSelected()) {
					GUIFrame.this.startSimulation();
				}
				else {
					GUIFrame.this.pauseSimulation();
				}
				GUIFrame.this.updateForSimulationState();
			}
		} );

		controlStop = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/stop.png")));
		controlStop.setToolTipText( "Stop" );
		controlStop.setMargin( noMargin );
		controlStop.setEnabled( false );
		controlStop.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				if( Simulation.getSimulationState() == Simulation.SIM_STATE_RUNNING ) {
					GUIFrame.this.pauseSimulation();
				}
				int userOption = JOptionPane.showConfirmDialog( null,
						"WARNING: If you stop the run, it cannot continue from the present time and can only be re-started from time 0.  Do you really want to stop?",
						"Confirm Stop",
						JOptionPane.YES_OPTION,
						JOptionPane.WARNING_MESSAGE );

				// stop only if yes
				if (userOption == JOptionPane.YES_OPTION) {
					GUIFrame.this.stopSimulation();
				}
			}
		} );

		// Separators have 5 pixels before and after and the preferred height of controlStartResume button
		Dimension separatorDim = new Dimension(11, controlStartResume.getPreferredSize().height);

		// dimension for 5 pixels gaps
		Dimension gapDim = new Dimension(5, separatorDim.height);

		mainToolBar.add( controlStartResume );
		mainToolBar.add(Box.createRigidArea(gapDim));
		mainToolBar.add( controlStop );

		mainToolBar.add(Box.createRigidArea(gapDim));
		JLabel pauseAt = new JLabel( "Pause at:" );
		mainToolBar.add(pauseAt);
		mainToolBar.add(Box.createRigidArea(gapDim));
		pauseTime = new JTextField("2000-00-00") {
			protected void processFocusEvent( FocusEvent fe ) {
				if ( fe.getID() == FocusEvent.FOCUS_GAINED ) {
					if(getText().equals(String.format("%c", 8734 ))) {
						this.setHorizontalAlignment(JTextField.RIGHT);
						this.setText("");
					}

					// select entire text string
					selectAll();
				}
				else if (fe.getID() == FocusEvent.FOCUS_LOST) {
					if(getText().isEmpty()) {
						this.setText(String.format("%c", 8734 ));
						this.setHorizontalAlignment(JTextField.CENTER);
					}
				}
				super.processFocusEvent( fe );
			}
		};

		pauseTime.setHorizontalAlignment(JTextField.RIGHT);

		// avoid height increase for pauseTime
		pauseTime.setMaximumSize(pauseTime.getPreferredSize());

		// avoid stretching for puaseTime when focusing in and out
		pauseTime.setPreferredSize(pauseTime.getPreferredSize());

		mainToolBar.add(pauseTime);

		pauseTime.setText(String.format("%c", 8734 )); //default-Infinity sign

		SpinnerNumberModel numberModel =
				new SpinnerModel(Simulation.DEFAULT_REAL_TIME_FACTOR,
				   Simulation.MIN_REAL_TIME_FACTOR, Simulation.MAX_REAL_TIME_FACTOR, 1);
		spinner = new JSpinner(numberModel);

		// make sure spinner TextField is no wider than 9 digits
		int diff =
			((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().getPreferredSize().width -
			Util.getPixelWidthOfString_ForFont("9", spinner.getFont()) * 9;
		Dimension dim = spinner.getPreferredSize();
		dim.width -= diff;
		spinner.setMaximumSize(dim);

		spinner.addChangeListener(new SpeedFactorListener());

		mainToolBar.addSeparator(separatorDim);
		controlRealTime = new JToggleButton( "Real Time" );
		controlRealTime.setToolTipText( "Toggle Real Time" );
		controlRealTime.setMargin( smallMargin );
		controlRealTime.addActionListener(new RealTimeActionListener());

		mainToolBar.add( controlRealTime );
		mainToolBar.add(Box.createRigidArea(gapDim));
		mainToolBar.add( spinner );
		mainToolBar.addSeparator(separatorDim);
		// End creation of real-time label and menu

		// Create view control label and controls
		JLabel viewLabel = new JLabel( "   View Control:   " );
		mainToolBar.add( viewLabel );

		// add a button to show isometric view in windows
		toolButtonIsometric = new JButton( "Isometric" );
		toolButtonIsometric.setToolTipText( "Set Isometric View" );
		toolButtonIsometric.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				if (RenderManager.isGood())
					RenderManager.inst().setIsometricView();
			}
		} );
		mainToolBar.add( toolButtonIsometric );

		// add a button to show xy-plane view in windows
		toolButtonXYPlane = new JButton( "XY-Plane" );
		toolButtonXYPlane.setToolTipText( "Set XY-Plane View" );
		toolButtonXYPlane.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				if (RenderManager.isGood())
					RenderManager.inst().setXYPlaneView();
			}
		} );
		mainToolBar.add( toolButtonXYPlane );

		mainToolBar.addSeparator(separatorDim);

		// add a button to undo the last step ( viewer and window )
		toolButtonUndo = new JButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/previous.png")));
		toolButtonUndo.setToolTipText( "Previous view" );
		toolButtonUndo.setEnabled( false );
		toolButtonUndo.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				// Not implemented
			}
		} );
		//mainToolBar.add( toolButtonUndo );

		// add a button to redo the last step ( viewer and window )
		toolButtonRedo = new JButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/next.png")));
		toolButtonRedo.setToolTipText( "Next view" );
		toolButtonRedo.setEnabled( false );
		toolButtonRedo.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				// Not implemented
			}
		} );
		//mainToolBar.add( toolButtonRedo );
		//mainToolBar.addSeparator(separatorDim);

		// End creation of view control label and buttons

		// Add toolbar to the window
		getContentPane().add( mainToolBar, BorderLayout.NORTH );
	}

	private static class WindowMenu extends JMenu implements MenuListener {

		WindowMenu(String text) {
			super(text);
			this.addMenuListener(this);
		}

		@Override
		public void menuCanceled(MenuEvent arg0) {}

		@Override
		public void menuDeselected(MenuEvent arg0) {
			this.removeAll();
		}

		@Override
		public void menuSelected(MenuEvent arg0) {
			if (!RenderManager.isGood()) { return; }

			ArrayList<Integer> windowIDs = RenderManager.inst().getOpenWindowIDs();
			for (int id : windowIDs) {
				String windowName = RenderManager.inst().getWindowName(id);
				this.add(new WindowSelector(id, windowName));
			}
		}
	}

	private static class WindowSelector extends JMenuItem implements ActionListener {
		private final int windowID;

		WindowSelector(int windowID, String windowName) {
			this.windowID = windowID;
			this.setText(windowName);
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!RenderManager.isGood()) { return; }

			RenderManager.inst().focusWindow(windowID);
		}
	}

	private static class NewRenderWindowMenu extends JMenu implements MenuListener {

		NewRenderWindowMenu(String text) {
			super(text);
			this.addMenuListener(this);
		}

		@Override
		public void menuSelected(MenuEvent e) {
			if (!RenderManager.isGood()) { return; }

			for (View view : View.getAll()) {
				this.add(new NewRenderWindowLauncher(view));
			}
			this.addSeparator();
			this.add(new ViewDefiner());
		}

		@Override
		public void menuCanceled(MenuEvent arg0) {
		}

		@Override
		public void menuDeselected(MenuEvent arg0) {
			this.removeAll();
		}
	}
	private static class NewRenderWindowLauncher extends JMenuItem implements ActionListener {
		private final View view;

		NewRenderWindowLauncher(View v) {
			view = v;
			this.setText(view.getName());
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!RenderManager.isGood()) { return; }

			RenderManager.inst().createWindow(view);
		}
	}

	private static class ViewDefiner extends JMenuItem implements ActionListener {
		ViewDefiner() {} {
			this.setText("Define new View");
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!RenderManager.isGood()) { return; }

			View tmp = InputAgent.defineEntityWithUniqueName(View.class, "View", true);
			RenderManager.inst().createWindow(tmp);
			FrameBox.setSelectedEntity(tmp);
		}
	}

	public void initializeStatusBar() {


		// Create the status bar
		JPanel statusBar = new JPanel();
		statusBar.setBorder( BorderFactory.createLineBorder( Color.darkGray ) );
		statusBar.setLayout( new FlowLayout( FlowLayout.LEFT, 10, 5 ) );

		// Create the display clock and label
		JLabel clockLabel = new JLabel( "Simulation Time (hrs):" );

		clockDisplay = new JLabel( "------.--", JLabel.RIGHT );
		clockDisplay.setPreferredSize( new Dimension( 55, 16 ) );
		clockDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );

		statusBar.add( clockLabel );
		statusBar.add( clockDisplay );
		//statusBar.addSeparator();

		// Create the progress bar
		progressBar = new JProgressBar( 0, 100 );
		progressBar.setValue( 0 );
		progressBar.setStringPainted( true );

		// Add the progress bar to the status bar
		statusBar.add( progressBar );

		// Create a speed-up factor display
		JLabel speedUpLabel = new JLabel( "Speed Up:" );
		speedUpDisplay = new JLabel( "------", JLabel.RIGHT );
		speedUpDisplay.setPreferredSize( new Dimension( 60, 16 ) );
		speedUpDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );

		statusBar.add( speedUpLabel );
		statusBar.add( speedUpDisplay );

		// Create a remaining run time display
		JLabel remainingLabel = new JLabel( "Time Remaining (mins):" );
		remainingDisplay = new JLabel( "------", JLabel.RIGHT );
		remainingDisplay.setPreferredSize( new Dimension( 40, 16 ) );
		remainingDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		statusBar.add( remainingLabel );
		statusBar.add( remainingDisplay );

		locatorPos = new JLabel( "(-, -, -)" );
		locatorPos.setPreferredSize( new Dimension( 140, 16 ) );
		locatorPos.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		locatorLabel = new JLabel( "Pos:     " );
		statusBar.add( locatorLabel );
		statusBar.add( locatorPos );

		// Add the status bar to the window
		getContentPane().add( statusBar, BorderLayout.SOUTH );
	}

	public void setClock( double clockContents ) {
		clockDisplay.setText(String.format("%.2f", clockContents));
	}

	public void setProgress( int val ) {
		if (lastValue == val)
			return;

		progressBar.setValue( val );
		progressBar.repaint(25);
		lastValue = val;

		if (Simulation.getSimulationState() >= Simulation.SIM_STATE_CONFIGURED) {
			String title = String.format("%d%% %s - %s", val, DisplayEntity.simulation.getModelName(), InputAgent.getRunName());
			setTitle(title);
		}
	}

	public void setProgressText( String txt ) {
		progressBar.setString( txt );
	}

	public void setSpeedUp( double val ) {
		speedUpDisplay.setText(String.format("%,.0f", val));
	}

	public void setRemaining( double val ) {
		remainingDisplay.setText(String.format("%.1f", val));
	}

	private void startSimulation() {

		// pause at a time
		double runToTime = Double.POSITIVE_INFINITY;
		if(! pauseTime.getText().equalsIgnoreCase(String.format("%c", 8734))) {

			try {
				if (Tester.isDate(pauseTime.getText())) {
					String[] str = pauseTime.getText().split("-");

					if (str.length < 3) {
						throw new NumberFormatException
						("Date string must be of form yyyy-mm-dd");
					}
					int year = Integer.valueOf(str[0]).intValue();
					int month = Integer.valueOf(str[1]).intValue();
					int day = Integer.valueOf(str[2]).intValue();
					double time =
							Clock.calcTimeForYear_Month_Day_Hour(year, month, day, 0.0);

					int startingYear = Clock.getStartingYear();
					int startingMonth = Clock.getStartingMonth();
					int startingDay = Clock.getStartingDay();
					double startingTime = Clock.calcTimeForYear_Month_Day_Hour(
							startingYear, startingMonth, startingDay, 0.0);

					runToTime = time - startingTime;
				} else {
					runToTime = Double.parseDouble(pauseTime.getText());
				}
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(this,	String.format(
						"Invalid time \n %s", pauseTime.getText()),
						"error", JOptionPane.ERROR_MESSAGE);

				// it is not running any more
				controlStartResume.setSelected(!
						controlStartResume.isSelected() );
				return;
			}
		}

		if(runToTime <= DisplayEntity.simulation.getCurrentTime() ) {
			return;
		}

		if( Simulation.getSimulationState() <= Simulation.SIM_STATE_CONFIGURED ) {
			if (InputAgent.isSessionEdited()) {
				InputAgent.saveAs(this);
			}
			DisplayEntity.simulation.start();
		}
		else if( Simulation.getSimulationState() == Simulation.SIM_STATE_PAUSED ) {

			// it is not a run to time
			if(Double.isInfinite( runToTime ) ) {
				DisplayEntity.simulation.resume();
				return;
			}
		}
		else if( Simulation.getSimulationState() == Simulation.SIM_STATE_STOPPED ) {
			DisplayEntity.simulation.restart();
		}
		else
			throw new ErrorException( "Invalid Simulation State for Start/Resume" );

		if( ! Double.isInfinite(runToTime) ) {
			DisplayEntity.simulation.getEventManager().runToTime(runToTime);
		}
	}

	private void pauseSimulation() {
		if( Simulation.getSimulationState() == Simulation.SIM_STATE_RUNNING )
			DisplayEntity.simulation.pause();
		else
			throw new ErrorException( "Invalid Simulation State for pause" );
	}

	private void stopSimulation() {
		if( Simulation.getSimulationState() == Simulation.SIM_STATE_RUNNING ||
			Simulation.getSimulationState() == Simulation.SIM_STATE_PAUSED )
			DisplayEntity.simulation.stop();
		else
			throw new ErrorException( "Invalid Simulation State for stop" );
	}

	public void updateForSimulationState() {
		switch( Simulation.getSimulationState() ) {
			case Simulation.SIM_STATE_LOADED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					viewMenu.getItem(i).setEnabled(true);
				}

				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText( "Run" );
				controlStop.setEnabled( false );
				controlStop.setSelected( false );
				toolButtonIsometric.setEnabled( true );
				toolButtonXYPlane.setEnabled( true );
				progressBar.setEnabled( false );
				if( showEventViewer != null )
					showEventViewer.setEnabled( false );
				if( showEventTracker != null )
					showEventTracker.setEnabled( false );
				break;

			case Simulation.SIM_STATE_UNCONFIGURED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					viewMenu.getItem(i).setEnabled(true);
				}

				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				controlStartResume.setEnabled( false );
				controlStartResume.setSelected( false );
				controlStop.setSelected( false );
				controlStop.setEnabled( false );
				toolButtonIsometric.setEnabled( true );
				toolButtonXYPlane.setEnabled( true );
				progressBar.setEnabled( false );
				showPosition.setState( true );
				setShowPositionXY();
				if( showEventViewer != null )
					showEventViewer.setEnabled( false );
				if( showEventTracker != null )
					showEventTracker.setEnabled( false );
				break;

			case Simulation.SIM_STATE_CONFIGURED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					viewMenu.getItem(i).setEnabled(true);
				}

				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( true );
				remainingDisplay.setEnabled( true );
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText( "Run" );
				controlStop.setSelected( false );
				controlStop.setEnabled( false );
				toolButtonIsometric.setEnabled( true );
				toolButtonXYPlane.setEnabled( true );
				progressBar.setEnabled( true );
				if( showEventViewer != null )
					showEventViewer.setEnabled( true );
				if( showEventTracker != null )
					showEventTracker.setEnabled( true );
				break;

			case Simulation.SIM_STATE_RUNNING:
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( true );
				controlStartResume.setToolTipText( "Pause" );
				controlStop.setEnabled( true );
				controlStop.setSelected( false );

				if( showEventViewer != null )
					showEventViewer.setEnabled( true );
				if( showEventTracker != null )
					showEventTracker.setEnabled( true );
				break;

			case Simulation.SIM_STATE_PAUSED:
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText( "Run" );
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				break;
			case Simulation.SIM_STATE_STOPPED:
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText( "Run" );
				controlStop.setEnabled( false );
				controlStop.setSelected( false );
				break;

			default:
				throw new ErrorException( "Unrecognized Graphics State" );
		}
		fileMenu.setEnabled( true );
	}

	/**
	 * updates RealTime button and Spinner
	 */
	public void updateForRealTime() {
		controlRealTime.setSelected( DisplayEntity.simulation.getRealTimeExecution() );
		spinner.setValue(DisplayEntity.simulation.getRealTimeFactor());

		//speedFactor.setText("" + simulation.getRealTimeFactor());
	}

	public static Image getWindowIcon() {
		return iconImage;
	}

	public void copyLocationToClipBoard(Vector3d pos) {
		String data = String.format("(%.3f, %.3f, %.3f)", pos.x, pos.y, pos.z);
		StringSelection stringSelection = new StringSelection(data);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents( stringSelection, null );
	}

	public void showLocatorPosition(Vector3d pos, Region region) {
		// null indicates nothing to display
		if( pos == null ) {
			locatorPos.setText( "(-, -, -)" );
		}
		else {
			if( showPosition.getState() ) {
				locatorPos.setText(String.format("(%.3f, %.3f, %.3f)",
					pos.x, pos.y, pos.z));
			}
		}
	}

	public void setShowPositionXY() {
		boolean show = showPosition.getState();
		showPosition.setState( show );
		locatorLabel.setVisible( show );
		locatorPos.setVisible( show );
		locatorLabel.setText( "Pos: " );
		locatorPos.setText( "(-, -, -)" );
	}

	public static void displayWindows(boolean viewOnly) {
		for (View v : View.getAll()) {
			if (v.showOnStart())
				RenderManager.inst().createWindow(v);
		}

		if (viewOnly)
			return;
		EntityPallet.getInstance().setVisible(true);
		EditBox.getInstance().setVisible(true);
		ObjectSelector.getInstance().setVisible(true);
		InfoBox.getInstance().setVisible(true);
	}

	// ******************************************************************************************************
	// MAIN
	// ******************************************************************************************************

	public static void main( String args[] ) {
		// Process the input arguments and filter out directives
		ArrayList<String> configFiles = new ArrayList<String>(args.length);
		boolean batch = false;
		boolean minimize = false;
		boolean quiet = false;

		for (String each : args) {
			// Batch mode
			if (each.equalsIgnoreCase("-b") ||
			    each.equalsIgnoreCase("-batch")) {
				batch = true;
				continue;
			}
			// z-buffer offset
			if (each.equalsIgnoreCase("-z") ||
			    each.equalsIgnoreCase("-zbuffer")) {
				// Parse the option, but do nothing
				continue;
			}
			// Minimize model window
			if (each.equalsIgnoreCase("-m") ||
			    each.equalsIgnoreCase("-minimize")) {
				minimize = true;
				continue;
			}
			// Do not open default windows
			if (each.equalsIgnoreCase("-q") ||
					each.equalsIgnoreCase("-quiet")) {
				quiet = true;
				continue;
			}
			// Not a program directive, add to list of config files
			configFiles.add(each);
		}

		// If not running in batch mode, create the splash screen
		JWindow splashScreen = null;
		if (!batch) {
			URL splashImage = GUIFrame.class.getResource("/resources/images/splashscreen.png");
			ImageIcon imageIcon = new ImageIcon(splashImage);
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			int splashX = (screen.width - imageIcon.getIconWidth()) / 2;
			int splashY = (screen.height - imageIcon.getIconHeight()) / 2;

			// Set the window's bounds, centering the window
			splashScreen = new JWindow();
			splashScreen.setAlwaysOnTop(true);
			splashScreen.setBounds(splashX, splashY, imageIcon.getIconWidth(), imageIcon.getIconHeight());

			// Build the splash screen
			splashScreen.getContentPane().add(new JLabel(imageIcon));

			// Display it
			splashScreen.setVisible(true);

			// Begin initializing the rendering system
			RenderManager.initialize();
		}

		FileEntity.setRootDirectory(System.getProperty("user.dir"));

		// create a graphic simulation
		System.out.println( "Loading Simulation Environment ... " );
		System.out.flush();

		GUIFrame gui = GUIFrame.instance();
		gui.updateForSimulationState();

		GraphicSimulation gsim = new GraphicSimulation();
		gui.setTitle(gsim.getModelName());
		gsim.setInputName("Simulation");

		System.out.println( "Simulation Environment Loaded" );

		if (batch)
			InputAgent.setBatch(true);

		if (minimize)
			gui.setExtendedState(JFrame.ICONIFIED);

		gui.setVisible(true);

		InputAgent.readURL(InputAgent.class.getResource("/resources/inputs/autoload.cfg"));

		if( configFiles.size() == 0 ) {
			InputAgent.loadDefault();
		}

		// Process any config files passed on command line
		for (int i = 0; i < configFiles.size(); i++) {
			// Consume regular configuration files
			gsim.configure(gui, configFiles.get(i));
			continue;
		}

		if(!quiet && !batch) {
			displayWindows(false);
		}

		// Hide the splash screen
		if (splashScreen != null) {
			try { Thread.sleep(1000); } catch (InterruptedException e) {}
			splashScreen.dispose();
			splashScreen = null;
			gui.toFront();
		}

		// Start the model if in batch mode
		if (batch) {
			if (InputAgent.numErrors() > 0)
				GUIFrame.shutdown(0);
			gsim.start();
		}
	}

	public static class SpeedFactorListener implements ChangeListener {

		public void stateChanged( ChangeEvent e ) {
			InputAgent.processEntity_Keyword_Value(DisplayEntity.simulation,
			   DisplayEntity.simulation.getInput("RealTimeFactor"),
			   String.format( "%d", ((JSpinner)e.getSource()).getValue()) );

			FrameBox.valueUpdate();
		}
	}

	/*
	 * this class is created so the next value will be value * 2 and the
	 * previous value will be value / 2
	 */
	public static class SpinnerModel extends SpinnerNumberModel {
		private int value;
		public SpinnerModel( int val, int min, int max, int stepSize) {
			super(val, min, max, stepSize);
		}
		public Object getPreviousValue() {
			value = this.getNumber().intValue() / 2;

			// Avoid going beyond limit
			if(this.getMinimum().compareTo(value) > 0 ) {
				return this.getMinimum();
			}
			return value;
		}

		public Object getNextValue() {
			value = this.getNumber().intValue() * 2;

			// Avoid going beyond limit
			if(this.getMaximum().compareTo(value) < 0 ) {
				return this.getMaximum();
			}
			return value;
		}
	}

	public static class RealTimeActionListener implements ActionListener {
		public void actionPerformed( ActionEvent event ) {
			Input<?> in = DisplayEntity.simulation.getInput("RealTime");
			String val = "FALSE";
			if(((JToggleButton)event.getSource()).isSelected())
				val = "TRUE";

			InputAgent.processEntity_Keyword_Value(DisplayEntity.simulation, in, val);

			FrameBox.valueUpdate();
		}
	}

	/**
	 * A miscelaneous utility to populate a JPopupMenu with a list of DisplayEntity menu items (for the right click menu)
	 * @param menu
	 * @param menuItems
	 */
	public static void populateMenu(JPopupMenu menu, ArrayList<ObjectSelector.DEMenuItem> menuItems) {

		for (final ObjectSelector.DEMenuItem item : menuItems) {
			JMenuItem mi = new JMenuItem(item.menuName);
			mi.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					item.action();
				}
			});
			menu.add(mi);
		}
	}

	public static boolean getShuttingDownFlag() {
		return shuttingDown.get();
	}

	public static void shutdown(int errorCode) {

		shuttingDown.set(true);
		if (RenderManager.isGood()) {
			RenderManager.inst().shutdown();
		}

		System.exit(errorCode);
	}

}