/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2018 The Catrobat Team
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
package org.catrobat.catroid.test.io;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.DefaultProjectHandler;
import org.catrobat.catroid.content.LegoNXTSetting;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Setting;
import org.catrobat.catroid.content.SingleSprite;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.BrickBaseType;
import org.catrobat.catroid.content.bricks.ComeToFrontBrick;
import org.catrobat.catroid.content.bricks.FormulaBrick;
import org.catrobat.catroid.content.bricks.HideBrick;
import org.catrobat.catroid.content.bricks.LegoNxtMotorMoveBrick;
import org.catrobat.catroid.content.bricks.PlaceAtBrick;
import org.catrobat.catroid.content.bricks.SetSizeToBrick;
import org.catrobat.catroid.content.bricks.ShowBrick;
import org.catrobat.catroid.content.bricks.SpeakBrick;
import org.catrobat.catroid.devices.mindstorms.nxt.sensors.NXTSensor;
import org.catrobat.catroid.drone.ardrone.DroneBrickFactory;
import org.catrobat.catroid.exceptions.CompatibilityProjectException;
import org.catrobat.catroid.exceptions.LoadingProjectException;
import org.catrobat.catroid.exceptions.OutdatedVersionProjectException;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.FormulaElement;
import org.catrobat.catroid.formulaeditor.Sensors;
import org.catrobat.catroid.io.StorageOperations;
import org.catrobat.catroid.io.XstreamSerializer;
import org.catrobat.catroid.test.utils.TestUtils;
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.catrobat.catroid.common.Constants.CODE_XML_FILE_NAME;
import static org.catrobat.catroid.common.Constants.PERMISSIONS_FILE_NAME;
import static org.catrobat.catroid.common.Constants.TMP_CODE_XML_FILE_NAME;
import static org.catrobat.catroid.utils.PathBuilder.buildProjectPath;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class XstreamSerializerTest {

	private final XstreamSerializer storageHandler;
	private final String projectName = "testProject";

	private Project currentProjectBuffer;

	private static final int SET_SPEED_INITIALLY = -70;
	private static final int DEFAULT_MOVE_TIME_IN_MILLISECONDS = 2000;
	private static final int DEFAULT_MOVE_POWER_IN_PERCENT = 20;

	public XstreamSerializerTest() {
		storageHandler = XstreamSerializer.getInstance();
	}

	@Before
	public void setUp() throws Exception {
		TestUtils.deleteProjects(projectName);
		DefaultProjectHandler.createAndSaveDefaultProject(InstrumentationRegistry.getTargetContext());

		currentProjectBuffer = ProjectManager.getInstance().getCurrentProject();
	}

	@After
	public void tearDown() throws Exception {
		ProjectManager.getInstance().setProject(currentProjectBuffer);

		TestUtils.deleteProjects(projectName);
	}

	@Test
	public void testSerializeProject() throws Exception {
		final int xPosition = 457;
		final int yPosition = 598;
		final float size = 0.8f;

		Project project = new Project(InstrumentationRegistry.getTargetContext(), projectName);
		Sprite firstSprite = new SingleSprite("first");
		Sprite secondSprite = new SingleSprite("second");
		Sprite thirdSprite = new SingleSprite("third");
		Sprite fourthSprite = new SingleSprite("fourth");
		Script testScript = new StartScript();
		Script otherScript = new StartScript();
		HideBrick hideBrick = new HideBrick();
		ShowBrick showBrick = new ShowBrick();
		SetSizeToBrick setSizeToBrick = new SetSizeToBrick(size);
		ComeToFrontBrick comeToFrontBrick = new ComeToFrontBrick();
		PlaceAtBrick placeAtBrick = new PlaceAtBrick(xPosition, yPosition);

		testScript.addBrick(hideBrick);
		testScript.addBrick(showBrick);
		testScript.addBrick(setSizeToBrick);
		testScript.addBrick(comeToFrontBrick);

		otherScript.addBrick(placeAtBrick);

		firstSprite.addScript(testScript);
		secondSprite.addScript(otherScript);

		project.getDefaultScene().addSprite(firstSprite);
		project.getDefaultScene().addSprite(secondSprite);
		project.getDefaultScene().addSprite(thirdSprite);
		project.getDefaultScene().addSprite(fourthSprite);

		storageHandler.saveProject(project);

		Project loadedProject = storageHandler.loadProject(projectName, InstrumentationRegistry.getContext());

		Scene preScene = project.getDefaultScene();
		Scene postScene = loadedProject.getDefaultScene();

		ArrayList<Sprite> preSpriteList = (ArrayList<Sprite>) project.getDefaultScene().getSpriteList();
		ArrayList<Sprite> postSpriteList = (ArrayList<Sprite>) loadedProject.getDefaultScene().getSpriteList();

		assertEquals(preScene.getName(), postScene.getName());

		assertEquals(preSpriteList.get(0).getName(), postSpriteList.get(0).getName());
		assertEquals(preSpriteList.get(1).getName(), postSpriteList.get(1).getName());
		assertEquals(preSpriteList.get(2).getName(), postSpriteList.get(2).getName());
		assertEquals(preSpriteList.get(3).getName(), postSpriteList.get(3).getName());
		assertEquals(preSpriteList.get(4).getName(), postSpriteList.get(4).getName());

		assertEquals(project.getName(), loadedProject.getName());

		Formula actualXPosition = ((FormulaBrick) postSpriteList.get(2).getScript(0).getBrickList().get(0))
				.getFormulaWithBrickField(Brick.BrickField.X_POSITION);

		Formula actualYPosition = ((FormulaBrick) postSpriteList.get(2).getScript(0).getBrickList().get(0))
				.getFormulaWithBrickField(Brick.BrickField.Y_POSITION);

		Formula actualSize = ((FormulaBrick) postSpriteList.get(1).getScript(0).getBrickList().get(2))
				.getFormulaWithBrickField(Brick.BrickField.SIZE);

		assertEquals(size, actualSize.interpretFloat(null));
		assertEquals(xPosition, actualXPosition.interpretFloat(null).intValue());
		assertEquals(yPosition, actualYPosition.interpretFloat(null).intValue());
	}

	@Test
	public void testSanityCheck() throws IOException {
		final int xPosition = 457;
		final int yPosition = 598;
		final float size = 0.8f;

		final Project project = new Project(InstrumentationRegistry.getTargetContext(), projectName);
		Sprite firstSprite = new SingleSprite("first");
		Sprite secondSprite = new SingleSprite("second");
		Sprite thirdSprite = new SingleSprite("third");
		Sprite fourthSprite = new SingleSprite("fourth");
		Script testScript = new StartScript();
		Script otherScript = new StartScript();
		HideBrick hideBrick = new HideBrick();
		ShowBrick showBrick = new ShowBrick();
		SetSizeToBrick setSizeToBrick = new SetSizeToBrick(size);
		ComeToFrontBrick comeToFrontBrick = new ComeToFrontBrick();
		PlaceAtBrick placeAtBrick = new PlaceAtBrick(xPosition, yPosition);

		testScript.addBrick(hideBrick);
		testScript.addBrick(showBrick);
		testScript.addBrick(setSizeToBrick);
		testScript.addBrick(comeToFrontBrick);

		otherScript.addBrick(placeAtBrick);

		firstSprite.addScript(testScript);
		secondSprite.addScript(otherScript);

		project.getDefaultScene().addSprite(firstSprite);
		project.getDefaultScene().addSprite(secondSprite);
		project.getDefaultScene().addSprite(thirdSprite);
		project.getDefaultScene().addSprite(fourthSprite);

		File tmpCodeFile = new File(buildProjectPath(project.getName()), TMP_CODE_XML_FILE_NAME);
		File currentCodeFile = new File(buildProjectPath(project.getName()), CODE_XML_FILE_NAME);

		assertFalse(tmpCodeFile.exists());
		assertFalse(currentCodeFile.exists());

		storageHandler.saveProject(project);

		assertTrue(currentCodeFile.exists());
		assertThat(currentCodeFile.length(), is(greaterThan(0L)));

		// simulate 1st Option: tmp_code.xml exists but code.xml doesn't exist
		// --> saveProject process will restore from tmp_code.xml
		assertTrue(tmpCodeFile.createNewFile());

		StorageOperations.transferData(currentCodeFile, tmpCodeFile);
		String currentCodeFileXml = Files.toString(currentCodeFile, Charsets.UTF_8);

		assertTrue(currentCodeFile.delete());

		storageHandler.saveProject(project);

		assertTrue(currentCodeFile.exists());
		assertThat(currentCodeFile.length(), is(greaterThan(0L)));
		assertEquals(currentCodeFileXml, Files.toString(currentCodeFile, Charsets.UTF_8));

		// simulate 2nd Option: tmp_code.xml and code.xml exist
		// --> saveProject process will discard tmp_code.xml and use code.xml
		assertTrue(tmpCodeFile.createNewFile());

		storageHandler.saveProject(project);

		assertFalse(tmpCodeFile.exists());
	}

	@Test
	public void testGetRequiredResources() {
		Brick.ResourcesSet resources = generateMultiplePermissionsProject().getRequiredResources();
		assertTrue(resources.contains(Brick.ARDRONE_SUPPORT));
		assertTrue(resources.contains(Brick.FACE_DETECTION));
		assertTrue(resources.contains(Brick.BLUETOOTH_LEGO_NXT));
		assertTrue(resources.contains(Brick.TEXT_TO_SPEECH));
	}

	@Test
	public void testWritePermissionFile() throws IOException {
		Project project = generateMultiplePermissionsProject();
		ProjectManager.getInstance().setProject(project);
		XstreamSerializer.getInstance().saveProject(project);

		File permissionsFile = new File(buildProjectPath(project.getName()), PERMISSIONS_FILE_NAME);
		assertTrue(permissionsFile.exists());

		//only for assertions. Add future permission; Vibration and LED not activated
		Set<String> permissions = new HashSet<>();
		permissions.add(Constants.ARDRONE_SUPPORT);
		permissions.add(Constants.BLUETOOTH_LEGO_NXT);
		permissions.add(Constants.TEXT_TO_SPEECH);
		permissions.add(Constants.FACE_DETECTION);

		BufferedReader reader = new BufferedReader(new FileReader(permissionsFile));
		String line;
		while ((line = reader.readLine()) != null) {
			assertTrue(permissions.contains(line));
		}
	}

	@Test
	public void testSerializeSettings() throws CompatibilityProjectException, OutdatedVersionProjectException,
			LoadingProjectException {
		NXTSensor.Sensor[] sensorMapping = new NXTSensor.Sensor[] {
				NXTSensor.Sensor.TOUCH, NXTSensor.Sensor.SOUND,
				NXTSensor.Sensor.LIGHT_INACTIVE, NXTSensor.Sensor.ULTRASONIC
		};

		Project project = generateMultiplePermissionsProject();
		ProjectManager.getInstance().setProject(project);

		String projectName = project.getName();
		SettingsFragment.setLegoMindstormsNXTSensorMapping(InstrumentationRegistry.getTargetContext(), sensorMapping);

		ProjectManager.getInstance().saveProject(InstrumentationRegistry.getTargetContext());
		Setting setting = project.getSettings().get(0);

		assertTrue(setting instanceof LegoNXTSetting);

		LegoNXTSetting nxtSetting = (LegoNXTSetting) setting;
		NXTSensor.Sensor[] actualSensorMapping = nxtSetting.getSensorMapping();

		assertEquals(4, actualSensorMapping.length);

		assertEquals(sensorMapping[0], actualSensorMapping[0]);
		assertEquals(sensorMapping[1], actualSensorMapping[1]);
		assertEquals(sensorMapping[2], actualSensorMapping[2]);
		assertEquals(sensorMapping[3], actualSensorMapping[3]);

		NXTSensor.Sensor[] changedSensorMapping = sensorMapping.clone();
		changedSensorMapping[0] = NXTSensor.Sensor.LIGHT_ACTIVE;

		SettingsFragment.setLegoMindstormsNXTSensorMapping(InstrumentationRegistry.getTargetContext(), changedSensorMapping);

		ProjectManager.getInstance().setProject(null);
		ProjectManager.getInstance().loadProject(projectName, InstrumentationRegistry.getTargetContext());

		actualSensorMapping = SettingsFragment.getLegoNXTSensorMapping(InstrumentationRegistry.getTargetContext());

		assertEquals(4, actualSensorMapping.length);

		assertEquals(sensorMapping[0], actualSensorMapping[0]);
		assertEquals(sensorMapping[1], actualSensorMapping[1]);
		assertEquals(sensorMapping[2], actualSensorMapping[2]);
		assertEquals(sensorMapping[3], actualSensorMapping[3]);

		project = ProjectManager.getInstance().getCurrentProject();

		setting = project.getSettings().get(0);
		nxtSetting = (LegoNXTSetting) setting;

		assertThat(setting, instanceOf(LegoNXTSetting.class));

		actualSensorMapping = nxtSetting.getSensorMapping();

		assertEquals(4, actualSensorMapping.length);

		assertEquals(sensorMapping[0], actualSensorMapping[0]);
		assertEquals(sensorMapping[1], actualSensorMapping[1]);
		assertEquals(sensorMapping[2], actualSensorMapping[2]);
		assertEquals(sensorMapping[3], actualSensorMapping[3]);
	}

	private Project generateMultiplePermissionsProject() {
		Project project = new Project(InstrumentationRegistry.getTargetContext(), projectName);

		LegoNxtMotorMoveBrick motorBrick = new LegoNxtMotorMoveBrick(
				LegoNxtMotorMoveBrick.Motor.MOTOR_A, SET_SPEED_INITIALLY);

		SetSizeToBrick setSizeToBrick = new SetSizeToBrick(
				new Formula(
						new FormulaElement(FormulaElement.ElementType.SENSOR, Sensors.FACE_SIZE.name(), null)));

		BrickBaseType moveBrick = DroneBrickFactory.getInstanceOfDroneBrick(
				DroneBrickFactory.DroneBricks.DRONE_MOVE_FORWARD_BRICK,
				DEFAULT_MOVE_TIME_IN_MILLISECONDS, DEFAULT_MOVE_POWER_IN_PERCENT);

		Sprite firstSprite = new SingleSprite("first");
		Script testScript = new StartScript();
		testScript.addBrick(new HideBrick());
		testScript.addBrick(new ShowBrick());
		testScript.addBrick(new SpeakBrick(""));
		testScript.addBrick(motorBrick);
		firstSprite.addScript(testScript);

		Sprite secondSprite = new SingleSprite("second");
		Script otherScript = new StartScript();
		otherScript.addBrick(setSizeToBrick);
		otherScript.addBrick(moveBrick);
		secondSprite.addScript(otherScript);

		project.getDefaultScene().addSprite(firstSprite);
		project.getDefaultScene().addSprite(secondSprite);

		return project;
	}

	@Test
	public void testExtractDefaultSceneNameFromXml() {
		final String firstSceneName = "First Scene";
		Project project = new Project(InstrumentationRegistry.getTargetContext(), projectName);
		project.getSceneList().get(0).setName(firstSceneName);
		storageHandler.saveProject(project);

		assertEquals(firstSceneName, XstreamSerializer.extractDefaultSceneNameFromXml(projectName));
	}
}
