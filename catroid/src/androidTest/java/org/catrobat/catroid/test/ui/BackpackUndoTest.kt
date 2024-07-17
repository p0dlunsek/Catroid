import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.pressBack
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.io.ResourceImporter
import org.catrobat.catroid.test.utils.TestUtils
import org.catrobat.catroid.ui.ProjectActivity
import org.catrobat.catroid.ui.controller.BackpackListManager
import org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity
import org.catrobat.catroid.ui.recyclerview.controller.LookController
import org.catrobat.catroid.ui.recyclerview.controller.SpriteController
import org.catrobat.catroid.uiespresso.ui.fragment.rvutils.RecyclerViewInteractionWrapper
import org.catrobat.catroid.uiespresso.util.UiTestUtils
import org.catrobat.catroid.uiespresso.util.rules.FragmentActivityTestRule
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.java.KoinJavaComponent
import java.io.File

@RunWith(Parameterized::class)
class BackpackUndoTest(private val fragmentId: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(BackpackActivity.FRAGMENT_SPRITES),
                //arrayOf(BackpackActivity.FRAGMENT_SOUNDS),
                //arrayOf(BackpackActivity.FRAGMENT_SCENES),
                //arrayOf(BackpackActivity.FRAGMENT_SCRIPTS),
                //arrayOf(BackpackActivity.FRAGMENT_LOOKS),
            )
        }
    }
    @get:Rule
    var baseActivityTestRule = FragmentActivityTestRule(
        BackpackActivity::class.java,
        BackpackActivity.EXTRA_FRAGMENT_POSITION,
        fragmentId
    )

    @get:Rule
    var activityTestRule = FragmentActivityTestRule(
        ProjectActivity::class.java,
        ProjectActivity.EXTRA_FRAGMENT_POSITION,
        1
    )


    private val backpackManager by KoinJavaComponent.inject(BackpackListManager::class.java)
    private lateinit var spriteController: SpriteController
    private lateinit var looksController: LookController
    private lateinit var sprite: Sprite
    private lateinit var imageFolder: File
    private lateinit var soundFolder: File
    private val fileName = "collision_donut.png"
    private lateinit var lookData: LookData
    private lateinit var project: Project
    private lateinit var soundInfo: SoundInfo
    private lateinit var soundInfo2: SoundInfo
    private lateinit var soundInfo3: SoundInfo

    @Before
    fun setUp() {
        imageFolder = File(
            ApplicationProvider.getApplicationContext<Context>().cacheDir,
            Constants.IMAGE_DIRECTORY_NAME
        )
        soundFolder = File(
            ApplicationProvider.getApplicationContext<Context>().cacheDir,
            Constants.SOUND_DIRECTORY_NAME
        )
        spriteController = SpriteController()
        looksController = LookController()
        project = UiTestUtils.createDefaultTestProject("testProject")
        //baseActivityTestRule.launchActivity()
        activityTestRule.launchActivity()
        addObjectToBackpack()
    }
    @After
    fun tearDown() {
        //TestUtils.clearBackPack(backpackManager)
    }

    private fun assignSize(): Int{
        var sizeBeforeDelete = 0
        when (fragmentId) {
            BackpackActivity.FRAGMENT_SPRITES -> {
                sizeBeforeDelete = backpackManager.sprites.size
            }
            BackpackActivity.FRAGMENT_SOUNDS -> {
                sizeBeforeDelete = backpackManager.backpackedSounds.size
            }
            BackpackActivity.FRAGMENT_LOOKS -> {
                sizeBeforeDelete = backpackManager.backpackedLooks.size
            }
            BackpackActivity.FRAGMENT_SCENES -> {
                sizeBeforeDelete = backpackManager.scenes.size
            }
            BackpackActivity.FRAGMENT_SCRIPTS -> {
                sizeBeforeDelete = backpackManager.backpackedScripts.size
            }
        }
        return sizeBeforeDelete
    }

    /*@Test
    fun testUndoLimitedToOneAction(){
        val x = 2
        val sizeBeforeDelete = assignSize()
        for (i in 1..x) {
            openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
            onView(withText(R.string.delete)).perform(ViewActions.click())

            RecyclerViewInteractionWrapper.onRecyclerView().atPosition(0)
                .performCheckItemClick()
            onView(withId(R.id.confirm)).perform(ViewActions.click())

            onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

            onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
                .perform(ViewActions.click())
        }
        onView(withId(R.id.menu_undo)).perform(ViewActions.click())

        Assert.assertEquals(sizeBeforeDelete - x + 1, assignSize())

        var exceptionOccurred = false

        try {
            onView(withId(R.id.menu_undo)).perform(ViewActions.click())
        } catch (e: androidx.test.espresso.NoMatchingViewException) {
            exceptionOccurred = true
        }
        assert(exceptionOccurred)
    }*/
    /*@Test
    fun testSingleUndo() {
        val sizeBeforeDelete = assignSize()

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.delete)).perform(ViewActions.click())

        RecyclerViewInteractionWrapper.onRecyclerView().atPosition(0)
            .performCheckItemClick()
        onView(withId(R.id.confirm)).perform(ViewActions.click())

        onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
            .perform(ViewActions.click())

        onView(withId(R.id.menu_undo)).perform(ViewActions.click())

        Assert.assertEquals(sizeBeforeDelete, assignSize())

        var exceptionOccurred = false

        try {
            onView(withId(R.id.menu_undo)).perform(ViewActions.click())
            } catch (e: androidx.test.espresso.NoMatchingViewException) {
            exceptionOccurred = true
        }
        assert(exceptionOccurred)


        /*pressBack()

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.backpack)).perform(ViewActions.click())*/
    }*/

    /*@Test
    fun testUndoOnAllDeleted() {

        val sizeBeforeDelete = assignSize()

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.delete)).perform(ViewActions.click())

        RecyclerViewInteractionWrapper.onRecyclerView().atPosition(0)
            .performCheckItemClick()
        RecyclerViewInteractionWrapper.onRecyclerView().atPosition(1)
            .performCheckItemClick()
        RecyclerViewInteractionWrapper.onRecyclerView().atPosition(2)
            .performCheckItemClick()
        onView(withId(R.id.confirm)).perform(ViewActions.click())

        onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
            .perform(ViewActions.click())

        onView(withId(R.id.menu_undo)).perform(ViewActions.click())

        Assert.assertEquals(sizeBeforeDelete, assignSize())

        var exceptionOccurred = false

        try {
            onView(withId(R.id.menu_undo)).perform(ViewActions.click())
        } catch (e: androidx.test.espresso.NoMatchingViewException) {
            exceptionOccurred = true
        }
        assert(exceptionOccurred)
    }*/

    @Test
    fun testUndoNotPossibleAfterReturning(){
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.backpack)).perform(ViewActions.click())

        var unpackText = onView(withText("Unpack"))
        unpackText.check(matches(isDisplayed()))
        onView(withText("Unpack")).perform(ViewActions.click())

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.delete)).perform(ViewActions.click())

        RecyclerViewInteractionWrapper.onRecyclerView().atPosition(0)
            .performCheckItemClick()
        onView(withId(R.id.confirm)).perform(ViewActions.click())

        onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
            .perform(ViewActions.click())

        pressBack()

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.backpack)).perform(ViewActions.click())

        unpackText = onView(withText("Unpack"))
        unpackText.check(matches(isDisplayed()))
        onView(withText("Unpack")).perform(ViewActions.click())

        var exceptionOccurred = false

        try {
            onView(withId(R.id.menu_undo)).perform(ViewActions.click())
        } catch (e: androidx.test.espresso.NoMatchingViewException) {
            exceptionOccurred = true
        }
        assert(exceptionOccurred)
    }

    private fun addObjectToBackpack() {
        when (fragmentId) {
            BackpackActivity.FRAGMENT_SPRITES -> {
                sprite = project.defaultScene.spriteList[1]
                backpackManager.sprites.add(spriteController.pack(sprite))
                backpackManager.sprites.add(spriteController.pack(sprite))
                backpackManager.sprites.add(spriteController.pack(sprite))
                backpackManager.saveBackpack()
            }
            BackpackActivity.FRAGMENT_LOOKS -> {
                val imageFile = ResourceImporter.createImageFileFromResourcesInDirectory(
                    InstrumentationRegistry.getInstrumentation().context.resources,
                    org.catrobat.catroid.test.R.raw.collision_donut,
                    imageFolder, fileName, 1.0
                )

                lookData = LookData("test", imageFile)

                backpackManager.backpackedLooks.add(lookData)
                backpackManager.backpackedLooks.add(lookData)
                backpackManager.backpackedLooks.add(lookData)
                backpackManager.saveBackpack()
            }
            BackpackActivity.FRAGMENT_SOUNDS -> {
                val soundFile = ResourceImporter.createSoundFileFromResourcesInDirectory(
                    InstrumentationRegistry.getInstrumentation().context.resources,
                    org.catrobat.catroid.test.R.raw.testsound,
                    imageFolder,
                    "testsoundui.mp3")

                val soundFile2 = ResourceImporter.createSoundFileFromResourcesInDirectory(
                    InstrumentationRegistry.getInstrumentation().context.resources,
                    org.catrobat.catroid.test.R.raw.testsound,
                    imageFolder,
                    "testsoundui.mp3")


                soundInfo = SoundInfo("testSound1",soundFile)
                soundInfo2 = SoundInfo("testSound2",soundFile2)
                soundInfo3 = SoundInfo("testSound3",soundFile2)

                backpackManager.backpackedSounds.add(soundInfo)
                backpackManager.backpackedSounds.add(soundInfo2)
                backpackManager.backpackedSounds.add(soundInfo3)
                backpackManager.saveBackpack()
            }
            BackpackActivity.FRAGMENT_SCENES -> {
                val scene2 = Scene("Scene2", project)

                backpackManager.scenes.add(scene2)
                backpackManager.scenes.add(scene2)
                backpackManager.scenes.add(scene2)
            }
            
            BackpackActivity.FRAGMENT_SCRIPTS -> {
                val scriptGroup = project.defaultScene.spriteList[1].scriptList

                backpackManager.addScriptToBackPack("start",scriptGroup)
                backpackManager.addScriptToBackPack("start1",scriptGroup)
                backpackManager.addScriptToBackPack("start2",scriptGroup)

                BackpackListManager.getInstance().saveBackpack()

            }
        }
    }
}
