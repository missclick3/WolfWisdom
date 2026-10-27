package ru.missclick.wolfwisdom.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.components.WolfGlassBackdrop
import ru.missclick.wolfwisdom.presentation.components.WolfGlassCard
import ru.missclick.wolfwisdom.presentation.components.WolfServiceScreen
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.rememberFeedbackPreferences
import ru.missclick.wolfwisdom.presentation.util.rememberFeedbackSettings
import ru.missclick.wolfwisdom.ui.theme.*

@Composable
fun SettingsRoot(onBackClick: () -> Unit = {}) {
    val preferences = rememberFeedbackPreferences()
    val settings by rememberFeedbackSettings(preferences)
    SettingsScreen(
        soundEffectsEnabled = settings.soundEffectsEnabled,
        vibrationEnabled = settings.vibrationEnabled,
        onAction = { action ->
            when (action) {
                SettingsAction.BackPressed -> onBackClick()
                is SettingsAction.LanguageSelected -> {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(action.language.tag))
                }
                is SettingsAction.SoundEffectsChanged -> preferences.setSoundEffectsEnabled(action.enabled)
                is SettingsAction.VibrationChanged -> preferences.setVibrationEnabled(action.enabled)
            }
        }
    )
}

@Composable
fun SettingsScreen(
    soundEffectsEnabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    onAction: (SettingsAction) -> Unit
) {
    val language = AppLanguage.fromTag(LocalConfiguration.current.locales[0].language)
    BackHandler { onAction(SettingsAction.BackPressed) }
    val device = currentDeviceConfiguration()
    val tablet = !device.isMobile
    val columnWidth = when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> 358.dp
        DeviceConfiguration.MOBILE_LANDSCAPE -> 420.dp
        else -> 520.dp
    }
    val contentTop = when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> 92.dp
        DeviceConfiguration.MOBILE_LANDSCAPE -> 80.dp
        DeviceConfiguration.TABLET_PORTRAIT -> 132.dp
        DeviceConfiguration.TABLET_LANDSCAPE -> 148.dp
    }
    WolfServiceScreen(
        title = stringResource(R.string.settings),
        onBackClick = { onAction(SettingsAction.BackPressed) },
        device = device
    ) { backdrop ->
        // The body scrolls independently, keeping Back accessible on short windows.
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WolfSpacing.Space16)
                .padding(top = contentTop - 60.dp, bottom = WolfSpacing.Space24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space12)
        ) {
            SettingsCard(
                title = stringResource(R.string.settings_sound),
                description = stringResource(R.string.settings_sound_description),
                icon = R.drawable.ic_settings_sound,
                checked = soundEffectsEnabled,
                onCheckedChange = { onAction(SettingsAction.SoundEffectsChanged(it)) },
                modifier = Modifier.widthIn(max = columnWidth).fillMaxWidth(),
                minHeight = if (tablet) 116.dp else 100.dp,
                backdrop = backdrop
            )
            SettingsCard(
                title = stringResource(R.string.settings_vibration),
                description = stringResource(R.string.settings_vibration_description),
                icon = R.drawable.ic_settings_vibration,
                checked = vibrationEnabled,
                onCheckedChange = { onAction(SettingsAction.VibrationChanged(it)) },
                modifier = Modifier.widthIn(max = columnWidth).fillMaxWidth(),
                minHeight = if (tablet) 116.dp else 100.dp,
                backdrop = backdrop
            )
            LanguageSettingsCard(
                language = language,
                onSelect = { onAction(SettingsAction.LanguageSelected(it)) },
                backdrop = backdrop,
                modifier = Modifier.widthIn(max = columnWidth).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LanguageSettingsCard(
    language: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    backdrop: WolfGlassBackdrop,
    modifier: Modifier = Modifier
) {
    WolfGlassCard(backdrop, modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = WolfSpacing.Space16)) {
            Text(
                stringResource(R.string.settings_language),
                modifier = Modifier.padding(horizontal = WolfSpacing.Space20, vertical = WolfSpacing.Space4),
                style = WolfTypography.Body,
                color = WolfColors.Text
            )
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = option == language,
                                role = Role.RadioButton,
                                onClick = { if (option != language) onSelect(option) }
                            )
                            .heightIn(min = 56.dp)
                            .padding(horizontal = WolfSpacing.Space20, vertical = WolfSpacing.Space8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space16)
                    ) {
                        Image(painterResource(option.flag), null, Modifier.size(36.dp, 24.dp))
                        Text(
                            stringResource(option.label), Modifier.weight(1f),
                            style = WolfTypography.Body, color = WolfColors.Text
                        )
                        RadioButton(
                            selected = option == language,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = WolfColors.Accent,
                                unselectedColor = WolfColors.Secondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    minHeight: Dp,
    backdrop: WolfGlassBackdrop,
    modifier: Modifier = Modifier
) {
    WolfGlassCard(backdrop, modifier.heightIn(min = minHeight)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = minHeight)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = WolfSpacing.Space20, vertical = WolfSpacing.Space16)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space16)
        ) {
            // Figma's 28dp leading slot contains the original 24dp artwork.
            Box(Modifier.size(28.dp), contentAlignment = Alignment.TopStart) {
                Image(painterResource(icon), null, Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space4)) {
                Text(title, style = WolfTypography.Body, color = WolfColors.Text)
                Text(description, style = WolfTypography.Caption, color = WolfColors.Secondary)
            }
            Image(
                painterResource(if (checked) R.drawable.ic_settings_toggle_on else R.drawable.ic_settings_toggle_off), null,
                Modifier.size(width = 48.dp, height = 28.dp)
            )
        }
    }
}

@Preview(name = "Settings · Phone portrait", widthDp = 390, heightDp = 844, locale = "ru")
@Preview(name = "Settings · Phone landscape", widthDp = 844, heightDp = 390, locale = "ru")
@Preview(name = "Settings · Tablet portrait", widthDp = 834, heightDp = 1112, locale = "ru")
@Preview(name = "Settings · Tablet landscape", widthDp = 1112, heightDp = 834, locale = "ru")
@Composable
private fun SettingsPreview() {
    WolfWisdomTheme { SettingsScreen(onAction = {}) }
}
