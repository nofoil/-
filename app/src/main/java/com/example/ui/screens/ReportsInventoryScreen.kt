package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InstallmentReminderEntity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.core.content.ContextCompat
import com.example.ui.screens.components.MonthlySalesBarChart
import com.example.ui.viewmodel.InventoryItem
import com.example.ui.viewmodel.MonthlySalesData
import com.example.ui.viewmodel.SalesViewModel
import com.example.util.ExcelExporter
import com.example.util.UiHelpers

@Composable
fun ReportsInventoryScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val merchants by viewModel.merchants.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val monthlySalesData by viewModel.monthlySalesData.collectAsStateWithLifecycle()
    val lowStockThreshold by viewModel.lowStockThreshold.collectAsStateWithLifecycle()
    val lastBackupDate by viewModel.lastBackupDate.collectAsStateWithLifecycle()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: التقارير والمالية, 1: المخزون, 2: تذكيرات الأقساط
    var showAddReminderDialog by remember { mutableStateOf(false) }

    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.saveBackupToUri(context, uri)
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Excel Export Banner Card
        ExcelExportBanner(
            onExportClick = {
                viewModel.exportToExcel { file ->
                    ExcelExporter.shareExportedFile(context, file)
                }
            }
        )

        // Database Backup & Restore Banner Card
        BackupRestoreBanner(
            lastBackupDate = lastBackupDate,
            onSaveBackupToStorage = {
                val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                saveBackupLauncher.launch("hamza_backup_$dateStr.json")
            },
            onShareBackup = {
                viewModel.exportBackupToFile { file ->
                    com.example.util.DatabaseBackupManager.shareBackupFile(context, file)
                }
            },
            onRestoreBackup = {
                openBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            }
        )

        // Sub Tabs
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = GoldDark
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("تقارير المبيعات", fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("إدارة المخزون (${inventory.size})", fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("مواعيد الأقساط (${reminders.count { !it.isPaid }})", fontWeight = if (selectedSubTab == 2) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTab) {
                0 -> FinancialReportsSection(merchants = merchants, customers = customers, monthlySalesData = monthlySalesData)
                1 -> InventorySection(
                    inventory = inventory,
                    lowStockThreshold = lowStockThreshold,
                    onSetThreshold = { viewModel.setLowStockThreshold(it) },
                    onSendAlerts = { viewModel.checkAndSendLowStockAlerts(context) }
                )
                2 -> RemindersSection(
                    reminders = reminders,
                    customers = customers,
                    onToggleReminder = { r -> viewModel.toggleReminderStatus(r) },
                    onDeleteReminder = { r -> viewModel.deleteReminder(r) },
                    onAddClick = { showAddReminderDialog = true }
                )
            }

            if (selectedSubTab == 2) {
                FloatingActionButton(
                    onClick = { showAddReminderDialog = true },
                    containerColor = GoldDark,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("add_reminder_fab")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة موعد قسط", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showAddReminderDialog) {
        AddReminderDialog(
            customers = customers,
            onDismiss = { showAddReminderDialog = false },
            onConfirm = { custId, name, date, amt, notes ->
                viewModel.addReminder(custId, name, date, amt, notes)
                showAddReminderDialog = false
            }
        )
    }

    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("تنبيه أمان: استعادة النسخة الاحتياطية ستقوم باستبدال جميع بيانات التجار، الزبائن، الأقساط والمخزون الحالية بالبيانات الموجودة في ملف النسخة الاحتياطية المختار. هل تود المتابعة؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri?.let { uri ->
                            viewModel.restoreBackupFromUri(context, uri)
                        }
                        showRestoreConfirmDialog = false
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، استعادة واستبدال البيانات")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreUri = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ExcelExportBanner(onExportClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "تصدير البيانات للأكسيل (Excel Export)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "تصدير تقرير شامل للزبائن والمشتريات والمخزون بصيغة CSV تدعم اللغة العربية",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("export_excel_action_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصدير الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BackupRestoreBanner(
    lastBackupDate: String?,
    onSaveBackupToStorage: () -> Unit,
    onShareBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("backup_restore_banner_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GoldDark.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "النسخ الاحتياطي لقاعدة البيانات (Backup & Restore)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (lastBackupDate != null)
                                "آخر نسخة محفوظة: $lastBackupDate"
                            else
                                "حفظ نسخة من بيانات المبيعات والمشتريات على ذاكرة الهاتف لضمان عدم ضياعها",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (lastBackupDate != null) EmeraldSuccess else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Save to device storage via SAF
                Button(
                    onClick = onSaveBackupToStorage,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("save_backup_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ بالهاتف", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                // Button 2: Share / send file (Google Drive / WhatsApp)
                OutlinedButton(
                    onClick = onShareBackup,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_backup_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp), tint = GoldLight)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", fontSize = 11.5.sp)
                }

                // Button 3: Restore from backup file
                OutlinedButton(
                    onClick = onRestoreBackup,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("restore_backup_button")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFF87171))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استعادة", fontSize = 11.5.sp)
                }
            }
        }
    }
}

