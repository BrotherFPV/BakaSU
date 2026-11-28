package org.bakasu.bakasu

import android.app.Application
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.bakasu.bakasu.di.appModules
import org.bakasu.bakasu.domain.usecase.InitializeApplicationUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KernelSUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            // 越狱专用进程：必须保持极简，不能初始化 Koin / Compose / DataStore，
            // 否则 crash_dump fork 时 fd 表被污染，exploit 的 OUT_FD 会被 sanitize。
            if (processName.endsWith(":jb")) {
                return
            }
            if (processName.endsWith("MagicaService")) {
                // avoid loading unnecessary thing when starting MagicaService
                return
            }
        }

        val koin = startKoin {
            androidLogger()
            androidContext(this@KernelSUApplication)
            modules(appModules)
        }.koin
        runBlocking(Dispatchers.IO) {
            koin.get<InitializeApplicationUseCase>()()
        }
    }
}
