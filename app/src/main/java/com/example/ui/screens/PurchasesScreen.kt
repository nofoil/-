package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.MerchantEntity
import com.example.data.model.MerchantProductEntity
import com.example.data.model.MerchantWithProducts
import com.example.ui.screens.components.CategoryInputField
import com.example.ui.screens.components.ImagePickerField
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.SalesViewModel
import com.example.util.UiHelpers

data class DraftProduct(
    val name: String = "",
    val category: String = "أجهزة كهربائية",
    val quantity: String = "1",
    val totalPrice: String = "",
    val photoUri: String? = null
) {
    val unitPrice: Double
        get() {
            val q = quantity.toIntOrNull() ?: 1
            val t = totalPrice.toDoubleOrNull() ?: 0.0
            return if (q > 0) t / q else 0.0
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: SalesViewModel,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val merchants by viewModel.merchants.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var merchantToEdit by remember { mutableStateOf<MerchantWithProducts?>(null) }
    var merchantToAddProduct by remember { mutableStateOf<MerchantWithProducts?>(null) }
    var merchantToDelete by remember { mutableStateOf<MerchantEntity?>(null) }

    val filteredMerchants = remember(merchants, searchQuery) {
        if (searchQuery.isBlank()) merchants
        else {
            val q = searchQuery.trim().lowercase()
            merchants.filter { mwp ->
                mwp.merchant.name.lowercase().contains(q) ||
                mwp.merchant.phone.contains(q) ||
                (mwp.merchant.notes ?: "").lowercase().contains(q) ||
                mwp.products.any { it.productName.lowercase().contains(q) || it.category.lowercase().contains(q) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (filteredMerchants.isEmpty()) {
            EmptyPurchasesState(
                isSearch = searchQuery.isNotBlank(),
                onAddClick = { showAddDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary KPI Bar
                item {
                    PurchasesSummaryKpi(merchants = merchants)
                }

                items(filteredMerchants, key = { it.merchant.id }) { merchantWithProducts ->
                    MerchantCard(
                        merchantWithProducts = merchantWithProducts,
                        onCallClick = { UiHelpers.dialPhoneNumber(context, merchantWithProducts.merchant.phone) },
                        onEditClick = { merchantToEdit = merchantWithProducts },
                        onDeleteClick = { merchantToDelete = merchantWithProducts.merchant },
                        onAddProductClick = { merchantToAddProduct = merchantWithProducts },
                        onDeleteProduct = { prodId -> viewModel.deleteMerchantProduct(prodId) }
                    )
                }
            }
        }

        // Add Merchant FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = GoldDark,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_merchant_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة مشتريات / تاجر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Dialog: Add New Merchant
    if (showAddDialog) {
        AddMerchantDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, purchaseDate, paid, payDate, notes, photoUri, products ->
                viewModel.addMerchant(
                    name = name,
                    phone = phone,
                    purchaseDate = purchaseDate,
                    paidAmount = paid,
                    paymentDate = payDate,
                    notes = notes,
                    photoUri = photoUri,
                    products = products
                )
                showAddDialog = false
            }
        )
    }

    // Dialog: Edit Merchant Details
    merchantToEdit?.let { mwp ->
        EditMerchantDialog(
            merchantWithProducts = mwp,
            onDismiss = { merchantToEdit = null },
            onConfirm = { updated ->
                viewModel.updateMerchant(updated)
                merchantToEdit = null
            }
        )
    }

    // Dialog: Add Another Product to Existing Merchant
    merchantToAddProduct?.let { mwp ->
        AddProductToMerchantDialog(
            merchantName = mwp.merchant.name,
            onDismiss = { merchantToAddProduct = null },
            onConfirm = { prodName, cat, qty, total, photoUri ->
                val unit = if (qty > 0) total / qty else 0.0
                viewModel.addProductToMerchant(
                    MerchantProductEntity(
                        merchantId = mwp.merchant.id,
                        productName = prodName,
                        category = cat,
                        quantity = qty,
                        totalPrice = total,
                        unitPrice = unit,
                        photoUri = photoUri
                    )
                )
                merchantToAddProduct = null
            }
        )
    }

    // Confirm Delete Dialog
    merchantToDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { merchantToDelete = null },
            title = { Text("حذف التاجر ومشترياته") },
            text = { Text("هل أنت متأكد من حذف التاجر \"${m.name}\" وجميع المنتجات المرتبطة به؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMerchant(m)
                        merchantToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { merchantToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun PurchasesSummaryKpi(merchants: List<MerchantWithProducts>) {
    val totalPurchases = merchants.sumOf { it.totalPurchaseCost }
    val totalPaid = merchants.sumOf { it.merchant.paidAmount }
    val totalRemaining = merchants.sumOf { it.remainingAmount }
    val totalProducts = merchants.sumOf { it.totalProductCount }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ملخص قسم المشتريات والتجار",
                    style = MaterialTheme.typography.titleSmall,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = GoldDark.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$totalProducts قطعة بالمخزن",
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KpiColumn(label = "إجمالي الفواتير", value = UiHelpers.formatMoney(totalPurchases), color = Color.White)
                KpiColumn(label = "ما تم دفعه", value = UiHelpers.formatMoney(totalPaid), color = EmeraldSuccess)
                KpiColumn(label = "المتبقي للموردين", value = UiHelpers.formatMoney(totalRemaining), color = if (totalRemaining > 0) Color(0xFFF87171) else Color.White)
            }
        }
    }
}

@Composable
private fun KpiColumn(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun MerchantCard(
    merchantWithProducts: MerchantWithProducts,
    onCallClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onDeleteProduct: (Long) -> Unit
) {
    val merchant = merchantWithProducts.merchant
    val products = merchantWithProducts.products
    val totalCost = merchantWithProducts.totalPurchaseCost
    val remaining = merchantWithProducts.remainingAmount
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("merchant_card_${merchant.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Merchant Header: Avatar, Name, Phone & Action buttons
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
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!merchant.photoUri.isNullOrBlank()) {
                            if (merchant.photoUri == "preset_founder") {
                                Image(
                                    painter = painterResource(id = R.drawable.hamza_founder),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (merchant.photoUri == "preset_logo") {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_hamza),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(merchant.photoUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = GoldDark,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = merchant.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Phone with direct Call button as required
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onCallClick() }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "اتصال بالتاجر",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = merchant.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Edit & Delete
                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = GoldDark, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Dates & Financial Figures
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("تاريخ الشراء: ${merchant.purchaseDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("تاريخ الدفع: ${merchant.paymentDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("إجمالي الفاتورة: ${UiHelpers.formatMoney(totalCost)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("ما تم دفعه: ${UiHelpers.formatMoney(merchant.paidAmount)}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                    Text(
                        text = "المتبقي: ${UiHelpers.formatMoney(remaining)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (remaining > 0) MaterialTheme.colorScheme.error else EmeraldSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Products section for this merchant (allows multiple products per merchant)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المنتجات الموردة (${products.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldDark
                        )
                        OutlinedButton(
                            onClick = onAddProductClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة منتج آخر", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (products.isEmpty()) {
                        Text(
                            text = "لا توجد منتجات مسجلة لهذا التاجر بعد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        products.forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Product Photo thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!prod.photoUri.isNullOrBlank()) {
                                            if (prod.photoUri == "preset_logo") {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_app_hamza),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(prod.photoUri)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = GoldDark,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            text = prod.productName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = prod.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldDark
                                            )
                                            Text(
                                                text = "• الكمية: ${prod.quantity}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "• القطعة: ${UiHelpers.formatMoney(prod.unitPrice)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = UiHelpers.formatMoney(prod.totalPrice),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { onDeleteProduct(prod.id) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "حذف الصنف",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        }
                    }
                }
            }

            // Notes under merchant as requested
            if (!merchant.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = merchant.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMerchantDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        phone: String,
        purchaseDate: String,
        paid: Double,
        payDate: String,
        notes: String?,
        photoUri: String?,
        products: List<MerchantProductEntity>
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(UiHelpers.getTodayDate()) }
    var paidAmountStr by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(UiHelpers.getTodayDate()) }
    var notes by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }

    // Draft Products list for this merchant (allows multiple products)
    val products = remember {
        mutableStateListOf(
            DraftProduct(name = "", category = "أجهزة كهربائية", quantity = "1", totalPrice = "")
        )
    }

    val totalCost by remember {
        derivedStateOf {
            products.sumOf { it.totalPrice.toDoubleOrNull() ?: 0.0 }
        }
    }

    val remainingAmount by remember {
        derivedStateOf {
            val paid = paidAmountStr.toDoubleOrNull() ?: 0.0
            (totalCost - paid).coerceAtLeast(0.0)
        }
    }

    val isFormValid by remember {
        derivedStateOf {
            name.isNotBlank() && phone.isNotBlank() &&
            products.isNotEmpty() && products.all { it.name.isNotBlank() && (it.totalPrice.toDoubleOrNull() ?: 0.0) > 0 }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "إضافة تاجر ومشتريات جديدة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldDark
                )
                Text(
                    text = "أدخل بيانات التاجر وفاتورة المنتجات المشتراة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Merchant Photo
                ImagePickerField(
                    label = "صورة التاجر",
                    selectedUri = photoUri,
                    onImageSelected = { photoUri = it },
                    isAvatar = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم التاجر *") },
                    placeholder = { Text("مثال: شركة النور للأجهزة") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldDark) },
                    modifier = Modifier.fillMaxWidth().testTag("merchant_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف التاجر *") },
                    placeholder = { Text("مثال: 01012345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldSuccess) },
                    modifier = Modifier.fillMaxWidth().testTag("merchant_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchaseDate,
                        onValueChange = { purchaseDate = it },
                        label = { Text("تاريخ الشراء") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text("تاريخ الدفع") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Products Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "منتجات الفاتورة (أكثر من منتج)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldDark
                    )
                    TextButton(
                        onClick = {
                            products.add(DraftProduct(category = "أجهزة كهربائية", quantity = "1"))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة منتج إضافي")
                    }
                }

                products.forEachIndexed { index, item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المنتج #${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = GoldDark
                                )
                                if (products.size > 1) {
                                    IconButton(
                                        onClick = { products.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "إزالة", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { products[index] = item.copy(name = it) },
                                label = { Text("اسم المنتج *") },
                                placeholder = { Text("مثال: طقم حلل جرانيت 10 قطع") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            CategoryInputField(
                                category = item.category,
                                onCategoryChange = { products[index] = item.copy(category = it) },
                                label = "تصنيف المنتج (كتابة حرة)"
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            ImagePickerField(
                                label = "صورة المنتج",
                                selectedUri = item.photoUri,
                                onImageSelected = { products[index] = item.copy(photoUri = it) },
                                isRequired = false
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.quantity,
                                    onValueChange = { products[index] = item.copy(quantity = it) },
                                    label = { Text("عدد القطع *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.totalPrice,
                                    onValueChange = { products[index] = item.copy(totalPrice = it) },
                                    label = { Text("إجمالي السعر *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            // Automatic unit price calculation
                            Text(
                                text = "سعر القطعة المحسوب تلقائياً: ${UiHelpers.formatMoney(item.unitPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Financial Summary of this purchase
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي الفاتورة:", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(UiHelpers.formatMoney(totalCost), color = GoldLight, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = paidAmountStr,
                            onValueChange = { paidAmountStr = it },
                            label = { Text("ما تم دفعه للتاجر (ج.م) *") },
                            placeholder = { Text("مثال: 5000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المتبقي للتاجر:", color = Color.White)
                            Text(
                                UiHelpers.formatMoney(remainingAmount),
                                color = if (remainingAmount > 0) Color(0xFFF87171) else EmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات أسفل التاجر (اختياري)") },
                    placeholder = { Text("أي شروط دفع أو اتفاقيات خاصة...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val paid = paidAmountStr.toDoubleOrNull() ?: 0.0
                            val entities = products.map {
                                val q = it.quantity.toIntOrNull() ?: 1
                                val t = it.totalPrice.toDoubleOrNull() ?: 0.0
                                MerchantProductEntity(
                                    merchantId = 0,
                                    productName = it.name.trim(),
                                    category = it.category.trim(),
                                    quantity = q,
                                    totalPrice = t,
                                    unitPrice = if (q > 0) t / q else 0.0,
                                    photoUri = it.photoUri
                                )
                            }
                            onConfirm(name, phone, purchaseDate, paid, paymentDate, notes, photoUri, entities)
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                        modifier = Modifier.testTag("submit_merchant_button")
                    ) {
                        Text("حفظ التاجر والمشتريات")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMerchantDialog(
    merchantWithProducts: MerchantWithProducts,
    onDismiss: () -> Unit,
    onConfirm: (MerchantEntity) -> Unit
) {
    val merchant = merchantWithProducts.merchant
    var name by remember { mutableStateOf(merchant.name) }
    var phone by remember { mutableStateOf(merchant.phone) }
    var purchaseDate by remember { mutableStateOf(merchant.purchaseDate) }
    var paidAmountStr by remember { mutableStateOf(merchant.paidAmount.toString()) }
    var paymentDate by remember { mutableStateOf(merchant.paymentDate) }
    var notes by remember { mutableStateOf(merchant.notes ?: "") }
    var photoUri by remember { mutableStateOf(merchant.photoUri) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("تعديل بيانات التاجر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldDark)
                Spacer(modifier = Modifier.height(10.dp))

                ImagePickerField(label = "صورة التاجر", selectedUri = photoUri, onImageSelected = { photoUri = it }, isAvatar = true)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم التاجر") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = purchaseDate, onValueChange = { purchaseDate = it }, label = { Text("تاريخ الشراء") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = paidAmountStr, onValueChange = { paidAmountStr = it }, label = { Text("ما تم دفعه") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = paymentDate, onValueChange = { paymentDate = it }, label = { Text("تاريخ الدفع") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val paid = paidAmountStr.toDoubleOrNull() ?: merchant.paidAmount
                            onConfirm(
                                merchant.copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    purchaseDate = purchaseDate.trim(),
                                    paidAmount = paid,
                                    paymentDate = paymentDate.trim(),
                                    notes = notes.trim(),
                                    photoUri = photoUri
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark)
                    ) {
                        Text("حفظ التعديلات")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProductToMerchantDialog(
    merchantName: String,
    onDismiss: () -> Unit,
    onConfirm: (productName: String, category: String, quantity: Int, totalPrice: Double, photoUri: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("أجهزة كهربائية") }
    var quantityStr by remember { mutableStateOf("1") }
    var totalPriceStr by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }

    val unitPrice by remember {
        derivedStateOf {
            val q = quantityStr.toIntOrNull() ?: 1
            val t = totalPriceStr.toDoubleOrNull() ?: 0.0
            if (q > 0) t / q else 0.0
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("إضافة منتج لـ \"$merchantName\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldDark)
                Spacer(modifier = Modifier.height(10.dp))

                ImagePickerField(
                    label = "صورة المنتج",
                    selectedUri = photoUri,
                    onImageSelected = { photoUri = it },
                    isRequired = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                CategoryInputField(category = category, onCategoryChange = { category = it })
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = quantityStr, onValueChange = { quantityStr = it }, label = { Text("الكمية") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = totalPriceStr, onValueChange = { totalPriceStr = it }, label = { Text("إجمالي السعر") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1.2f))
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("سعر القطعة التلقائي: ${UiHelpers.formatMoney(unitPrice)}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val q = quantityStr.toIntOrNull() ?: 1
                            val t = totalPriceStr.toDoubleOrNull() ?: 0.0
                            onConfirm(name, category, q, t, photoUri)
                        },
                        enabled = name.isNotBlank() && (totalPriceStr.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark)
                    ) {
                        Text("إضافة المنتج")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPurchasesState(isSearch: Boolean, onAddClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = GoldDark.copy(alpha = 0.15f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = GoldDark,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearch) "لا توجد نتائج مطابقة لبحثك في المشتريات" else "لا توجد مشتريات أو تجار مسجلين بعد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearch) "جرب البحث بكلمة أخرى أو اسم منتج مختلف" else "ابدأ بتسجيل أول تاجر وفاتورة بضائع (أجهزة أو أدوات مائدة)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isSearch) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldDark)
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة تاجر ومشتريات الآن")
                }
            }
        }
    }
}
