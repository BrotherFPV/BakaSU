package df.root

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 越狱专用服务，运行在独立的 :jb 进程里。
 *
 * 为什么要独立进程：DFRoot 的 exploit 依赖 crash_dump32 里的 fd 传递，
 * 而 crash_dump 由应用进程 fork。BakaSU 主进程在启动时已加载 Koin / Compose /
 * DataStore 等大量组件，fd 与线程表很脏，会把 OUT_FD sanitize 掉，导致
 * insmod 打不开 ko。分体版 DFRoot 的 Application 几乎为空，所以成功率高。
 *
 * 这里让 :jb 进程的 Application 直接 return（不做任何初始化），
 * 执行内容与分体版完全一致：ExploitRunner + 内置 ko + 指定管理器为自己的包名。
 */
class JailbreakService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            runCatching { DfRootManager.run(this@JailbreakService) }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
