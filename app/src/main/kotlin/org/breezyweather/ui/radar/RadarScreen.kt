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

package org.breezyweather.ui.radar

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import breezyweather.domain.radar.model.RadarInfo
import breezyweather.domain.radar.model.RadarUrl
import org.breezyweather.R
import org.breezyweather.common.extensions.formatPercent
import org.breezyweather.common.extensions.getFormattedTime
import org.breezyweather.common.extensions.is12Hour
import org.breezyweather.common.extensions.setSystemBarStyle
import org.breezyweather.ui.common.widgets.Material3Scaffold
import org.breezyweather.ui.common.widgets.insets.BWCenterAlignedTopAppBar
import org.breezyweather.ui.settings.preference.composables.ListPreferenceViewWithCard
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import org.breezyweather.unit.ratio.Ratio.Companion.fraction
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.rememberRasterSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import java.util.Date
import kotlin.math.roundToInt

/**
 * TODO:
 * - Add onboarding screen (tile selector, radar source selector)
 * - Add configuration for tiles (change URL)
 * - Save configuration (opacity (global), color scheme (source-specific), preferred type radar/sattelite (source-specific))
 */
@Composable
internal fun RadarScreen(
    onBackPressed: () -> Unit,
    radarViewModel: RadarViewModel = viewModel(),
) {
    val radarUiState by radarViewModel.uiState.collectAsState()

    val baseStyle = remember {
        // TODO: Configurable
        BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty")
    }
    val camera = rememberCameraState(
        firstPosition = CameraPosition(target = Position(latitude = 0.0, longitude = 0.0), zoom = 1.0)
    )

    var radarInfo by remember { mutableStateOf<Map<String, RadarInfo>>(emptyMap()) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var selectedColorScheme by remember { mutableStateOf<String?>(null) }

    var opacity by remember { mutableFloatStateOf(0.8f) }

    var sliderIndex by remember { mutableFloatStateOf(0f) }
    var sliderValues by remember { mutableStateOf<List<RadarUrl>>(emptyList()) }

    val context = LocalContext.current
    val activity = LocalActivity.current

    val isLightTheme = ThemeManager.isLightTheme(context, radarUiState.location)

    LaunchedEffect(Unit) {
        sliderIndex = 0f
        radarViewModel.getRadarInfos(context)?.info?.let { info ->
            radarInfo = info
            selectedType = info.keys.firstOrNull() // TODO: Should look into saved config, and fallback to first
            if (selectedType != null) {
                // TODO: Should look into saved config, and fallback to first
                selectedColorScheme = if (!radarInfo[selectedType]!!.colorSchemes.isNullOrEmpty()) {
                    radarInfo[selectedType]!!.colorSchemes!!.first().replacePath
                } else {
                    null
                }
                sliderValues = if (selectedColorScheme != null) {
                    radarInfo[selectedType]!!.url
                        .map { url ->
                            url.copy(url = url.url.replace("\$color", selectedColorScheme!!))
                        }
                } else {
                    radarInfo[selectedType]!!.url
                }
                // Position to most recent image
                sliderIndex = info[selectedType]!!.url
                    .indexOfLast { it.time < Date() }
                    .takeIf { it != -1 }?.toFloat()
                    ?: 0f
            } else {
                sliderValues = emptyList()
                sliderIndex = 0f
            }
        }
    }

    LaunchedEffect(radarUiState.location) {
        // re-setting the status bar color once the location is fetched
        if (radarUiState.location != null && activity != null) {
            activity.window.setSystemBarStyle(isLightTheme)
            camera.animateTo(
                finalPosition = camera.position.copy(
                    target = Position(
                        latitude = radarUiState.location!!.latitude,
                        longitude = radarUiState.location!!.longitude
                    ),
                    zoom = 10.0
                )
            )
        }
    }

    BreezyWeatherTheme(!isLightTheme) {
        Material3Scaffold(
            topBar = {
                BWCenterAlignedTopAppBar(
                    title = stringResource(R.string.radar_title),
                    onBackPressed = onBackPressed
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MaplibreMap(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    baseStyle = baseStyle,
                    cameraState = camera
                ) {
                    if (sliderValues.isNotEmpty()) {
                        RasterLayer(
                            id = "radar",
                            source = rememberRasterSource(
                                tiles = listOf(sliderValues[sliderIndex.roundToInt()].url),
                                tileSize = 256
                            ),
                            opacity = const(opacity)
                        )
                    }
                }
                if (radarInfo.size > 1 && selectedType != null) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.normal_margin))
                    ) {
                        ListPreferenceViewWithCard(
                            title = "Type", // TODO: stringRessource()
                            summary = { _, value ->
                                radarInfo[value]!!.name
                            },
                            selectedKey = selectedType!!,
                            valueArray = radarInfo.keys.toTypedArray(),
                            nameArray = radarInfo.values.map { value -> value.name }.toTypedArray(),
                            withState = false,
                            isFirst = true,
                            isLast = true,
                            onValueChanged = { newValue ->
                                selectedType = newValue
                                sliderValues = if (selectedColorScheme != null) {
                                    radarInfo[selectedType]!!.url
                                        .map { url ->
                                            url.copy(url = url.url.replace("\$color", selectedColorScheme!!))
                                        }
                                } else {
                                    radarInfo[selectedType]!!.url
                                }
                                // Position to most recent image
                                sliderIndex = radarInfo[selectedType]!!.url
                                    .indexOfLast { url -> url.time < Date() }
                                    .takeIf { index -> index != -1 }?.toFloat()
                                    ?: 0f
                            }
                        )
                    }
                }
                if (!radarInfo[selectedType]?.colorSchemes.isNullOrEmpty()) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.normal_margin))
                    ) {
                        ListPreferenceViewWithCard(
                            title = "Color scheme", // TODO: stringRessource()
                            summary = { _, value ->
                                radarInfo[selectedType]!!.colorSchemes!!
                                    .firstOrNull { cs -> cs.replacePath == value }?.name
                                    ?: "Unknown"
                            },
                            selectedKey = selectedColorScheme!!,
                            valueArray = radarInfo[selectedType]!!.colorSchemes!!
                                .map { value -> value.replacePath }.toTypedArray(),
                            nameArray = radarInfo[selectedType]!!.colorSchemes!!
                                .map { value -> value.name }.toTypedArray(),
                            withState = false,
                            isFirst = true,
                            isLast = true,
                            onValueChanged = { newValue ->
                                selectedColorScheme = newValue
                                sliderValues = radarInfo[selectedType]!!.url
                                    .map { url ->
                                        url.copy(url = url.url.replace("\$color", newValue))
                                    }
                            }
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.normal_margin))
                ) {
                    Slider(
                        value = opacity,
                        onValueChange = { newValue ->
                            opacity = newValue
                        },
                        steps = 9,
                        valueRange = 0f..1f
                    )

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = opacity.toDouble().fraction.formatPercent(context),
                        textAlign = TextAlign.Center
                    )
                }
                if (sliderValues.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.normal_margin))
                    ) {
                        // TODO: Add Play/Pause button

                        Slider(
                            value = sliderIndex,
                            onValueChange = { newValue ->
                                sliderIndex = newValue
                            },
                            steps = sliderValues.size - 2,
                            valueRange = 0f..(sliderValues.size - 1).toFloat()
                        )

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = sliderValues[sliderIndex.roundToInt()].time
                                .getFormattedTime(radarUiState.location, context, context.is12Hour),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
