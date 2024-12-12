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
package org.catrobat.catroid.ui.recyclerview.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.ContactsContract.Data
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.PluralsRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.Constants.TMP_IMAGE_FILE_NAME
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.SharedPreferenceKeys
import org.catrobat.catroid.content.GroupSprite
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.merge.ImportProjectHelper
import org.catrobat.catroid.ui.FinderDataManager
import org.catrobat.catroid.ui.ProjectActivity
import org.catrobat.catroid.ui.ProjectListActivity
import org.catrobat.catroid.ui.ProjectListActivity.Companion.IMPORT_LOCAL_INTENT
import org.catrobat.catroid.ui.SpriteActivity
import org.catrobat.catroid.ui.ScriptFinder
import org.catrobat.catroid.ui.UiUtils
import org.catrobat.catroid.ui.WebViewActivity
import org.catrobat.catroid.ui.controller.BackpackListManager
import org.catrobat.catroid.ui.loadFragment
import org.catrobat.catroid.ui.recyclerview.adapter.MultiViewSpriteAdapter
import org.catrobat.catroid.ui.recyclerview.adapter.draganddrop.TouchHelperAdapterInterface
import org.catrobat.catroid.ui.recyclerview.adapter.draganddrop.TouchHelperCallback
import org.catrobat.catroid.ui.recyclerview.adapter.multiselection.MultiSelectionManager
import org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity
import org.catrobat.catroid.ui.recyclerview.controller.SpriteController
import org.catrobat.catroid.ui.recyclerview.dialog.TextInputDialog
import org.catrobat.catroid.ui.recyclerview.dialog.textwatcher.DuplicateInputTextWatcher
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider
import org.catrobat.catroid.ui.recyclerview.viewholder.CheckableViewHolder
import org.catrobat.catroid.ui.removeTabLayout
import org.catrobat.catroid.utils.SnackbarUtil
import org.catrobat.catroid.utils.ToastUtil
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException

@SuppressLint("NotifyDataSetChanged")
class SpriteListFragment : RecyclerViewFragment<Sprite?>() {
    private val spriteController = SpriteController()
    private val projectManager: ProjectManager by inject()

    private var currentSprite: Sprite? = null
    private lateinit var currentProject: Project
    private lateinit var currentScene: Scene

