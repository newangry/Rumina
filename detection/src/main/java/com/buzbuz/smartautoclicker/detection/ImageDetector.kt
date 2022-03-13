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
package com.buzbuz.smartautoclicker.detection

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.Keep

interface ImageDetector : AutoCloseable {

    fun setScreenImage(screenBitmap: Bitmap)

    fun detectCondition(conditionBitmap: Bitmap, threshold: Int): DetectionResult

    fun detectCondition(conditionBitmap: Bitmap, position: Rect, threshold: Int): DetectionResult
}

data class DetectionResult(var isDetected: Boolean, var centerX: Int, var centerY: Int) {

    @Keep
    fun setResults(isDetected: Boolean, centerX: Int, centerY: Int) {
        this.isDetected = isDetected
        this.centerX = centerX
        this.centerY = centerY
    }
}