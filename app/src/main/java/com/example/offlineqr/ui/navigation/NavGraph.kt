package com.example.offlineqr.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.example.offlineqr.R
import com.example.offlineqr.ui.create.CreateQrScreen
import com.example.offlineqr.ui.create.CreateQrViewModel
import com.example.offlineqr.ui.detail.QrDetailScreen
import com.example.offlineqr.ui.detail.QrDetailViewModel
import com.example.offlineqr.ui.folders.FolderDetailScreen
import com.example.offlineqr.ui.folders.FolderViewModel
import com.example.offlineqr.ui.home.HomeScreen
import com.example.offlineqr.ui.home.HomeViewModel
import com.example.offlineqr.ui.scanner.ScannerScreen
import com.example.offlineqr.ui.scanner.ScannerViewModel
import com.example.offlineqr.ui.settings.SettingsScreen
import com.example.offlineqr.ui.settings.SettingsViewModel

@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom navigation on Scanner and CreateQr screens
    val isBottomBarVisible = currentDestination != null &&
            !currentDestination.hasRoute<Screen.Scanner>() &&
            !currentDestination.hasRoute<Screen.CreateQr>()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BottomNavigationBar(
                    currentDestination = currentDestination,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Home) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToScanner = {
                        navController.navigate(Screen.Scanner) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings) {
                            popUpTo(Screen.Home) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Screen.Home> {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onCreateQrClick = { navController.navigate(Screen.CreateQr()) },
                    onQrClick = { qrId -> navController.navigate(Screen.QrDetail(qrId)) },
                    onFolderClick = { folderId -> navController.navigate(Screen.FolderDetail(folderId)) },
                    onEditQrClick = { qrId -> navController.navigate(Screen.CreateQr(editQrId = qrId)) }
                )
            }

            composable<Screen.Scanner> {
                val viewModel: ScannerViewModel = hiltViewModel()
                ScannerScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onCreateQrClick = { navController.navigate(Screen.CreateQr()) },
                    onScanSuccess = { qrId ->
                        navController.navigate(Screen.QrDetail(qrId)) {
                            popUpTo(Screen.Home) { inclusive = false }
                        }
                    }
                )
            }

            composable<Screen.CreateQr> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.CreateQr>()
                val viewModel: CreateQrViewModel = hiltViewModel()
                androidx.compose.runtime.LaunchedEffect(route.editQrId) {
                    route.editQrId?.let { id ->
                        viewModel.loadForEdit(id)
                    }
                }
                CreateQrScreen(
                    viewModel = viewModel,
                    onCloseClick = { navController.popBackStack() },
                    onSaveSuccess = { qrId ->
                        navController.navigate(Screen.QrDetail(qrId)) {
                            popUpTo(Screen.Home) { inclusive = false }
                        }
                    }
                )
            }

            composable<Screen.Settings> {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel
                )
            }

            composable<Screen.FolderDetail> {
                val viewModel: FolderViewModel = hiltViewModel()
                FolderDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onQrClick = { qrId -> navController.navigate(Screen.QrDetail(qrId)) }
                )
            }

            composable<Screen.QrDetail> {
                val viewModel: QrDetailViewModel = hiltViewModel()
                QrDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { qrId -> navController.navigate(Screen.CreateQr(editQrId = qrId)) }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigateToHome: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHomeActive = currentDestination?.hasRoute<Screen.Home>() == true
    val isSettingsActive = currentDestination?.hasRoute<Screen.Settings>() == true

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .height(72.dp)
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onNavigateToHome)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isHomeActive) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = stringResource(R.string.nav_home),
                        tint = if (isHomeActive) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.nav_home),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isHomeActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isHomeActive) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Prominent Elevated Central Scanner Action
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onNavigateToScanner),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = stringResource(R.string.nav_scanner),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Settings Tab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onNavigateToSettings)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSettingsActive) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = if (isSettingsActive) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.nav_settings),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSettingsActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSettingsActive) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
