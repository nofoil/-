package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerProductEntity
import com.example.data.model.CustomerWithDetails
import com.example.data.model.InstallmentEntity
import com.example.data.model.InstallmentReminderEntity
import com.example.data.model.MerchantEntity
import com.example.data.model.MerchantProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MerchantEntity::class,
        MerchantProductEntity::class,
        CustomerEntity::class,
        CustomerProductEntity::class,
        InstallmentEntity::class,
        InstallmentReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun salesDao(): SalesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hamza_sales_db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.salesDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: SalesDao) {
                // Starter Merchant 1
                val m1Id = dao.insertMerchant(
                    MerchantEntity(
                        name = "مؤسسة الأمل للأجهزة الكهربائية",
                        phone = "01012345678",
                        purchaseDate = "2026-10-15",
                        paidAmount = 18000.0,
                        paymentDate = "2026-10-15",
                        notes = "المورد الرئيسي للخلاطات والأفران الكهربائية، خصم خاص على الكميات الكبيرة."
                    )
                )
                dao.insertMerchantProducts(
                    listOf(
                        MerchantProductEntity(
                            merchantId = m1Id,
                            productName = "خلاط كهربائي تورنيدو 1000 وات",
                            category = "أجهزة كهربائية",
                            quantity = 15,
                            totalPrice = 12000.0,
                            unitPrice = 800.0
                        ),
                        MerchantProductEntity(
                            merchantId = m1Id,
                            productName = "فرن كهربائي شارب 45 لتر",
                            category = "أجهزة كهربائية",
                            quantity = 5,
                            totalPrice = 10000.0,
                            unitPrice = 2000.0
                        )
                    )
                )

                // Starter Merchant 2 (Dinnerware / Tableware)
                val m2Id = dao.insertMerchant(
                    MerchantEntity(
                        name = "شركة النور لأطقم المائدة والصيني",
                        phone = "01198765432",
                        purchaseDate = "2026-10-20",
                        paidAmount = 15000.0,
                        paymentDate = "2026-10-20",
                        notes = "أطقم مائدة تركي وبورسلين فاخر، استلام فوري من المخزن."
                    )
                )
                dao.insertMerchantProducts(
                    listOf(
                        MerchantProductEntity(
                            merchantId = m2Id,
                            productName = "طقم صيني 66 قطعة بورسلين فاخر",
                            category = "أطباق وأدوات مائدة",
                            quantity = 8,
                            totalPrice = 16000.0,
                            unitPrice = 2000.0
                        ),
                        MerchantProductEntity(
                            merchantId = m2Id,
                            productName = "طقم ملاعق وشوك استانلس ستيل 84 قطعة",
                            category = "أدوات مائدة",
                            quantity = 10,
                            totalPrice = 6000.0,
                            unitPrice = 600.0
                        )
                    )
                )

                // Starter Customer 1
                val c1Id = dao.insertCustomer(
                    CustomerEntity(
                        name = "محمود حسن إبراهيم",
                        nationalId = "29408151201934",
                        address = "القاهرة - مدينة نصر - الحي السابع شارع الطيران",
                        gpsLocation = "30.0561, 31.3301",
                        phone = "01099887766",
                        notes = "عميل ملتزم بمواعيد الأقساط الشهرية يوم 1 من كل شهر."
                    )
                )
                val p1Id = dao.insertCustomerProduct(
                    CustomerProductEntity(
                        customerId = c1Id,
                        productName = "فرن كهربائي شارب 45 لتر",
                        category = "أجهزة كهربائية",
                        productPrice = 2800.0,
                        initialPaidAmount = 800.0,
                        saleDate = "2026-10-25"
                    )
                )
                dao.insertCustomerProduct(
                    CustomerProductEntity(
                        customerId = c1Id,
                        productName = "طقم ملاعق وشوك استانلس ستيل",
                        category = "أدوات مائدة",
                        productPrice = 900.0,
                        initialPaidAmount = 300.0,
                        saleDate = "2026-10-28"
                    )
                )
                dao.insertInstallment(
                    InstallmentEntity(
                        customerId = c1Id,
                        amount = 600.0,
                        paymentDate = "2026-11-01",
                        remainingAfterPayment = 2000.0,
                        notes = "قسط شهر نوفمبر تم استلامه نقداً"
                    )
                )
                dao.insertReminder(
                    InstallmentReminderEntity(
                        customerId = c1Id,
                        customerName = "محمود حسن إبراهيم",
                        dueDate = "2026-12-01",
                        expectedAmount = 600.0,
                        isPaid = false,
                        notes = "قسط شهر ديسمبر المستحق"
                    )
                )

                // Starter Customer 2
                val c2Id = dao.insertCustomer(
                    CustomerEntity(
                        name = "فاطمة السيد علي",
                        nationalId = "29612041402856",
                        address = "الجيزة - الدقي - شارع مصدق بجوار بنك مصر",
                        gpsLocation = "30.0384, 31.2114",
                        phone = "01234567890",
                        notes = "تجهيز عروسة، مطلوب متابعة مواعيد الأقساط هاتفياً قبل السداد بيومين."
                    )
                )
                dao.insertCustomerProduct(
                    CustomerProductEntity(
                        customerId = c2Id,
                        productName = "طقم صيني 66 قطعة بورسلين فاخر",
                        category = "أطباق وأدوات مائدة",
                        productPrice = 3200.0,
                        initialPaidAmount = 1200.0,
                        saleDate = "2026-11-02"
                    )
                )
                dao.insertInstallment(
                    InstallmentEntity(
                        customerId = c2Id,
                        amount = 500.0,
                        paymentDate = "2026-11-05",
                        remainingAfterPayment = 1500.0,
                        notes = "دفعة إضافية تم تحويلها فودافون كاش"
                    )
                )
                dao.insertReminder(
                    InstallmentReminderEntity(
                        customerId = c2Id,
                        customerName = "فاطمة السيد علي",
                        dueDate = "2026-11-15",
                        expectedAmount = 500.0,
                        isPaid = false,
                        notes = "قسط منتصف الشهر"
                    )
                )

                // Starter Customer 3 (August 2026 sales)
                val c3Id = dao.insertCustomer(
                    CustomerEntity(
                        name = "طارق عبد الله مرسي",
                        nationalId = "29105051601298",
                        address = "القاهرة - المعادي شارع النصر",
                        phone = "01055443322",
                        notes = "شراء نقدي مع قسطين"
                    )
                )
                dao.insertCustomerProduct(
                    CustomerProductEntity(
                        customerId = c3Id,
                        productName = "خلاط كهربائي تورنيدو 1000 وات",
                        category = "أجهزة كهربائية",
                        productPrice = 1400.0,
                        initialPaidAmount = 700.0,
                        saleDate = "2026-08-15"
                    )
                )

                // Starter Customer 4 (September 2026 sales)
                val c4Id = dao.insertCustomer(
                    CustomerEntity(
                        name = "سارة كمال الشناوي",
                        nationalId = "29709081203491",
                        address = "الإسكندرية - سموحة شارع فوزي معاذ",
                        phone = "01122334455",
                        notes = "طقم صواني فرن وأواني"
                    )
                )
                dao.insertCustomerProduct(
                    CustomerProductEntity(
                        customerId = c4Id,
                        productName = "طقم صيني وبورسلين تركي",
                        category = "أطباق وأدوات مائدة",
                        productPrice = 2400.0,
                        initialPaidAmount = 1000.0,
                        saleDate = "2026-09-20"
                    )
                )
            }
        }
    }
}
