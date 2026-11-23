package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.GeneratedShayariResult
import com.example.engine.ShayariCategory
import com.example.engine.ShayariVault
import com.example.engine.StylishFontStyle
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioAiHubPanel(
    height: Dp,
    onInsertText: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ShayariCategory.ROMANTIC) }
    var topicInput by remember { mutableStateOf("") }
    var generationCounter by remember { mutableIntStateOf(0) }
    var generatedResult by remember { mutableStateOf<GeneratedShayariResult?>(null) }
    var expandedCardKey by remember { mutableStateOf<String?>(null) }

    val activeLines = remember(selectedCategory, generatedResult) {
        generatedResult?.lines ?: ShayariVault.linesForCategory(selectedCategory)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height.coerceAtLeast(264.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0B0716))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(NeonMagenta.copy(alpha = 0.55f), NeonCyan.copy(alpha = 0.55f))),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(8.dp)
            .testTag("studio_ai_hub_panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Compact Topic Input Bar + "Generate ✨" button + Return to Keyboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161028))
                    .border(1.dp, Color(0xFF33265A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (topicInput.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ai_hub_topic_hint),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = topicInput,
                    onValueChange = { if (it.length <= 28) topicInput = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.sp,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_ai_topic_input")
                )
            }

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber)))
                    .clickable {
                        generationCounter += 1
                        generatedResult = ShayariVault.generatePoeticShayaris(topicInput, generationCounter)
                        expandedCardKey = null
                    }
                    .padding(horizontal = 10.dp)
                    .testTag("studio_ai_generate_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.ai_hub_generate_btn),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2235))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 9.dp)
                    .testTag("studio_ai_hub_close"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌨",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }
        }

        // Row 2: 5 Category Tabs (Romantic, Emotional, Motivation, Attitude, Quick Replies)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(ShayariCategory.entries, key = { it.id }) { category ->
                val isSelected = generatedResult == null && category == selectedCategory
                val accent = Color(category.accentHex)
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) accent.copy(alpha = 0.22f) else Color(0xFF151026)
                        )
                        .border(
                            width = if (isSelected) 1.25.dp else 1.dp,
                            color = if (isSelected) accent else Color(0xFF2B2248),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable {
                            selectedCategory = category
                            generatedResult = null
                            expandedCardKey = null
                        }
                        .padding(horizontal = 10.dp)
                        .testTag("studio_ai_tab_${category.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${category.emoji} ${category.tabTitle}",
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accent else TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Quick topic pills OR active generated theme banner
        val activeGen = generatedResult
        if (activeGen != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1132))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (activeGen.usedFallbackTheme) {
                        "✨ কিউরেটেড থিম: ${activeGen.resolvedTopic}"
                    } else {
                        "✨ বিষয়: ${activeGen.resolvedTopic} (৪টি শায়ারি)"
                    },
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.ai_hub_back_to_vault),
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable {
                            generatedResult = null
                            expandedCardKey = null
                        }
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(ShayariVault.quickTopicSuggestions, key = { it }) { topic ->
                    Box(
                        modifier = Modifier
                            .height(25.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF14192E))
                            .border(1.dp, Color(0xFF273A5C), RoundedCornerShape(999.dp))
                            .clickable {
                                topicInput = topic
                                generationCounter += 1
                                generatedResult = ShayariVault.generatePoeticShayaris(topic, generationCounter)
                                expandedCardKey = null
                            }
                            .padding(horizontal = 9.dp)
                            .testTag("studio_ai_quick_topic_$topic"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨ $topic",
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 10.5.sp,
                            color = Color(0xFF9AE6FF),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Scrollable Curated / Generated Shayari Cards with clean 2x2 Variation Chips on Tap
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(
                items = activeLines,
                key = { idx, line -> "${generatedResult?.resolvedTopic ?: selectedCategory.id}_${idx}_${line.hashCode()}" }
            ) { idx, line ->
                val cardKey = "${generatedResult?.resolvedTopic ?: selectedCategory.id}_$idx"
                val isExpanded = expandedCardKey == cardKey
                val effectiveCategory = if (generatedResult != null) ShayariCategory.ROMANTIC else selectedCategory
                val accentColor = Color(effectiveCategory.accentHex)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isExpanded) Color(0xFF1B1333) else Color(0xFF140F24))
                        .border(
                            width = if (isExpanded) 1.25.dp else 1.dp,
                            color = if (isExpanded) accentColor.copy(alpha = 0.8f) else Color(0xFF281F44),
                            shape = RoundedCornerShape(11.dp)
                        )
                        .clickable {
                            expandedCardKey = if (isExpanded) null else cardKey
                        }
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                        .testTag("studio_ai_card_$idx"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = line,
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = TextPrimary
                    )

                    AnimatedVisibility(visible = isExpanded) {
                        val variations = remember(line, effectiveCategory) {
                            ShayariVault.buildVariations(line, effectiveCategory)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            variations.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    pair.forEachIndexed { colIdx, variation ->
                                        val isDirectSend = variation.chipLabel == "সরাসরি পাঠান"
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isDirectSend) {
                                                        Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber))
                                                    } else {
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFF231842), Color(0xFF182A3D))
                                                        )
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isDirectSend) Color.Transparent else NeonCyan.copy(alpha = 0.45f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    onInsertText(variation.text)
                                                    expandedCardKey = null
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                                .testTag("studio_ai_variation_${idx}_${colIdx}_${variation.chipLabel}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = variation.chipLabel,
                                                fontFamily = NotoSansBengaliFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDirectSend) Color.White else NeonCyan,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudioStylishFontPanel(
    activeStyle: StylishFontStyle,
    height: Dp,
    onSelectStyle: (StylishFontStyle) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0F17))
            .padding(10.dp)
            .testTag("studio_fonts_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.stylish_fonts_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Text(
                text = "⌨",
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1D2030))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("studio_fonts_close")
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(StylishFontStyle.entries.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    pair.forEach { style ->
                        val selected = style == activeStyle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) NeonCyan.copy(alpha = 0.16f) else Color(0xFF1A1E2B))
                                .border(
                                    width = if (selected) 1.3.dp else 1.dp,
                                    color = if (selected) NeonCyan else ObsidianBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectStyle(style) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("studio_font_${style.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = style.label,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) NeonCyan else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
