package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.db.SalesDao
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerProductEntity
import com.example.data.model.FullDatabaseBackup
import com.example.data.model.InstallmentEntity
import com.example.data.model.InstallmentReminderEntity
import com.example.data.model.MerchantEntity
import com.example.data.model.MerchantProductEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseBackupManager {

    suspend fun createBackupObject(dao: SalesDao): FullDatabaseBackup {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return FullDatabaseBackup(
            version = 1,
            appName = "hamza®",
            backupDate = dateStr,
            merchants = dao.getAllMerchantsSync(),
            merchantProducts = dao.getAllMerchantProductsSync(),
            customers = dao.getAllCustomersSync(),
            customerProducts = dao.getAllCustomerProductsSync(),
            installments = dao.getAllInstallmentsSync(),
            reminders = dao.getAllRemindersSync()
        )
    }

    fun toJson(backup: FullDatabaseBackup): String {
        val root = JSONObject().apply {
            put("version", backup.version)
            put("appName", backup.appName)
            put("backupDate", backup.backupDate)

            // Merchants
            val merchantsArray = JSONArray()
            backup.merchants.forEach { m ->
                merchantsArray.put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("phone", m.phone)
                    put("photoUri", m.photoUri ?: JSONObject.NULL)
                    put("purchaseDate", m.purchaseDate)
                    put("paidAmount", m.paidAmount)
                    put("paymentDate", m.paymentDate)
                    put("notes", m.notes ?: JSONObject.NULL)
                    put("createdAt", m.createdAt)
                })
            }
            put("merchants", merchantsArray)

            // Merchant Products
            val mProductsArray = JSONArray()
            backup.merchantProducts.forEach { mp ->
                mProductsArray.put(JSONObject().apply {
                    put("id", mp.id)
                    put("merchantId", mp.merchantId)
                    put("productName", mp.productName)
                    put("category", mp.category)
                    put("quantity", mp.quantity)
                    put("totalPrice", mp.totalPrice)
                    put("unitPrice", mp.unitPrice)
                    put("photoUri", mp.photoUri ?: JSONObject.NULL)
                })
            }
            put("merchantProducts", mProductsArray)

            // Customers
            val customersArray = JSONArray()
            backup.customers.forEach { c ->
                customersArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("nationalId", c.nationalId)
                    put("address", c.address)
                    put("gpsLocation", c.gpsLocation ?: JSONObject.NULL)
                    put("photoUri", c.photoUri ?: JSONObject.NULL)
                    put("phone", c.phone)
                    put("notes", c.notes ?: JSONObject.NULL)
                    put("createdAt", c.createdAt)
                })
            }
            put("customers", customersArray)

            // Customer Products
            val cProductsArray = JSONArray()
            backup.customerProducts.forEach { cp ->
                cProductsArray.put(JSONObject().apply {
                    put("id", cp.id)
                    put("customerId", cp.customerId)
                    put("productName", cp.productName)
                    put("category", cp.category)
                    put("productPrice", cp.productPrice)
                    put("initialPaidAmount", cp.initialPaidAmount)
                    put("saleDate", cp.saleDate)
                    put("productPhotoUri", cp.productPhotoUri ?: JSONObject.NULL)
                    put("notes", cp.notes ?: JSONObject.NULL)
                })
            }
            put("customerProducts", cProductsArray)

            // Installments
            val installmentsArray = JSONArray()
            backup.installments.forEach { inst ->
                installmentsArray.put(JSONObject().apply {
                    put("id", inst.id)
                    put("customerId", inst.customerId)
                    put("amount", inst.amount)
                    put("paymentDate", inst.paymentDate)
                    put("remainingAfterPayment", inst.remainingAfterPayment)
                    put("notes", inst.notes ?: JSONObject.NULL)
                    put("createdAt", inst.createdAt)
                })
            }
            put("installments", installmentsArray)

            // Reminders
            val remindersArray = JSONArray()
            backup.reminders.forEach { r ->
                remindersArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("customerId", r.customerId)
                    put("customerName", r.customerName)
                    put("dueDate", r.dueDate)
                    put("expectedAmount", r.expectedAmount)
                    put("isPaid", r.isPaid)
                    put("notes", r.notes ?: JSONObject.NULL)
                })
            }
            put("reminders", remindersArray)
        }
        return root.toString(2)
    }

    fun fromJson(jsonStr: String): FullDatabaseBackup {
        val root = JSONObject(jsonStr)
        val version = root.optInt("version", 1)
        val appName = root.optString("appName", "hamza®")
        val backupDate = root.optString("backupDate", "")

        val merchants = mutableListOf<MerchantEntity>()
        val mArray = root.optJSONArray("merchants") ?: JSONArray()
        for (i in 0 until mArray.length()) {
            val obj = mArray.getJSONObject(i)
            merchants.add(
                MerchantEntity(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    phone = obj.getString("phone"),
                    photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri"),
                    purchaseDate = obj.optString("purchaseDate", ""),
                    paidAmount = obj.optDouble("paidAmount", 0.0),
                    paymentDate = obj.optString("paymentDate", ""),
                    notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val merchantProducts = mutableListOf<MerchantProductEntity>()
        val mpArray = root.optJSONArray("merchantProducts") ?: JSONArray()
        for (i in 0 until mpArray.length()) {
            val obj = mpArray.getJSONObject(i)
            merchantProducts.add(
                MerchantProductEntity(
                    id = obj.optLong("id", 0),
                    merchantId = obj.getLong("merchantId"),
                    productName = obj.getString("productName"),
                    category = obj.optString("category", "أجهزة كهربائية"),
                    quantity = obj.optInt("quantity", 1),
                    totalPrice = obj.optDouble("totalPrice", 0.0),
                    unitPrice = obj.optDouble("unitPrice", 0.0),
                    photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri")
                )
            )
        }

        val customers = mutableListOf<CustomerEntity>()
        val cArray = root.optJSONArray("customers") ?: JSONArray()
        for (i in 0 until cArray.length()) {
            val obj = cArray.getJSONObject(i)
            customers.add(
                CustomerEntity(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    nationalId = obj.optString("nationalId", ""),
                    address = obj.optString("address", ""),
                    gpsLocation = if (obj.isNull("gpsLocation")) null else obj.optString("gpsLocation"),
                    photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri"),
                    phone = obj.optString("phone", ""),
                    notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val customerProducts = mutableListOf<CustomerProductEntity>()
        val cpArray = root.optJSONArray("customerProducts") ?: JSONArray()
        for (i in 0 until cpArray.length()) {
            val obj = cpArray.getJSONObject(i)
            customerProducts.add(
                CustomerProductEntity(
                    id = obj.optLong("id", 0),
                    customerId = obj.getLong("customerId"),
                    productName = obj.getString("productName"),
                    category = obj.optString("category", "أجهزة كهربائية"),
                    productPrice = obj.optDouble("productPrice", 0.0),
                    initialPaidAmount = obj.optDouble("initialPaidAmount", 0.0),
                    saleDate = obj.optString("saleDate", ""),
                    productPhotoUri = if (obj.isNull("productPhotoUri")) null else obj.optString("productPhotoUri"),
                    notes = if (obj.isNull("notes")) null else obj.optString("notes")
                )
            )
        }

        val installments = mutableListOf<InstallmentEntity>()
        val instArray = root.optJSONArray("installments") ?: JSONArray()
        for (i in 0 until instArray.length()) {
            val obj = instArray.getJSONObject(i)
            installments.add(
                InstallmentEntity(
                    id = obj.optLong("id", 0),
                    customerId = obj.getLong("customerId"),
                    amount = obj.optDouble("amount", 0.0),
                    paymentDate = obj.optString("paymentDate", ""),
                    remainingAfterPayment = obj.optDouble("remainingAfterPayment", 0.0),
                    notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val reminders = mutableListOf<InstallmentReminderEntity>()
        val rArray = root.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until rArray.length()) {
            val obj = rArray.getJSONObject(i)
            reminders.add(
                InstallmentReminderEntity(
                    id = obj.optLong("id", 0),
                    customerId = obj.getLong("customerId"),
                    customerName = obj.optString("customerName", ""),
                    dueDate = obj.optString("dueDate", ""),
                    expectedAmount = obj.optDouble("expectedAmount", 0.0),
                    isPaid = obj.optBoolean("isPaid", false),
                    notes = if (obj.isNull("notes")) null else obj.optString("notes")
                )
            )
        }

        return FullDatabaseBackup(
            version = version,
            appName = appName,
            backupDate = backupDate,
            merchants = merchants,
            merchantProducts = merchantProducts,
            customers = customers,
            customerProducts = customerProducts,
            installments = installments,
            reminders = reminders
        )
    }

    fun createBackupFileInCache(context: Context, jsonStr: String): File {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "hamza_backup_$dateStr.json"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(jsonStr)
            }
        }
        return file
    }

    fun writeBackupToUri(context: Context, uri: Uri, jsonStr: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                    writer.write(jsonStr)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): FullDatabaseBackup? {
        return try {
            val sb = java.lang.StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { ins ->
                BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            fromJson(sb.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية لقاعدة بيانات hamza®")
                putExtra(Intent.EXTRA_TEXT, "مرفق ملف النسخة الاحتياطية لتطبيق hamza® لمبيعات ومشتريات الأجهزة وأدوات المائدة.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة وحفظ النسخة الاحتياطية:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
