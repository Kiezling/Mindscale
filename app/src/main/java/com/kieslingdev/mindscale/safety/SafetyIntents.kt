package com.kieslingdev.mindscale.safety

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/**
 * The single Android edge where a [SafetyAction] becomes an `Intent`
 * (`docs/specs/SPEC-safety-card.md`, D-7, Invariant 4).
 *
 * `ACTION_DIAL` opens the dialer pre-filled and **does not dial**; the user still taps
 * call. `ACTION_CALL` is forbidden here and everywhere in this app, and no `CALL_PHONE`
 * permission is declared — an app that could place a call on its own is not something a
 * local measurement instrument should be able to do, and a mis-tap on a screen someone
 * opened while distressed is a foreseeable harm.
 *
 * `Uri.fromParts` rather than `Uri.parse("tel:$number")`: the scheme-specific part is
 * given decoded and encoded on the way out, so a `#` in a stored number is percent-encoded
 * instead of silently truncating the rest of it into a fragment.
 *
 * No `resolveActivity` and no manifest `<queries>` element. Android 11 package visibility
 * filters *querying*, not *starting*, so an implicit start still works; a device with no
 * handler at all raises `ActivityNotFoundException`, which the caller catches and reports
 * with the number left on screen.
 */
fun intentFor(action: SafetyAction): Intent = when (action) {
    is SafetyAction.Dial ->
        Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", action.number, null))

    is SafetyAction.Text ->
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", action.number, null))

    is SafetyAction.OpenPage ->
        Intent(Intent.ACTION_VIEW, action.url.toUri())
}
