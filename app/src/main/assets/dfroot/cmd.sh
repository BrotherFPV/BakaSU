#!/system/bin/sh
# 完全复刻 2026-11-25 22:08 的 _deploy/cmd.sh（当时验证成功的版本）：
#   ksupatch 只传 in/out 两个参数，不传 uid / 证书 —— 一旦传 uid 就会去 patch
#   track_throne 加冕点，而那个偏移在我们的 ko 上对不齐，ksupatch 会放弃并 rc=1。
#   加冕由内核模块按签名自动完成，这里不做任何用户空间触发。
[ "$(id -u)" = "0" ] || exit 0
D=/data/user_de/0/df.root
L=$D/loader.log

if ! grep -q '^kernelsu ' /proc/modules 2>/dev/null; then
  [ -f $D/kernelsu_base.ko ] || exit 0
  [ -x $D/ksupatch ] || exit 0
  {
    echo "[$(date '+%m-%d %H:%M:%S')] whoami: $(id 2>&1)"
    echo "[$(date '+%m-%d %H:%M:%S')] selinux: $(getenforce 2>&1)"
  } >> $L 2>&1
  setenforce 0 2>/dev/null
  echo 0 > /proc/sys/kernel/kptr_restrict 2>/dev/null
  echo "[$(date '+%m-%d %H:%M:%S')] ksupatch loading (2 args)" >> $L
  $D/ksupatch $D/kernelsu_base.ko $D/kernelsu_run.ko >> $L 2>&1
  echo "[$(date '+%m-%d %H:%M:%S')] ksupatch rc=$?" >> $L
  [ -f $D/kernelsu_run.ko ] && cp -f $D/kernelsu_run.ko $D/kernelsu_custom.ko 2>/dev/null
fi

if grep -q '^kernelsu ' /proc/modules 2>/dev/null; then
  echo "[$(date '+%m-%d %H:%M:%S')] module: $(grep '^kernelsu ' /proc/modules 2>&1)" >> $L
  dmesg 2>/dev/null | grep -iE "Crowning|is_manager|throne" | tail -10 >> $L
fi
echo "done"
echo "【提示】若管理器未显示 root，请大退并重新打开本应用（或点日志页的软重启）。" >> $L
exit 0
