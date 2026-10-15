package com.skye.llmusage.ui.glass

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skye.llmusage.ui.theme.LocalGlassEnabled

/**
 * 玻璃模式的通用控件样式;经典模式一律回落 M3 默认外观。
 */

/** 筛选 Chip:胶囊造型,未选为半透明磨砂,选中为主题色填充(iOS 胶囊语义) */
@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalGlassEnabled.current) {
        FilterChip(selected = selected, onClick = onClick, label = label, modifier = modifier)
        return
    }
    val dark = LocalGlassDark.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (dark) Color(0x3D1E2338) else Color(0x66FBFAFF),
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

/** iOS 节标题:卡片外小号灰字,与卡片内容左对齐 */
@Composable
fun GlassSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
    )
}

/** iOS 节脚注:卡片下方的小号灰字说明 */
@Composable
fun GlassSectionFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, top = 6.dp, bottom = 4.dp),
    )
}

/** 弹窗:玻璃模式为纸面材质大圆角(与内容卡一致),经典模式默认 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
) {
    if (LocalGlassEnabled.current) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            containerColor = if (LocalGlassDark.current) CardDarkSurface else CardLightSurface,
            shape = RoundedCornerShape(24.dp),
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
        )
    }
}
