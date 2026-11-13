package com.gmail.heagoo.apkeditor.translate

import android.content.Context
import common.types.StringItem
import io.github.yinvoker.foxlet.Foxlet
import io.github.yinvoker.foxlet.LanguagePair
import io.github.yinvoker.foxlet.InstalledModel
import io.github.yinvoker.foxlet.Threading
import io.github.yinvoker.foxlet.Workload
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Local Firefox model execution. The caller obtains download consent before constructing. */
class FoxletInference(context: Context, from: String, to: String, private val trace: TranslationTrace,
                      private val cancelled: AtomicBoolean) : TranslationSession {
    private val job = Job()
    private val client: Foxlet
    private val first: InstalledModel
    private val second: InstalledModel?
    init {
        if (android.os.Build.VERSION.SDK_INT < 28 || !android.os.Process.is64Bit())
            throw IOException("Bergamot requires Android 9+ and an arm64 process")
        if (!android.os.Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a"))
            throw IOException("Bergamot requires arm64-v8a")
        trace.stage(TranslationTrace.Stage.LOAD_MODEL)
        val created = runBlocking(job) { Foxlet.create(context.applicationContext) {
            translation { threading = Threading.Auto(Workload.BATCH) }
        } }
        try {
            if (cancelled.get()) throw IOException("Translation cancelled")
            first = runBlocking(job) { created.models.prepare(LanguagePair(from, if (from != "en" && to != "en") "en" else to)) }
            second = if (from != "en" && to != "en") runBlocking(job) { created.models.prepare(LanguagePair("en", to)) } else null
            if (cancelled.get()) throw IOException("Translation cancelled")
            client = created
            trace.stage(TranslationTrace.Stage.MODEL_READY)
        } catch (e: Throwable) {
            runBlocking { created.shutdown() }
            throw e
        }
    }
    override fun translate(value: String, language: String, sourceQualifier: String, resourceName: String): String {
        if (cancelled.get()) throw IOException("Translation cancelled")
        trace.stringStart(value.length, 0)
        val output = runBlocking(job) {
            if (second != null) client.translator.translatePivot(value, first, second!!)
            else client.translator.translate(value, first)
        }
        if (cancelled.get()) throw IOException("Translation cancelled")
        trace.stage(TranslationTrace.Stage.VALIDATE)
        return TranslationOutput.clean(value, output)
    }
    override fun translateBatch(items: List<StringItem>, language: String, sourceQualifier: String): List<String> =
        throw UnsupportedOperationException()
    override fun requestCancel() { cancelled.set(true); job.cancel() }
    override fun close() { runBlocking { client.shutdown() }; job.cancel() }
}
