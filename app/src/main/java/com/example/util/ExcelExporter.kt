package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.CustomerWithDetails
import com.example.data.model.MerchantWithProducts
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    fun exportComprehensiveReport(
        context: Context,
        merchants: List<MerchantWithProducts>,
        customers: List<CustomerWithDetails>
    ): File? {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "hamza_sales_report_$dateStr.csv"
            val file = File(context.cacheDir, fileName)

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // UTF-8 BOM so Excel automatically recognizes Arabic characters properly
                    writer.write("\uFEFF")

                    writer.write("=== تقرير مبيعات ومشتريات hamza® ===\n")
                    writer.write("تاريخ الاستخراج: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n\n")

                    // 1. ملخص الأداء المالي
                    val totalPurchases = merchants.sumOf { it.totalPurchaseCost }
                    val totalPurchasesPaid = merchants.sumOf { it.merchant.paidAmount }
                    val totalPurchasesRemaining = merchants.sumOf { it.remainingAmount }

                    val totalSales = customers.sumOf { it.totalPurchasedAmount }
                    val totalSalesPaid = customers.sumOf { it.totalPaidOverall }
                    val totalReceivables = customers.sumOf { it.totalRemainingDebt }

                    writer.write("--- أولاً: الملخص المالي العام ---\n")
                    writer.write("البيان,القيمة (ج.م)\n")
                    writer.write("إجمالي مبيعات الزبائن,$totalSales\n")
                    writer.write("إجمالي المحصل من الزبائن,$totalSalesPaid\n")
                    writer.write("إجمالي المتبقي ديون لدى الزبائن,$totalReceivables\n")
                    writer.write("إجمالي تكلفة المشتريات,$totalPurchases\n")
                    writer.write("إجمالي المدفوع للموردين والتجار,$totalPurchasesPaid\n")
                    writer.write("إجمالي المتبقي للموردين والتجار,$totalPurchasesRemaining\n\n")

                    // 2. تقرير المبيعات والزبائن
                    writer.write("--- ثانياً: تقرير الزبائن والمبيعات والأقساط ---\n")
                    writer.write("م,اسم الزبون,رقم الهاتف,رقم الهوية,العنوان,المنتجات المشتراة,التصنيف,إجمالي المشتريات,المدفوع مقدماً,الأقساط المسددة,إجمالي المدفوع,المبلغ المتبقي,ملاحظات\n")
                    customers.forEachIndexed { index, c ->
                        val productsNames = c.products.joinToString(" | ") { it.productName }.replace(",", " - ")
                        val categories = c.products.joinToString(" | ") { it.category }.replace(",", " - ")
                        val safeAddress = c.customer.address.replace(",", " - ")
                        val safeNotes = (c.customer.notes ?: "").replace(",", " - ").replace("\n", " ")

                        writer.write(
                            "${index + 1}," +
                            "\"${c.customer.name}\"," +
                            "\"${c.customer.phone}\"," +
                            "\"${c.customer.nationalId}\"," +
                            "\"$safeAddress\"," +
                            "\"$productsNames\"," +
                            "\"$categories\"," +
                            "${c.totalPurchasedAmount}," +
                            "${c.totalInitialPaid}," +
                            "${c.totalInstallmentsPaid}," +
                            "${c.totalPaidOverall}," +
                            "${c.totalRemainingDebt}," +
                            "\"$safeNotes\"\n"
                        )
                    }
                    writer.write("\n")

                    // 3. تقرير التجار والمشتريات
                    writer.write("--- ثالثاً: تقرير التجار والمشتريات ---\n")
                    writer.write("م,اسم التاجر,رقم الهاتف,تاريخ الشراء,المنتجات الموردة,التصنيفات,إجمالي عدد القطع,إجمالي الفاتورة,ما تم دفعه,تاريخ الدفع,المتبقي للتاجر,ملاحظات\n")
                    merchants.forEachIndexed { index, m ->
                        val prodNames = m.products.joinToString(" | ") { "${it.productName} (${it.quantity} قطعة بسعر ${it.unitPrice})" }.replace(",", " - ")
                        val categories = m.products.joinToString(" | ") { it.category }.replace(",", " - ")
                        val safeNotes = (m.merchant.notes ?: "").replace(",", " - ").replace("\n", " ")

                        writer.write(
                            "${index + 1}," +
                            "\"${m.merchant.name}\"," +
                            "\"${m.merchant.phone}\"," +
                            "\"${m.merchant.purchaseDate}\"," +
                            "\"$prodNames\"," +
                            "\"$categories\"," +
                            "${m.totalProductCount}," +
                            "${m.totalPurchaseCost}," +
                            "${m.merchant.paidAmount}," +
                            "\"${m.merchant.paymentDate}\"," +
                            "${m.remainingAmount}," +
                            "\"$safeNotes\"\n"
                        )
                    }
                    writer.write("\n")

                    // 4. تقرير الأقساط التفصيلية
                    writer.write("--- رابعاً: سجل الأقساط المسددة بالتفصيل ---\n")
                    writer.write("اسم الزبون,الهاتف,مبلغ القسط,تاريخ السداد,المتبقي بعد القسط,ملاحظات القسط\n")
                    customers.forEach { c ->
                        c.installments.forEach { inst ->
                            val safeNotes = (inst.notes ?: "").replace(",", " - ").replace("\n", " ")
                            writer.write(
                                "\"${c.customer.name}\"," +
                                "\"${c.customer.phone}\"," +
                                "${inst.amount}," +
                                "\"${inst.paymentDate}\"," +
                                "${inst.remainingAfterPayment}," +
                                "\"$safeNotes\"\n"
                            )
                        }
                    }
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareExportedFile(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير مبيعات hamza® - ملف إكسيل")
                putExtra(Intent.EXTRA_TEXT, "مرفق تقرير المبيعات والمشتريات والمخزون المالي لتطبيق hamza®")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة تقرير الإكسيل عبر:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
