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
package com.buzbuz.smartautoclicker.feature.tutorial.data.monitoring

import android.graphics.Rect
import android.view.View

import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialMonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.monitoring.TutorialMonitoredViewType

import kotlinx.coroutines.flow.StateFlow

class MonitoredViewsManager private constructor(): TutorialMonitoredViewsManager {

    companion object {

        /** Singleton preventing multiple instances of the MonitoredViewsManager at the same time. */
        @Volatile
        private var INSTANCE: MonitoredViewsManager? = null

        /**
         * Get the MonitoredViewsManager singleton, or instantiates it if it wasn't yet.
         *
         * @return the MonitoredViewsManager singleton.
         */
        fun getInstance(): MonitoredViewsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MonitoredViewsManager()
                INSTANCE = instance
                instance
            }
        }
    }

    private val monitoredViews: MutableMap<TutorialMonitoredViewType, ViewMonitor> = mutableMapOf()
    private val monitoredClicks: MutableMap<TutorialMonitoredViewType, () -> Unit> = mutableMapOf()

    override fun attach(type: TutorialMonitoredViewType, monitoredView: View) {
        if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor()
        monitoredViews[type]?.attachView(monitoredView)
    }

    override fun detach(type: TutorialMonitoredViewType) {
        monitoredViews[type]?.detachView()
    }

    override fun notifyClick(type: TutorialMonitoredViewType) {
        monitoredClicks[type]?.invoke()
    }

    internal fun setExpectedViews(types: Set<TutorialMonitoredViewType>) {
        types.forEach { type ->
            if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor()
        }
    }

    internal fun clearExpectedViews() {
        monitoredViews.clear()
    }

    internal fun getViewPosition(type: TutorialMonitoredViewType): StateFlow<Rect>? =
        monitoredViews[type]?.position

    internal fun performClick(type: TutorialMonitoredViewType): Boolean =
        monitoredViews[type]?.performClick() ?: false

    internal fun monitorNextClick(type: TutorialMonitoredViewType, listener: () -> Unit) {
        monitoredClicks[type] = {
            monitoredClicks.remove(type)
            listener()
        }
    }
}