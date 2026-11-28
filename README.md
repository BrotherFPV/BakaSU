# BakaSU Wear（DFRoot × BakaSU 合并版）

把 **DFRoot 越狱引擎**与 **BakaSU 管理器**合并成单一 APK，在小米 / OPPO 等 Android 手表上实现「一键越狱 + root 管理」，全程不需要电脑。

## 特性

- **一键越狱**：内置 DFRoot exploit（`libdfroot.so` + `libbootstrap.so`），点击即执行，无需分体安装
- **内置内核模块**：`kernelsu.ko` 已内置，并注入本应用证书白名单（724 字节 / `6ad4e577…`）
- **独立越狱进程**：越狱在 `:jb` 独立进程执行，跳过 Application 全部初始化，避免 Koin / Compose 污染 `crash_dump` 的 fd 环境
- **表冠触觉修复**：集成 [cnwearoverlay](https://github.com/Star-ZER0/cnwearoverlay)（小米 / OPPO 表冠震动、`WearHapticFeedbackConstants` 兼容）
- **纯 Compose 手表 UI**：无 WebView，底部导航 40dp，针对圆形小屏适配
- **实时日志**：越狱输出实时滚动显示、可一键复制；结束后提供「软重启」按钮
- **宽容 SELinux 开关**：主页一键 `setenforce 0`（需已取得 root）

## 构建

需要 JDK 21、Android SDK 37、NDK（用于 `llvm-strip`）。

```bash
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。

### 签名（重要）

内核按证书白名单校验管理器，因此 **必须使用证书为 `6ad4e5776a8463a1777414dc801527da08aec474ea27e0823eadd279d888c294`（724 字节）的密钥签名**，且：

- **只启用 v2 签名**（`--v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled false`）
- 带 v3 签名块会被内核的 APK 解析器判为无效

```bash
zipalign -f 4 app-release-unsigned.apk aligned.apk
apksigner sign --ks your.keystore \
  --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled false \
  --out app-release-signed.apk aligned.apk
```

## 安装

```bash
adb uninstall df.root          # 若装过 DFRoot 原版，包名冲突需先卸载
adb install -r BakaSU_merged.apk
```

打开应用 → 主页点「未越狱 / 点击执行 DFRoot 越狱」→ 查看日志 → 结束后按提示大退重开（或点「软重启」）。

## 已知限制

- **每次重启设备后需要重新越狱**（内核模块不持久化）
- **越狱存在概率性失败**：`crash_dump` 上下文在 SELinux Enforcing 下读取应用私有目录偶发被拒。失败时**重启设备后重试**，不要在失败状态上连续重试（会因页面状态残留而降低成功率）
- `/dev/df` 残留会被判定为"已越狱"，因此重试前建议完整重启
- 仅支持 `android14-5.15` KMI（arm64 / arm32）

## 目录说明

```
app/src/main/assets/dfroot/
  cmd.sh              # bootstrap 以 root 调用的加载脚本（ksupatch 2 参数）
  ksupatch            # 符号修补 + finit_module 装载器
  kernelsu_base.ko    # 251 符号版本，内含本应用签名白名单
app/src/main/java/df/root/
  ExploitRunner.java  # DFRoot exploit 主流程
  DfRootManager.kt    # 工具链释放、权限放开、日志
  JailbreakService.kt # :jb 独立进程入口
  IReporter.java
```

## 致谢

- [DFRoot](https://github.com/) —— exploit 引擎
- [BakaSU](https://github.com/Baka-SU/BakaSU) —— 管理器与内核模块
- [cnwearoverlay](https://github.com/Star-ZER0/cnwearoverlay) —— 手表触觉修复

## 免责声明

本项目仅供**自有设备**的越狱与 root 管理研究使用。使用本工具可能使设备失去保修、导致数据丢失或系统不稳定，一切后果由使用者自行承担。请勿用于未经授权的设备。
