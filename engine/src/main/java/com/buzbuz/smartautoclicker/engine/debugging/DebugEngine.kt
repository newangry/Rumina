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
package com.buzbuz.smartautoclicker.engine.debugging

import android.graphics.Rect

import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.detection.DetectionResult
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.engine.ProcessorResult

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** Engine for the debugging of a scenario processing. */
class DebugEngine(
    private val scenario: Scenario,
    private val events: List<Event>,
) {
    /** Record the detection session duration. */
    private val sessionRecorder = ProcessingRecorder()
    /** Record all images processed. */
    private val imageRecorder = ProcessingRecorder()
    /** Map of event id to their recorder. */
    private val eventsRecorderMap: MutableMap<Long, ProcessingRecorder> = mutableMapOf()
    /** Map of condition id to their recorder. */
    private val conditionsRecorderMap: MutableMap<Long, ProcessingRecorder> = mutableMapOf()

    /** The event currently processed. */
    private var currProcEvtId: Long? = null
    /** The condition currently processed. */
    private var currProcCondId: Long? = null

    /** The debug report. Set once the detection session is complete. */
    private val _debugReport = MutableStateFlow<DebugReport?>(null)
    val debugReport: Flow<DebugReport> = _debugReport.filterNotNull()

    /** The DebugInfo for the current image. */
    private val currentInfo = MutableSharedFlow<DebugInfo>()
    /** The DebugInfo for the current image. */
    val lastResult = currentInfo
    /** The DebugInfo for the last positive detection. */
    val lastPositiveInfo = currentInfo
        .filter { it.detectionResult.isDetected }

    /** Start the session recorder at the DebugEngine creation. */
    init { sessionRecorder.onProcessingStart() }

    internal fun onImageProcessingStarted() {
        imageRecorder.onProcessingStart()
    }

    internal fun onEventProcessingStarted(event: Event) {
        if (currProcEvtId != null) throw IllegalStateException("start called without a complete")

        currProcEvtId = event.id
        if (!eventsRecorderMap.containsKey(event.id)) {
            eventsRecorderMap[event.id] = ProcessingRecorder()
        }
        eventsRecorderMap[event.id]!!.onProcessingStart()
    }

    internal fun onConditionProcessingStarted(condition: Condition) {
        if (currProcCondId != null) throw IllegalStateException("start called without a complete")

        currProcCondId = condition.id
        if (!conditionsRecorderMap.containsKey(condition.id)) {
            conditionsRecorderMap[condition.id] = ProcessingRecorder()
        }
        conditionsRecorderMap[condition.id]!!.onProcessingStart()
    }

    internal fun onConditionProcessingCompleted(detected: Boolean) {
        if (currProcCondId == null) throw IllegalStateException("completed called before start")

        conditionsRecorderMap[currProcCondId]?.onProcessingEnd(detected)
        currProcCondId = null
    }

    internal suspend fun onEventProcessingCompleted(result: ProcessorResult) {
        if (currProcEvtId == null) throw IllegalStateException("completed called before start")

        eventsRecorderMap[currProcEvtId]?.onProcessingEnd(result.eventMatched && result.event != null)
        currProcEvtId = null

        // Notify current detection progress
        if (result.event != null && result.condition != null && result.detectionResult != null) {
            val halfWidth = result.condition.area.width() / 2
            val halfHeight = result.condition.area.height() / 2

            val coordinates = if (result.detectionResult.position.x == 0 && result.detectionResult.position.y == 0) Rect()
            else Rect(
                result.detectionResult.position.x - halfWidth,
                result.detectionResult.position.y - halfHeight,
                result.detectionResult.position.x + halfWidth,
                result.detectionResult.position.y + halfHeight
            )

            currentInfo.emit(DebugInfo(result.event, result.condition, result.detectionResult, coordinates))
        }
    }

    internal fun onImageProcessingCompleted() {
        imageRecorder.onProcessingEnd()
    }

    internal fun onSessionEnded() {
        sessionRecorder.onProcessingEnd()

        val conditions = mutableListOf<Condition>()
        val eventsReport = events.map { event ->
            event.conditions?.let { conditions.addAll(it) }
            event to (eventsRecorderMap[event.id]?.toProcessingDebugInfo() ?: ProcessingDebugInfo())
        }
        val conditionReport = conditions.map { condition ->
            condition to (conditionsRecorderMap[condition.id]?.toProcessingDebugInfo() ?: ProcessingDebugInfo())
        }

        _debugReport.value = DebugReport(
            scenario,
            sessionRecorder.toProcessingDebugInfo(),
            imageRecorder.toProcessingDebugInfo(),
            eventsReport,
            conditionReport,
        )
    }

    /** Clear the values in the engine. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        currentInfo.resetReplayCache()
    }
}

/** Debug information for the scenario processing */
data class DebugInfo(
    val event: Event,
    val condition: Condition,
    val detectionResult: DetectionResult,
    val conditionArea: Rect,
)