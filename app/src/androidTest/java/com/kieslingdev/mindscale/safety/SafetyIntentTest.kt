package com.kieslingdev.mindscale.safety

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The phone-action safety contract (`docs/specs/SPEC-safety-card.md`, D-7, Invariant 4).
 *
 * These need real `Intent`/`Uri`, so they are instrumented rather than JVM tests.
 */
@RunWith(AndroidJUnit4::class)
class SafetyIntentTest {

    @Test
    fun dialingOpensTheDialerPreFilledAndNeverPlacesTheCall() {
        val intent = intentFor(SafetyAction.Dial("988"))
        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel", intent.data?.scheme)
        assertEquals("988", intent.data?.schemeSpecificPart)
        assertEquals("tel:988", intent.data.toString())
    }

    @Test
    fun textingOpensTheMessagingAppWithNoBody() {
        val intent = intentFor(SafetyAction.Text("988"))
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("smsto", intent.data?.scheme)
        assertEquals("988", intent.data?.schemeSpecificPart)
        assertNull(intent.getStringExtra("sms_body"))
        assertNull(intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun openingTheDirectoryHandsAUrlToTheBrowser() {
        val intent = intentFor(SafetyAction.OpenPage(FIND_A_HELPLINE_URL))
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://findahelpline.com", intent.data.toString())
    }

    /**
     * `Uri.fromParts` encodes the scheme-specific part, so a stored `#` is percent-encoded
     * rather than silently truncating the rest of the number into a fragment — which is
     * exactly what `Uri.parse("tel:$raw")` would have done.
     */
    @Test
    fun reservedCharactersInAStoredNumberSurviveEncoding() {
        val intent = intentFor(SafetyAction.Dial("5550100#2"))
        assertEquals("5550100#2", intent.data?.schemeSpecificPart)
        assertEquals("tel:5550100%232", intent.data.toString())
    }

    /**
     * The property that actually matters at runtime: the *installed* app requests no
     * telephony permission, so an `ACTION_CALL` would be impossible as well as forbidden.
     * The complementary source-level scan for `ACTION_CALL` lives in the JVM suite.
     */
    @Test
    fun theInstalledAppRequestsNoCallOrTelephonyPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toList()
            .orEmpty()

        listOf("CALL_PHONE", "SEND_SMS", "READ_PHONE_STATE", "PROCESS_OUTGOING_CALLS")
            .forEach { permission ->
                assertFalse(
                    "The installed app must not request $permission",
                    requested.any { it.contains(permission) }
                )
            }
    }
}
