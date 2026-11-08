package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerProductEntity
import com.example.data.model.CustomerWithDetails
import com.example.data.model.InstallmentEntity
import com.example.data.model.InstallmentReminderEntity
import com.example.data.model.MerchantEntity
import com.example.data.model.MerchantProductEntity
import com.example.data.model.MerchantWithProducts
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {

    // --- Merchants & Purchases ---
    @Transaction
    @Query("SELECT * FROM merchants ORDER BY id DESC")
    fun getAllMerchantsWithProducts(): Flow<List<MerchantWithProducts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: MerchantEntity): Long

    @Update
    suspend fun updateMerchant(merchant: MerchantEntity)

    @Delete
    suspend fun deleteMerchant(merchant: MerchantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchantProducts(products: List<MerchantProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchantProduct(product: MerchantProductEntity): Long

    @Update
    suspend fun updateMerchantProduct(product: MerchantProductEntity)

    @Query("DELETE FROM merchant_products WHERE id = :productId")
    suspend fun deleteMerchantProduct(productId: Long)

    @Query("SELECT * FROM merchant_products")
    fun getAllMerchantProducts(): Flow<List<MerchantProductEntity>>

    // --- Customers, Sales & Installments ---
    @Transaction
    @Query("SELECT * FROM customers ORDER BY id DESC")
    fun getAllCustomersWithDetails(): Flow<List<CustomerWithDetails>>

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerWithDetails(customerId: Long): CustomerWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerProduct(product: CustomerProductEntity): Long

    @Update
    suspend fun updateCustomerProduct(product: CustomerProductEntity)

    @Query("DELETE FROM customer_products WHERE id = :productId")
    suspend fun deleteCustomerProduct(productId: Long)

    @Query("SELECT * FROM customer_products")
    fun getAllCustomerProducts(): Flow<List<CustomerProductEntity>>

    // --- Installments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity): Long

    @Delete
    suspend fun deleteInstallment(installment: InstallmentEntity)

    @Query("SELECT * FROM customer_installments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    fun getInstallmentsForCustomer(customerId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM customer_installments ORDER BY createdAt DESC")
    fun getAllInstallments(): Flow<List<InstallmentEntity>>

    // --- Reminders ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: InstallmentReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: InstallmentReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: InstallmentReminderEntity)

    @Query("SELECT * FROM installment_reminders ORDER BY dueDate ASC")
    fun getAllReminders(): Flow<List<InstallmentReminderEntity>>

    // --- Backup & Restore Methods ---
    @Query("SELECT * FROM merchants")
    suspend fun getAllMerchantsSync(): List<MerchantEntity>

    @Query("SELECT * FROM merchant_products")
    suspend fun getAllMerchantProductsSync(): List<MerchantProductEntity>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersSync(): List<CustomerEntity>

    @Query("SELECT * FROM customer_products")
    suspend fun getAllCustomerProductsSync(): List<CustomerProductEntity>

    @Query("SELECT * FROM customer_installments")
    suspend fun getAllInstallmentsSync(): List<InstallmentEntity>

    @Query("SELECT * FROM installment_reminders")
    suspend fun getAllRemindersSync(): List<InstallmentReminderEntity>

    @Query("DELETE FROM merchants")
    suspend fun deleteAllMerchants()

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Transaction
    suspend fun restoreDatabase(
        merchants: List<MerchantEntity>,
        merchantProducts: List<MerchantProductEntity>,
        customers: List<CustomerEntity>,
        customerProducts: List<CustomerProductEntity>,
        installments: List<InstallmentEntity>,
        reminders: List<InstallmentReminderEntity>
    ) {
        deleteAllMerchants()
        deleteAllCustomers()

        merchants.forEach { insertMerchant(it) }
        merchantProducts.forEach { insertMerchantProduct(it) }
        customers.forEach { insertCustomer(it) }
        customerProducts.forEach { insertCustomerProduct(it) }
        installments.forEach { insertInstallment(it) }
        reminders.forEach { insertReminder(it) }
    }
}
