package com.kieslingdev.mindscale.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SafetyActionsTest {

    @Test
    fun formattingIsStrippedAndDialableCharactersSurvive() {
        assertEquals("+15550100199", SafetyActions.dialString("+1 (555) 010-0199"))
        assertEquals("5550100", SafetyActions.dialString("555.0100"))
        assertEquals("5550100,123", SafetyActions.dialString("555-0100,123"))
        assertEquals("5550100#", SafetyActions.dialString("555 0100 #"))
        assertEquals("*675550100", SafetyActions.dialString("*67 555-0100"))
    }

    /**
     * A stored number with nothing dialable left in it gets no Call button at all, rather
     * than a button that cannot work (D-7).
     */
    @Test
    fun aNumberWithNoDigitsYieldsNoDialString() {
        assertNull(SafetyActions.dialString("+-()"))
        assertNull(SafetyActions.dialString(""))
        assertNull(SafetyActions.dialString("call me"))
    }

    @Test
    fun unavailableMessagesAreExactAndKeepTheNumberVisible() {
        assertEquals(
            "No app on this device can open the dialer. The number is 988.",
            SafetyActions.unavailableMessage(SafetyAction.Dial("988"))
        )
        assertEquals(
            "No app on this device can send a text message. The number is 988.",
            SafetyActions.unavailableMessage(SafetyAction.Text("988"))
        )
        assertEquals(
            "No app on this device can open a web page. The address is https://findahelpline.com.",
            SafetyActions.unavailableMessage(SafetyAction.OpenPage(FIND_A_HELPLINE_URL))
        )
    }
}
