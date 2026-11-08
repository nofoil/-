package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nationalId: String,
    val address: String, // كتابة
    val gpsLocation: String? = null, // تحديد الموقع GPS أو رابط خرائط جوجل (اختياري)
    val photoUri: String? = null, // صورة العميل
    val phone: String, // رقم الهاتف
    val notes: String? = null, // ملاحظات (اختياري)
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_products",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class CustomerProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val productName: String,
    val category: String, // تصنيف المنتج - كتابة حرة
    val productPrice: Double, // مبلغ المنتج
    val initialPaidAmount: Double, // ما تم دفعه مبدئياً عند الشراء
    val saleDate: String, // التاريخ
    val productPhotoUri: String? = null, // صورة المنتج
    val notes: String? = null
) {
    val initialRemaining: Double
        get() = (productPrice - initialPaidAmount).coerceAtLeast(0.0)
}

@Entity(
    tableName = "customer_installments",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val amount: Double, // مبلغ القسط
    val paymentDate: String, // تاريخ السداد
    val remainingAfterPayment: Double, // المبلغ المتبقي بعد السداد
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "installment_reminders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class InstallmentReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val dueDate: String, // تاريخ الاستحقاق
    val expectedAmount: Double,
    val isPaid: Boolean = false,
    val notes: String? = null
)

data class CustomerWithDetails(
    @Embedded
    val customer: CustomerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val products: List<CustomerProductEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val installments: List<InstallmentEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val reminders: List<InstallmentReminderEntity> = emptyList()
) {
    val totalPurchasedAmount: Double
        get() = products.sumOf { it.productPrice }

    val totalInitialPaid: Double
        get() = products.sumOf { it.initialPaidAmount }

    val totalInstallmentsPaid: Double
        get() = installments.sumOf { it.amount }

    val totalPaidOverall: Double
        get() = totalInitialPaid + totalInstallmentsPaid

    val totalRemainingDebt: Double
        get() = (totalPurchasedAmount - totalPaidOverall).coerceAtLeast(0.0)
}
