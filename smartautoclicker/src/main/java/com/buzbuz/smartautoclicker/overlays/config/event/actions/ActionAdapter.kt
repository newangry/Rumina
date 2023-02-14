/*
 * Copyright (C) 2023 Kevin Buzeau
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.config.event.actions

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.databinding.ItemActionBinding
import com.buzbuz.smartautoclicker.domain.edition.EditedAction
import com.buzbuz.smartautoclicker.overlays.base.bindings.ActionDetails
import com.buzbuz.smartautoclicker.overlays.base.bindings.bind

import java.util.Collections

/**
 * Displays the actions in a list.
 * Also provide a item displayed in the last position to add a new action.
 *
 * @param actionClickedListener  the listener called when the user clicks on a action.
 */
class ActionAdapter(
    private val actionClickedListener: (EditedAction, Int) -> Unit,
    private val actionReorderListener: (List<Pair<EditedAction, ActionDetails>>) -> Unit,
) : ListAdapter<Pair<EditedAction, ActionDetails>, ActionViewHolder>(ActionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder =
        ActionViewHolder(ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.onBind(getItem(position), actionClickedListener)
    }

    /**
     * Swap the position of two events in the list.
     *
     * @param from the position of the click to be moved.
     * @param to the new position of the click to be moved.
     */
    fun moveActions(from: Int, to: Int) {
        val newList = currentList.toMutableList()
        Collections.swap(newList, from, to)
        submitList(newList)
    }

    /** Notify for an item drag and drop completion. */
    fun notifyMoveFinished() {
        actionReorderListener(currentList)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [ActionAdapter] list. */
object ActionDiffUtilCallback: DiffUtil.ItemCallback<Pair<EditedAction, ActionDetails>>() {

    override fun areItemsTheSame(
        oldItem: Pair<EditedAction, ActionDetails>,
        newItem: Pair<EditedAction, ActionDetails>,
    ): Boolean = oldItem.first.itemId == newItem.first.itemId

    override fun areContentsTheSame(
        oldItem: Pair<EditedAction, ActionDetails>,
        newItem: Pair<EditedAction, ActionDetails>,
    ): Boolean = oldItem == newItem
}

/**
 * View holder displaying an action in the [ActionAdapter].
 * @param viewBinding the view binding for this item.
 */
class ActionViewHolder(private val viewBinding: ItemActionBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a action item.
     *
     * @param action the action to be represented by this item.
     * @param actionClickedListener listener notified upon user click on this item.
     */
    fun onBind(action: Pair<EditedAction, ActionDetails>, actionClickedListener: (EditedAction, Int) -> Unit) {
        viewBinding.bind(action.second, bindingAdapterPosition, true) { _, index ->
            actionClickedListener(action.first, index)
        }
    }
}

/** ItemTouchHelper attached to the adapter */
class ActionReorderTouchHelper : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    /** Tells if the user is currently dragging an item. */
    private var isDragging: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        println("TOTO: onMove from=${viewHolder.bindingAdapterPosition} to=${target.bindingAdapterPosition}")
        isDragging = true
        (recyclerView.adapter as ActionAdapter).moveActions(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (isDragging) {
            println("TOTO: clearView")
            (recyclerView.adapter as ActionAdapter).notifyMoveFinished()
            isDragging = false
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* Nothing do to */ }
}