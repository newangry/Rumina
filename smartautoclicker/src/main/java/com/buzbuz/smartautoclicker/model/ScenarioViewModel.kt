/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.model

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.database.ClickRepository
import com.buzbuz.smartautoclicker.database.ClickScenario
import com.buzbuz.smartautoclicker.SmartAutoClickerService
import com.buzbuz.smartautoclicker.database.ClickCondition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AndroidViewModel for create/delete/list click scenarios from an LifecycleOwner. */
class ScenarioViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository providing access to the click database. */
    private val clickRepository = ClickRepository.getRepository(application)
    /** Callback upon the availability of the [SmartAutoClickerService]. */
    private val serviceConnection: (SmartAutoClickerService.LocalService?) -> Unit = { localService ->
        clickerService = localService
    }
    /** Listener upon conditions without clicks. */
    private val clicklessConditionObserver = object : Observer<List<ClickCondition>> {
        override fun onChanged(conditions: List<ClickCondition>?) {
            if (conditions.isNullOrEmpty()) {
                return
            }

            viewModelScope.launch(Dispatchers.IO) {
                BitmapManager.getInstance(application).deleteBitmaps(conditions.map { it.path })
                clickRepository.deleteClicklessConditions()
            }
        }
    }

    /**
     * Reference on the [SmartAutoClickerService].
     * Will be not null only if the Accessibility Service is enabled.
     */
    private var clickerService: SmartAutoClickerService.LocalService? = null

    /** LiveData upon the list of scenarios. */
    val clickScenario = clickRepository.scenarios

    init {
        SmartAutoClickerService.getLocalService(serviceConnection)
        clickRepository.clicklessConditions.observeForever(clicklessConditionObserver)
    }

    override fun onCleared() {
        clickRepository.clicklessConditions.removeObserver(clicklessConditionObserver)
        SmartAutoClickerService.getLocalService(null)
        super.onCleared()
    }

    /**
     * Tells if the overlay permission is granted for this application.
     *
     * @return true if the permission is granted, false if not.
     */
    fun isOverlayPermissionValid(): Boolean = Settings.canDrawOverlays(getApplication())

    /**
     * Tells if the Accessibility Service of this application is started.
     *
     * @return true if the service is started, false if not.
     */
    fun isAccessibilityPermissionValid(): Boolean = clickerService != null

    /**
     * Tells if all application permission are granted.
     *
     * @return true if they are all granted, false if at least one is not.
     */
    fun arePermissionsGranted(): Boolean = isOverlayPermissionValid() && isAccessibilityPermissionValid()

    /**
     * Create a new click scenario.
     *
     * @param name the name of this new scenario.
     */
    fun createScenario(name: String) {
        viewModelScope.launch(Dispatchers.IO) { clickRepository.createScenario(name) }
    }

    /**
     * Rename the selected scenario.
     *
     * @param scenario the scenario to be renamed
     * @param name the new name of the scenario
     */
    fun renameScenario(scenario: ClickScenario, name: String) {
        viewModelScope.launch(Dispatchers.IO) { clickRepository.renameScenario(scenario.id, name) }
    }

    /**
     * Delete a click scenario.
     *
     * This will also delete all clicks associated with the scenario.
     *
     * @param scenario the scenario to be deleted.
     */
    fun deleteScenario(scenario: ClickScenario) {
        viewModelScope.launch(Dispatchers.IO) { clickRepository.deleteScenario(scenario) }
    }

    /**
     * Start the overlay UI and instantiates the detection objects for a given scenario.
     *
     * This requires the media projection permission code and its data intent, they both can be retrieved using the
     * results of the activity intent provided by
     * [android.media.projection.MediaProjectionManager.createScreenCaptureIntent] (this Intent shows the dialog
     * warning about screen recording privacy). Any attempt to call this method without the correct screen capture
     * intent result will leads to a crash.
     *
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param scenario the identifier of the scenario of clicks to be used for detection.
     */
    fun loadScenario(resultCode: Int, data: Intent, scenario: ClickScenario) {
        clickerService?.start(resultCode, data, scenario)
    }

    /** Stop the overlay UI and release all associated resources. */
    fun stopScenario() {
        clickerService?.stop()
    }
}