package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val photoUri: String? = null,
    val purchaseDate: String,
    val paidAmount: Double,
    val paymentDate: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "merchant_products",
    foreignKeys = [
        ForeignKey(
            entity = MerchantEntity::class,
            parentColumns = ["id"],
            childColumns = ["merchantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["merchantId"])]
)
data class MerchantProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantId: Long,
    val productName: String,
    val category: String, // free-form text input
    val quantity: Int, // عدد المنتجات
    val totalPrice: Double, // إجمالي المبلغ
    val unitPrice: Double = if (quantity > 0) totalPrice / quantity else 0.0, // سعر القطعة يحسب تلقائي
    val photoUri: String? = null
)

data class MerchantWithProducts(
    @Embedded
    val merchant: MerchantEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "merchantId"
    )
    val products: List<MerchantProductEntity> = emptyList()
) {
    val totalPurchaseCost: Double
        get() = products.sumOf { it.totalPrice }

    val remainingAmount: Double
        get() = (totalPurchaseCost - merchant.paidAmount).coerceAtLeast(0.0)

    val totalProductCount: Int
        get() = products.sumOf { it.quantity }
}
