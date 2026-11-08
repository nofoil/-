package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UiHelpers {

    private val currencyFormat = DecimalFormat("#,##0.##")

    fun formatMoney(amount: Double): String {
        return "${currencyFormat.format(amount)} ج.م"
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun dialPhoneNumber(context: Context, phone: String) {
        try {
            val cleanPhone = phone.replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح لوحة الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phone: String, message: String = "") {
        try {
            var cleanPhone = phone.replace(" ", "").replace("-", "").replace("+", "")
            // Normalize Egyptian numbers: 010... -> 2010...
            if (cleanPhone.startsWith("0")) {
                cleanPhone = "2$cleanPhone"
            }
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
        }
    }

    fun openMaps(context: Context, locationQuery: String) {
        try {
            val trimmed = locationQuery.trim()
            val uri = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                Uri.parse(trimmed)
            } else {
                Uri.parse("geo:0,0?q=${Uri.encode(trimmed)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الخرائط", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(context: Context, onLocationReceived: (String) -> Unit) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                Toast.makeText(context, "خدمة تحديد الموقع غير متوفرة", Toast.LENGTH_SHORT).show()
                return
            }

            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(context, "يرجى تفعيل الـ GPS في إعدادات الهاتف", Toast.LENGTH_SHORT).show()
                return
            }

            var bestLocation: Location? = null
            if (isGpsEnabled) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (bestLocation == null && isNetworkEnabled) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (bestLocation != null) {
                val locStr = "https://maps.google.com/?q=${bestLocation.latitude},${bestLocation.longitude}"
                onLocationReceived(locStr)
                Toast.makeText(context, "تم جلب إحداثيات الموقع بنجاح", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "جارٍ البحث عن إشارة GPS، يمكنك لصق الرابط يدوياً", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر جلب الموقع: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
