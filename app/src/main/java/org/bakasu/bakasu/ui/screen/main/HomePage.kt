package org.bakasu.bakasu.ui.screen.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.system.Os
import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.MenuBook
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.Block
import androidx.compose.material.icons.twotone.DeveloperBoard
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.FilterList
import androidx.compose.material.icons.twotone.Group
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.PowerSettingsNew
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Smartphone
import androidx.compose.material.icons.twotone.Tag
import androidx.compose.material.icons.twotone.TaskAlt
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material.icons.twotone.VolunteerActivism
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.BuildConfig
import org.bakasu.bakasu.Natives.KernelPatchImplementation
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.HomeSystemInfo
import org.bakasu.bakasu.domain.model.KernelStatus
import org.bakasu.bakasu.domain.model.ManagerUpdateChannel
import org.bakasu.bakasu.domain.model.ManagerUpdateInfo
import org.bakasu.bakasu.domain.usecase.EnqueueManagerUpdateUseCase
import org.bakasu.bakasu.magica.MagicaService
import org.bakasu.bakasu.ui.component.KsuIsValid
import org.bakasu.bakasu.ui.component.SwipeableSnackbarHost
import org.bakasu.bakasu.ui.component.WarningCard
import org.bakasu.bakasu.ui.component.rememberConfirmDialog
import org.bakasu.bakasu.ui.component.rememberLoadingDialog
import org.bakasu.bakasu.ui.component.settings.SegmentedColumn
import org.bakasu.bakasu.ui.component.settings.SettingsBaseWidget
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.navigation.Route
import org.bakasu.bakasu.ui.screen.LabelText
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.util.LocalPermissionRequestInterface
import org.bakasu.bakasu.ui.util.LocalSnackbarHost
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.bakasu.bakasu.ui.util.downloader.downloadManagerUpdate
import org.bakasu.bakasu.ui.viewmodel.HomeUiAction
import org.bakasu.bakasu.ui.viewmodel.HomeUiEvent
import org.bakasu.bakasu.ui.viewmodel.HomeUiState
import org.bakasu.bakasu.ui.viewmodel.HomeViewModel
import org.koin.compose.koinInject
import com.topjohnwu.superuser.ShellUtils
import org.bakasu.bakasu.data.shell.KsuCliRepository
import org.koin.compose.viewmodel.koinViewModel

