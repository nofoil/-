package com.example.data.model

data class FullDatabaseBackup(
    val version: Int = 1,
    val appName: String = "hamza®",
    val backupDate: String,
    val merchants: List<MerchantEntity>,
    val merchantProducts: List<MerchantProductEntity>,
    val customers: List<CustomerEntity>,
    val customerProducts: List<CustomerProductEntity>,
    val installments: List<InstallmentEntity>,
    val reminders: List<InstallmentReminderEntity>
)
