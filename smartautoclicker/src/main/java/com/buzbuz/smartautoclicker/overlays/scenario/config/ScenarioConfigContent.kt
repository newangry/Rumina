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
package com.buzbuz.smartautoclicker.overlays.scenario.config

import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentScenarioConfigBinding
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.ConditionOperator
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.overlays.base.DialogButton
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.endcondition.EndConditionConfigDialog
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ScenarioConfigContent(private val scenarioId: Long) : NavBarDialogContent() {

    /** View model for this content. */
    private val viewModel: ScenarioConfigViewModel by lazy { ViewModelProvider(this).get(ScenarioConfigViewModel::class.java) }

    private lateinit var viewBinding: ContentScenarioConfigBinding
    private lateinit var endConditionAdapter: EndConditionAdapter

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setScenario(scenarioId)

        viewBinding = ContentScenarioConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            scenarioNameInputEditText.apply {
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.setScenarioName(s.toString())
                    }
                })
            }

            textSpeed.setOnClickListener { viewModel.decreaseDetectionQuality() }
            textPrecision.setOnClickListener { viewModel.increaseDetectionQuality() }
            seekbarQuality.apply {
                addOnChangeListener { _, value, fromUser ->
                    if (fromUser) viewModel.setDetectionQuality(value.roundToInt())
                }
            }

            endConditionsOperatorButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                when (checkedId) {
                    R.id.end_conditions_and_button -> viewModel.setConditionOperator(AND)
                    R.id.end_conditions_or_button -> viewModel.setConditionOperator(OR)
                }
            }

            endConditionAdapter = EndConditionAdapter(
                addEndConditionClickedListener = ::onAddEndConditionClicked,
                endConditionClickedListener = ::onEndConditionClicked,
            )
            endConditionsList.adapter = endConditionAdapter
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.scenarioName.collect(::updateScenarioName) }
                launch { viewModel.detectionQuality.collect(::updateQuality) }
                launch { viewModel.endConditionOperator.collect(::updateEndConditionOperator) }
                launch { viewModel.endConditions.collect(::updateEndConditions) }
                launch { viewModel.isValidConfig.collect(::updateSaveButton) }
            }
        }
    }

    override fun onDialogButtonClicked(buttonType: DialogButton) {
        if (buttonType == DialogButton.SAVE) {
            viewModel.saveModifications()
        }
    }

    private fun updateSaveButton(isEnabled: Boolean) {
        navBarDialog.setSaveButtonState(navBarId, isEnabled)
    }

    private fun updateScenarioName(name: String?) {
        viewBinding.scenarioNameInputEditText.setText(name)
    }

    private fun updateQuality(quality: Int?) {
        if (quality == null) return

        viewBinding.apply {
            textQualityValue.text = quality.toString()

            val isNotInitialized = seekbarQuality.value == 0f
            seekbarQuality.value = quality.toFloat()

            if (isNotInitialized) {
                seekbarQuality.valueFrom = SLIDER_QUALITY_MIN
                seekbarQuality.valueTo = SLIDER_QUALITY_MAX
            }
        }
    }

    private fun updateEndConditionOperator(@ConditionOperator operator: Int?) {
        viewBinding.apply {
            val (text, buttonId) = when (operator) {
                AND -> context.getString(R.string.condition_operator_and) to R.id.end_conditions_and_button
                OR -> context.getString(R.string.condition_operator_or) to R.id.end_conditions_or_button
                else -> return@apply
            }

            endConditionsOperatorDesc.text = text
            if (endConditionsOperatorButton.checkedButtonId != buttonId) {
                endConditionsOperatorButton.check(buttonId)
            }
        }
    }

    private fun updateEndConditions(endConditions: List<EndConditionListItem>) {
        viewBinding.apply {
            if (endConditions.isEmpty()) {
                endConditionsList.visibility = View.GONE
                endConditionsNoEvents.visibility = View.VISIBLE
            } else {
                endConditionsList.visibility = View.VISIBLE
                endConditionsNoEvents.visibility = View.GONE
            }
        }

        endConditionAdapter.submitList(endConditions)
    }

    private fun onAddEndConditionClicked() {
        viewModel.createNewEndCondition().let { endCondition ->
            navBarDialog.showSubOverlayController(EndConditionConfigDialog(
                context = context,
                endCondition = endCondition,
                endConditions = viewModel.configuredEndConditions.value,
                onConfirmClicked = { newEndCondition -> viewModel.addEndCondition(newEndCondition) },
                onDeleteClicked = { viewModel.deleteEndCondition(endCondition) }
            ))
        }
    }

    private fun onEndConditionClicked(endCondition: EndCondition, index: Int) {
        navBarDialog.showSubOverlayController(EndConditionConfigDialog(
            context = context,
            endCondition = endCondition,
            endConditions = viewModel.configuredEndConditions.value,
            onConfirmClicked = { newEndCondition -> viewModel.updateEndCondition(newEndCondition, index) },
            onDeleteClicked = { viewModel.deleteEndCondition(endCondition) }
        ))
    }
}