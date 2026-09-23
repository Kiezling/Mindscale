package com.kieslingdev.mindscale.settings

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** R-9: privacy/product information stays reachable at large font sizes. */
@RunWith(AndroidJUnit4::class)
class SettingsPrivacyTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var database: MindScaleDatabase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        viewModel = SettingsViewModel(
            settingsDao = database.trackSettingsDao(),
            dataControlDao = database.dataControlDao(),
            savedStateHandle = SavedStateHandle(),
            ioContext = Dispatchers.IO
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun privacyInformationIsReachableAt200PercentFont() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f)
            ) {
                MindScaleTheme { SettingsRoute(viewModel = viewModel, focus = SettingsFocus.TOP) }
            }
        }

        composeTestRule.onNodeWithTag("settings_section_privacy_and_product_information")
            .performClick()

        listOf(
            "privacy_local_storage" to PrivacyContent.LOCAL_STORAGE,
            "privacy_exports" to PrivacyContent.EXPORTS,
            "privacy_external_actions" to PrivacyContent.EXTERNAL_ACTIONS,
            "privacy_erase" to PrivacyContent.ERASE,
            "privacy_medical_disclaimer" to PrivacyContent.MEDICAL_DISCLAIMER
        ).forEach { (tag, copy) ->
            composeTestRule.onNodeWithTag("settings_screen")
                .performScrollToNode(hasTestTag(tag))
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
            composeTestRule.onNodeWithText(copy).assertIsDisplayed()
        }
    }

    @Test
    fun privacyPolicyLinkOpensExactHttpsUrlAndKeepsOfflineInformationReachable() {
        composeTestRule.setContent {
            MindScaleTheme { SettingsRoute(viewModel = viewModel, focus = SettingsFocus.TOP) }
        }
        composeTestRule.onNodeWithTag("settings_section_privacy_and_product_information").performClick()
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("privacy_local_storage"))
        composeTestRule.onNodeWithText(PrivacyContent.LOCAL_STORAGE).assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("privacy_publisher_support"))
        composeTestRule.onNodeWithText(PrivacyContent.PUBLISHER_SUPPORT).assertIsDisplayed()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val launchedIntent = AtomicReference<Intent?>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_VIEW) return null
                launchedIntent.set(intent)
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            composeTestRule.onNodeWithTag("settings_screen")
                .performScrollToNode(hasTestTag("privacy_policy_link"))
            composeTestRule.onNodeWithTag("privacy_policy_link").performClick()
            composeTestRule.waitUntil(timeoutMillis = 10_000) { monitor.hits >= 1 }

            val intent = requireNotNull(launchedIntent.get())
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("https://kiezling.github.io/Mindscale/", intent.data.toString())
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}
