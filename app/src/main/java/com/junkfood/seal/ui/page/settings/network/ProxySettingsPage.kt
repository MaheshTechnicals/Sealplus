package com.junkfood.seal.ui.page.settings.network

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.util.ProxyManager
import com.junkfood.seal.util.ProxyValidator
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxySettingsPage(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Load current configuration
    var proxyConfig by remember { mutableStateOf(ProxyManager.loadProxyConfig()) }
    
    // UI States
    var proxyEnabled by remember { mutableStateOf(proxyConfig.enabled) }
    var useFreeProxy by remember { mutableStateOf(proxyConfig.useFreeProxy) }
    var selectedCountry by remember { 
        mutableStateOf(ProxyManager.ProxyCountry.fromCode(proxyConfig.freeProxyCountry) ?: ProxyManager.ProxyCountry.USA) 
    }
    var selectedFreeProxy by remember { mutableStateOf(proxyConfig.freeProxyAddress) }
    var customHost by remember { mutableStateOf(proxyConfig.customProxyHost) }
    var customPort by remember { mutableStateOf(proxyConfig.customProxyPort.toString()) }
    var customType by remember { 
        mutableStateOf(ProxyManager.ProxyType.fromString(proxyConfig.customProxyType)) 
    }

    // Loading states
    var isLoadingProxies by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var isSpeedTesting by remember { mutableStateOf(false) }
    
    // Data states
    var freeProxyList by remember { mutableStateOf<List<String>>(emptyList()) }
    var connectionStatus by remember { mutableStateOf<ProxyValidator.ValidationResult?>(null) }
    var speedTestResult by remember { mutableStateOf<ProxyValidator.SpeedTestResult?>(null) }
    
    // Dialog states
    var showProxyListDialog by remember { mutableStateOf(false) }

    // Save configuration helper
    fun saveConfig() {
        val newConfig = proxyConfig.copy(
            enabled = proxyEnabled,
            useFreeProxy = useFreeProxy,
            freeProxyCountry = selectedCountry.code,
            freeProxyAddress = selectedFreeProxy,
            customProxyHost = customHost,
            customProxyPort = customPort.toIntOrNull() ?: 0,
            customProxyType = customType.name,
            lastValidated = System.currentTimeMillis(),
            isWorking = connectionStatus is ProxyValidator.ValidationResult.Success
        )
        ProxyManager.saveProxyConfig(newConfig)
        proxyConfig = newConfig
    }

    // Fetch free proxies
    fun fetchFreeProxies() {
        scope.launch {
            isLoadingProxies = true
            try {
                val result = ProxyManager.fetchFreeProxies(selectedCountry)
                result.onSuccess { proxies ->
                    freeProxyList = proxies
                    if (proxies.isNotEmpty()) {
                        showProxyListDialog = true
                    } else {
                        withContext(Dispatchers.Main) {
                            context.makeToast("No proxies available for ${selectedCountry.displayName}")
                        }
                    }
                }.onFailure { error ->
                    withContext(Dispatchers.Main) {
                        context.makeToast(error.message ?: "Failed to fetch proxies")
                    }
                }
            } finally {
                isLoadingProxies = false
            }
        }
    }

    // Test connection
    fun testConnection() {
        scope.launch {
            isTesting = true
            connectionStatus = ProxyValidator.ValidationResult.Testing
            try {
                val testConfig = proxyConfig.copy(
                    enabled = true,
                    useFreeProxy = useFreeProxy,
                    freeProxyAddress = selectedFreeProxy,
                    customProxyHost = customHost,
                    customProxyPort = customPort.toIntOrNull() ?: 0,
                    customProxyType = customType.name
                )
                val proxy = testConfig.toJavaProxy()
                connectionStatus = ProxyValidator.validateProxyConnection(proxy)
                
                when (val status = connectionStatus) {
                    is ProxyValidator.ValidationResult.Success -> {
                        withContext(Dispatchers.Main) {
                            context.makeToast("✓ Proxy working: ${status.latencyMs}ms")
                        }
                    }
                    is ProxyValidator.ValidationResult.Failed -> {
                        withContext(Dispatchers.Main) {
                            context.makeToast("✗ Connection failed: ${status.error}")
                        }
                    }
                    else -> {}
                }
            } finally {
                isTesting = false
            }
        }
    }

    // Speed test
    fun runSpeedTest() {
        scope.launch {
            isSpeedTesting = true
            speedTestResult = null
            try {
                val testConfig = proxyConfig.copy(
                    enabled = true,
                    useFreeProxy = useFreeProxy,
                    freeProxyAddress = selectedFreeProxy,
                    customProxyHost = customHost,
                    customProxyPort = customPort.toIntOrNull() ?: 0,
                    customProxyType = customType.name
                )
                val proxy = testConfig.toJavaProxy()
                speedTestResult = ProxyValidator.performSpeedTest(proxy)
                
                speedTestResult?.let { result ->
                    if (result.success) {
                        withContext(Dispatchers.Main) {
                            context.makeToast("Speed test completed")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            context.makeToast("Speed test failed: ${result.error}")
                        }
                    }
                }
            } finally {
                isSpeedTesting = false
            }
        }
    }

    // Proxy List Dialog
    if (showProxyListDialog) {
        ProxyListDialog(
            proxies = freeProxyList,
            onDismiss = { showProxyListDialog = false },
            onSelect = { proxy ->
                selectedFreeProxy = proxy
                showProxyListDialog = false
                connectionStatus = null
                speedTestResult = null
                saveConfig()
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.proxy_settings)) },
                navigationIcon = { BackButton(onClick = onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Global Proxy Toggle
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (proxyEnabled) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.enable_proxy),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.proxy_toggle_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = proxyEnabled,
                            onCheckedChange = { 
                                proxyEnabled = it
                                saveConfig()
                            }
                        )
                    }
                }
            }

            // Connection Status
            item {
                AnimatedVisibility(
                    visible = proxyEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ConnectionStatusCard(
                        status = connectionStatus,
                        isTesting = isTesting,
                        onTest = { testConnection() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Speed Test Results
            item {
                AnimatedVisibility(
                    visible = proxyEnabled && speedTestResult != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    speedTestResult?.let { result ->
                        SpeedTestResultCard(
                            result = result,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Proxy Type Selector
            item {
                AnimatedVisibility(
                    visible = proxyEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PreferenceSubtitle(text = stringResource(R.string.proxy_type))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProxyTypeButton(
                                text = stringResource(R.string.free_proxy),
                                selected = useFreeProxy,
                                onClick = { 
                                    useFreeProxy = true
                                    connectionStatus = null
                                    speedTestResult = null
                                    saveConfig()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ProxyTypeButton(
                                text = stringResource(R.string.custom_proxy),
                                selected = !useFreeProxy,
                                onClick = { 
                                    useFreeProxy = false
                                    connectionStatus = null
                                    speedTestResult = null
                                    saveConfig()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Free Proxy Section
            item {
                AnimatedVisibility(
                    visible = proxyEnabled && useFreeProxy,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    FreeProxySection(
                        selectedCountry = selectedCountry,
                        onCountryChange = { 
                            selectedCountry = it
                            connectionStatus = null
                            speedTestResult = null
                            saveConfig()
                        },
                        selectedProxy = selectedFreeProxy,
                        isLoading = isLoadingProxies,
                        onFetchProxies = { fetchFreeProxies() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Custom Proxy Section
            item {
                AnimatedVisibility(
                    visible = proxyEnabled && !useFreeProxy,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    CustomProxySection(
                        host = customHost,
                        onHostChange = { 
                            customHost = it
                            connectionStatus = null
                            speedTestResult = null
                        },
                        port = customPort,
                        onPortChange = { 
                            customPort = it
                            connectionStatus = null
                            speedTestResult = null
                        },
                        proxyType = customType,
                        onProxyTypeChange = { 
                            customType = it
                            connectionStatus = null
                            speedTestResult = null
                            saveConfig()
                        },
                        onSave = { saveConfig() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Action Buttons
            item {
                AnimatedVisibility(
                    visible = proxyEnabled && 
                        ((useFreeProxy && selectedFreeProxy.isNotEmpty()) || 
                        (!useFreeProxy && customHost.isNotEmpty())),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { testConnection() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTesting && !isSpeedTesting
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isTesting) stringResource(R.string.testing) else stringResource(R.string.test_connection))
                        }

                        FilledTonalButton(
                            onClick = { runSpeedTest() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTesting && !isSpeedTesting && 
                                connectionStatus is ProxyValidator.ValidationResult.Success
                        ) {
                            if (isSpeedTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSpeedTesting) stringResource(R.string.testing_speed) else stringResource(R.string.run_speed_test))
                        }
                    }
                }
            }

            // Info Section
            item {
                AnimatedVisibility(
                    visible = proxyEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.proxy_info_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.proxy_info_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    status: ProxyValidator.ValidationResult?,
    isTesting: Boolean,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = when (status) {
                is ProxyValidator.ValidationResult.Success -> 
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                is ProxyValidator.ValidationResult.Failed -> 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        is ProxyValidator.ValidationResult.Success -> Icons.Filled.CheckCircle
                        is ProxyValidator.ValidationResult.Failed -> Icons.Filled.Error
                        else -> Icons.Outlined.CloudQueue
                    },
                    contentDescription = null,
                    tint = when (status) {
                        is ProxyValidator.ValidationResult.Success -> MaterialTheme.colorScheme.secondary
                        is ProxyValidator.ValidationResult.Failed -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = stringResource(R.string.connection_status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (status) {
                is ProxyValidator.ValidationResult.Success -> {
                    Text(
                        text = "✓ ${status.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is ProxyValidator.ValidationResult.Failed -> {
                    Text(
                        text = "✗ ${status.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ProxyValidator.ValidationResult.Testing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.testing_connection),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                null -> {
                    Text(
                        text = stringResource(R.string.not_tested),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedTestResultCard(
    result: ProxyValidator.SpeedTestResult,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (result.success) 
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = null,
                    tint = if (result.success) 
                        MaterialTheme.colorScheme.tertiary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.speed_test_results),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (result.success) {
                SpeedMetricRow(
                    label = stringResource(R.string.latency),
                    value = "${result.latencyMs}ms",
                    quality = result.getLatencyDescription()
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpeedMetricRow(
                    label = stringResource(R.string.download_speed),
                    value = "${"%.2f".format(result.getSpeedMbps())} Mbps",
                    quality = null
                )
                if (result.uploadLatencyMs > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SpeedMetricRow(
                        label = stringResource(R.string.upload_latency),
                        value = "${result.uploadLatencyMs}ms",
                        quality = null
                    )
                }
            } else {
                Text(
                    text = "✗ ${result.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SpeedMetricRow(
    label: String,
    value: String,
    quality: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            quality?.let {
                Text(
                    text = "($it)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProxyTypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeProxySection(
    selectedCountry: ProxyManager.ProxyCountry,
    onCountryChange: (ProxyManager.ProxyCountry) -> Unit,
    selectedProxy: String,
    isLoading: Boolean,
    onFetchProxies: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PreferenceSubtitle(text = stringResource(R.string.free_proxy_settings))

        // Country Selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedCountry.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_country)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ProxyManager.ProxyCountry.entries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text(country.displayName) },
                        onClick = {
                            onCountryChange(country)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Selected Proxy Display
        if (selectedProxy.isNotEmpty()) {
            OutlinedTextField(
                value = selectedProxy,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.selected_proxy)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CloudQueue,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Fetch Button
        Button(
            onClick = onFetchProxies,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) stringResource(R.string.loading_proxies) else stringResource(R.string.fetch_proxies))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomProxySection(
    host: String,
    onHostChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    proxyType: ProxyManager.ProxyType,
    onProxyTypeChange: (ProxyManager.ProxyType) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PreferenceSubtitle(text = stringResource(R.string.custom_proxy_settings))

        // Host Input
        OutlinedTextField(
            value = host,
            onValueChange = onHostChange,
            label = { Text(stringResource(R.string.proxy_host)) },
            placeholder = { Text("example.com or 192.168.1.1") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Computer,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Port Input
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text(stringResource(R.string.proxy_port)) },
            placeholder = { Text("8080") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Proxy Type Selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = proxyType.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.proxy_protocol)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ProxyManager.ProxyType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            onProxyTypeChange(type)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = host.isNotEmpty() && port.toIntOrNull() != null
        ) {
            Text(stringResource(R.string.save_configuration))
        }
    }
}

@Composable
private fun ProxyListDialog(
    proxies: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_proxy)) },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp)
            ) {
                items(proxies.size) { index ->
                    val proxy = proxies[index]
                    TextButton(
                        onClick = {
                            selectedIndex = index
                            onSelect(proxy)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = proxy,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (index < proxies.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview(name = "Proxy Settings Light")
@Preview(name = "Proxy Settings Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProxySettingsPagePreview() {
    SealTheme {
        ProxySettingsPage()
    }
}
