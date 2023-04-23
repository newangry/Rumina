/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.action.swipe

import android.content.Context
import android.graphics.Point
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.baseui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.baseui.bindings.setLabel
import com.buzbuz.smartautoclicker.baseui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.baseui.bindings.setText
import com.buzbuz.smartautoclicker.baseui.utils.DurationInputFilter
import com.buzbuz.smartautoclicker.baseui.overlays.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogConfigActionSwipeBinding
import com.buzbuz.smartautoclicker.domain.edition.EditedAction
import com.buzbuz.smartautoclicker.overlays.base.utils.setError
import com.buzbuz.smartautoclicker.overlays.config.action.ClickSwipeSelectorMenu
import com.buzbuz.smartautoclicker.overlays.config.action.CoordinatesSelector

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class SwipeDialog(
    context: Context,
    private val editedSwipe: EditedAction,
    private val onDeleteClicked: (EditedAction) -> Unit,
    private val onConfirmClicked: (EditedAction) -> Unit,
) : OverlayDialogController(context, R.style.AppTheme) {

    /** The view model for this dialog. */
    private val viewModel: SwipeViewModel by lazy {
        ViewModelProvider(this).get(SwipeViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionSwipeBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setConfiguredSwipe(editedSwipe)

        viewBinding = DialogConfigActionSwipeBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_swipe)

                buttonDismiss.setOnClickListener { destroy() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            editNameLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }

            editSwipeDurationLayout.apply {
                textField.filters = arrayOf(DurationInputFilter())
                setLabel(R.string.input_field_label_swipe_duration)
                setOnTextChangedListener {
                    viewModel.setSwipeDuration(if (it.isNotEmpty()) it.toString().toLong() else null)
                }
            }

            onPositionSelectButton.setOnClickListener { showPositionSelector() }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { viewModel.swipeDuration.collect(::updateSwipeDuration) }
                launch { viewModel.swipeDurationError.collect(viewBinding.editSwipeDurationLayout::setError)}
                launch { viewModel.positions.collect(::updateSwipePositionsButtonText) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        onConfirmClicked(viewModel.getConfiguredSwipe())
        destroy()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked(editedSwipe)
        destroy()
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateSwipeDuration(newDuration: String?) {
        viewBinding.editSwipeDurationLayout.setText(newDuration)
    }

    private fun updateSwipePositionsButtonText(positions: Pair<Point, Point>?) {
        if (positions == null) {
            viewBinding.onPositionSelectButton.setText(R.string.button_text_swipe_positions_select)
            return
        }

        viewBinding.onPositionSelectButton.text = context.getString(
            R.string.item_desc_swipe_positions,
            positions.first.x,
            positions.first.y,
            positions.second.x,
            positions.second.y,
        )
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showPositionSelector() {
        showSubOverlay(
            overlayController = ClickSwipeSelectorMenu(
                context = context,
                selector = CoordinatesSelector.Two(),
                onCoordinatesSelected = { selector ->
                    (selector as CoordinatesSelector.Two).let {
                        viewModel.setPositions(it.coordinates1!!, it.coordinates2!!)
                    }
                },
            ),
            hideCurrent = true,
        )
    }
}