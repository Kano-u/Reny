# AGENTS.md

## 编写代码的范例

编写的代码应当通过 ktlint 和 detekt 的检查。

## 当前环境

当前默认使用的Shell为：git bash。

## 用户特质

用户只具有少量的编程知识，你应该多询问用户，来了解用户的需求。

## 本机没有安卓环境

本机没有提供可供编程的Java开发环境。
全部编译都放在 GitHub 云端，并且会自动进行 ktlint 和 detekt 的检查。

## gh 命令行工具

当前的环境变量：`GH_TOKEN` 已经包含可信的凭据。
推送到云端仓库时，优先使用 gh 命令行工具。

## 非常重要、需要遵循的原则

遵循 Go 语言"少就是多" 的哲学
1. 有且仅有一种方法把事情做好做对。
例如，最优的命令若因权限失败，不退回其他方案；我们应该给它权限，让命令跑通。
写代码遇到多方案，先问用户保留哪个，并说明各自优点。只有用户明确要多方案，才保留。

遵循Arch Linux哲学。
1. 永远不考虑兼容性，像Arch Linux一样，只追求最新稳定的方案。

遵循"只留关键，不堆兼容"的原则
比如番茄炒蛋，用户提出“不够咸”，AI便改成“加盐版”，后来又提出多蛋、少番茄、加点醋；
若最终写成“番茄炒蛋（多蛋、少番茄、一点醋、加盐版）”，就是在堆兼容记录。
改代码也一样：直接简明地改。
非常重要的是：不必保留这类兼容代码和备注！

## 项目目录说明

- `app/src/main/java/io/github/kanou/reny/InputActivity.kt`：透明输入面板、输入框和发送按钮。
- `app/src/main/java/io/github/kanou/reny/SettingsActivity.kt`：设置页，包含主题和发送行为。
- `app/src/main/java/io/github/kanou/reny/RenyConfig.kt`：全部用户数据的唯一来源，读写应用私有目录下的 `config.json`。
- `app/src/main/java/io/github/kanou/reny/SendBehavior.kt`：发送行为枚举。
- `app/src/main/java/io/github/kanou/reny/TermuxRunner.kt`：检查 Termux 和权限，通过 `RUN_COMMAND` 调用 `~/reny.sh`。
- `app/src/main/java/io/github/kanou/reny/VoiceImeOption.kt`：发送栏输入框的私有 IME 标记常量。
- `app/src/main/java/io/github/kanou/reny/RenyVoiceHook.kt`：LSPosed 模块入口，在豆包输入法进程内自动拉起原生语音识别。
- `app/src/main/assets/xposed_init`：LSPosed 入口类声明，内容必须与 `RenyVoiceHook` 的全限定名一致。
- `app/proguard-rules.pro`：保留 Xposed 入口类，防止 R8 混淆后模块静默失效。
- `app/src/main/java/io/github/kanou/reny/ui/theme/`：主题配色和主题偏好。
- `app/src/main/res/`：Android 字符串、颜色、主题和图标资源。
- `app/src/main/AndroidManifest.xml`：Activity、Termux 权限、包可见性和 Xposed 元数据声明。
- `app/build.gradle.kts`：Android 模块构建配置。
- `gradle/libs.versions.toml`：依赖和插件版本。
- `.github/workflows/release.yml`：云端 Release 构建和 APK 发布。

## Reny 同时是 LSPosed 模块

Reny 的 APK 既是普通应用，也是 LSPosed 模块，作用域为豆包输入法
（`com.bytedance.android.doubaoime`）。发送栏弹出豆包键盘后会自动触发原生语音识别，
机制来自 DouBao 项目的逆向成果，详见 `Plan.md`。

改动相关代码时注意：

- 新增或重命名 `RenyVoiceHook` 时，必须同步改 `assets/xposed_init` 与 `proguard-rules.pro`
  两处，否则模块会毫无提示地失效。
- `RenyVoiceHook` 运行在豆包输入法进程内，与 `VOICE_IME_OPTION` 的写入方跨进程，
  字面量只能定义在 `VoiceImeOption.kt`。
- Xposed API 依赖必须是 `compileOnly`，运行时由 LSPosed 框架提供。