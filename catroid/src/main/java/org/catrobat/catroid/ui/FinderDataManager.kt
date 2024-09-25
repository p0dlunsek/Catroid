/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

import java.util.ArrayList

class FinderDataManager {
    companion object {
        val instance:FinderDataManager by lazy {
            FinderDataManager()
        }
    }
    enum class InitiatingFragmentEnum(val id: Int){
        NONE(0),
        SCRIPT(3),
        LOOK(4),
        SOUND(5)
    }

    private var initiatingFragment: InitiatingFragmentEnum = InitiatingFragmentEnum.NONE
    private var searchResults: MutableList<Array<Int>>? = null
    private var searchResultIndex = -1
    private var searchQuery = ""
    private var searchOrder = arrayOf(-1, -1)

    fun getSearchOrder(): Array<Int>{
        return searchOrder
    }

    fun setSearchOrder(order: Array<Int>){
        searchOrder = order
    }
    fun setSearchQuery(searchquery: String){
        searchQuery = searchquery
    }

    fun getSearchQuery(): String{
        return searchQuery
    }
    fun setSearchResultIndex(searchresultIndex:Int){
        searchResultIndex = searchresultIndex
    }

    fun getSearchResultIndex(): Int{
        return searchResultIndex
    }
    fun addtoSearchResults(array: Array<Int>){
        searchResults?.add(array)
    }

    fun getInitiatingFragment(): InitiatingFragmentEnum{
        return initiatingFragment
    }

    fun setInitiatingFragment(initiatingfragment:InitiatingFragmentEnum){
        initiatingFragment = initiatingfragment
    }

    fun initializeSearchResults(){
        searchResults = ArrayList()
    }

    fun clearSearchResults(){
        searchResults?.clear()
    }

    fun getSearchResults(): MutableList<Array<Int>>? {
        return searchResults
    }
}