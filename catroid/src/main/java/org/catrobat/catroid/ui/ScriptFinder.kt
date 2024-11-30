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
package org.catrobat.catroid.ui

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Nameable
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.databinding.ViewScriptFinderBinding
import org.catrobat.catroid.utils.ToastUtil
import org.koin.java.KoinJavaComponent.bind
import org.koin.java.KoinJavaComponent.inject
import java.util.ArrayList
import java.util.Locale

class ScriptFinder(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    enum class Type(val id: Int){
        SCENE(1),
        SPRITE(2),
        SCRIPT(3),
        LOOK(4),
        SOUND(5)
    }

    private var onResultFoundListener: OnResultFoundListener? = null
    private var onCloseListener: OnCloseListener? = null
    private var onOpenListener: OnOpenListener? = null
    private val projectManager: ProjectManager by inject(ProjectManager::class.java)
    private var binding: ViewScriptFinderBinding

    fun showNavigationButtons() {
        binding.find.visibility = GONE
        binding.findNext.visibility = VISIBLE
        binding.findPrevious.visibility = VISIBLE
        binding.searchPositionIndicator.visibility = VISIBLE
    }

    private fun hideNavigationButtons() {
        binding.findNext.visibility = GONE
        binding.findPrevious.visibility = GONE
        binding.searchPositionIndicator.visibility = GONE
        binding.find.visibility = VISIBLE
    }

    private fun formatSearchQuery(query: CharSequence): String = query.toString().trim()
        .toLowerCase(Locale.ROOT)

    init {
        orientation = VERTICAL
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewScriptFinderBinding.inflate(inflater, this)

        binding.find.setOnClickListener { find() }
        binding.findNext.setOnClickListener { findNext() }
        binding.findPrevious.setOnClickListener { findPrevious() }
        binding.close.setOnClickListener { close() }

        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(newText: CharSequence, start: Int, before: Int, count: Int) {
                if (FinderDataManager.instance.getSearchQuery() == formatSearchQuery(newText)) {
                    showNavigationButtons()
                } else {
                    hideNavigationButtons()
                }
            }

            override fun afterTextChanged(s: Editable) = Unit
        }
        binding.searchBar.addTextChangedListener(textWatcher)
        binding.searchBar.setOnEditorActionListener { _, actionId, keyEvent ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_DONE -> find()
                else -> if (keyEvent.action == KeyEvent.KEYCODE_ENTER) find()
            }
            false
        }
    }

    companion object {
        val TAG = ScriptFinder::class.java.simpleName

        @Suppress("ComplexMethod", "TooGenericExceptionCaught")
        fun searchBrickViews(v: View?, searchQuery: String): Boolean {
            try {
                if (v is Spinner) {
                    val selectedItem = v.selectedItem
                    if (selectedItem is Nameable && selectedItem.name.toLowerCase(Locale.ROOT)
                            .contains(searchQuery)
                    ) {
                        return true
                    }
                } else if (v is ViewGroup) {
                    for (i in 0 until v.childCount) {
                        val child = v.getChildAt(i)
                        val queryFoundInBrick = searchBrickViews(child, searchQuery)
                        if (queryFoundInBrick) {
                            return true
                        }
                    }
                } else if (v is TextView && v.text.toString().toLowerCase(Locale.ROOT)
                        .contains(searchQuery)
                ) {
                    return true
                }
            } catch (e: NullPointerException) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
            return false
        }
    }

    private fun find() {
        val query = formatSearchQuery(binding.searchBar.text)
        if (query.isNotEmpty()) {
            if (FinderDataManager.instance.getSearchQuery() != query) {
                FinderDataManager.instance.setSearchQuery(query)
                binding.searchBar.setText(query)
                fillIndices(query)
                binding.find.visibility = GONE
                binding.findNext.visibility = VISIBLE
                binding.findPrevious.visibility = VISIBLE
            } else if (FinderDataManager.instance.getSearchResults() != null) {
                findNext()
            }
        } else {
            ToastUtil.showError(
                context,
                context.getString(R.string.query_field_is_empty)
            )
        }
    }

    private fun findNext() {
        FinderDataManager.instance.getSearchResults()?.let {
            if (it.isNotEmpty()) {
                FinderDataManager.instance.setSearchResultIndex((FinderDataManager.instance.getSearchResultIndex() + 1) % it.size)
                updateUI()
            } else {
                binding.searchPositionIndicator.text = "0/0"
                ToastUtil.showError(context, context.getString(R.string.no_results_found))
            }
        }
    }

    private fun findPrevious() {
        FinderDataManager.instance.getSearchResults()?.let {
            if (it.isNotEmpty()) {
                FinderDataManager.instance.setSearchResultIndex(if (FinderDataManager.instance
                                                                        .getSearchResultIndex() == 0)
                    it.size - 1 else FinderDataManager.instance.getSearchResultIndex() - 1)
                updateUI()
            } else {
                binding.searchPositionIndicator.text = "0/0"
                ToastUtil.showError(context, context.getString(R.string.no_results_found))
            }
        }
    }

    fun onFragmentChanged(SceneAndSpriteName:String) {
        openForChangeFragment()

        binding.searchPositionIndicator.text = String.format(
            Locale.ROOT, "%d/%d", FinderDataManager.instance.getSearchResultIndex() + 1,
            FinderDataManager.instance.getSearchResults()?.size
        )
        binding.sceneAndSpriteName.text = SceneAndSpriteName
    }
    private fun updateUI() {
        val result = FinderDataManager.instance.getSearchResults()?.get(FinderDataManager.instance.getSearchResultIndex())
        binding.searchPositionIndicator.text = String.format(
            Locale.ROOT, "%d/%d", FinderDataManager.instance.getSearchResultIndex() + 1,
            FinderDataManager.instance.getSearchResults()?.size
        )

        result?.let {
            FinderDataManager.instance.getSearchResults()?.size?.let { it1 ->
                onResultFoundListener?.onResultFound(
                    it[0], it[1], it[2],it[3], it1,
                    binding.sceneAndSpriteName
                )
            }
        }
    }

    fun fillIndices(query: String) {
        FinderDataManager.instance.setSearchResultIndex(-1)
        val activeScene = projectManager.currentlyEditedScene
        val activeSprite: Sprite? = projectManager.currentSprite

        if (FinderDataManager.instance.getSearchResults() != null) {
            FinderDataManager.instance.clearSearchResults()
        } else {
            FinderDataManager.instance.initializeSearchResults()
        }
        startThreadToFillIndices(query, activeScene, activeSprite)
    }

    private fun startThreadToFillIndices(query: String, activeScene: Scene, activeSprite: Sprite?) {
        Thread {
            val query = FinderDataManager.instance.getSearchQuery()
            val activity = context as Activity
            if (!activity.isFinishing) {
                activity.runOnUiThread {
                    binding.find.visibility = GONE
                    binding.findNext.visibility = GONE
                    binding.findPrevious.visibility = GONE
                    binding.progressBar.visibility = VISIBLE
                }
            }
            val scenes = projectManager.currentProject.sceneList

            for (i in scenes.indices) {
                val scene = scenes[i]
                if (FinderDataManager.instance.getInitiatingFragment() == FinderDataManager.InitiatingFragmentEnum.SCENE){
                    if (scene.name.toLowerCase(Locale.ROOT).contains(query))
                        FinderDataManager.instance.addtoSearchResults(arrayOf(i, i, i, Type.SCENE.id))
                }
                val spriteList = scene.spriteList
                for (j in spriteList.indices) {
                    val sprite = spriteList[j]
                    val scriptList = sprite.scriptList
                    val bricks: List<Brick> = ArrayList()
                    projectManager.setCurrentSceneAndSprite(
                        scene.name,
                        sprite.name
                    )
                    for (script in scriptList) {
                        script.setParents()
                        script.addToFlatList(bricks)
                    }
                    for (order in FinderDataManager.instance.getSearchOrder()){
                        when (order) {
                            2 -> {
                                    if (sprite.name.toLowerCase(Locale.ROOT).contains(query))
                                        FinderDataManager.instance.addtoSearchResults(arrayOf(i, j, j, Type.SPRITE.id))
                            }
                            3 -> {
                                for (k in bricks.indices) {
                                    val brick = bricks[k]
                                    if (searchBrickViews(brick.getView(context), query)) {
                                        FinderDataManager.instance.addtoSearchResults(arrayOf(i, j, k, Type.SCRIPT.id))
                                    }
                                }
                            }
                            4 -> {
                                val lookList = sprite.lookList
                                for (k in lookList.indices) {
                                    val look = lookList[k]
                                    if (look.name.toLowerCase(Locale.ROOT).contains(query)) {
                                        FinderDataManager.instance.addtoSearchResults(arrayOf(i, j, k, Type.LOOK.id))
                                    }
                                }
                            }
                            5 -> {
                                val soundList = sprite.soundList
                                for (k in soundList.indices) {
                                    val sound = soundList[k]
                                    if (sound.name.toLowerCase(Locale.ROOT).contains(query)) {
                                        FinderDataManager.instance.addtoSearchResults(arrayOf(i, j, k, Type.SOUND.id))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!activity.isFinishing) {
                activity.runOnUiThread {
                    binding.findNext.visibility = VISIBLE
                    binding.findPrevious.visibility = VISIBLE
                    binding.searchPositionIndicator.visibility = VISIBLE
                    binding.progressBar.visibility = GONE
                    if (activeSprite != null){
                        projectManager.setCurrentSceneAndSprite(
                            activeScene.name,
                            activeSprite.name
                        )
                    }
                    findNext()
                }
            }
        }.start()
    }

    val isOpen: Boolean
        get() = visibility == VISIBLE

    fun setInitiatingFragment(fragmentEnum: FinderDataManager.InitiatingFragmentEnum){
        FinderDataManager.instance.setInitiatingFragment(fragmentEnum)
    }
    fun open() {
        this.visibility = VISIBLE
        binding.searchBar.isFocusable
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.toggleSoftInputFromWindow(
                binding.searchBar.applicationWindowToken,
                InputMethodManager.SHOW_FORCED, 0
            )
            onOpenListener?.onOpen()
            binding.searchBar.requestFocus()
    }

    fun openForChangeFragment(){
        this.visibility = VISIBLE
        showNavigationButtons()
        onOpenListener?.onOpen()
        binding.searchBar.setText(FinderDataManager.instance.getSearchQuery())
        binding.searchBar.isFocusableInTouchMode = false
    }

    fun close() {
        this.visibility = GONE
        FinderDataManager.instance.clearSearchResults()
        binding.searchBar.text.clear()
        binding.searchBar.isFocusableInTouchMode = true
        FinderDataManager.instance.setSearchQuery("")
        FinderDataManager.instance.setInitiatingFragment(FinderDataManager.InitiatingFragmentEnum.NONE)
        FinderDataManager.instance.type = -1
        onCloseListener?.onClose()
        hideNavigationButtons()
        this.hideKeyboard()
    }

    val isClosed: Boolean
        get() = visibility == GONE

    fun setOnResultFoundListener(onResultFoundListener: OnResultFoundListener?) {
        this.onResultFoundListener = onResultFoundListener
    }

    fun setOnCloseListener(onCloseListener: OnCloseListener?) {
        this.onCloseListener = onCloseListener
    }

    fun setOnOpenListener(onOpenListener: OnOpenListener?) {
        this.onOpenListener = onOpenListener
    }

    interface OnResultFoundListener {
        fun onResultFound(
            sceneIndex: Int,
            spriteIndex: Int,
            brickIndex: Int,
            type: Int,
            totalResults: Int,
            textView: TextView?
        )
    }

    interface OnCloseListener {
        fun onClose()
    }

    interface OnOpenListener {
        fun onOpen()
    }
}