/**
 * @author ShirkNeko
 * @date 2025/9/29.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePage(
    bottomPadding: Dp,
) {
    val context = LocalContext.current
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatch(HomeUiAction.AwaitInitialData)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.Error -> if (event.message.isNotBlank()) {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ReportDrawnWhen { uiState.isInitialDataLoaded }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(
            initialHeightOffset = -154f,
            initialHeightOffsetLimit = -154f, // from debugger
        ),
    )
    val scrollState = rememberScrollState()
    val navigator = LocalNavigator.current
    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()
    var showJailbreakLog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                uiState = uiState,
                onReboot = { viewModel.dispatch(HomeUiAction.Reboot(it)) },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = adaptiveScaffoldWindowInsets(includeBottom = false),
        snackbarHost = {
            SwipeableSnackbarHost(
                modifier = Modifier.padding(bottom = bottomPadding),
                hostState = LocalSnackbarHost.current,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blurSource()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding() + 2.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // 状态卡片
            if (uiState.isCoreDataLoaded) {
                if (uiState.systemStatus.isManager && !uiState.systemStatus.isFullFeatured) {
                    if ((
                            uiState.systemStatus.kernelUAPIVersion
                                ?: 1
                            ) > uiState.systemStatus.managerUAPIVersion
                    ) {
                        WarningCard(
                            message = stringResource(R.string.require_manager_version),
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                // 合并版：安装页已隐藏，点击不再跳转
                            },
                        )
                    } else {
                        WarningCard(
                            message = if (uiState.systemStatus.lkmMode == true) {
                                stringResource(R.string.require_kernel_version)
                            } else {
                                stringResource(R.string.require_kernel_version_gki)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                // 合并版：安装页已隐藏，点击不再跳转
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 警告信息
                if (BuildConfig.DEBUG) {
                    WarningCard(
                        message = stringResource(R.string.debug_version_notice),
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (!uiState.systemStatus.isOfficialSignature) {
                    WarningCard(
                        message = stringResource(
                            R.string.unofficial_version_notice,
                            stringResource(R.string.app_name),
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (BuildConfig.IS_PR_BUILD || uiState.systemStatus.isPrBuild) {
                    WarningCard(
                        message = stringResource(
                            id = R.string.home_pr_build_warning,
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (uiState.systemStatus.kernelPatchImplementation == KernelPatchImplementation.OFFICIAL) {
                    WarningCard(
                        message = stringResource(
                            R.string.conflict_with_apatch,
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (uiState.systemStatus.ksuVersion != null && !uiState.systemStatus.isRootAvailable) {
                    WarningCard(
                        message = stringResource(id = R.string.grant_root_failed),
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                StatusCard(
                    uiState = uiState,
                    onClickInstall = {
                        // 合并版：越狱放到独立 :jb 进程执行，保持 crash_dump 的 fd 环境干净
                        context.startService(
                            Intent(context, df.root.JailbreakService::class.java),
                        )
                        showJailbreakLog = true
                    },
                    onClickJailbreak = {
                        loadingDialog.showLoading()
                        context.startService(Intent(context, MagicaService::class.java))
                        // Manager will be force-stopped and restarted by late-load on success.
                        // If that doesn't happen within timeout, jailbreak likely failed.
                        scope.launch(Dispatchers.IO) {
                            delay(30_000.milliseconds)
                            withContext(Dispatchers.Main) {
                                loadingDialog.hide()
                                Toast.makeText(
                                    context,
                                    R.string.jailbreak_timeout,
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (showJailbreakLog) {
                // 越狱跑在 :jb 独立进程，StateFlow 跨进程不可见，这里直接轮询它写出的日志文件
                val logFile = remember {
                    java.io.File(
                        context.getExternalFilesDir(null) ?: context.filesDir,
                        "jailbreak.log",
                    )
                }
                var logText by remember { mutableStateOf("") }
                var jbRunning by remember { mutableStateOf(true) }
                LaunchedEffect(showJailbreakLog) {
                    while (true) {
                        logText = runCatching { logFile.readText() }.getOrDefault("")
                        jbRunning = !logText.contains("=== 越狱引擎退出码")
                        kotlinx.coroutines.delay(600)
                    }
                }
                Dialog(onDismissRequest = { if (!jbRunning) showJailbreakLog = false }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            Box(Modifier.fillMaxWidth()) {
                                Text(
                                    "越狱日志",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                                TextButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                            as android.content.ClipboardManager
                                        cm.setPrimaryClip(
                                            android.content.ClipData.newPlainText(
                                                "jailbreak-log",
                                                logText,
                                            ),
                                        )
                                        Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                ) {
                                    Text("复制")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            val logScroll = rememberScrollState()
                            // 日志更新时自动滚到底部（越狱输出是持续追加的）
                            LaunchedEffect(logText) {
                                logScroll.animateScrollTo(logScroll.maxValue)
                            }
                            Text(
                                text = logText.ifEmpty { "等待输出…" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(logScroll),
                            )
                            Spacer(Modifier.height(8.dp))
                            if (!jbRunning) {
                                Text(
                                    "越狱已结束。请「大退」并重新打开本应用，管理器才能识别 root。" +
                                        "若不想手动重开，可点下方「软重启」。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (!jbRunning) {
                                    Button(
                                        onClick = {
                                            val ok = runCatching {
                                                Runtime.getRuntime().exec(
                                                    arrayOf("su", "-c", "setprop ctl.restart zygote"),
                                                ).waitFor() == 0
                                            }.getOrDefault(false)
                                            Toast.makeText(
                                                context,
                                                if (ok) "已请求软重启" else "软重启失败，请手动大退重启",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("软重启")
                                    }
                                }
                                Button(
                                    onClick = { showJailbreakLog = false },
                                    enabled = !jbRunning,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(if (jbRunning) "越狱进行中…" else "关闭")
                                }
                            }
                        }
                    }
                }
            }

            SelinuxPermissiveCard(uiState.systemStatus.isSELinuxPermissive)
            Spacer(modifier = Modifier.height(10.dp))
            ManagerUpdateCard(uiState.stableManagerUpdate)
            ManagerUpdateCard(uiState.betaManagerUpdate)
            if (uiState.isBetaManagerUpdateCheckFailed) {
                WarningCard(
                    message = stringResource(R.string.beta_update_check_failed),
                    icon = {
                        Icon(
                            imageVector = Icons.TwoTone.Error,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (uiState.isExtendedDataLoaded) {
                InfoCard(
                    systemStatus = uiState.systemStatus,
                    systemInfo = uiState.systemInfo,
                    isSimpleMode = uiState.isSimpleMode,
                    showHomeCardIcons = uiState.showHomeCardIcons,
                )
            }

            // 链接卡片
            if (!uiState.isSimpleMode) {
                DonateCard(uiState.showHomeCardIcons)
                LearnMoreCard(uiState.showHomeCardIcons)
            }

            Spacer(Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun ManagerUpdateCard(update: ManagerUpdateInfo?) {
    val visibilityState = remember { MutableTransitionState(false) }
    var displayedUpdate by remember { mutableStateOf<ManagerUpdateInfo?>(null) }

    LaunchedEffect(update) {
        if (update != null) displayedUpdate = update
        visibilityState.targetState = update != null
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        displayedUpdate?.let { updateInfo ->
            ManagerUpdateCardContent(updateInfo)
        }
    }
}

@Composable
private fun ManagerUpdateCardContent(updateInfo: ManagerUpdateInfo) {
    val context = LocalContext.current
    val permissionRequestInterface = LocalPermissionRequestInterface.current
    val enqueueManagerUpdate = koinInject<EnqueueManagerUpdateUseCase>()
    val channelTitle = stringResource(
        if (updateInfo.channel == ManagerUpdateChannel.STABLE) {
            R.string.manager_update_stable
        } else {
            R.string.manager_update_beta
        },
    )
    val message = if (updateInfo.channel == ManagerUpdateChannel.STABLE) {
        stringResource(R.string.new_version_available, updateInfo.versionCode)
    } else {
        stringResource(R.string.beta_version_available, updateInfo.versionCode)
    }
    val updateText = stringResource(R.string.module_update)
    val details = stringResource(
        R.string.manager_update_details,
        updateInfo.versionName,
        updateInfo.versionCode,
        updateInfo.abi,
    )
    val dialogContent = if (updateInfo.changelog.isBlank()) {
        details
    } else {
        "$details\n\n${updateInfo.changelog}"
    }
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            downloadManagerUpdate(
                context,
                permissionRequestInterface,
                updateInfo,
                enqueueManagerUpdate,
            )
        },
    )

    WarningCard(
        message = message,
        color = MaterialTheme.colorScheme.outlineVariant,
        icon = {
            Icon(
                imageVector = Icons.TwoTone.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        onClick = {
            updateDialog.showConfirm(
                title = channelTitle,
                content = dialogContent,
                markdown = updateInfo.changelog.isNotBlank(),
                confirm = updateText,
            )
        },
    )

    Spacer(modifier = Modifier.height(10.dp))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RebootDropdownItems(
    items: Map<Int, String>,
    onReboot: (String) -> Unit,
) {
    items.onEachIndexed { index, (id, reason) ->
        DropdownMenuItem(
            shape = MenuDefaults.itemShape(index, items.size).shape,
            text = { Text(stringResource(id)) },
            onClick = { onReboot(reason) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopBar(
    uiState: HomeUiState,
    onReboot: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val navigator = LocalNavigator.current

    LargeFlexibleTopAppBar(
        modifier = Modifier.blurEffect(),
        title = {
            Text(
                text = stringResource(R.string.app_name),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor =
                if (themeConfig.isEnableBlur) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
                },
            scrolledContainerColor =
                if (themeConfig.isEnableBlur) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
                },
        ),
        actions = {
            if (uiState.isCoreDataLoaded) {
                // SuSFS 配置按钮
                if (uiState.systemInfo.susfsVersionSupported) {
                    IconButton(onClick = {
                        navigator.push(Route.SuSFSConfig)
                    }) {
                        Icon(
                            imageVector = Icons.TwoTone.Tune,
                            contentDescription = stringResource(R.string.susfs_config_setting_title),
                        )
                    }
                }

                // 重启按钮
                var showDropdown by remember { mutableStateOf(false) }
                KsuIsValid(uiState.systemStatus) {
                    if (uiState.systemStatus.isRootAvailable) {
                        IconButton(onClick = {
                            showDropdown = true
                        }) {
                            Icon(
                                imageVector = Icons.TwoTone.PowerSettingsNew,
                                contentDescription = stringResource(id = R.string.reboot),
                            )

                            DropdownMenuPopup(expanded = showDropdown, onDismissRequest = {
                                showDropdown = false
                            }) {
                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShapes(),
                                ) {
                                    val pm =
                                        LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                                    var methods = mapOf(
                                        R.string.reboot to "",
                                        R.string.reboot_soft to "soft_reboot",
                                        R.string.reboot_recovery to "recovery",
                                        R.string.reboot_bootloader to "bootloader",
                                        R.string.reboot_download to "download",
                                        R.string.reboot_edl to "edl",
                                    )

                                    @Suppress("DEPRECATION")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                                        methods = methods + (R.string.reboot_userspace to "userspace")
                                    }

                                    RebootDropdownItems(methods, onReboot)
                                }
                            }
                        }
                    }
                }
            }
        },
        windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun StatusCard(
    uiState: HomeUiState,
    onClickInstall: () -> Unit = {},
    onClickJailbreak: () -> Unit = {},
) {
    val systemStatus = uiState.systemStatus
    val onClick = { _: Offset ->
        if (systemStatus.isRootAvailable || systemStatus.kernelVersion.isGKI()) {
            onClickInstall()
        }
    }

    when {
        systemStatus.ksuVersion != null -> {
            val workingModeText = when {
                systemStatus.isSafeMode -> stringResource(id = R.string.safe_mode)
                else -> stringResource(id = R.string.home_working)
            }

            val workingModeSurfaceText = when {
                systemStatus.lkmMode == true -> "LKM"
                else -> "Built-in"
            }

            SettingsBaseWidget(
                icon = Icons.TwoTone.TaskAlt,
                iconSize = 18.dp,
                title = workingModeText,
                description = stringResource(
                    R.string.home_short_info,
                    uiState.systemInfo.superuserCount,
                    uiState.systemInfo.moduleCount,
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                foreContent = {
                    Spacer(Modifier.width(8.dp))

                    // 工作模式标签
                    LabelText(
                        label = workingModeSurfaceText,
                        containerColor = MaterialTheme.colorScheme.primary,
                    )

                    if (systemStatus.isLateLoadMode) {
                        Spacer(Modifier.width(6.dp))
                        LabelText(
                            label = stringResource(id = R.string.jailbreak_mode),
                            containerColor = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // 架构标签
                    if (Os.uname().machine != "aarch64") {
                        Spacer(Modifier.width(6.dp))
                        LabelText(
                            label = Os.uname().machine,
                            containerColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                onClick = onClick,
            )
        }

        systemStatus.kernelVersion.isGKI() -> {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Warning,
                iconSize = 18.dp,
                isError = true,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                title = stringResource(R.string.home_not_installed),
                description = stringResource(R.string.home_click_to_install),
                onClick = onClick,
                trailingContent = if (systemStatus.isSELinuxPermissive) {
                    {
                        Button(
                            onClick = onClickJailbreak,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),

                        ) {
                            Text(stringResource(R.string.home_jailbreak))
                        }
                    }
                } else {
                    null
                },
            )
        }

        else -> {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Block,
                iconSize = 18.dp,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                isError = true,
                title = stringResource(R.string.home_unsupported),
                description = stringResource(R.string.home_unsupported_reason),
            )
        }
    }
}

@Composable
fun LearnMoreCard(
    showIcon: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.home_learn_kernelsu_url)

    SegmentedColumn(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.learn_more),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    ) {
        item {
            SettingsBaseWidget(
                icon = Icons.AutoMirrored.TwoTone.MenuBook.takeIf { showIcon },
                iconPlaceholder = false,
                title = stringResource(R.string.home_learn_kernelsu),
                description = stringResource(R.string.home_click_to_learn_kernelsu),
                onClick = {
                    uriHandler.openUri(url)
                },
            )
        }
    }
}

@Composable
fun DonateCard(
    showIcon: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    SegmentedColumn(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_support_title),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    ) {
        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.VolunteerActivism.takeIf { showIcon },
                iconPlaceholder = false,
                title = stringResource(R.string.home_support_title),
                description = stringResource(R.string.home_support_content),
                onClick = {
                    uriHandler.openUri("https://patreon.com/weishu")
                },
            )
        }
    }
}

@Composable
private fun InfoCard(
    systemStatus: KernelStatus,
    systemInfo: HomeSystemInfo,
    isSimpleMode: Boolean,
    showHomeCardIcons: Boolean,
) {
    val managersList = systemInfo.managersList

    SegmentedColumn(
        title = stringResource(R.string.home_version_info),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    ) {
        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Smartphone.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_device_model),
                description = systemInfo.deviceModel,
            )
        }

        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.DeveloperBoard.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_kernel),
                description = systemInfo.kernelRelease,
            )
        }

        item(
            visible = !isSimpleMode,
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Android.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_android_version),
                description = systemInfo.androidVersion,
            )
        }

        item(
            visible = systemStatus.isManager,
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Memory.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_kernel_version),
                description = systemStatus.ksuFullVersion.orEmpty(),
            )
        }

        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Tag.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_manager_version),
                description = "${systemInfo.managerVersion.first} (${systemInfo.managerVersion.second}/${systemInfo.managerVersion.third})",
            )
        }

        item(
            visible = !isSimpleMode && systemInfo.susfsEnabled && systemInfo.susfsVersion.isNotEmpty(),
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Settings.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_susfs_version),
                description = systemInfo.susfsVersion,
            )
        }
    }

    SegmentedColumn(
        title = stringResource(R.string.home_status_info),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    ) {
        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Security.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_selinux_status),
                description = systemInfo.selinuxStatus,
            )
        }

        item {
            val seccompDisplay = when (systemInfo.seccompStatus) {
                -1 -> stringResource(R.string.seccomp_status_not_supported)
                0 -> stringResource(R.string.seccomp_status_disabled)
                1 -> stringResource(R.string.seccomp_status_strict)
                2 -> stringResource(R.string.seccomp_status_filter)
                else -> stringResource(R.string.seccomp_status_unknown)
            }

            SettingsBaseWidget(
                icon = Icons.TwoTone.FilterList.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_seccomp_status),
                description = seccompDisplay,
            )
        }

        item(
            visible = !isSimpleMode && managersList != null,
        ) {
            val signatureMap =
                managersList?.managers.orEmpty().groupBy { it.signatureIndex }
            val managersText = buildString {
                signatureMap.toSortedMap().forEach { (signatureIndex, managers) ->
                    append(managers.joinToString(", ") { "UID: ${it.uid}" })
                    append(" ")
                    append(
                        when (signatureIndex) {
                            0 -> "(${stringResource(R.string.app_name)})"

                            255 -> "(${stringResource(R.string.dynamic_managerature)})"

                            else -> if (signatureIndex >= 1) {
                                "(${
                                    stringResource(
                                        R.string.signature_index,
                                        signatureIndex,
                                    )
                                })"
                            } else {
                                "(${stringResource(R.string.unknown_signature)})"
                            }
                        },
                    )
                    append(" | ")
                }
            }.trimEnd(' ', '|')

            SettingsBaseWidget(
                icon = Icons.TwoTone.Group.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.multi_manager_list),
                description = managersText.ifEmpty { stringResource(R.string.no_active_manager) },
            )
        }

        item(
            visible = !isSimpleMode && systemStatus.isFullFeatured,
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Tune.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_hook_type),
                description = systemStatus.hookType,
            )
        }

        item(
            visible = !isSimpleMode && systemInfo.zygiskImplement.isNotEmpty() && systemInfo.zygiskImplement != "None",
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Extension.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_zygisk_implement),
                description = systemInfo.zygiskImplement,
            )
        }

        item(
            visible = !isSimpleMode && systemInfo.metaModuleImplement.isNotEmpty() && systemInfo.metaModuleImplement != "None",
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Extension.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_meta_module_implement),
                description = systemInfo.metaModuleImplement,
            )
        }
    }
}

/** 合并版新增：一键把 SELinux 切到 Permissive（需已通过越狱获得 root）。 */
@Composable
private fun SelinuxPermissiveCard(currentlyPermissive: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ksuCli: KsuCliRepository = koinInject()
    var permissive by remember { mutableStateOf(currentlyPermissive) }
    var busy by remember { mutableStateOf(false) }

    SettingsBaseWidget(
        icon = Icons.TwoTone.Warning,
        iconSize = 18.dp,
        title = "宽容 SELinux",
        description = if (permissive) {
            "当前为 Permissive（已临时关闭强制模式）"
        } else {
            "点击临时关闭 SELinux 强制模式，便于注入与调试"
        },
        onClick = {
            if (busy) return@SettingsBaseWidget
            busy = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        // 本机没有 /system/bin/su，root 由 ksud 提供，必须走 KsuCli 的 root shell
                        val cmdOk = ksuCli.withNewRootShell {
                            ShellUtils.fastCmdResult(this, "setenforce 0")
                        }
                        val enforce = runCatching {
                            com.topjohnwu.superuser.io.SuFile("/sys/fs/selinux/enforce")
                                .readText().trim()
                        }.getOrDefault("")
                        cmdOk && enforce == "0"
                    }.getOrDefault(false)
                }
                permissive = ok
                busy = false
                Toast.makeText(
                    context,
                    if (ok) "已设为 Permissive" else "设置失败（需要先越狱获得 root）",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
    )
}
