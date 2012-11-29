/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2012 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation;

import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation3D.InputAgent;

public class TimeListInput extends ListInput<DoubleVector> {
	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	protected int[] validCounts; // valid list sizes not including units

	public TimeListInput(String key, String cat, DoubleVector def) {
		super(key, cat, def);
		validCounts = new int[] { };
	}

	public void parse(StringVector input)
	throws InputErrorException {
		DoubleVector temp;

		// If there is more than one value, and the last one is not a number, then assume it is a unit
		if( input.size() > 1 && !Tester.isDouble( input.get( input.size()-1 )) ) {

			// Determine the units
			Unit unit = Input.parseUnits(input.get(input.size()- 1));

			// Determine the default units
			Unit defaultUnit = Input.tryParseEntity(unitString.replaceAll("[()]", "").trim(), Unit.class);
			if( defaultUnit == null ) {
				throw new InputErrorException("Could not determine default units %s", unitString);
			}

			// Determine the conversion factor to the default units
			double conversionFactor = unit.getConversionFactorToUnit(defaultUnit);

			// Parse and convert the values
			Input.assertCountRange(input.subString(0, input.size()-2), minCount, maxCount);
			Input.assertCount(input.subString(0,input.size()-2), validCounts);
			temp = Input.parseTimeVector(input.subString(0,input.size()-2), minValue, maxValue, conversionFactor);
		}
		else {
			// Parse the values
			Input.assertCountRange(input, minCount, maxCount);
			Input.assertCount(input, validCounts);
			temp = Input.parseTimeVector(input, minValue, maxValue);

			if(unitString.length() > 0)
				InputAgent.logWarning("Missing units.  Assuming %s.", unitString);
		}

		value = temp;
		this.updateEditingFlags();
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidCounts(int... list) {
		validCounts = list;
	}
}