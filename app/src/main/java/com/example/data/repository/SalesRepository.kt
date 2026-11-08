package com.example.data.repository

import com.example.data.db.SalesDao
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerProductEntity
import com.example.data.model.CustomerWithDetails
import com.example.data.model.InstallmentEntity
import com.example.data.model.InstallmentReminderEntity
import com.example.data.model.MerchantEntity
import com.example.data.model.MerchantProductEntity
import com.example.data.model.MerchantWithProducts
import kotlinx.coroutines.flow.Flow

class SalesRepository(private val dao: SalesDao) {

    val merchantsWithProducts: Flow<List<MerchantWithProducts>> = dao.getAllMerchantsWithProducts()
    val customersWithDetails: Flow<List<CustomerWithDetails>> = dao.getAllCustomersWithDetails()
    val allReminders: Flow<List<InstallmentReminderEntity>> = dao.getAllReminders()
    val allInstallments: Flow<List<InstallmentEntity>> = dao.getAllInstallments()
    val allMerchantProducts: Flow<List<MerchantProductEntity>> = dao.getAllMerchantProducts()
    val allCustomerProducts: Flow<List<CustomerProductEntity>> = dao.getAllCustomerProducts()

    suspend fun addMerchant(merchant: MerchantEntity, products: List<MerchantProductEntity>): Long {
        val merchantId = dao.insertMerchant(merchant)
        val linkedProducts = products.map { it.copy(merchantId = merchantId) }
        if (linkedProducts.isNotEmpty()) {
            dao.insertMerchantProducts(linkedProducts)
        }
        return merchantId
    }

    suspend fun updateMerchant(merchant: MerchantEntity) {
        dao.updateMerchant(merchant)
    }

    suspend fun deleteMerchant(merchant: MerchantEntity) {
        dao.deleteMerchant(merchant)
    }

    suspend fun addProductToMerchant(product: MerchantProductEntity): Long {
        return dao.insertMerchantProduct(product)
    }

    suspend fun updateMerchantProduct(product: MerchantProductEntity) {
        dao.updateMerchantProduct(product)
    }

    suspend fun deleteMerchantProduct(productId: Long) {
        dao.deleteMerchantProduct(productId)
    }

    // Customers
    suspend fun addCustomerWithProduct(
        customer: CustomerEntity,
        product: CustomerProductEntity
    ): Long {
        val customerId = dao.insertCustomer(customer)
        val linkedProduct = product.copy(customerId = customerId)
        dao.insertCustomerProduct(linkedProduct)
        return customerId
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        dao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        dao.deleteCustomer(customer)
    }

    suspend fun addProductToCustomer(product: CustomerProductEntity): Long {
        return dao.insertCustomerProduct(product)
    }

    suspend fun updateCustomerProduct(product: CustomerProductEntity) {
        dao.updateCustomerProduct(product)
    }

    suspend fun deleteCustomerProduct(productId: Long) {
        dao.deleteCustomerProduct(productId)
    }

    // Installments
    suspend fun addInstallment(installment: InstallmentEntity): Long {
        return dao.insertInstallment(installment)
    }

    suspend fun deleteInstallment(installment: InstallmentEntity) {
        dao.deleteInstallment(installment)
    }

    // Reminders
    suspend fun addReminder(reminder: InstallmentReminderEntity): Long {
        return dao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: InstallmentReminderEntity) {
        dao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: InstallmentReminderEntity) {
        dao.deleteReminder(reminder)
    }

    // --- Backup & Restore ---
    suspend fun getBackupObject(): com.example.data.model.FullDatabaseBackup {
        return com.example.util.DatabaseBackupManager.createBackupObject(dao)
    }

    suspend fun restoreFromBackup(backup: com.example.data.model.FullDatabaseBackup) {
        dao.restoreDatabase(
            merchants = backup.merchants,
            merchantProducts = backup.merchantProducts,
            customers = backup.customers,
            customerProducts = backup.customerProducts,
            installments = backup.installments,
            reminders = backup.reminders
        )
    }
}
