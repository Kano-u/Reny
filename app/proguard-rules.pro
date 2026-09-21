# LSPosed 通过 assets/xposed_init 里的全限定名反射入口类，不能被混淆
-keep class io.github.kanou.reny.RenyVoiceHook { *; }

# Xposed API 是 compileOnly，运行时由 LSPosed 框架提供
-dontwarn de.robv.android.xposed.**
