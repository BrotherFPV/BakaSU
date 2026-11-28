package df.root

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 合并版越狱入口：把 DFRoot 的 exploit 引擎接到 BakaSU 管理器里。
 *
 * - 自身 APK 内已打包 libksud.so，因此 ExploitRunner 的 hasKsud() 会认可本应用
 * - 释放 ksupatch / kernelsu.ko / cmd.sh 到 /data/user_de/0/df.root（引擎的硬编码路径）
 * - 引擎执行 cmd.sh 完成内核模块加载与加冕
 */
object DfRootManager {

    const val DE_DIR = "/data/user_de/0/df.root"

    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    val isRunning: Boolean get() = _running.value

    /** 日志落盘位置：/sdcard/Android/data/df.root/files/jailbreak.log，adb 可直接 pull。 */
    private var logFile: File? = null

    fun clearLog() {
        _log.value = ""
    }

    private fun append(text: String) {
        _log.value += text
        // 双通道：logcat 永远可抓（不依赖 debuggable / 外部存储可用性）
        runCatching { android.util.Log.i("DfRootJB", text.trimEnd()) }
        runCatching { logFile?.appendText(text) }
    }

    private fun prepareLogFile(ctx: Context) {
        val dir = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        runCatching { if (!dir.exists()) dir.mkdirs() }
        logFile = File(dir, "jailbreak.log")
        runCatching { logFile?.writeText("") }
        android.util.Log.i("DfRootJB", "log file -> ${logFile?.absolutePath} exists=${logFile?.exists()}")
    }

    /** 引擎把关键输出写在私有目录，这里收回内存并落到外部文件，便于 adb 取证。 */
    private fun collectArtifacts() {
        val devDf = File("/dev/df").exists()
        runCatching {
            val st = android.system.Os.stat("/dev/df")
            append(
                "devdfStat: mode=${(st.st_mode and 0x1FF).toString(8)} uid=${st.st_uid} " +
                    "rdev=${st.st_rdev} isChardev=${(st.st_mode and 0xF000) == 0x2000}\n",
            )
        }.onFailure { append("devdfStat failed: ${it.message}\n") }
        val mods = runCatching {
            File("/proc/modules").readLines()
                .filter { it.contains("ksu", ignoreCase = true) }
                .joinToString(" | ")
        }.getOrDefault("?")
        append("\ncheck: /dev/df=$devDf\n")
        append("check: ksu_modules=${mods.ifEmpty { "none" }}\n")

        listOf("loader.log", "cmd.out", "diag.txt").forEach { name ->
            val f = File(DE_DIR, name)
            if (f.exists() && f.length() > 0L) {
                append("\n--- $name ---\n")
                runCatching {
                    val text = f.readText()
                    // bootstrap 每轮都会往 diag.txt 追加，内容高度重复，只取最后一轮
                    if (name == "diag.txt") {
                        append(text.lines().takeLast(15).joinToString("\n"))
                    } else {
                        append(text)
                    }
                }
                append("\n")
            } else {
                append("\n--- $name --- (missing/empty)\n")
            }
        }
    }

    private fun ensureToolchain(ctx: Context) {
        val dir = File(DE_DIR)
        if (!dir.exists()) dir.mkdirs()

        // 旧版本遗留的 kernelsu.ko 会让 bootstrap 抢先直接 insmod（未修补符号，必失败并打断流程）
        runCatching { File(dir, "kernelsu.ko").delete() }
        runCatching { File(dir, "kernelsu_auto.ko").delete() }
        runCatching { File(dir, "loader.log").delete() }
        runCatching { File(dir, "loader.lock").delete() }
        runCatching { File(dir, ".ksupatch_started").delete() }
        // diag.txt / cmd.out 由 bootstrap 每轮追加，不清理会一直膨胀到上限
        runCatching { File(dir, "diag.txt").delete() }
        runCatching { File(dir, "cmd.out").delete() }

        // 每次都从 APK 覆盖：只按"文件是否存在"判断会导致升级后设备上仍是旧 ko，
        // 曾因此出现 ksupatch 拿 251 符号的旧 ko 去套 263 的偏移，直接 rc=1。
        listOf("ksupatch", "kernelsu_base.ko", "cmd.sh").forEach { name ->
            val out = File(dir, name)
            runCatching {
                ctx.assets.open("dfroot/$name").use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            }
            if (!out.canExecute()) out.setExecutable(true, false)
        }
    }

    /** 写入 su_manager，让 ExploitRunner 认可本应用；同时关闭 DFRoot 的软重启开关。 */
    private fun ensurePrefs(ctx: Context) {
        ctx.getSharedPreferences("dfroot", Context.MODE_PRIVATE)
            .edit()
            .putString("su_manager", ctx.packageName)
            .putBoolean("soft_reboot", false)
            .apply()
        // bootstrap 直接读取 DE 目录下的 XML，这里同步一份
        val prefsDir = File(DE_DIR, "shared_prefs")
        if (!prefsDir.exists()) prefsDir.mkdirs()
        File(prefsDir, "dfroot.xml").writeText(
            "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" +
                "<map>\n" +
                "    <string name=\"su_manager\">${ctx.packageName}</string>\n" +
                "    <boolean name=\"soft_reboot\" value=\"false\" />\n" +
                "</map>\n",
        )
    }

