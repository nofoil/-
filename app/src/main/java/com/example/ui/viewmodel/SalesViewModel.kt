package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerProductEntity
import com.example.data.model.CustomerWithDetails
import com.example.data.model.InstallmentEntity
import com.example.data.model.InstallmentReminderEntity
import com.example.data.model.MerchantEntity
import com.example.data.model.MerchantProductEntity
import com.example.data.model.MerchantWithProducts
import com.example.data.repository.SalesRepository
import com.example.util.ExcelExporter
import com.example.util.StockNotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InventoryItem(
    val productName: String,
    val category: String,
    val totalPurchasedQty: Int,
    val totalSoldQty: Int,
    val remainingStock: Int,
    val averageBuyPrice: Double
)

data class MonthlySalesData(
    val yearMonth: String,
    val monthLabel: String,
    val totalSales: Double,
    val salesCount: Int,
    val totalCollected: Double,
    val growthPercentage: Double?
)

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SalesRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = SalesRepository(database.salesDao())
    }

    val merchants: StateFlow<List<MerchantWithProducts>> = repository.merchantsWithProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerWithDetails>> = repository.customersWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<InstallmentReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Sales evolution state
    val monthlySalesData: StateFlow<List<MonthlySalesData>> = customers.combine(repository.allCustomerProducts) { customerList, _ ->
        val monthList = mutableListOf<String>()
        val sdfYM = SimpleDateFormat("yyyy-MM", Locale.US)
        for (i in 5 downTo 0) {
            val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -i) }
            monthList.add(sdfYM.format(c.time))
        }

        val arabicMonths = mapOf(
            "01" to "يناير", "02" to "فبراير", "03" to "مارس", "04" to "أبريل",
            "05" to "مايو", "06" to "يونيو", "07" to "يوليو", "08" to "أغسطس",
            "09" to "سبتمبر", "10" to "أكتوبر", "11" to "نوفمبر", "12" to "ديسمبر"
        )

        var prevSales: Double? = null

        monthList.map { ym ->
            val mSuffix = ym.substringAfter("-")
            val label = arabicMonths[mSuffix] ?: mSuffix

            var salesSum = 0.0
            var salesCount = 0
            var initialPaidSum = 0.0
            var installmentsSum = 0.0

            customerList.forEach { cust ->
                cust.products.forEach { prod ->
                    if (prod.saleDate.startsWith(ym)) {
                        salesSum += prod.productPrice
                        salesCount += 1
                        initialPaidSum += prod.initialPaidAmount
                    }
                }
                cust.installments.forEach { inst ->
                    if (inst.paymentDate.startsWith(ym)) {
                        installmentsSum += inst.amount
                    }
                }
            }

            val totalCollected = initialPaidSum + installmentsSum

            val growth = if (prevSales != null && prevSales!! > 0) {
                ((salesSum - prevSales!!) / prevSales!!) * 100.0
            } else if (prevSales != null && salesSum > 0) {
                100.0
            } else null

            prevSales = salesSum

            MonthlySalesData(
                yearMonth = ym,
                monthLabel = label,
                totalSales = salesSum,
                salesCount = salesCount,
                totalCollected = totalCollected,
                growthPercentage = growth
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lowStockThreshold = MutableStateFlow(3)
    val lowStockThreshold: StateFlow<Int> = _lowStockThreshold.asStateFlow()

    fun setLowStockThreshold(threshold: Int) {
        _lowStockThreshold.value = threshold
    }

    private val _messageEvents = MutableSharedFlow<String>()
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Dynamic Inventory aggregation
    val inventory: StateFlow<List<InventoryItem>> = combine(
        repository.allMerchantProducts,
        repository.allCustomerProducts
    ) { merchantProds, customerProds ->
        val inventoryMap = mutableMapOf<String, InventoryItem>()

        // 1. Group bought products
        merchantProds.forEach { mp ->
            val key = mp.productName.trim().lowercase()
            val existing = inventoryMap[key]
            if (existing == null) {
                inventoryMap[key] = InventoryItem(
                    productName = mp.productName.trim(),
                    category = mp.category.ifBlank { "عام" },
                    totalPurchasedQty = mp.quantity,
                    totalSoldQty = 0,
                    remainingStock = mp.quantity,
                    averageBuyPrice = mp.unitPrice
                )
            } else {
                val newPurchased = existing.totalPurchasedQty + mp.quantity
                val avgPrice = if (newPurchased > 0) {
                    ((existing.averageBuyPrice * existing.totalPurchasedQty) + mp.totalPrice) / newPurchased
                } else existing.averageBuyPrice
                inventoryMap[key] = existing.copy(
                    totalPurchasedQty = newPurchased,
                    remainingStock = newPurchased - existing.totalSoldQty,
                    averageBuyPrice = avgPrice
                )
            }
        }

        // 2. Count sold products (each customer product sale counts as 1 or match)
        customerProds.forEach { cp ->
            val key = cp.productName.trim().lowercase()
            val existing = inventoryMap[key]
            if (existing != null) {
                val newSold = existing.totalSoldQty + 1
                inventoryMap[key] = existing.copy(
                    totalSoldQty = newSold,
                    remainingStock = (existing.totalPurchasedQty - newSold).coerceAtLeast(0)
                )
            } else {
                // If sold without prior merchant stock entry
                inventoryMap[key] = InventoryItem(
                    productName = cp.productName.trim(),
                    category = cp.category.ifBlank { "عام" },
                    totalPurchasedQty = 1,
                    totalSoldQty = 1,
                    remainingStock = 0,
                    averageBuyPrice = 0.0
                )
            }
        }

        inventoryMap.values.toList().sortedBy { it.remainingStock }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Merchant Actions ---
    fun addMerchant(
        name: String,
        phone: String,
        purchaseDate: String,
        paidAmount: Double,
        paymentDate: String,
        notes: String?,
        photoUri: String?,
        products: List<MerchantProductEntity>
    ) {
        viewModelScope.launch {
            val merchant = MerchantEntity(
                name = name.trim(),
                phone = phone.trim(),
                photoUri = photoUri,
                purchaseDate = purchaseDate.ifBlank { todayDate() },
                paidAmount = paidAmount,
                paymentDate = paymentDate.ifBlank { todayDate() },
                notes = notes?.trim()
            )
            repository.addMerchant(merchant, products)
            _messageEvents.emit("تم إضافة التاجر وبضائعه بنجاح")
        }
    }

    fun updateMerchant(merchant: MerchantEntity) {
        viewModelScope.launch {
            repository.updateMerchant(merchant)
            _messageEvents.emit("تم تحديث بيانات التاجر")
        }
    }

    fun deleteMerchant(merchant: MerchantEntity) {
        viewModelScope.launch {
            repository.deleteMerchant(merchant)
            _messageEvents.emit("تم حذف التاجر وسجلاته")
        }
    }

    fun addProductToMerchant(product: MerchantProductEntity) {
        viewModelScope.launch {
            repository.addProductToMerchant(product)
            _messageEvents.emit("تمت إضافة المنتج للتاجر")
        }
    }

    fun deleteMerchantProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteMerchantProduct(productId)
            _messageEvents.emit("تم حذف المنتج من التاجر")
        }
    }

    // --- Customer Actions ---
    fun addCustomer(
        name: String,
        nationalId: String,
        address: String,
        gpsLocation: String?,
        phone: String,
        photoUri: String?,
        notes: String?,
        productName: String,
        productCategory: String,
        productPrice: Double,
        paidAmount: Double,
        saleDate: String,
        productPhotoUri: String?
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                name = name.trim(),
                nationalId = nationalId.trim(),
                address = address.trim(),
                gpsLocation = gpsLocation?.trim()?.ifBlank { null },
                phone = phone.trim(),
                photoUri = photoUri,
                notes = notes?.trim()?.ifBlank { null }
            )
            val product = CustomerProductEntity(
                customerId = 0,
                productName = productName.trim(),
                category = productCategory.trim(),
                productPrice = productPrice,
                initialPaidAmount = paidAmount,
                saleDate = saleDate.ifBlank { todayDate() },
                productPhotoUri = productPhotoUri
            )
            repository.addCustomerWithProduct(customer, product)
            _messageEvents.emit("تم تسجيل الزبون والمنتج بنجاح")

            // Check if stock became low after this sale
            evaluateLowStockForProduct(productName.trim())
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            _messageEvents.emit("تم تحديث بيانات الزبون بنجاح")
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _messageEvents.emit("تم حذف الزبون وسجلاته")
        }
    }

    fun addProductToCustomer(
        customerId: Long,
        productName: String,
        category: String,
        price: Double,
        paidAmount: Double,
        saleDate: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val product = CustomerProductEntity(
                customerId = customerId,
                productName = productName.trim(),
                category = category.trim(),
                productPrice = price,
                initialPaidAmount = paidAmount,
                saleDate = saleDate.ifBlank { todayDate() },
                productPhotoUri = photoUri
            )
            repository.addProductToCustomer(product)
            _messageEvents.emit("تم إضافة منتج جديد للزبون")

            // Check if stock became low after this sale
            evaluateLowStockForProduct(productName.trim())
        }
    }

    private fun evaluateLowStockForProduct(productName: String) {
        val currentInventory = inventory.value
        val threshold = _lowStockThreshold.value
        val item = currentInventory.find { it.productName.equals(productName, ignoreCase = true) }
        // If remaining after this sale is <= threshold, notify!
        val remaining = (item?.remainingStock ?: 0) - 1
        if (remaining <= threshold) {
            StockNotificationHelper.sendLowStockNotification(
                context = getApplication<Application>().applicationContext,
                productName = productName,
                remainingStock = remaining.coerceAtLeast(0),
                threshold = threshold
            )
        }
    }

    fun checkAndSendLowStockAlerts(context: android.content.Context) {
        viewModelScope.launch {
            val currentInventory = inventory.value
            val threshold = _lowStockThreshold.value
            val lowItems = currentInventory.filter { it.remainingStock <= threshold }
            if (lowItems.isNotEmpty()) {
                StockNotificationHelper.sendMultipleLowStockAlert(
                    context = context,
                    lowStockItems = lowItems.map { it.productName to it.remainingStock },
                    threshold = threshold
                )
                _messageEvents.emit("تم إرسال إشعار بالنواقص (${lowItems.size} أصناف)")
            } else {
                _messageEvents.emit("جميع المنتجات متوفرة بكميات كافية أعلى من الحد الأدنى ($threshold)")
            }
        }
    }

    fun deleteCustomerProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteCustomerProduct(productId)
            _messageEvents.emit("تم حذف المنتج من حساب الزبون")
        }
    }

    // --- Installments ---
    fun addInstallment(
        customerId: Long,
        amount: Double,
        paymentDate: String,
        remainingAfterPayment: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            val installment = InstallmentEntity(
                customerId = customerId,
                amount = amount,
                paymentDate = paymentDate.ifBlank { todayDate() },
                remainingAfterPayment = remainingAfterPayment,
                notes = notes?.trim()?.ifBlank { null }
            )
            repository.addInstallment(installment)
            _messageEvents.emit("تم تسجيل القسط بنجاح")
        }
    }

    fun deleteInstallment(installment: InstallmentEntity) {
        viewModelScope.launch {
            repository.deleteInstallment(installment)
            _messageEvents.emit("تم حذف القسط")
        }
    }

    // --- Reminders ---
    fun addReminder(
        customerId: Long,
        customerName: String,
        dueDate: String,
        expectedAmount: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            val reminder = InstallmentReminderEntity(
                customerId = customerId,
                customerName = customerName,
                dueDate = dueDate.ifBlank { todayDate() },
                expectedAmount = expectedAmount,
                notes = notes?.trim()?.ifBlank { null }
            )
            repository.addReminder(reminder)
            _messageEvents.emit("تمت إضافة تذكير بالقسط بنجاح")
        }
    }

    fun toggleReminderStatus(reminder: InstallmentReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isPaid = !reminder.isPaid))
        }
    }

    fun deleteReminder(reminder: InstallmentReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            _messageEvents.emit("تم حذف التذكير")
        }
    }

    // --- Excel Export ---
    fun exportToExcel(onReadyToShare: (File) -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val file = ExcelExporter.exportComprehensiveReport(
                context = context,
                merchants = merchants.value,
                customers = customers.value
            )
            if (file != null) {
                onReadyToShare(file)
                _messageEvents.emit("تم إنشاء ملف الإكسيل بنجاح")
            } else {
                _messageEvents.emit("تعذر تصدير ملف الإكسيل، يرجى المحاولة مجدداً")
            }
        }
    }

    // --- Backup & Restore ---
    private val _lastBackupDate = MutableStateFlow<String?>(null)
    val lastBackupDate: StateFlow<String?> = _lastBackupDate.asStateFlow()

    fun exportBackupToFile(onReady: (File) -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val backup = repository.getBackupObject()
            val jsonStr = com.example.util.DatabaseBackupManager.toJson(backup)
            val file = com.example.util.DatabaseBackupManager.createBackupFileInCache(context, jsonStr)
            _lastBackupDate.value = backup.backupDate
            onReady(file)
            _messageEvents.emit("تم إنشاء النسخة الاحتياطية بنجاح")
        }
    }

    fun saveBackupToUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val backup = repository.getBackupObject()
            val jsonStr = com.example.util.DatabaseBackupManager.toJson(backup)
            val success = com.example.util.DatabaseBackupManager.writeBackupToUri(context, uri, jsonStr)
            if (success) {
                _lastBackupDate.value = backup.backupDate
                _messageEvents.emit("تم حفظ النسخة الاحتياطية بنجاح على ذاكرة الهاتف")
            } else {
                _messageEvents.emit("تعذر حفظ النسخة الاحتياطية على الهاتف")
            }
        }
    }

    fun restoreBackupFromUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val backup = com.example.util.DatabaseBackupManager.readBackupFromUri(context, uri)
                if (backup != null) {
                    repository.restoreFromBackup(backup)
                    _messageEvents.emit("تمت استعادة البيانات بنجاح (${backup.customers.size} زبائن، ${backup.merchants.size} تجار)")
                } else {
                    _messageEvents.emit("الملف المحدد غير صالح كنسخة احتياطية لتطبيق hamza®")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _messageEvents.emit("حدث خطأ أثناء استعادة النسخة: ${e.localizedMessage}")
            }
        }
    }

    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
