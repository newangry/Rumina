/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogBaseMultiChoiceBinding
import com.buzbuz.smartautoclicker.databinding.ItemMultiChoiceBinding
import com.buzbuz.smartautoclicker.databinding.ItemMultiChoiceSmallBinding

import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * [OverlayDialogController] implementation for a dialog displaying a list of choices to the user.
 *
 * @param T the type of choices in the list. Must extends [DialogChoice].
 * @param context the Android Context for the dialog shown by this controller.
 * @param dialogTitleText the title of the dialog.
 * @param choices the choices to be displayed.
 * @param onChoiceSelected the callback to be notified upon user choice selection.
 */
class MultiChoiceDialog<T : DialogChoice>(
    context: Context,
    @StringRes private val dialogTitleText: Int,
    private val choices: List<T>,
    private val onChoiceSelected: (T) -> Unit
) : OverlayDialogController(context) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseMultiChoiceBinding
    /** The adapter displaying the choices. */
    private lateinit var adapter: ChoiceAdapter<T>

    override fun onCreateDialog(): BottomSheetDialog {
        viewBinding = DialogBaseMultiChoiceBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(dialogTitleText)
                buttonDismiss.setOnClickListener { dismiss() }
            }

            adapter = ChoiceAdapter(choices) { choice ->
                onChoiceSelected.invoke(choice)
                dismiss()
            }
        }


        return BottomSheetDialog(context).apply {
            setContentView(viewBinding.root)
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewBinding.list.adapter = adapter
    }
}

/**
 * Adapter displaying the choices in the dialog.
 *
 * @param T the type of choices in the list.
 * @param choices the choices to be displayed in the list.
 * @param onChoiceSelected called when the user clicks on a choice.
 */
private class ChoiceAdapter<T : DialogChoice>(
    private val choices: List<T>,
    private val onChoiceSelected: (T) -> Unit,
): RecyclerView.Adapter<MultiChoiceViewHolder<T>>() {

    override fun getItemCount(): Int = choices.size

    override fun getItemViewType(position: Int): Int {
        val item = choices[position]

        return when {
            item.description == null && item.iconId == null -> R.layout.item_multi_choice_small
            else -> R.layout.item_multi_choice
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiChoiceViewHolder<T> =
        when (viewType) {
            R.layout.item_multi_choice_small ->
                SmallChoiceViewHolder(ItemMultiChoiceSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_multi_choice ->
                ChoiceViewHolder(ItemMultiChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: MultiChoiceViewHolder<T>, position: Int) {
         holder.onBind(choices[position], onChoiceSelected)
    }
}

/**
 * Base view holder for a choice.
 * @param itemView the root view of the item.
 */
private abstract class MultiChoiceViewHolder<T : DialogChoice>(itemView: View): ViewHolder(itemView) {

    /**
     * Binds a choice to this view holder.
     * @param choice the choice object to be bound.
     * @param onChoiceSelected listener upon user click on the choice item.
     */
    abstract fun onBind(choice: T, onChoiceSelected: (T) -> Unit)
}

/**
 * View holder for a choice with an icon and a description.
 * @param holderViewBinding the view binding containing the holder root view.
 */
private class ChoiceViewHolder<T : DialogChoice>(
    val holderViewBinding: ItemMultiChoiceBinding,
) : MultiChoiceViewHolder<T>(holderViewBinding.root) {

    override fun onBind(choice: T, onChoiceSelected: (T) -> Unit) {
        holderViewBinding.apply {
            root.setOnClickListener { onChoiceSelected.invoke(choice) }

            choiceTitle.setText(choice.title)
            choiceDescription.apply {
                if (choice.description != null) {
                    visibility = View.VISIBLE
                    setText(choice.description)
                } else {
                    visibility = View.GONE
                }
            }
            choiceIcon.apply {
                if (choice.iconId != null) {
                    visibility = View.VISIBLE
                    setImageResource(choice.iconId)
                } else {
                    visibility = View.GONE
                    setImageResource(0)
                }
            }
        }
    }
}

/**
 * View holder for a choice with only a title.
 * @param holderViewBinding the view binding containing the holder root view.
 */
private class SmallChoiceViewHolder<T : DialogChoice>(
    val holderViewBinding: ItemMultiChoiceSmallBinding,
) : MultiChoiceViewHolder<T>(holderViewBinding.root) {

    override fun onBind(choice: T, onChoiceSelected: (T) -> Unit) {
        holderViewBinding.apply {
            root.setOnClickListener { onChoiceSelected.invoke(choice) }
            choiceTitle.setText(choice.title)
        }
    }
}

/** Base class for a dialog choice. */
open class DialogChoice(
    @StringRes val title: Int,
    @StringRes val description: Int? = null,
    @DrawableRes val iconId: Int? = null,
)