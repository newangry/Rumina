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
package com.buzbuz.smartautoclicker.feature.scenario.config.data.base

import com.buzbuz.smartautoclicker.core.domain.model.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.domain.model.Identifier

internal class IdentifierCreator {

    /** The last generated domain id for an item. */
    private var lastGeneratedDomainId: Long = 0

    /** */
    fun generateNewIdentifier(): Identifier =
        Identifier(databaseId = DATABASE_ID_INSERTION, domainId = ++lastGeneratedDomainId)

    fun resetIdCount() {
        lastGeneratedDomainId = 0
    }
}