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
package com.buzbuz.smartautoclicker.overlays.config.action.pause

import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogConfigActionPauseBinding
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.base.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.overlays.base.utils.DurationInputFilter
import com.buzbuz.smartautoclicker.overlays.base.utils.OnAfterTextChangedListener

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class PauseDialog(
    context: Context,
    private val pause: Action.Pause,
    private val onDeleteClicked: (Action.Pause) -> Unit,
    private val onConfirmClicked: (Action.Pause) -> Unit,
) : OverlayDialogController(context) {

    /** The view model for this dialog. */
    private val viewModel: PauseViewModel by lazy {
        ViewModelProvider(this).get(PauseViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionPauseBinding

    override fun onCreateDialog(): BottomSheetDialog {
        viewModel.setConfiguredSwipe(pause)

        viewBinding = DialogConfigActionPauseBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_action_type_pause)

                buttonDismiss.setOnClickListener { dismiss() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            editNameText.addTextChangedListener(object : OnAfterTextChangedListener() {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setName(s.toString())
                }
            })

            editPauseDurationText.apply {
                filters = arrayOf(DurationInputFilter())
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.setPauseDuration(if (!s.isNullOrEmpty()) s.toString().toLong() else null)
                    }
                })
            }
        }

        return BottomSheetDialog(context).apply {
            setContentView(viewBinding.root)
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.pauseDuration.collect(::updatePauseDuration) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        onConfirmClicked(viewModel.getConfiguredPause())
        dismiss()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked(pause)
        dismiss()
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameText.setText(newName)
    }

    private fun updatePauseDuration(newDuration: String?) {
        viewBinding.editPauseDurationText.setText(newDuration)
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }
}