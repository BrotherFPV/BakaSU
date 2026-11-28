# v1.0

首次发布：将 DFRoot 越狱引擎与 BakaSU 管理器合并为单一 APK，在 Android 手表上实现一键越狱。

## 新增

- 一键越狱：内置 DFRoot exploit，无需电脑、无需分体安装
- 内置 `kernelsu.ko`（251 符号版），并注入本应用证书白名单（724B / `6ad4e577…`）
- 越狱在 `:jb` 独立进程执行，Application 跳过全部初始化，避免 fd 污染导致 `insmod` 失败
- 越狱日志实时显示、自动滚动、一键复制
- 越狱结束后提供「软重启」按钮，并提示大退重开
- 主页新增「宽容 SELinux」开关（需已取得 root）
- 集成 cnwearoverlay：修复小米 / OPPO 表冠触觉与 `WearHapticFeedbackConstants` 兼容

## 界面

- 侧边栏改为底部导航，高度 80dp → 40dp
- 移除全部 WebView 依赖（模块 README 改纯文本渲染，WebUI 入口禁用）
- 日志字体缩小至原 70%

## 安装

```bash
adb uninstall df.root
adb install -r BakaSU_merged_m35.apk
```

## 已知问题

- 设备重启后需重新越狱
- 越狱成功率受 SELinux 状态影响；失败请**重启后重试**，不要在失败状态连续点击
- `/dev/df` 残留会被判为已越狱，重试前建议完整重启

## 校验

- 签名证书 SHA-256：`6ad4e5776a8463a1777414dc801527da08aec474ea27e0823eadd279d888c294`
- 仅 v2 签名（无 v1 / v3）