@Composable
private fun FinancialReportsSection(
    merchants: List<com.example.data.model.MerchantWithProducts>,
    customers: List<com.example.data.model.CustomerWithDetails>,
    monthlySalesData: List<MonthlySalesData>
) {
    val totalPurchases = merchants.sumOf { it.totalPurchaseCost }
    val totalPurchasesPaid = merchants.sumOf { it.merchant.paidAmount }
    val totalPurchasesRemaining = merchants.sumOf { it.remainingAmount }

    val totalSales = customers.sumOf { it.totalPurchasedAmount }
    val totalSalesCollected = customers.sumOf { it.totalPaidOverall }
    val totalReceivables = customers.sumOf { it.totalRemainingDebt }

    val expectedGrossProfit = totalSales - totalPurchases

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vertical Bar Chart: Monthly Sales and Growth
        item {
            MonthlySalesBarChart(monthlyData = monthlySalesData)
        }

        // Profit / Margin Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (expectedGrossProfit >= 0) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (expectedGrossProfit >= 0) EmeraldSuccess.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("صافي الأرباح المتوقعة (هامش الربح التجاري)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("إجمالي المبيعات ناقص تكلفة البضائع المشتراة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = UiHelpers.formatMoney(expectedGrossProfit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (expectedGrossProfit >= 0) EmeraldSuccess else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Sales Performance
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("تقرير مبيعات الزبائن والتحصيلات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    MetricRow(title = "إجمالي قيمة المبيعات المسجلة:", value = UiHelpers.formatMoney(totalSales), isBold = true)
                    MetricRow(title = "إجمالي النقدية المحصلة (مقدمات + أقساط):", value = UiHelpers.formatMoney(totalSalesCollected), valueColor = EmeraldSuccess)
                    MetricRow(title = "إجمالي الديون المستحقة بالخارج لدى الزبائن:", value = UiHelpers.formatMoney(totalReceivables), valueColor = if (totalReceivables > 0) Color(0xFFF87171) else EmeraldSuccess)
                    MetricRow(title = "عدد الزبائن الكلي:", value = "${customers.size} عميل")
                }
            }
        }

        // Purchases Performance
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("تقرير مشتريات البضائع والموردين", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    MetricRow(title = "إجمالي فواتير المشتريات من التجار:", value = UiHelpers.formatMoney(totalPurchases), isBold = true)
                    MetricRow(title = "إجمالي ما تم سداده للموردين:", value = UiHelpers.formatMoney(totalPurchasesPaid), valueColor = EmeraldSuccess)
                    MetricRow(title = "إجمالي المتبقي للموردين (مستحقات للتجار):", value = UiHelpers.formatMoney(totalPurchasesRemaining), valueColor = if (totalPurchasesRemaining > 0) Color(0xFFF87171) else EmeraldSuccess)
                    MetricRow(title = "عدد التجار المسجلين:", value = "${merchants.size} تاجر / مورد")
                }
            }
        }
    }
}

@Composable
private fun MetricRow(title: String, value: String, isBold: Boolean = false, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold, color = valueColor)
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
}

