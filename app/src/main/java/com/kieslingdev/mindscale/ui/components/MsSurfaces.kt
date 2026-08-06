package com.kieslingdev.mindscale.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms

/**
 * The design's card: a 14 dp radius on the card surface, separated from the page by a hairline
 * rather than by a shadow (`docs/specs/SPEC-visual-foundation.md`, D-12 and D-13).
 *
 * The whole prototype contains five distinct shadows, of which the common one is
 * `0 1px 2px rgba(ink, .03)` — visually almost nothing. Near-flat is the identity, so this sets
 * no tonal or shadow elevation at all.
 *
 * [emphasized] switches the border to gold and the radius to 16 dp, matching the two places the
 * design raises a card's status.
 *
 * Like every component in this package it sets no `testTag`: tags belong to the caller and reach
 * the node through [modifier], so swapping bespoke markup for a component cannot silently drop
 * one of the 158 tags the connected suite depends on (D-15).
 */
@Composable
fun MsCard(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    contentPadding: Dp = MsSpacing.xlPlus,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = MaterialTheme.ms
    val shape = if (emphasized) MaterialTheme.shapes.large else MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.card)
            .border(
                width = MsSpacing.hairline,
                color = if (emphasized) palette.gold else palette.hairline,
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * The 1 dp rule that carries separation in this design.
 *
 * [faint] selects the design's `.07` alpha used between settings rows, rather than the `.09`
 * used for card borders and the navigation's top edge. Both are decorative separators and are
 * deliberately exempt from the 3:1 non-text floor (D-6, D-23).
 */
@Composable
fun MsHairline(modifier: Modifier = Modifier, faint: Boolean = false) {
    val palette = MaterialTheme.ms
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(MsSpacing.hairline)
            .background(if (faint) palette.hairlineFaint else palette.hairline)
    ) {}
}
