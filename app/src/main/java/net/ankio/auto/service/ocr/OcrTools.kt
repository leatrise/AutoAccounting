package net.ankio.auto.service.ocr

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.view.Display
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.SystemUtils
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * OCR 助手：负责截图与前台应用检测
 */
object OcrTools {

    private val SERVICE_CLASS = SelectToSpeakService::class.java
    // ======================== 核心功能 ========================

    /**
     * 当前前台应用包名（供 OCR 规则引擎 app 参数）。
     */
    suspend fun getTopApp(): String? {
        if (SelectToSpeakService.instance == null) {
            Logger.w("[OcrTopApp] SelectToSpeakService.instance is null (accessibility not connected)")
        }



        return SelectToSpeakService.instance?.getTopPackage()
    }

    /** 截取当前屏幕 */
    suspend fun takeScreenshot(outFile: File): Boolean = withContext(Dispatchers.IO) {
        val service = SelectToSpeakService.instance ?: return@withContext false

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext false

        suspendCancellableCoroutine { cont ->
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        val bitmap =
                            Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                        val success = bitmap?.let {
                            val saved = saveBitmap(it, outFile)
                            it.recycle()
                            saved
                        } ?: false
                        result.hardwareBuffer.close()
                        cont.resume(success)
                    }

                    override fun onFailure(errorCode: Int) {
                        Logger.e("Screenshot failed: $errorCode")
                        cont.resume(false)
                    }
                })
        }
    }

    /** 读取当前页面文本（无障碍树） */
    suspend fun collectPageText(maxDepth: Int = 50): String? = withContext(Dispatchers.Default) {
        val service = SelectToSpeakService.instance ?: return@withContext null
        service.collectPageText(maxDepth)
    }

    private fun saveBitmap(bitmap: Bitmap, file: File): Boolean = try {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    } catch (e: Exception) {
        false
    }

    // ======================== 权限管理 ========================

    fun hasPermission() = SystemUtils.isAccessibilityServiceEnabled(SERVICE_CLASS)


    /** 尝试开启无障碍：允许自动开启时先写 Secure Settings，否则直接跳系统设置页 */
    suspend fun requestPermission(allowAutoEnable: Boolean = true): Boolean {
        if (hasPermission()) return true

        if (allowAutoEnable && SystemUtils.canWriteSecureSettings()) {
            tryEnableViaSecureSettings()
            delay(800) // 等待服务启动
        }

        if (!hasPermission()) {
            withContext(Dispatchers.Main) { openSettings() }
            return false
        }
        return true
    }

    private suspend fun tryEnableViaSecureSettings() = withContext(Dispatchers.IO) {
        runCatching {
            SystemUtils.enableAccessibilityService(SERVICE_CLASS)
        }.onFailure {
            Logger.e("Enable accessibility via WRITE_SECURE_SETTINGS failed", it)
        }
    }

    private fun openSettings() {
        SystemUtils.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    suspend fun collapseStatusBar() {
        SelectToSpeakService.instance?.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
        )
        delay(500)
    }
}
