/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.ui;

import java.util.ArrayList;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.controllers.VideoRecorder;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.TimeInput;

public class VideoRecorderEntity extends Entity {
	@Keyword(desc = "Simulated time between screen captures",
	         example = "This is placeholder example text")
	private final TimeInput captureInterval;

	@Keyword(desc = "How long the simulation waits until starting video recording",
	         example = "This is placeholder example text")
	private final TimeInput captureStartTime;

	@Keyword(desc = "Number of frames to capture",
	         example = "This is placeholder example text")
	private final IntegerInput captureFrames;

	@Keyword(desc = "If the video recorder should save out PNG files of individual frames",
	         example = "This is placeholder example text")
	private final BooleanInput saveImages;

	@Keyword(desc = "If the video recorder should save out an AVI file",
	         example = "This is placeholder example text")
	private final BooleanInput saveVideo;

	@Keyword(desc = "The size of the video/image in pixels",
	         example = "This is placeholder example text")
	private final IntegerListInput captureArea;

	@Keyword(desc = "The background color to use for video recording",
	         example = "This is placeholder example text")
	private final ColourInput videoBGColor;

	@Keyword(desc = "The list of views to draw in the video",
	         example = "This is placeholder example text")
	private final EntityListInput<View> captureViews;

	@Keyword(desc = "The name of the video file to record",
	         example = "This is placeholder example text")
	private final StringInput videoName;

	@Keyword(desc = "Enable video capture",
	         example = "VidRecorder VideoCapture { TRUE }")
	private final BooleanInput videoCapture;

	private boolean hasRunStartup = false;
	private VideoRecorder recorder;
	private int numFramesWritten = 0;
	protected Process captureThread = null;

	{
		captureStartTime = new TimeInput( "CaptureStartTime", "Key Inputs", 0.0 );
		captureStartTime.setValidRange( 0, Double.POSITIVE_INFINITY );
		captureStartTime.setUnits( "h" );
		this.addInput( captureStartTime, true );

		captureInterval = new TimeInput( "CaptureInterval", "Key Inputs", 1.0 );
		captureInterval.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		captureInterval.setUnits( "h" );
		this.addInput( captureInterval, true );

		videoBGColor = new ColourInput("VideoBackgroundColor", "Key Inputs", ColourInput.WHITE);
		this.addInput(videoBGColor, true, "Colour");

		captureFrames = new IntegerInput("CaptureFrames", "Key Inputs", 0);
		captureFrames.setValidRange(0, 30000);
		this.addInput(captureFrames, true);

		saveImages = new BooleanInput("SaveImages", "Key Inputs", false);
		this.addInput(saveImages, true);

		saveVideo = new BooleanInput("SaveVideo", "Key Inputs", false);
		this.addInput(saveVideo, true);

		IntegerVector defArea = new IntegerVector(2);
		defArea.add(1000);
		defArea.add(1000);
		captureArea = new IntegerListInput("CaptureArea", "Key Inputs", defArea);
		captureArea.setValidCount(2);
		captureArea.setValidRange(0, 3000);
		this.addInput(captureArea, true);

		captureViews = new EntityListInput<View>(View.class, "CaptureViews", "Key Inputs", new ArrayList<View>(0));
		this.addInput(captureViews, true);

		videoName = new StringInput("VideoName", "Key Inputs", "");
		this.addInput(videoName, true);

		videoCapture = new BooleanInput("VideoCapture", "Key Inputs", false);
		this.addInput(videoCapture, true);
	}

	@Override
	public void startUp() {
		super.startUp();

		if (videoCapture.getValue())
			this.startProcess("doCaptureNetwork");

		this.hasRunStartup = true;
	}

	@Override
    public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == videoCapture) {
			// Start the capture if we are already running and we set the input
			// to true
			if (hasRunStartup && videoCapture.getValue())
				this.startExternalProcess("doCaptureNetwork");
		}
	}

	public double getCaptureInterval() {
		return captureInterval.getValue();
	}

	/**
	 * Capture JPEG images of the screen at regular simulated intervals
	 */
	public void doCaptureNetwork() {

		// If the capture network is already in progress, then stop the previous network
		if( captureThread != null ) {
			Process.terminate(captureThread);
			captureThread = null;
		}

		scheduleWait(captureStartTime.getValue(), 10);

		if (!RenderManager.isGood()) {
			RenderManager.initialize(false);
		}

		int width = captureArea.getValue().get(0);
		int height = captureArea.getValue().get(1);

		ArrayList<View> views = captureViews.getValue();

		String videoFileName = String.format("%s_%s", InputAgent.getRunName(), videoName.getValue());

		recorder = new VideoRecorder(views, videoFileName, width, height, captureFrames.getDefaultValue(),
		                             saveImages.getValue(), saveVideo.getValue(), videoBGColor.getValue());

		// Otherwise, start capturing
		while (videoCapture.getValue()) {

			RenderManager.inst().blockOnScreenShot(recorder);
			++numFramesWritten;

			if (numFramesWritten == captureFrames.getValue()) {
				break;
			}

			// Wait until the next time to capture a frame
			// (priority 10 is used to allow higher priority events to complete first)
			captureThread = Process.current();
			scheduleWait( this.getCaptureInterval(), 10 );
			captureThread = null;
		}

		recorder.freeResources();
		recorder = null;
	}

}
