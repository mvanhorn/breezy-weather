/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.main

import android.content.Intent
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.firstArg
import io.mockk.mockk
import org.junit.jupiter.api.Test

class MainActivityTest {

    @Test
    fun consumeNavigationActionClearsAlertAction() {
        val intent = intentWithAction(MainActivity.ACTION_SHOW_ALERTS).apply {
            every { getStringExtra(MainActivity.KEY_MAIN_ACTIVITY_LOCATION_FORMATTED_ID) } returns "location"
            every { getStringExtra(MainActivity.KEY_MAIN_ACTIVITY_ALERT_ID) } returns "alert"
        }

        consumeNavigationAction(intent) shouldBe MainActivity.ACTION_SHOW_ALERTS
        intent.getStringExtra(MainActivity.KEY_MAIN_ACTIVITY_LOCATION_FORMATTED_ID) shouldBe "location"
        intent.getStringExtra(MainActivity.KEY_MAIN_ACTIVITY_ALERT_ID) shouldBe "alert"
        consumeNavigationAction(intent) shouldBe null
    }

    @Test
    fun consumeNavigationActionUsesNewIntentAndPreservesExtras() {
        val previousIntent = intentWithAction(MainActivity.ACTION_SHOW_ALERTS)
        val newIntent = intentWithAction(MainActivity.ACTION_SHOW_DAILY_FORECAST).apply {
            every { getStringExtra(MainActivity.KEY_MAIN_ACTIVITY_LOCATION_FORMATTED_ID) } returns "location"
            every { getIntExtra(MainActivity.KEY_DAILY_INDEX, 0) } returns 2
        }

        consumeNavigationAction(newIntent) shouldBe MainActivity.ACTION_SHOW_DAILY_FORECAST
        newIntent.getStringExtra(MainActivity.KEY_MAIN_ACTIVITY_LOCATION_FORMATTED_ID) shouldBe "location"
        newIntent.getIntExtra(MainActivity.KEY_DAILY_INDEX, 0) shouldBe 2
        previousIntent.action shouldBe MainActivity.ACTION_SHOW_ALERTS
        newIntent.action shouldBe null
    }

    @Test
    fun consumeNavigationActionClearsEveryOneShotAction() {
        listOf(
            MainActivity.ACTION_SHOW_DAILY_FORECAST,
            MainActivity.ACTION_MANAGEMENT,
            MainActivity.ACTION_VIEW
        ).forEach { action ->
            val intent = intentWithAction(action)

            consumeNavigationAction(intent) shouldBe action
            intent.action shouldBe null
        }
    }

    @Test
    fun consumeNavigationActionReturnsNullForMissingAction() {
        listOf(null, "").forEach { action ->
            val intent = intentWithAction(action)

            consumeNavigationAction(intent) shouldBe null
            intent.action shouldBe null
        }
    }

    private fun intentWithAction(action: String?): Intent {
        var storedAction = action
        return mockk<Intent>().also { intent ->
            every { intent.action } answers { storedAction }
            every { intent.action = any() } answers { storedAction = firstArg<String?>() }
        }
    }
}
