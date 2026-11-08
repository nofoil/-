package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerProductEntity
import com.example.data.model.CustomerWithDetails
import com.example.data.model.InstallmentEntity
import com.example.data.model.MerchantEntity
import com.example.data.model.MerchantProductEntity
import com.example.data.model.MerchantWithProducts
import com.example.util.ExcelExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("hamza®", appName)
    }

    @Test
    fun `test merchant calculations and product unit price`() {
        val merchant = MerchantEntity(
            name = "مورد أجهزة كهربائية",
            phone = "01012345678",
            purchaseDate = "2026-11-01",
            paidAmount = 3000.0,
            paymentDate = "2026-11-01"
        )
        val p1 = MerchantProductEntity(
            merchantId = 1,
            productName = "خلاط كهربائي",
            category = "أجهزة كهربائية",
            quantity = 10,
            totalPrice = 5000.0,
            unitPrice = 500.0
        )
        val mwp = MerchantWithProducts(merchant, listOf(p1))

        assertEquals(5000.0, mwp.totalPurchaseCost, 0.001)
        assertEquals(2000.0, mwp.remainingAmount, 0.001)
        assertEquals(10, mwp.totalProductCount)
    }

    @Test
    fun `test customer installment ledger calculations`() {
        val customer = CustomerEntity(
            name = "أحمد محمد",
            nationalId = "29001011234567",
            address = "مدينة نصر",
            phone = "01098765432"
        )
        val product = CustomerProductEntity(
            customerId = 1,
            productName = "طقم صيني فاخر",
            category = "أدوات مائدة",
            productPrice = 4000.0,
            initialPaidAmount = 1000.0,
            saleDate = "2026-11-01"
        )
        val installment = InstallmentEntity(
            customerId = 1,
            amount = 500.0,
            paymentDate = "2026-11-05",
            remainingAfterPayment = 2500.0
        )
        val cwd = CustomerWithDetails(
            customer = customer,
            products = listOf(product),
            installments = listOf(installment)
        )

        assertEquals(4000.0, cwd.totalPurchasedAmount, 0.001)
        assertEquals(1000.0, cwd.totalInitialPaid, 0.001)
        assertEquals(500.0, cwd.totalInstallmentsPaid, 0.001)
        assertEquals(1500.0, cwd.totalPaidOverall, 0.001)
        assertEquals(2500.0, cwd.totalRemainingDebt, 0.001)
    }

    @Test
    fun `test excel report generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = ExcelExporter.exportComprehensiveReport(
            context = context,
            merchants = emptyList(),
            customers = emptyList()
        )
        assertNotNull(file)
        assertTrue(file!!.exists())
        val content = file.readText()
        assertTrue(content.contains("تقرير مبيعات ومشتريات hamza®"))
    }

    @Test
    fun `test stock notification channel creation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.StockNotificationHelper.createNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = nm.getNotificationChannel(com.example.util.StockNotificationHelper.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals("تنبيهات نقص المخزون", channel.name)
    }

    @Test
    fun `test database backup serialization and deserialization`() {
        val merchant = MerchantEntity(
            id = 10L,
            name = "مورد أجهزة حمزة",
            phone = "01000000001",
            purchaseDate = "2026-11-01",
            paidAmount = 2000.0,
            paymentDate = "2026-11-02",
            notes = "ملاحظات توريد"
        )
        val mProduct = MerchantProductEntity(
            id = 101L,
            merchantId = 10L,
            productName = "غسالة أوتوماتيك",
            category = "أجهزة كهربائية",
            quantity = 5,
            totalPrice = 25000.0,
            unitPrice = 5000.0
        )
        val customer = CustomerEntity(
            id = 20L,
            name = "محمد علي",
            nationalId = "29505051234567",
            address = "القاهرة",
            phone = "01122334455",
            notes = "عميل مميز"
        )
        val cProduct = CustomerProductEntity(
            id = 201L,
            customerId = 20L,
            productName = "غسالة أوتوماتيك",
            category = "أجهزة كهربائية",
            productPrice = 30000.0,
            initialPaidAmount = 10000.0,
            saleDate = "2026-11-05"
        )
        val installment = InstallmentEntity(
            id = 301L,
            customerId = 20L,
            amount = 2000.0,
            paymentDate = "2026-11-06",
            remainingAfterPayment = 18000.0
        )
        val reminder = com.example.data.model.InstallmentReminderEntity(
            id = 401L,
            customerId = 20L,
            customerName = "محمد علي",
            dueDate = "2026-12-01",
            expectedAmount = 2000.0,
            notes = "قسط ديسمبر"
        )

        val originalBackup = com.example.data.model.FullDatabaseBackup(
            backupDate = "2026-11-07 15:00:00",
            merchants = listOf(merchant),
            merchantProducts = listOf(mProduct),
            customers = listOf(customer),
            customerProducts = listOf(cProduct),
            installments = listOf(installment),
            reminders = listOf(reminder)
        )

        val json = com.example.util.DatabaseBackupManager.toJson(originalBackup)
        assertTrue(json.contains("hamza®"))
        assertTrue(json.contains("مورد أجهزة حمزة"))
        assertTrue(json.contains("محمد علي"))

        val restored = com.example.util.DatabaseBackupManager.fromJson(json)
        assertNotNull(restored)
        assertEquals(1, restored!!.merchants.size)
        assertEquals("مورد أجهزة حمزة", restored.merchants[0].name)
        assertEquals(1, restored.merchantProducts.size)
        assertEquals("غسالة أوتوماتيك", restored.merchantProducts[0].productName)
        assertEquals(5000.0, restored.merchantProducts[0].unitPrice, 0.001)
        assertEquals(1, restored.customers.size)
        assertEquals("محمد علي", restored.customers[0].name)
        assertEquals(1, restored.customerProducts.size)
        assertEquals(30000.0, restored.customerProducts[0].productPrice, 0.001)
        assertEquals(1, restored.installments.size)
        assertEquals(2000.0, restored.installments[0].amount, 0.001)
        assertEquals(1, restored.reminders.size)
        assertEquals("2026-12-01", restored.reminders[0].dueDate)

        // Verify creating cache file
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = com.example.util.DatabaseBackupManager.createBackupFileInCache(context, json)
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertEquals(json, file.readText())
    }
}