    internal inner class MultiViewTouchHelperCallback(adapterInterface: TouchHelperAdapterInterface?) :
        TouchHelperCallback(adapterInterface) {

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            when (actionState) {
                ItemTouchHelper.ACTION_STATE_IDLE -> {
                    val items = adapter.items
                    for (sprite in items) {
                        if (sprite is GroupSprite) {
                            continue
                        }
                        if (sprite?.toBeConverted() == true) {
                            val convertedSprite = spriteController.convert(sprite)
                            items[items.indexOf(sprite)] = convertedSprite
                        }
                    }
                    for (item in items) {
                        if (item is GroupSprite) {
                            item.isCollapsed = item.isCollapsed
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    public override fun shouldShowEmptyView() = adapter.itemCount == 1

    override fun onResume() {
        currentScene = ProjectManager.getInstance().currentlyEditedScene
        initializeAdapter()
        val activity = getActivity() as ProjectActivity
        super.onResume()
        SnackbarUtil.showHintSnackbar(requireActivity(), R.string.hint_objects)

        if (FinderDataManager.instance.getInitiatingFragment() != FinderDataManager.InitiatingFragmentEnum.NONE) {
            when(FinderDataManager.instance.type){

                ScriptFinder.Type.SCENE.id -> {
                    activity.onBackPressed()
                }

                ScriptFinder.Type.SOUND.id , ScriptFinder.Type.LOOK.id , ScriptFinder.Type.SCRIPT.id -> {
                    val intent = Intent(requireContext(), SpriteActivity::class.java)
                    intent.putExtra(
                        SpriteActivity.EXTRA_FRAGMENT_POSITION,
                        SpriteActivity.FRAGMENT_SOUNDS
                    )
                    startActivity(intent)
                }
            }

            val sceneAndSpriteName = createActionBarTitle()
            scriptfinder.onFragmentChanged(sceneAndSpriteName)
            val indexSearch = FinderDataManager.instance.getSearchResultIndex()
            val value = FinderDataManager.instance.getSearchResults()?.get(indexSearch)?.get(2)
            if (value != null) {
                recyclerView.scrollToPosition(value)
            }
            hideKeyboard()
        }
        else{
            scriptfinder.close()
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val parentView = super.onCreateView(inflater, container, savedInstanceState)
        recyclerView = parentView!!.findViewById(R.id.recycler_view)
        currentProject = ProjectManager.getInstance().currentProject
        currentScene = ProjectManager.getInstance().currentlyEditedScene
        val activity = getActivity() as ProjectActivity

        scriptfinder?.setOnResultFoundListener(object : ScriptFinder.OnResultFoundListener {
            override fun onResultFound(
                sceneIndex: Int,
                spriteIndex: Int,
                brickIndex: Int,
                type: Int,
                totalResults: Int,
                textView: TextView?
            )   {

                currentProject = ProjectManager.getInstance().currentProject
                currentScene = currentProject.sceneList[sceneIndex]

                if(type == ScriptFinder.Type.SPRITE.id) {
                    textView?.text = createActionBarTitle()
                }
                else{
                    currentSprite = currentScene.spriteList[spriteIndex]
                }
                if (type != ScriptFinder.Type.SPRITE.id) {

                    FinderDataManager.instance.currentMatchIndex = brickIndex

                    when (type) {
                        ScriptFinder.Type.SCENE.id -> {
                            activity.onBackPressed()
                        }
                        ScriptFinder.Type.SCRIPT.id -> {
                            ProjectManager.getInstance().setCurrentlyEditedScene(currentScene)
                            projectManager.currentSprite = currentSprite
                            val intent = Intent(requireContext(), SpriteActivity::class.java)
                            intent.putExtra(
                                SpriteActivity.EXTRA_FRAGMENT_POSITION,
                                SpriteActivity.FRAGMENT_SCRIPTS
                            )
                            startActivity(intent)
                        }
                        ScriptFinder.Type.LOOK.id -> {
                            ProjectManager.getInstance().setCurrentlyEditedScene(currentScene)
                            projectManager.currentSprite = currentSprite
                            val intent = Intent(requireContext(), SpriteActivity::class.java)
                            intent.putExtra(
                                SpriteActivity.EXTRA_FRAGMENT_POSITION,
                                SpriteActivity.FRAGMENT_LOOKS
                            )
                            startActivity(intent)
                        }
                        ScriptFinder.Type.SOUND.id -> {
                            ProjectManager.getInstance().setCurrentlyEditedScene(currentScene)
                            projectManager.currentSprite = currentSprite
                            val intent = Intent(requireContext(), SpriteActivity::class.java)
                            intent.putExtra(
                                SpriteActivity.EXTRA_FRAGMENT_POSITION,
                                SpriteActivity.FRAGMENT_SOUNDS
                            )
                            startActivity(intent)
                        }
                    }
                }
                else{
                    initializeAdapter()
                    adapter.notifyDataSetChanged()
                    val indexSearch = FinderDataManager.instance.getSearchResultIndex()
                    val value = FinderDataManager.instance.getSearchResults()?.get(indexSearch)?.get(2)
                    if (value != null) {
                        scriptfinder.showNavigationButtons()
                        recyclerView.scrollToPosition(value)
                    }
                }
                hideKeyboard()
            }
        })

        scriptfinder?.setOnCloseListener(object : ScriptFinder.OnCloseListener {
            override fun onClose() {
                activity.findViewById<View>(R.id.toolbar).visibility = View.VISIBLE
            }
        })

        scriptfinder?.setOnOpenListener(object : ScriptFinder.OnOpenListener {
            override fun onOpen() {
                activity.findViewById<View>(R.id.toolbar).visibility = View.GONE
                scriptfinder.setInitiatingFragment(FinderDataManager.InitiatingFragmentEnum.SPRITE)
                val order = arrayOf(2,3,4,5)
                FinderDataManager.instance.setSearchOrder(order)
            }
        })
        return parentView

    }
    fun createActionBarTitle(): String {
        return currentScene.name
    }
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
    override fun onAdapterReady() {
        super.onAdapterReady()
        val callback: ItemTouchHelper.Callback = MultiViewTouchHelperCallback(adapter)
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.new_group).isVisible = true
        menu.findItem(R.id.find).isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        this.itemCountThreshold = 2
        when (item.itemId) {
            R.id.new_group -> showNewGroupDialog()
            else -> handleSelectedOptionItem(item)
        }
        return true
    }

    private fun handleSelectedOptionItem(item: MenuItem) {
        if (adapter.items.size == 1) {
            ToastUtil.showError(activity, R.string.am_empty_list)
            resetActionModeParameters()
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun showNewGroupDialog() {
        val builder = TextInputDialog.Builder(requireContext())
        val uniqueNameProvider = UniqueNameProvider()
        builder.setHint(getString(R.string.sprite_group_name_label))
            .setTextWatcher(DuplicateInputTextWatcher<Sprite>(adapter.items))
            .setText(
                uniqueNameProvider.getUniqueNameInNameables(
                    getString(R.string.default_group_name),
                    adapter.items
                )
            )
            .setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, textInput: String? ->
                adapter.add(GroupSprite(textInput))
            }
        builder.setTitle(R.string.new_group)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun initializeAdapter() {
        sharedPreferenceDetailsKey = SharedPreferenceKeys.SHOW_DETAILS_SPRITES_PREFERENCE_KEY
        val items = projectManager.currentlyEditedScene.spriteList
        adapter = MultiViewSpriteAdapter(items)
        emptyView.setText(R.string.fragment_sprite_text_description)
        onAdapterReady()
    }

    override fun packItems(selectedItems: List<Sprite?>) {
        setShowProgressBar(true)
        var packedItemCnt = 0
        for (item in selectedItems) {
            try {
                BackpackListManager.getInstance().sprites.add(spriteController.pack(item))
                BackpackListManager.getInstance().saveBackpack()
                packedItemCnt++
            } catch (e: IOException) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }
        if (packedItemCnt > 0) {
            ToastUtil.showSuccess(
                requireContext(), resources.getQuantityString(
                    R.plurals.packed_sprites,
                    packedItemCnt,
                    packedItemCnt
                )
            )
            switchToBackpack()
        }
        finishActionMode()
    }

    override fun isBackpackEmpty() = BackpackListManager.getInstance().sprites.isEmpty()

    override fun switchToBackpack() {
        val intent = Intent(requireContext(), BackpackActivity::class.java)
        intent.putExtra(BackpackActivity.EXTRA_FRAGMENT_POSITION, BackpackActivity.FRAGMENT_SPRITES)
        startActivity(intent)
    }

    override fun copyItems(selectedItems: List<Sprite?>) {
        setShowProgressBar(true)
        val currentProject = projectManager.currentProject
        val currentScene = projectManager.currentlyEditedScene
        var copiedItemCnt = 0
        for (item in selectedItems) {
            try {
                adapter.add(spriteController.copy(item, currentProject, currentScene))
                copiedItemCnt++
            } catch (e: IOException) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }
        if (copiedItemCnt > 0) {
            ToastUtil.showSuccess(
                requireContext(), resources.getQuantityString(
                    R.plurals.copied_sprites,
                    copiedItemCnt,
                    copiedItemCnt
                )
            )
        }
        finishActionMode()
    }

    @PluralsRes
    override fun getDeleteAlertTitleId() = R.plurals.delete_sprites

    override fun deleteItems(selectedItems: List<Sprite?>) {
        setShowProgressBar(true)
        var deletedItemsCount = 0
        for (item in selectedItems) {
            if (item is GroupSprite) {
                for (sprite in item.groupItems) {
                    sprite.setConvertToSprite(true)
                    val convertedSprite = spriteController.convert(sprite)
                    adapter.items[adapter.items.indexOf(sprite)] = convertedSprite
                }
                adapter.notifyDataSetChanged()
            }
            spriteController.delete(item)
            adapter.remove(item)
            deletedItemsCount++
        }
        ToastUtil.showSuccess(
            requireContext(), resources.getQuantityString(
                R.plurals.deleted_sprites,
                deletedItemsCount,
                deletedItemsCount
            )
        )
        finishActionMode()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMPORT_OBJECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = if (data?.hasExtra(IMPORT_LOCAL_INTENT) == true) {
                Uri.fromFile(File(data.getStringExtra(IMPORT_LOCAL_INTENT)))
            } else {
                Uri.fromFile(File(data?.getStringExtra(WebViewActivity.MEDIA_FILE_PATH)))
            }

            val currentScene = projectManager.currentlyEditedScene
            val resolvedName: String
            val resolvedFileName =
                StorageOperations.resolveFileName(requireActivity().contentResolver, uri)
            val lookFileName: String
            val useDefaultSpriteName = resolvedFileName == null ||
                StorageOperations.getSanitizedFileName(resolvedFileName) == TMP_IMAGE_FILE_NAME
            if (useDefaultSpriteName) {
                resolvedName = getString(R.string.default_sprite_name)
                lookFileName = resolvedName + Constants.CATROBAT_EXTENSION
            } else {
                lookFileName = resolvedFileName
            }
            val importProjectHelper = ImportProjectHelper(
                lookFileName,
                currentScene,
                requireActivity()
            )
            if (!importProjectHelper.checkForConflicts()) {
                return
            }
            if (currentSprite != null) {
                importProjectHelper.addObjectDataToNewSprite(currentSprite)
            } else {
                importProjectHelper.rejectImportDialog(null)
            }
        }
    }

    private fun addFromLibrary(selectedItem: Sprite?) {
        currentSprite = selectedItem
        val intent = Intent(requireContext(), WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.INTENT_PARAMETER_URL, FlavoredConstants.LIBRARY_OBJECT_URL)
        startActivityForResult(intent, IMPORT_OBJECT_REQUEST_CODE)
    }

    private fun addFromLocalProject(item: Sprite?) {
        currentSprite = item
        val intent = Intent(requireContext(), ProjectListActivity::class.java)
        intent.putExtra(
            IMPORT_LOCAL_INTENT,
            getString(R.string.import_sprite_from_project_launcher))
        startActivityForResult(intent, IMPORT_OBJECT_REQUEST_CODE)
    }

    override fun getRenameDialogTitle() = R.string.rename_sprite_dialog

    override fun getRenameDialogHint() = R.string.sprite_name_label

    override fun renameItem(item: Sprite?, name: String) {
        item?.rename(name)
        finishActionMode()
    }

    override fun onItemClick(item: Sprite?, selectionManager: MultiSelectionManager?) {
        if (scriptfinder.isOpen){
            scriptfinder.close()
        }
        if (item is GroupSprite) {
            item.isCollapsed = !item.isCollapsed
            adapter.notifyDataSetChanged()
        } else {
            when (actionModeType) {
                RENAME -> {
                    super.onItemClick(item, null)
                    return
                }
                NONE -> {
                    projectManager.currentSprite = item
                    val intent = Intent(requireContext(), SpriteActivity::class.java)
                    intent.putExtra(
                        SpriteActivity.EXTRA_FRAGMENT_POSITION,
                        SpriteActivity.FRAGMENT_SCRIPTS
                    )
                    startActivity(intent)
                }
                else -> super.onItemClick(item, selectionManager)
            }
        }
    }

    override fun onItemLongClick(item: Sprite?, holder: CheckableViewHolder) {
        super.onItemLongClick(item, holder)
    }

    override fun onSettingsClick(item: Sprite?, view: View) {
        val itemList = mutableListOf<Sprite?>()
        itemList.add(item)
        val hiddenMenuOptionIds = mutableListOf<Int>(
            R.id.new_group, R.id.project_options, R.id.new_scene, R.id.show_details, R.id.edit
        )
        if (item is GroupSprite) {
            hiddenMenuOptionIds.add(R.id.backpack)
            hiddenMenuOptionIds.add(R.id.copy)
            hiddenMenuOptionIds.add(R.id.from_library)
            hiddenMenuOptionIds.add(R.id.from_local)
        }
        val popupMenu = UiUtils.createSettingsPopUpMenu(
            view, requireContext(), R.menu
                .menu_project_activity, hiddenMenuOptionIds.toIntArray()
        )
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.backpack -> packItems(itemList)
                R.id.copy -> copyItems(itemList)
                R.id.delete -> deleteItems(itemList)
                R.id.rename -> showRenameDialog(item)
                R.id.from_library -> addFromLibrary(item)
                R.id.from_local -> addFromLocalProject(item)
            }
            true
        }
        if (item !is GroupSprite) {
            popupMenu.menu.findItem(R.id.backpack).setTitle(R.string.pack)
            popupMenu.menu.removeItem(R.id.from_local)
        }
        popupMenu.show()
    }

    val isSingleVisibleSprite: Boolean
        get() = adapter.items.size == 2 && adapter.items[1] !is GroupSprite

    companion object {
        val TAG: String = SpriteListFragment::class.java.simpleName
        const val IMPORT_OBJECT_REQUEST_CODE = 0
    }
}
