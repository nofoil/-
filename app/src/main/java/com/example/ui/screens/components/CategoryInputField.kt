package com.example.ui.screens.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldDark

@Composable
fun CategoryInputField(
    category: String,
    onCategoryChange: (String) -> Unit,
    label: String = "تصنيف المنتج (كتابة حرة)",
    isRequired: Boolean = true,
    modifier: Modifier = Modifier
) {
    val quickSuggestions = listOf(
        "أجهزة كهربائية",
        "أطباق ومائدة",
        "أطقم صيني وبورسلين",
        "أواني طهي وجرانيت",
        "أدوات مائدة وشوك وملاعق",
        "خلاطات ومحضرات طعام",
        "أفران وميكروويف",
        "كاسات وأطقم شاي",
        "أجهزة منزلية صغيرة",
        "أدوات مطبخ متفرقة"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            label = {
                Text(
                    text = if (isRequired) "$label *" else label
                )
            },
            placeholder = { Text("اكتب أي تصنيف تريده بحرية...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = GoldDark
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("category_input_field")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickSuggestions.forEach { suggestion ->
                val isSelected = category.trim() == suggestion
                AssistChip(
                    onClick = { onCategoryChange(suggestion) },
                    label = {
                        Text(
                            text = suggestion,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = if (isSelected) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        AssistChipDefaults.assistChipColors()
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
