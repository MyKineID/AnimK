package com.animk.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.animk.app.data.model.MediaItem
import com.animk.app.ui.theme.LocalCustomColors

@Composable
fun MediaRow(
    title: String,
    icon: ImageVector? = null,
    items: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit,
    collapsedCount: Int = 8
) {
    if (items.isEmpty()) return
    val custom = LocalCustomColors.current
    var expanded by remember { mutableStateOf(false) }
    val displayItems = if (expanded) items else items.take(collapsedCount)
    val showExpand = items.size > collapsedCount

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .then(if (showExpand) Modifier.clickable { expanded = !expanded } else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = custom.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = custom.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (showExpand) {
                Text(
                    text = if (expanded) "Show Less" else "Show More",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = custom.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Collapsed: horizontal scroll
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayItems, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onMediaClick(item) }
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .width(120.dp)
                            .height(170.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        color = custom.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Expanded: grid layout
        if (expanded && showExpand) {
            Spacer(modifier = Modifier.height(12.dp))
            // Show remaining items in a second row
            val remaining = items.drop(collapsedCount)
            if (remaining.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(remaining, key = { it.id }) { item ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { onMediaClick(item) }
                        ) {
                            AsyncImage(
                                model = item.posterUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = custom.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
