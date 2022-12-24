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
package com.buzbuz.smartautoclicker.overlays.config.event

import android.app.Application
import com.buzbuz.smartautoclicker.R

import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavigationViewModel
import com.buzbuz.smartautoclicker.overlays.config.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class EventDialogViewModel(application: Application) : NavigationViewModel(application) {

    /** The event currently configured. */
    val configuredEvent: Flow<Event> = EditionRepository.getInstance(application).configuredEvent
        .mapNotNull { it?.event }

    /** Visibility of the top bar delete button. */
    val deleteButtonVisibility: Flow<Boolean> = configuredEvent.map { it.id != 0L }

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = configuredEvent
        .map { configuredItem ->
            buildMap {
                put(R.id.page_event, configuredItem.name.isNotEmpty())
                put(R.id.page_conditions, configuredItem.conditions?.isNotEmpty() ?: false)
                put(R.id.page_actions, configuredItem.actions?.isNotEmpty() ?: false)
            }
        }

    /** Tells if the configured event is valid and can be saved. */
    val eventCanBeSaved: Flow<Boolean> = navItemsValidity
        .map { itemsValidity ->
            var allValid = true
            itemsValidity.values.forEach { validity ->
                allValid = allValid && validity
            }
            allValid
        }
}