    /**
     * 关键：引擎写的 ko 是由 crash_dump 上下文里的 shellcode 去 insmod 的，
     * 那个进程既不是 root 也不是本应用，读不了 0700 的私有目录，
     * 于是报 "insmod log: open failed: Permission denied"。
     * 这里把目录放开到 0755、ko 放开到 0644（应用对自己的目录有 chmod 权限）。
     */
    private fun relaxPermissions() {
        val dir = File(DE_DIR)
        val notes = StringBuilder()

        fun step(name: String, block: () -> Boolean) {
            val r = runCatching(block)
            notes.append(name).append('=')
                .append(r.getOrElse { "EX:${it.javaClass.simpleName}(${it.message})" })
                .append(' ')
        }

        step("dirRead") { dir.setReadable(true, false) }
        step("dirExec") { dir.setExecutable(true, false) }
        step("dirWrite") { dir.setWritable(true, false) }
        val ko = File(dir, "ko")
        step("koCreate") { ko.exists() || ko.createNewFile() }
        step("koRead") { ko.setReadable(true, false) }
        step("ksupatchRead") { File(dir, "ksupatch").setReadable(true, false) }
        step("cmdShRead") { File(dir, "cmd.sh").setReadable(true, false) }
        step("koSize") { ko.length() > 0 }

        val line = "relaxPerm: $notes"
        android.util.Log.i("DfRootJB", line)
        append(line + "\n")

        // File.setXxx 返回 true 只代表调用没抛异常，不代表权限真的改了。
        // 用 stat 读真实 mode / owner，判断是不是目录 owner 不是本应用（历史残留）。
        runCatching {
            val st = android.system.Os.stat(DE_DIR)
            val mode = (st.st_mode and 0x1FF).toString(8)
            val myUid = android.os.Process.myUid()
            val info = "dirStat: mode=$mode uid=${st.st_uid} myUid=$myUid ownerOk=${st.st_uid == myUid}"
            android.util.Log.i("DfRootJB", info)
            append(info + "\n")
        }.onFailure {
            append("dirStat failed: ${it.javaClass.simpleName} ${it.message}\n")
        }

        runCatching {
            val koStat = android.system.Os.stat("$DE_DIR/ko")
            val koMode = (koStat.st_mode and 0x1FF).toString(8)
            val info = "koStat: mode=$koMode uid=${koStat.st_uid} size=${koStat.st_size}"
            android.util.Log.i("DfRootJB", info)
            append(info + "\n")
        }.onFailure {
            append("koStat failed: ${it.javaClass.simpleName} ${it.message}\n")
        }
    }

    /**
     * 引擎的 "launching bootstrap" 是在 crash_dump 上下文里 exec 的，
     * Enforcing 下 SELinux 通常不允许 crash_dump 执行 app 私有目录的二进制，
     * 所以 cmd.sh 从来没被跑到。这里由应用自己（app 域有 execute 权限）再执行一次；
     * 此时 dfroot.ko 已在内核、/dev/df 已就绪，bootstrap 可以据此拿到 root。
     */
    private fun selfRunBootstrap() {
        val boot = File(DE_DIR, "bootstrap")
        if (!boot.exists()) {
            append("self-run bootstrap: missing\n")
            return
        }
        append("self-run bootstrap: ${boot.absolutePath} exec=${boot.canExecute()}\n")
        runCatching {
            val p = ProcessBuilder(boot.absolutePath)
                .redirectErrorStream(true)
                .start()
            p.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { append("bootstrap> $it\n") }
            }
            append("bootstrap exit=${p.waitFor()}\n")
        }.onFailure {
            append("bootstrap EX: ${it.javaClass.simpleName} ${it.message}\n")
        }
    }


    /** 只有 /proc/modules 里出现 dfroot 才算真正加载；/dev/df 可能是残留节点。 */
    private fun isDfrootLoaded(): Boolean = runCatching {
        File("/proc/modules").readLines().any { it.startsWith("dfroot ") }
    }.getOrDefault(false)

    suspend fun run(ctx: Context): Int = withContext(Dispatchers.IO) {
        if (_running.value) return@withContext -1
        _running.value = true
        try {
            clearLog()
            prepareLogFile(ctx)
            append("=== 合并版越狱开始 ===\n")
            append("package=${ctx.packageName} dir=$DE_DIR\n")
            ensureToolchain(ctx)
            ensurePrefs(ctx)
            relaxPermissions()
            val nld = ctx.applicationInfo.nativeLibraryDir
            append(
                "self-check: libksud=${File(nld, "libksud.so").exists()} " +
                    "libdfroot=${File(nld, "libdfroot.so").exists()} " +
                    "libbootstrap=${File(nld, "libbootstrap.so").exists()}\n",
            )
            append(
                "self-check: cmd.sh=${File(DE_DIR, "cmd.sh").exists()} " +
                    "ksupatch=${File(DE_DIR, "ksupatch").exists()} " +
                    "ko=${File(DE_DIR, "kernelsu_base.ko").exists()}\n",
            )
            append("prefs su_manager=${ctx.packageName}\n")
            var rc = ExploitRunner.run(ctx) { msg -> append(msg) }
            append("\n=== 越狱引擎退出码: $rc ===\n")

            // 引擎在 "insmod log: open failed" 时会误报 rc=2，但 lsmod 里其实已经有 kernelsu。
            // 以 loader.log 里的 finit_module OK 为准。
            if (rc != 0) {
                val loaderLog = runCatching { File(DE_DIR, "loader.log").readText() }.getOrDefault("")
                if (loaderLog.contains("finit_module OK")) {
                    append("\n[修正] loader.log 显示 finit_module OK，内核模块已加载（引擎超时属误报）\n")
                    rc = 0
                }
            }
            collectArtifacts()
            if (rc == 0) {
                append("若内核模块已加载，管理器会在稍后识别到 root。\n")
            }
            rc
        } catch (t: Throwable) {
            append("ERROR: ${t.javaClass.simpleName}: ${t.message}\n")
            t.stackTraceToString().let { append(it) }
            -1
        } finally {
            _running.value = false
        }
    }
}
