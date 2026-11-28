# BakaSU Wear

为 **小米 Watch 5** 提供 KernelSU 管理器与一键越狱（内置 DFRoot exploit），全程无需电脑。

项目主页：**https://brotherfpv.fly**

## 特性

- **一键越狱**：内置 DFRoot exploit（`libdfroot.so` + `libbootstrap.so`），点击即执行
- **内置内核模块**：`kernelsu.ko` 随包分发，已注入本应用证书白名单（724 B / `6ad4e577…`）
- **独立越狱进程**：越狱在 `:jb` 进程内执行，跳过 Application 初始化，避免 `crash_dump` 的 fd 被污染
- **纯 Compose 界面**：无 WebView 依赖
- **实时日志**：越狱输出滚动显示、可复制；结束后提供软重启入口

## 依赖项目

- [BakaSU](https://github.com/Baka-SU/BakaSU) — 管理器与内核模块
- [cnwearoverlay](https://github.com/Star-ZER0/cnwearoverlay)

## 构建

需要 JDK 21、Android SDK 37。

```bash
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。

### 签名要求

内核按证书白名单校验管理器，**必须使用证书指纹为
`6ad4e5776a8463a1777414dc801527da08aec474ea27e0823eadd279d888c294`（724 字节）的密钥签名**，
并且：

- 只启用 v2 签名
- v1 / v3 签名块会导致内核的 APK 解析器判定无效

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

打开应用 → 主页点击「点击执行 DFRoot 越狱」→ 查看日志 → 结束后按提示大退重开（或使用软重启）。

## 已知限制

- 设备每次重启后需要重新越狱（内核模块不持久化）
- 越狱存在概率性失败：`crash_dump` 上下文在 SELinux Enforcing 下读取应用私有目录偶发被拒。失败时请**重启设备后重试**，不要在失败状态上连续重试
- `/dev/df` 残留会被判定为已越狱，重试前建议完整重启
- 仅适配 `android14-5.15` KMI（arm64 / arm32）

## 许可

| 范围 | 许可证 |
|---|---|
| 本项目源码（`app/src/main/java`、资源、脚本等） | **GPL-3.0**，见 [LICENSE](LICENSE) |
| 内核模块及相关二进制（`assets/dfroot/kernelsu_base.ko`、`libdfroot.so`、`libbootstrap.so`） | **GPL-2.0**，见 [LICENSE-GPL-2.0](LICENSE-GPL-2.0) |

内核模块源自 [BakaSU](https://github.com/Baka-SU/BakaSU)，按 GPL-2.0 分发。

## 免责声明

仅供自有设备的越狱与 root 管理研究使用。使用本工具可能导致设备失去保修、数据丢失或系统不稳定，后果由使用者自行承担。请勿用于未经授权的设备。
