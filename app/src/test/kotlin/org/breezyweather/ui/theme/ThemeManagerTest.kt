/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.breezyweather.common.options.DarkMode
import org.breezyweather.domain.settings.SettingsManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ThemeManagerTest {

    private lateinit var settings: SettingsManager

    @BeforeEach
    fun setUp() {
        settings = mockk()
        mockkObject(SettingsManager.Companion)
        every { SettingsManager.getInstance(any()) } returns settings
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SettingsManager.Companion)
    }

    @Test
    fun systemFollowsEffectiveNightModeWhenLocationDayNightIsDisabled() {
        every { settings.darkMode } returns DarkMode.SYSTEM
        every { settings.dayNightModeForLocations } returns false
        val themed = themedContext(Configuration.UI_MODE_NIGHT_YES)

        // Daytime location, configured preference can still be light: effective night wins.
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe false

        themed.applicationConfiguration.uiMode = Configuration.UI_MODE_NIGHT_NO
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe true
    }

    @Test
    fun systemFollowsApplicationConfigurationInsteadOfActivityOverride() {
        every { settings.darkMode } returns DarkMode.SYSTEM
        every { settings.dayNightModeForLocations } returns false

        val activityLightApplicationDark = themedContext(
            applicationUiMode = Configuration.UI_MODE_NIGHT_YES,
            activityUiMode = Configuration.UI_MODE_NIGHT_NO
        )
        ThemeManager.isLightTheme(activityLightApplicationDark.context, daylight = true) shouldBe false

        val activityDarkApplicationLight = themedContext(
            applicationUiMode = Configuration.UI_MODE_NIGHT_NO,
            activityUiMode = Configuration.UI_MODE_NIGHT_YES
        )
        ThemeManager.isLightTheme(activityDarkApplicationLight.context, daylight = true) shouldBe true
    }

    @Test
    fun systemRereadsNightMaskWithoutCaching() {
        every { settings.darkMode } returns DarkMode.SYSTEM
        every { settings.dayNightModeForLocations } returns false
        val themed = themedContext(Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO)

        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe true

        themed.applicationConfiguration.uiMode = Configuration.UI_MODE_TYPE_CAR or Configuration.UI_MODE_NIGHT_YES
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe false

        themed.applicationConfiguration.uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe true

        // Undefined night bits are not dark, even when other uiMode bits are set.
        themed.applicationConfiguration.uiMode =
            Configuration.UI_MODE_TYPE_CAR or Configuration.UI_MODE_NIGHT_UNDEFINED
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe true
    }

    @Test
    fun explicitLightAndDarkPreferencesIgnoreEffectiveNightMode() {
        val themed = themedContext(Configuration.UI_MODE_NIGHT_YES)

        every { settings.darkMode } returns DarkMode.LIGHT
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe true
        ThemeManager.isLightTheme(themed.context, daylight = false) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = null) shouldBe false

        every { settings.darkMode } returns DarkMode.DARK
        every { settings.dayNightModeForLocations } returns true
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe true
        ThemeManager.isLightTheme(themed.context, daylight = false) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = null) shouldBe false

        every { settings.dayNightModeForLocations } returns false
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = false) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = null) shouldBe false

        themed.applicationConfiguration.uiMode = Configuration.UI_MODE_NIGHT_NO
        every { settings.dayNightModeForLocations } returns false
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe false
    }

    @Test
    fun systemLocationDayNightNighttimeAndMissingLocation() {
        every { settings.darkMode } returns DarkMode.SYSTEM
        val themed = themedContext(Configuration.UI_MODE_NIGHT_NO)

        every { settings.dayNightModeForLocations } returns false
        ThemeManager.isLightTheme(themed.context, daylight = false) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = null) shouldBe false

        every { settings.dayNightModeForLocations } returns true
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe true
        ThemeManager.isLightTheme(themed.context, daylight = false) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = null) shouldBe false

        themed.applicationConfiguration.uiMode = Configuration.UI_MODE_NIGHT_YES
        every { settings.dayNightModeForLocations } returns true
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe true
        ThemeManager.isLightTheme(themed.context, daylight = false) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = null) shouldBe false

        every { settings.dayNightModeForLocations } returns false
        ThemeManager.isLightTheme(themed.context, daylight = true) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = false) shouldBe false
        ThemeManager.isLightTheme(themed.context, daylight = null) shouldBe false
    }

    private fun themedContext(
        applicationUiMode: Int,
        activityUiMode: Int = applicationUiMode,
    ): ThemedContext {
        val applicationConfiguration = Configuration().apply {
            uiMode = applicationUiMode
        }
        val applicationResources = mockk<Resources> {
            every { configuration } returns applicationConfiguration
        }
        val applicationContext = mockk<Context>()
        every { applicationContext.resources } returns applicationResources
        every { applicationContext.applicationContext } returns applicationContext

        val activityResources = mockk<Resources> {
            every { configuration } returns Configuration().apply { uiMode = activityUiMode }
        }
        val activityContext = mockk<Context>()
        every { activityContext.resources } returns activityResources
        every { activityContext.applicationContext } returns applicationContext
        return ThemedContext(activityContext, applicationConfiguration)
    }

    private class ThemedContext(
        val context: Context,
        val applicationConfiguration: Configuration,
    )
}
