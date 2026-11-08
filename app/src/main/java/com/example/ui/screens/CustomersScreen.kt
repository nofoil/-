package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerProductEntity
import com.example.data.model.CustomerWithDetails
import com.example.data.model.InstallmentEntity
import com.example.ui.screens.components.CategoryInputField
import com.example.ui.screens.components.ImagePickerField
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.SalesViewModel
import com.example.util.UiHelpers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: SalesViewModel,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToAddProduct by remember { mutableStateOf<CustomerWithDetails?>(null) }
    var customerToAddInstallment by remember { mutableStateOf<CustomerWithDetails?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            val q = searchQuery.trim().lowercase()
            customers.filter { cwd ->
                cwd.customer.name.lowercase().contains(q) ||
                cwd.customer.phone.contains(q) ||
                cwd.customer.nationalId.contains(q) ||
                cwd.customer.address.lowercase().contains(q) ||
                cwd.products.any { it.productName.lowercase().contains(q) || it.category.lowercase().contains(q) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (filteredCustomers.isEmpty()) {
            EmptyCustomersState(
                isSearch = searchQuery.isNotBlank(),
                onAddClick = { showAddCustomerDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Customers Summary KPI
                item {
                    CustomersSummaryKpi(customers = customers)
                }

                items(filteredCustomers, key = { it.customer.id }) { customerWithDetails ->
                    CustomerCard(
                        customerWithDetails = customerWithDetails,
                        onCallClick = { UiHelpers.dialPhoneNumber(context, customerWithDetails.customer.phone) },
                        onWhatsAppClick = {
                            val msg = "مرحباً يا فندم، بخصوص مشترياتك من hamza®، المتبقي في حسابك: ${UiHelpers.formatMoney(customerWithDetails.totalRemainingDebt)}"
                            UiHelpers.openWhatsApp(context, customerWithDetails.customer.phone, msg)
                        },
                        onOpenMapsClick = { loc -> UiHelpers.openMaps(context, loc) },
                        onEditClick = { customerToEdit = customerWithDetails.customer },
                        onDeleteClick = { customerToDelete = customerWithDetails.customer },
                        onAddProductClick = { customerToAddProduct = customerWithDetails },
                        onAddInstallmentClick = { customerToAddInstallment = customerWithDetails },
                        onDeleteProduct = { prodId -> viewModel.deleteCustomerProduct(prodId) },
                        onDeleteInstallment = { inst -> viewModel.deleteInstallment(inst) }
                    )
                }
            }
        }

        // Add Customer FAB
        FloatingActionButton(
            onClick = { showAddCustomerDialog = true },
            containerColor = GoldDark,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_customer_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة زبون جديد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Dialog: Add Customer (Mandatory validation enforced, except notes & GPS)
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, nationalId, address, gps, phone, photoUri, notes, prodName, category, price, paid, date, prodPhoto ->
                viewModel.addCustomer(
                    name = name,
                    nationalId = nationalId,
                    address = address,
                    gpsLocation = gps,
                    phone = phone,
                    photoUri = photoUri,
                    notes = notes,
                    productName = prodName,
                    productCategory = category,
                    productPrice = price,
                    paidAmount = paid,
                    saleDate = date,
                    productPhotoUri = prodPhoto
                )
                showAddCustomerDialog = false
            }
        )
    }

    // Dialog: Edit Customer
    customerToEdit?.let { customer ->
        EditCustomerDialog(
            customer = customer,
            onDismiss = { customerToEdit = null },
            onConfirm = { updated ->
                viewModel.updateCustomer(updated)
                customerToEdit = null
            }
        )
    }

    // Dialog: Add Another Product to Customer
    customerToAddProduct?.let { cwd ->
        AddProductToCustomerDialog(
            customerName = cwd.customer.name,
            onDismiss = { customerToAddProduct = null },
            onConfirm = { prodName, category, price, paid, date, prodPhoto ->
                viewModel.addProductToCustomer(
                    customerId = cwd.customer.id,
                    productName = prodName,
                    category = category,
                    price = price,
                    paidAmount = paid,
                    saleDate = date,
                    photoUri = prodPhoto
                )
                customerToAddProduct = null
            }
        )
    }

    // Dialog: Add Installment to Customer
    customerToAddInstallment?.let { cwd ->
        AddInstallmentDialog(
            customerWithDetails = cwd,
            onDismiss = { customerToAddInstallment = null },
            onConfirm = { amount, date, remaining, notes ->
                viewModel.addInstallment(
                    customerId = cwd.customer.id,
                    amount = amount,
                    paymentDate = date,
                    remainingAfterPayment = remaining,
                    notes = notes
                )
                customerToAddInstallment = null
            }
        )
    }

    // Confirm Delete Customer Dialog
    customerToDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("حذف الزبون") },
            text = { Text("هل أنت متأكد من حذف الزبون \"${cust.name}\" وجميع منتجاته وأقساطه؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun CustomersSummaryKpi(customers: List<CustomerWithDetails>) {
    val totalSales = customers.sumOf { it.totalPurchasedAmount }
    val totalCollected = customers.sumOf { it.totalPaidOverall }
    val totalRemainingDebt = customers.sumOf { it.totalRemainingDebt }

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
                    text = "ملخص الزبائن والمبيعات والأقساط",
                    style = MaterialTheme.typography.titleSmall,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = GoldDark.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${customers.size} عميل",
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
                Column {
                    Text("إجمالي المبيعات", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text(UiHelpers.formatMoney(totalSales), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text("المحصل الكلي", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text(UiHelpers.formatMoney(totalCollected), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
                Column {
                    Text("المتبقي ديون بالخارج", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text(
                        UiHelpers.formatMoney(totalRemainingDebt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalRemainingDebt > 0) Color(0xFFF87171) else EmeraldSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(
    customerWithDetails: CustomerWithDetails,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onOpenMapsClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onAddInstallmentClick: () -> Unit,
    onDeleteProduct: (Long) -> Unit,
    onDeleteInstallment: (InstallmentEntity) -> Unit
) {
    val customer = customerWithDetails.customer
    val products = customerWithDetails.products
    val installments = customerWithDetails.installments
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Customer Avatar, Name, National ID, Phone & Actions
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
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!customer.photoUri.isNullOrBlank()) {
                            if (customer.photoUri == "preset_founder") {
                                Image(
                                    painter = painterResource(id = R.drawable.hamza_founder),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (customer.photoUri == "preset_logo") {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_hamza),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(customer.photoUri)
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
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "الرقم القومي: ${customer.nationalId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Edit Customer Button & Delete Customer
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(34.dp).testTag("edit_customer_${customer.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل بيانات الزبون", tint = GoldDark, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف الزبون", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Phone & Direct Contact Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onCallClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onWhatsAppClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Address & Maps
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!customer.gpsLocation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { onOpenMapsClick(customer.gpsLocation) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(13.dp), tint = GoldDark)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("الخرائط", fontSize = 10.5.sp, color = GoldDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Products Section for this Customer (with photos!)
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
                            text = "المنتجات المشتراة (${products.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldDark
                        )
                        // Button: Add Another Product to this customer
                        OutlinedButton(
                            onClick = onAddProductClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp).testTag("add_another_product_btn_${customer.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("إضافة منتج آخر", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    products.forEach { prod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product Photo Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!prod.productPhotoUri.isNullOrBlank()) {
                                        if (prod.productPhotoUri == "preset_logo") {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_app_hamza),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(prod.productPhotoUri)
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
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = prod.productName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(prod.category, style = MaterialTheme.typography.labelSmall, color = GoldDark)
                                        Text("• التاريخ: ${prod.saleDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("السعر: ${UiHelpers.formatMoney(prod.productPrice)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text("مقدم: ${UiHelpers.formatMoney(prod.initialPaidAmount)}", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                    }
                                }
                            }

                            if (products.size > 1) {
                                IconButton(
                                    onClick = { onDeleteProduct(prod.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف المنتج",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary for Customer
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إجمالي المشتريات", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(UiHelpers.formatMoney(customerWithDetails.totalPurchasedAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("إجمالي المسدد", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(UiHelpers.formatMoney(customerWithDetails.totalPaidOverall), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                    Column {
                        Text("المبلغ المتبقي", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(
                            UiHelpers.formatMoney(customerWithDetails.totalRemainingDebt),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (customerWithDetails.totalRemainingDebt > 0) Color(0xFFF87171) else EmeraldSuccess
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section: Installments Paid (الأقساط المدفوعة)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الأقساط المسددة (${installments.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }

                        // Button to add installment
                        Button(
                            onClick = onAddInstallmentClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.height(28.dp).testTag("add_installment_btn_${customer.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("إضافة قسط", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (installments.isEmpty()) {
                        Text(
                            text = "لم يتم تسجيل أي أقساط مدفوعة لهذا الزبون حتى الآن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        installments.forEach { inst ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "قسط: ${UiHelpers.formatMoney(inst.amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("تاريخ السداد: ${inst.paymentDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("المتبقي بعده: ${UiHelpers.formatMoney(inst.remainingAfterPayment)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (!inst.notes.isNullOrBlank()) {
                                        Text(inst.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteInstallment(inst) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف القسط",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            }

            // Customer Notes (if any)
            if (!customer.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customer.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        nationalId: String,
        address: String,
        gps: String?,
        phone: String,
        photoUri: String?,
        notes: String?,
        productName: String,
        productCategory: String,
        productPrice: Double,
        paid: Double,
        saleDate: String,
        productPhoto: String?
    ) -> Unit
) {
    val context = LocalContext.current

    // Form fields
    var name by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gpsLocation by remember { mutableStateOf("") }
    var customerPhotoUri by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("أجهزة كهربائية") }
    var productPhotoUri by remember { mutableStateOf<String?>(null) }
    var productPriceStr by remember { mutableStateOf("") }
    var paidAmountStr by remember { mutableStateOf("") }
    var saleDate by remember { mutableStateOf(UiHelpers.getTodayDate()) }

    val remainingAmount by remember {
        derivedStateOf {
            val price = productPriceStr.toDoubleOrNull() ?: 0.0
            val paid = paidAmountStr.toDoubleOrNull() ?: 0.0
            (price - paid).coerceAtLeast(0.0)
        }
    }

    // MANDATORY VALIDATION:
    // All fields are mandatory EXCEPT notes and GPS address as requested:
    // "جعل حقول انشاء الزبون ضرورية لانشاء الزبون ولا يمكن اتمامه الا بعد ملئها كلها"
    // "لا تجعل ملئ حقل ملاحظات ضروريا عند انشاء زبون جديد"
    // "لا تجعل ملئ حقل تحديد موقعي الحالي (GPS) أو ألصق رابط الموقع من خرائط جوجل / الإحداثيات ضروريا"
    val isFormComplete by remember {
        derivedStateOf {
            name.isNotBlank() &&
            nationalId.isNotBlank() &&
            phone.isNotBlank() &&
            address.isNotBlank() &&
            !customerPhotoUri.isNullOrBlank() &&
            productName.isNotBlank() &&
            category.isNotBlank() &&
            !productPhotoUri.isNullOrBlank() &&
            (productPriceStr.toDoubleOrNull() ?: 0.0) > 0 &&
            paidAmountStr.isNotBlank() &&
            saleDate.isNotBlank()
        }
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            UiHelpers.fetchCurrentLocation(context) { loc -> gpsLocation = loc }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "تسجيل زبون ومبيعات جديدة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldDark
                )
                Text(
                    text = "جميع الحقول مميزة بـ (*) إلزامية لحفظ الزبون (الملاحظات والـ GPS اختيارية)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Customer Photo (Mandatory)
                ImagePickerField(
                    label = "صورة العميل",
                    selectedUri = customerPhotoUri,
                    onImageSelected = { customerPhotoUri = it },
                    isRequired = true,
                    isAvatar = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Customer Name (Mandatory)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الزبون *") },
                    placeholder = { Text("الاسم ثلاثي أو رباعي") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldDark) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. National ID (Mandatory)
                OutlinedTextField(
                    value = nationalId,
                    onValueChange = { nationalId = it },
                    label = { Text("رقم الهوية / الرقم القومي *") },
                    placeholder = { Text("14 رقماً") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = GoldDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_national_id_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Phone Number (Mandatory)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف *") },
                    placeholder = { Text("مثال: 010xxxxxxxx") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldSuccess) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Address Written (Mandatory)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان بالتفصيل *") },
                    placeholder = { Text("المدينة، الحي، اسم الشارع، ورقم العقار") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = GoldDark) },
                    modifier = Modifier.fillMaxWidth().testTag("customer_address_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. GPS / Google Maps (OPTIONAL as explicitly requested)
                OutlinedTextField(
                    value = gpsLocation,
                    onValueChange = { gpsLocation = it },
                    label = { Text("تحديد موقعي GPS أو رابط خرائط جوجل (اختياري)") },
                    placeholder = { Text("ألصق رابط الموقع أو اضغط زر GPS") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    UiHelpers.fetchCurrentLocation(context) { loc -> gpsLocation = loc }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "تحديد موقعي الحالي", tint = GoldDark)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("customer_gps_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "بيانات المنتج المشتراة للزبون",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 7. Product Photo (Mandatory as requested)
                ImagePickerField(
                    label = "صورة المنتج",
                    selectedUri = productPhotoUri,
                    onImageSelected = { productPhotoUri = it },
                    isRequired = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 8. Product Name (Mandatory)
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("اسم المنتج *") },
                    placeholder = { Text("مثال: طقم صيني 66 قطعة أو فرن كهربائي") },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = GoldDark) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_product_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 9. Category (Mandatory, free-form typing allowed without restriction)
                CategoryInputField(
                    category = category,
                    onCategoryChange = { category = it },
                    label = "تصنيف المنتج (كتابة حرة)",
                    isRequired = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 10. Financials & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = productPriceStr,
                        onValueChange = { productPriceStr = it },
                        label = { Text("مبلغ المنتج (ج.م) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("customer_price_input")
                    )
                    OutlinedTextField(
                        value = paidAmountStr,
                        onValueChange = { paidAmountStr = it },
                        label = { Text("ما تم دفعه (ج.م) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("customer_paid_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = saleDate,
                    onValueChange = { saleDate = it },
                    label = { Text("التاريخ *") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Computed Remaining
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المبلغ المتبقي ديناً على الزبون:", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text(
                            UiHelpers.formatMoney(remainingAmount),
                            color = if (remainingAmount > 0) Color(0xFFF87171) else EmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 11. Notes (OPTIONAL as explicitly requested)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    placeholder = { Text("ملاحظات حول طريقة السداد أو مواعيد الأقساط...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mandatory fields warning if incomplete
                if (!isFormComplete) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = "يرجى استكمال جميع الحقول الإلزامية المطلوبة (الاسم، الهوية، الهاتف، العنوان، صورة العميل، المنتج، تصنيفه، صورته، السعر، والمدفوع)",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = productPriceStr.toDoubleOrNull() ?: 0.0
                            val paid = paidAmountStr.toDoubleOrNull() ?: 0.0
                            onConfirm(
                                name, nationalId, address, gpsLocation, phone, customerPhotoUri, notes,
                                productName, category, price, paid, saleDate, productPhotoUri
                            )
                        },
                        enabled = isFormComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                        modifier = Modifier.testTag("submit_customer_button")
                    ) {
                        Text("إنشاء الزبون والمنتج")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditCustomerDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onConfirm: (CustomerEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(customer.name) }
    var nationalId by remember { mutableStateOf(customer.nationalId) }
    var phone by remember { mutableStateOf(customer.phone) }
    var address by remember { mutableStateOf(customer.address) }
    var gpsLocation by remember { mutableStateOf(customer.gpsLocation ?: "") }
    var photoUri by remember { mutableStateOf(customer.photoUri) }
    var notes by remember { mutableStateOf(customer.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("تعديل بيانات الزبون", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldDark)
                Spacer(modifier = Modifier.height(10.dp))

                ImagePickerField(label = "صورة العميل", selectedUri = photoUri, onImageSelected = { photoUri = it }, isAvatar = true)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الزبون *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = nationalId, onValueChange = { nationalId = it }, label = { Text("الرقم القومي / الهوية *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = gpsLocation, onValueChange = { gpsLocation = it }, label = { Text("الموقع GPS أو الرابط") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                customer.copy(
                                    name = name.trim(),
                                    nationalId = nationalId.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    gpsLocation = gpsLocation.trim().ifBlank { null },
                                    photoUri = photoUri,
                                    notes = notes.trim().ifBlank { null }
                                )
                            )
                        },
                        enabled = name.isNotBlank() && phone.isNotBlank() && nationalId.isNotBlank(),
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
private fun AddProductToCustomerDialog(
    customerName: String,
    onDismiss: () -> Unit,
    onConfirm: (productName: String, category: String, price: Double, paid: Double, saleDate: String, photoUri: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("أجهزة كهربائية") }
    var priceStr by remember { mutableStateOf("") }
    var paidStr by remember { mutableStateOf("") }
    var saleDate by remember { mutableStateOf(UiHelpers.getTodayDate()) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    val remaining by remember {
        derivedStateOf {
            val p = priceStr.toDoubleOrNull() ?: 0.0
            val d = paidStr.toDoubleOrNull() ?: 0.0
            (p - d).coerceAtLeast(0.0)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("إضافة منتج آخر لـ \"$customerName\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldDark)
                Spacer(modifier = Modifier.height(10.dp))

                // Product image picker
                ImagePickerField(label = "صورة المنتج", selectedUri = photoUri, onImageSelected = { photoUri = it }, isRequired = false)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                CategoryInputField(category = category, onCategoryChange = { category = it })
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("السعر (ج.م)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = paidStr, onValueChange = { paidStr = it }, label = { Text("المقدم (ج.م)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("المتبقي: ${UiHelpers.formatMoney(remaining)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (remaining > 0) Color(0xFFF87171) else EmeraldSuccess)

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = saleDate, onValueChange = { saleDate = it }, label = { Text("تاريخ البيع") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = priceStr.toDoubleOrNull() ?: 0.0
                            val d = paidStr.toDoubleOrNull() ?: 0.0
                            onConfirm(name, category, p, d, saleDate, photoUri)
                        },
                        enabled = name.isNotBlank() && (priceStr.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark)
                    ) {
                        Text("إضافة المنتج للزبون")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddInstallmentDialog(
    customerWithDetails: CustomerWithDetails,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, paymentDate: String, remainingAfterPayment: Double, notes: String?) -> Unit
) {
    val currentRemaining = customerWithDetails.totalRemainingDebt
    var amountStr by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(UiHelpers.getTodayDate()) }
    var notes by remember { mutableStateOf("") }

    val remainingAfter by remember {
        derivedStateOf {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            (currentRemaining - amt).coerceAtLeast(0.0)
        }
    }

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
                Text(
                    text = "إضافة قسط جديد لـ \"${customerWithDetails.customer.name}\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المتبقي الحالي قبل السداد:", style = MaterialTheme.typography.bodySmall)
                        Text(UiHelpers.formatMoney(currentRemaining), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Installment Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ القسط المسدد (ج.م) *") },
                    placeholder = { Text("مثال: 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null, tint = EmeraldSuccess) },
                    modifier = Modifier.fillMaxWidth().testTag("installment_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Date
                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("تاريخ السداد *") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("installment_date_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Remaining after payment
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المبلغ المتبقي بعد السداد:", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text(
                            UiHelpers.formatMoney(remainingAfter),
                            color = if (remainingAfter > 0) GoldLight else EmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات القسط (اختياري)") },
                    placeholder = { Text("مثال: تم الاستلام نقداً / تحويل محفظة إلكترونية...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            onConfirm(amt, paymentDate, remainingAfter, notes)
                        },
                        enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier.testTag("submit_installment_btn")
                    ) {
                        Text("حفظ القسط")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCustomersState(isSearch: Boolean, onAddClick: () -> Unit) {
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
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = GoldDark,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearch) "لا توجد نتائج مطابقة لبحثك في الزبائن" else "لا يوجد زبائن مسجلين بعد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearch) "تأكد من كتابة الاسم أو رقم الهاتف بدقة" else "ابدأ بتسجيل أول زبون ومتابعة أقساطه ومبيعاته",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isSearch) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldDark)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة زبون جديد الآن")
                }
            }
        }
    }
}
