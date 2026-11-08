package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.PurchasesScreen
import com.example.ui.screens.ReportsInventoryScreen
import com.example.ui.screens.components.HamzaHeader
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.HamzaTheme
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.SalesViewModel
import com.example.util.ExcelExporter

enum class HamzaNavTab(
    val title: String,
    val iconFilled: androidx.compose.ui.graphics.vector.ImageVector,
    val iconOutlined: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
) {
    PURCHASES("المشتريات", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "nav_purchases"),
    CUSTOMERS("الزبائن والأقساط", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_customers"),
    REPORTS("التقارير والمخزون", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_reports")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.util.StockNotificationHelper.createNotificationChannel(this)
        setContent {
            HamzaTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HamzaApp()
                }
            }
        }
    }
}

@Composable
fun HamzaApp(viewModel: SalesViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messageEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle system back navigation to return to first tab if not on it
    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HamzaHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onExportClick = {
                    viewModel.exportToExcel { file ->
                        ExcelExporter.shareExportedFile(context, file)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                HamzaNavTab.values().forEachIndexed { index, tab ->
                    val isSelected = currentTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.iconFilled else tab.iconOutlined,
                                contentDescription = tab.title,
                                tint = if (isSelected) GoldLight else androidx.compose.ui.graphics.Color(0xFF94A3B8)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp,
                                color = if (isSelected) GoldLight else androidx.compose.ui.graphics.Color(0xFF94A3B8)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = GoldDark.copy(alpha = 0.3f),
                            selectedIconColor = GoldLight,
                            unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_crossfade") { tabIndex ->
                when (tabIndex) {
                    0 -> PurchasesScreen(
                        viewModel = viewModel,
                        searchQuery = searchQuery
                    )
                    1 -> CustomersScreen(
                        viewModel = viewModel,
                        searchQuery = searchQuery
                    )
                    2 -> ReportsInventoryScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
