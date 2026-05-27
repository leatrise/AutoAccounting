package net.ankio.auto.service

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import net.ankio.auto.service.api.ICoreService
import net.ankio.auto.service.ocr.FlipDetector
import net.ankio.auto.storage.Logger
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.intent.IntentType

/**
 * 翻转手机触发屏幕识别（OCR）的独立子服务。
 */
class FlipOcrTriggerService : ICoreService() {

    private var detector: FlipDetector? = null

    override fun onCreate(coreService: CoreService) {
        super.onCreate(coreService)
        if (!PrefManager.ocrFlipTrigger) {
            Logger.d("FlipOcrTrigger: disabled by preference")
            return
        }

        val sensorManager = coreService.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        detector = FlipDetector(sensorManager) {
            Logger.d("[Flip→OCR] device flip, dispatching CoreService OCR intent")
            val intent = Intent(coreService, CoreService::class.java).apply {
                putExtra("intentType", IntentType.OCR.name)
                putExtra("manual", true)
            }
            coreService.startService(intent)
        }.also { d ->
            if (d.start()) {
                Logger.d("FlipOcrTrigger: sensor listener started")
            } else {
                Logger.e("FlipOcrTrigger: unavailable (no gravity/accelerometer sensor)")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {
        // 仅依赖生命周期；OCR 由 Intent 分发给 [OcrService]
    }

    override fun onDestroy() {
        detector?.stop()
        detector = null
    }
}
