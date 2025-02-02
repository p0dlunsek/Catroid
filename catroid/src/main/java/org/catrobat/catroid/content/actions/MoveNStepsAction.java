/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.actions;

import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class MoveNStepsAction extends TemporalAction {

	private Scope scope;
	private Formula steps;

	@Override
	protected void update(float percent) {
		try {
			Double stepsValue = steps == null ? Double.valueOf(0d)
					: steps.interpretDouble(scope);
			double radians = Math.toRadians(scope.getSprite().look.getMotionDirectionInUserInterfaceDimensionUnit());
			scope.getSprite().look.changePositionInInterfaceDimensionUnit((float) (stepsValue * Math.sin(radians)), (float) (stepsValue * Math.cos(radians)));
			scope.getSprite().movedByStepsBrick = true;
		} catch (InterpretationException interpretationException) {
			Log.d(getClass().getSimpleName(), "Formula interpretation for this specific Brick failed.", interpretationException);
		}
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public void setSteps(Formula steps) {
		this.steps = steps;
	}
}