@Composable
private fun InventorySection(
    inventory: List<InventoryItem>,
    lowStockThreshold: Int,
    onSetThreshold: (Int) -> Unit,
    onSendAlerts: () -> Unit
) {
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSendAlerts()
        }
    }

    val lowStockItems = remember(inventory, lowStockThreshold) {
        inventory.filter { it.remainingStock <= lowStockThreshold }
    }

    if (inventory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("لا توجد أصناف في المخزون حالياً. أضف بضائع في قسم المشتريات لمتابعة المخزون.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Low Stock Alert Control & Configuration Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    modifier = Modifier.fillMaxWidth().testTag("low_stock_alert_control_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GoldDark.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = GoldLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "نظام التنبيهات لنواقص المخزون",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "إشعار فوري عند وصول رصيد أي منتج لمستوى منخفض",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Threshold selector chips
                        Text(
                            text = "تحديد مستوى التنبيه (الحد الأدنى للكمية):",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 3, 5).forEach { thresholdValue ->
                                val isSelected = lowStockThreshold == thresholdValue
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetThreshold(thresholdValue) },
                                    label = {
                                        Text(
                                            text = "$thresholdValue ${if (thresholdValue == 1) "قطعة" else if (thresholdValue == 2) "قطعتان" else "قطع"}",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldDark,
                                        selectedLabelColor = Color.White,
                                        containerColor = Slate800,
                                        labelColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Test / Send Alert Button
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val isGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (isGranted) {
                                        onSendAlerts()
                                    } else {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    onSendAlerts()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("send_stock_alert_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص المخزون وإرسال إشعار النواقص الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Low stock warning banner if any items match
                        if (lowStockItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "يوجد ${lowStockItems.size} أصناف وصلت إلى الحد الأدنى ($lowStockThreshold أو أقل) وتتطلب إعادة طلب من التاجر.",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قائمة الأصناف بالمخزن (${inventory.size} صنف)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("حد التنبيه الحالي: $lowStockThreshold قطع", fontSize = 11.5.sp, color = GoldDark, fontWeight = FontWeight.Bold)
                }
            }

            items(inventory, key = { it.productName }) { item ->
                val isBelowThreshold = item.remainingStock <= lowStockThreshold

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (isBelowThreshold) androidx.compose.foundation.BorderStroke(1.dp, if (item.remainingStock <= 0) MaterialTheme.colorScheme.error else GoldLight) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(item.category, style = MaterialTheme.typography.labelSmall, color = GoldDark)
                            }

                            // Stock Status Badge based on configured lowStockThreshold
                            val statusColor = when {
                                item.remainingStock > lowStockThreshold -> EmeraldSuccess
                                item.remainingStock in 1..lowStockThreshold -> GoldLight
                                else -> MaterialTheme.colorScheme.error
                            }
                            val statusText = when {
                                item.remainingStock > lowStockThreshold -> "متوفر بالمخزن"
                                item.remainingStock in 1..lowStockThreshold -> "كمية منخفضة (تنبيه)"
                                else -> "نفد من المخزن!"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (isBelowThreshold) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("إجمالي المشتريات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${item.totalPurchasedQty} قطعة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("إجمالي المبيعات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${item.totalSoldQty} مباع", fontWeight = FontWeight.Bold, color = EmeraldSuccess, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("المتبقي في المخزون", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${item.remainingStock} قطعة",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isBelowThreshold) MaterialTheme.colorScheme.error else GoldDark,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersSection(
    reminders: List<InstallmentReminderEntity>,
    customers: List<com.example.data.model.CustomerWithDetails>,
    onToggleReminder: (InstallmentReminderEntity) -> Unit,
    onDeleteReminder: (InstallmentReminderEntity) -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current

    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoldDark, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("لا توجد مواعيد أقساط مسجلة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("سجل مواعيد استحقاق أقساط الزبائن لمتابعتها وتذكيرهم هاتفياً", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = GoldDark)) {
                    Text("إضافة موعد قسط الآن")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reminders, key = { it.id }) { reminder ->
                val customer = customers.find { it.customer.id == reminder.customerId }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (reminder.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { onToggleReminder(reminder) }) {
                                Icon(
                                    imageVector = if (reminder.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "تم السداد",
                                    tint = if (reminder.isPaid) EmeraldSuccess else GoldDark
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column {
                                Text(
                                    text = reminder.customerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تاريخ الاستحقاق: ${reminder.dueDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "المبلغ المتوقع: ${UiHelpers.formatMoney(reminder.expectedAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                                if (!reminder.notes.isNullOrBlank()) {
                                    Text(reminder.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Quick Call / WhatsApp / Delete
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (customer != null) {
                                IconButton(
                                    onClick = { UiHelpers.dialPhoneNumber(context, customer.customer.phone) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "اتصال", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val msg = "مرحباً يا فندم، نود تذكيرك بموعد استحقاق قسط hamza® بتاريخ ${reminder.dueDate} وقيمته ${UiHelpers.formatMoney(reminder.expectedAmount)}"
                                        UiHelpers.openWhatsApp(context, customer.customer.phone, msg)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = { onDeleteReminder(reminder) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    customers: List<com.example.data.model.CustomerWithDetails>,
    onDismiss: () -> Unit,
    onConfirm: (customerId: Long, customerName: String, dueDate: String, amount: Double, notes: String?) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(UiHelpers.getTodayDate()) }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("إضافة تذكير بموعد قسط", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldDark)
                Spacer(modifier = Modifier.height(12.dp))

                // Customer Selection Dropdown
                if (customers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.customer?.name ?: "اختر الزبون",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الزبون *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.customer.name) },
                                    onClick = {
                                        selectedCustomer = c
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("لا يوجد زبائن مسجلين. سجل زبوناً أولاً.", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("تاريخ الاستحقاق (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ القسط المتوقع (ج.م) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            selectedCustomer?.let { c ->
                                val amt = amountStr.toDoubleOrNull() ?: 0.0
                                onConfirm(c.customer.id, c.customer.name, dueDate, amt, notes)
                            }
                        },
                        enabled = selectedCustomer != null && (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark)
                    ) {
                        Text("حفظ التذكير")
                    }
                }
            }
        }
    }
}
