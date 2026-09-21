package io.github.kanou.reny

import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * LSPosed 模块：Reny 发送栏弹出豆包键盘后，自动复刻工具栏「点击说话」的原生调用路径。
 *
 * 模板来自 DouBao 项目的已验证实现，去掉了状态广播，并改用包名 +
 * [VOICE_IME_OPTION] 双校验，避免设置页输入框误触发。
 */
class RenyVoiceHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE || lpparam.processName != TARGET_PACKAGE) {
            return
        }

        XposedBridge.log("$TAG loading in ${lpparam.processName}")
        targetClassLoader = lpparam.classLoader
        runCatching { hookInputViewLifecycle(lpparam) }
            .onFailure { XposedBridge.log("$TAG hook failed: ${it.message}") }
    }

    private fun hookInputViewLifecycle(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            IME_SERVICE,
            lpparam.classLoader,
            "onStartInputView",
            EditorInfo::class.java,
            java.lang.Boolean.TYPE,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    maybeAutoStartForEditor(param.args?.getOrNull(0) as? EditorInfo)
                }
            },
        )

        XposedHelpers.findAndHookMethod(
            IME_SERVICE,
            lpparam.classLoader,
            "onFinishInputView",
            java.lang.Boolean.TYPE,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    autoTriggerGeneration++
                    autoTriggerArmed = true
                }
            },
        )

        XposedBridge.log("$TAG input view lifecycle hooks registered")
    }

    private fun maybeAutoStartForEditor(editorInfo: EditorInfo?) {
        if (editorInfo == null || editorInfo.packageName != APP_PACKAGE) return

        // 首次真机确认 Compose 是否把 PlatformImeOptions 写进了 EditorInfo
        XposedBridge.log("$TAG editor privateImeOptions=${editorInfo.privateImeOptions}")
        if (editorInfo.privateImeOptions != VOICE_IME_OPTION || !autoTriggerArmed) return

        autoTriggerArmed = false
        autoTriggerGeneration++
        val generation = autoTriggerGeneration

        val trigger =
            Runnable {
                if (generation == autoTriggerGeneration) {
                    requestNativeToolbarAsr()
                }
            }
        Handler(Looper.getMainLooper()).postDelayed(trigger, AUTO_TRIGGER_DELAY_MS)
    }

    /**
     * 完全复刻工具栏「点击说话」的调用链：DoFunctionKey(6, "tool")。
     * 它内部执行 Q0("tool")，成功后调用 InputView.f0(true) 显示原生 ASR 波纹。
     */
    private fun requestNativeToolbarAsr() {
        val classLoader = targetClassLoader
        if (classLoader == null) {
            XposedBridge.log("$TAG target class loader unavailable")
            return
        }

        val result =
            runCatching {
                XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass(KEYBOARD_JNI, classLoader),
                    "DoFunctionKey",
                    TOOLBAR_START_ASR,
                    TRIGGER_SOURCE,
                )
            }
        if (result.isSuccess) {
            XposedBridge.log("$TAG DoFunctionKey($TOOLBAR_START_ASR, \"$TRIGGER_SOURCE\") invoked")
        } else {
            XposedBridge.log("$TAG native toolbar path unavailable: ${result.exceptionOrNull()}")
        }
    }

    companion object {
        private const val TAG = "[RenyVoice]"
        private const val TARGET_PACKAGE = "com.bytedance.android.doubaoime"
        private const val IME_SERVICE = "com.bytedance.android.doubaoime.ImeService"
        private const val KEYBOARD_JNI = "com.bytedance.android.doubaoime.KeyboardJni"
        private const val APP_PACKAGE = "io.github.kanou.reny"

        // KeyboardJni.DoFunctionKey(6, "tool") 是工具栏「点击说话」的 native 入口
        private const val TOOLBAR_START_ASR = 6
        private const val TRIGGER_SOURCE = "tool"

        // 等键盘 UI 完成布局后再触发，否则原生 ASR 波纹拿不到已就绪的 InputView
        private const val AUTO_TRIGGER_DELAY_MS = 500L

        private var targetClassLoader: ClassLoader? = null
        private var autoTriggerArmed = true
        private var autoTriggerGeneration = 0
    }
}
