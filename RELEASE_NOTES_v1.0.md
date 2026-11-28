# v1.0

首个版本：为 **小米 Watch 5** 提供 KernelSU 管理器与一键越狱，无需电脑。

## 功能

- 一键越狱：内置 DFRoot exploit，点击即执行
- 内置 `kernelsu.ko`，并注入本应用证书白名单（724 B / `6ad4e577…`）
- 越狱在 `:jb` 独立进程执行，跳过 Application 初始化，避免 fd 污染导致 `insmod` 失败
- 越狱日志实时显示、自动滚动、一键复制
- 越狱结束后提供软重启按钮，并提示大退重开
- 主页提供宽容 SELinux 开关（需已取得 root）

## 依赖

- [BakaSU](https://github.com/Baka-SU/BakaSU)
- [cnwearoverlay](https://github.com/Star-ZER0/cnwearoverlay)

## 安装

```bash
adb uninstall df.root
adb install -r BakaSU_merged_m35.apk
```

## 已知问题

- 设备重启后需重新越狱
- 越狱成功率受 SELinux 状态影响；失败请重启后重试，不要在失败状态连续点击
- `/dev/df` 残留会被判为已越狱，重试前建议完整重启

## 许可

- 源码：GPL-3.0
- 内核模块（`kernelsu_base.ko` 等）：GPL-2.0

## 校验

- 签名证书 SHA-256：`6ad4e5776a8463a1777414dc801527da08aec474ea27e0823eadd279d888c294`
- 仅 v2 签名
