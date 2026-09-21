# Plan — 把豆包语音自动触发集成进 Reny

> 状态：**代码已完成，云端构建通过；待真机验证语音效果**
> 来源项目：`C:\Users\Kano\Documents\ChatGPT\DouBao`
> 目标项目：`C:\Users\Kano\Documents\ChatGPT\Reny`

---

## 执行结果（2026-09-21）

Phase 1–4 已完成，提交 `ebf4657`，云端构建 `35600420928` 全绿：

| 检查项 | 结果 |
|--------|------|
| `./gradlew ktlintCheck detekt` | BUILD SUCCESSFUL（`detekt` 与 `ktlintMainSourceSetCheck` 均实际执行） |
| `./gradlew assembleRelease` | BUILD SUCCESSFUL |
| APK 已发布 | Release `latest`，2.09 MB |

对构建产物做了逐项核验（`unzip` + dex/manifest/arsc 字节级检查）：

| 核验项 | 结果 |
|--------|------|
| `assets/xposed_init` | 内容为 `io.github.kanou.reny.RenyVoiceHook` |
| R8 是否保留入口类 | `classes.dex` 中存在 `RenyVoiceHook`，keep 规则生效 |
| 4 个 Xposed 元数据 | `xposedmodule` / `xposeddescription` / `xposedminversion` / `xposedscope` 均在 manifest 中 |
| `xposedscope` 取值 | `com.bytedance.android.doubaoime` |
| 关键字面量 | `DoFunctionKey`、`onStartInputView`、`onFinishInputView`、`ImeService`、`KeyboardJni`、`io.github.kanou.reny.voice` 均在 dex 中 |
| 原有 App 功能 | `InputActivity`、`SettingsActivity`、`TermuxSettingsActivity`、Termux 权限、任务栈 affinity 均未受影响 |
| APK 签名 | v2/v3 签名块存在 |

**计划阶段的风险全部消解或降级：**

- `privateImeOptions` 是否会送达 —— 已通过反编译 Compose 1.12.1 字节码确认完整链路：
  `PlatformImeOptions.getPrivateImeOptions()` → `ImeOptions.getPlatformImeOptions()`
  → `TextInputServiceAndroid_androidKt.update()` 写入
  `EditorInfo.privateImeOptions`。风险由「中」降为「低」，但那行诊断日志保留，
  真机上仍可一眼确认。
- R8 混淆入口类 —— keep 规则已生效并已在 APK 中核验。
- ktlint / detekt —— 云端已实际执行并通过。

**剩余唯一未验证项：真机上的语音触发效果**（需要装有 LSPosed + 豆包输入法的手机），
步骤见下方 Phase 5。

---

## 0. 已定决策

| 项目 | 决定 |
|------|------|
| 集成形态 | **合并为单个 APK**。Reny 自身即 LSPosed 模块，不再需要「豆包语音桥」 |
| 识别结果 | **只触发，不上屏**。Reny 不拦截文本，豆包自己把文字上屏到输入框 |
| 触发范围 | **仅 Reny 发送栏**。用 `privateImeOptions` 标记区分发送栏与设置页输入框 |
| Xposed API | **沿用经典 API**（`de.robv.android.xposed:api:82` + `assets/xposed_init`） |
| 状态指示 | **不加**。不引入 PING/STATUS 广播，不做设置页状态行 |
| 计划粒度 | 本文件，分阶段详细，含文件级改动 |

---

## 1. DouBao 现有实现是怎么跑通的

### 1.1 一句话原理

豆包输入法的语音识别**完全在其自身进程内完成**，不依赖系统 `SpeechRecognizer`。
所以只要把 LSPosed 模块注入 `com.bytedance.android.doubaoime` 进程，就能在
「键盘刚显示、输入框已就绪」这一刻，直接复刻工具栏「点击说话」按钮的**原生调用路径**。

### 1.2 触发链路（已验证成功的那一条）

```
Reny 输入框获得焦点
  → 系统拉起豆包 ImeService
  → ImeService.onStartInputView(EditorInfo)        ← Hook 点
  → 校验 editorInfo（当前是 Reny 的输入框吗？）
  → 延迟 500ms（等键盘 UI 完成布局）
  → KeyboardJni.DoFunctionKey(6, "tool")           ← 真正的触发指令
      → 内部执行 AsrManager.Q0("tool")
      → 成功后调 InputView.f0(true)  显示原生 ASR 波纹
  → 麦克风录音 → SAMICore SDK → WebSocket → 豆包自己把文本上屏
```

`DoFunctionKey(6, "tool")` 就是工具栏那个「点击说话」按钮按下时走的方法，
参数 `6` 是功能键 ID，`"tool"` 是来源标识。因为走的是原生 UI 路径，
**波纹动画、权限检查、上屏行为全都由豆包自己负责**，模块不需要碰任何一处。

### 1.3 关键代码位置

| 文件 | 职责 |
|------|------|
| `DoubaoVoiceXposed/app/src/main/java/com/kanou/doubaovoicexposed/DoubaoVoiceHook.kt` | 全部逻辑（约 150 行，单文件） |
| `DoubaoVoiceXposed/app/src/main/assets/xposed_init` | 一行，声明入口类全名 |
| `DoubaoVoiceXposed/app/src/main/AndroidManifest.xml` | 4 个 `meta-data`：`xposedmodule` / `xposeddescription` / `xposedminversion` / `xposedscope` |
| `DoubaoVoiceXposed/app/src/main/res/values/arrays.xml` | `xposedscope` 数组，内容为 `com.bytedance.android.doubaoime` |

### 1.4 逐段拆解 `DoubaoVoiceHook.kt`

| 片段 | 作用 | 迁移到 Reny 时 |
|------|------|----------------|
| `handleLoadPackage` 过滤 `TARGET_PACKAGE` + 进程名 | 只在豆包主进程生效，其他 App 零开销 | 原样保留 |
| `hookImeService` → `ImeService.onCreate`，存 `imeContext`，注册 PING 广播 | 给外部 App 做状态自检 | **删除**（已定决策：不做状态指示） |
| `hookInputViewLifecycle` → `onStartInputView` + `onFinishInputView` | 生命周期检测 | 原样保留 |
| `maybeAutoStartForEditor` → 校验 `editorInfo.packageName` | 只对本 App 输入框触发 | **改为** 包名 + `privateImeOptions` 双校验 |
| `autoTriggerArmed` / `autoTriggerGeneration` | 防止重复触发、防止过期延迟任务误触发 | 原样保留（这是关键逻辑，不是冗余） |
| `requestNativeToolbarAsr` → `DoFunctionKey(6, "tool")` | 触发本体 | 原样保留 |
| `sendStatus` / `ACTION_STATUS` / `ACTION_PING` | 广播状态回传 | **删除** |

### 1.5 触发后为什么不会误伤其他 App

`onStartInputView` 每次键盘弹出都会调用，`editorInfo.packageName` 就是**当前拥有焦点的输入框所属 App 的包名**。
DouBao 项目用它判断「是不是自己在输入」，Reny 沿用同一机制。

---

## 2. 迁移到 Reny 的差异点

| 维度 | DouBao 项目 | Reny 项目 | 处理 |
|------|------------|-----------|------|
| 形态 | 纯 Xposed 模块 | 普通 Compose App | 合二为一：App 同时是 Xposed 模块 |
| 包名 | `com.kanou.doubaovoicexposed` | `io.github.kanou.reny` | 常量改为 `io.github.kanou.reny` |
| 入口类名 | `com.kanou.doubaovoicexposed.DoubaoVoiceHook` | — | 新建 `io.github.kanou.reny.RenyVoiceHook` |
| 输入框控件 | 传统 `EditText` | Compose `BasicTextField` | 需要补 `KeyboardOptions.platformImeOptions` 打标记 |
| 设置页输入框 | 无需区分 | `TermuxSettingsActivity` 有 3 个 `OutlinedTextField` | 必须区分，否则改 Termux 路径时会误弹语音 |
| 构建配置 | AGP 8.5.2 / Kotlin 1.9.24 / minSdk 29 / compileSdk 34 | AGP 9.4.1 / Kotlin 2.2.10 / minSdk 35 / compileSdk 37 | 以 Reny 为准，不动版本 |
| 依赖仓库 | 有 `https://api.xposed.info/` | 无 | **必须补进 `settings.gradle.kts`** |
| 代码压缩 | `isMinifyEnabled = false` | `isMinifyEnabled = true` + `isShrinkResources = true` | **必须补 R8 keep 规则**，否则 `xposed_init` 找不到类 |
| 代码检查 | 无 | ktlint + detekt（`config/detekt.yml`） | Hook 代码必须一次过检 |
| 签名 | 硬编码 keystore 在仓库里 | 走 GitHub Secrets | 保持不变 |
| 发布 | 独立 workflow | `.github/workflows/release.yml` | 不需要改 |

---

## 3. 分阶段实施计划

### Phase 1 — 构建骨架

**目标**：让 Gradle 能认识 Xposed API，并让 APK 带上「我是 Xposed 模块」的元数据。

1. `settings.gradle.kts`
   在 `dependencyResolutionManagement.repositories` 中补一行：
   ```kotlin
   maven { url = uri("https://api.xposed.info/") }
   ```
   位置放在 `mavenCentral()` 之后。

2. `gradle/libs.versions.toml`
   新增：
   ```toml
   xposedApi = "82"

   [libraries]
   xposed-api = { group = "de.robv.android.xposed", name = "api", version.ref = "xposedApi" }
   ```

3. `app/build.gradle.kts`
   新增依赖（`compileOnly`，不会打进 APK）：
   ```kotlin
   compileOnly(libs.xposed.api)
   ```

4. 新建 `app/src/main/assets/xposed_init`，内容**仅一行**：
   ```
   io.github.kanou.reny.RenyVoiceHook
   ```

5. 新建 `app/src/main/res/values/arrays.xml`：
   ```xml
   <resources>
       <string-array name="xposedscope">
           <item>com.bytedance.android.doubaoime</item>
       </string-array>
   </resources>
   ```

6. `app/src/main/AndroidManifest.xml`
   在 `<application>` 内、`<activity>` 之前插入 4 个 `meta-data`：
   ```xml
   <meta-data android:name="xposedmodule" android:value="true" />
   <meta-data android:name="xposeddescription" android:value="为 Reny 输入框自动拉起豆包原生语音识别" />
   <meta-data android:name="xposedminversion" android:value="93" />
   <meta-data android:name="xposedscope" android:resource="@array/xposedscope" />
   ```
   **不需要**新增任何权限、Activity、Service。

**验收**：`./gradlew assembleRelease` 通过，`unzip -p app-release.apk assets/xposed_init` 能看到入口类名。

---

### Phase 2 — 给发送栏打标记

**目标**：让发送栏的输入框在 `EditorInfo.privateImeOptions` 里留下一个只有 Reny 认识的值。

1. 新建 `app/src/main/java/io/github/kanou/reny/VoiceImeOption.kt`：
   ```kotlin
   package io.github.kanou.reny

   /** 发送栏输入框的私有 IME 标记，Xposed 模块靠它区分发送栏与设置页输入框。 */
   const val VOICE_IME_OPTION = "io.github.kanou.reny.voice"
   ```
   > 为什么要独立常量：`InputActivity`（写入方）和 `RenyVoiceHook`（读取方）
   > 分属 App 与 IME 两个进程，字面量只能有一处定义，避免写歪。

2. `app/src/main/java/io/github/kanou/reny/InputActivity.kt`
   只改 `BasicTextField` 一处，新增 `keyboardOptions` 参数：
   ```kotlin
   keyboardOptions =
       KeyboardOptions(
           platformImeOptions = PlatformImeOptions(VOICE_IME_OPTION),
       ),
   ```
   新增两个 import：
   ```kotlin
   import androidx.compose.foundation.text.KeyboardOptions
   import androidx.compose.ui.text.input.PlatformImeOptions
   ```
   > 已核实：`androidx.compose.foundation.text.KeyboardOptions` 与
   > `androidx.compose.ui.text.input.PlatformImeOptions` 均存在于当前
   > Compose 1.12.1 依赖中，`PlatformImeOptions` 确有 `privateImeOptions` 字段。

3. `TermuxSettingsActivity.kt` **完全不动**，它的 3 个输入框天然不带标记，因此不会触发语音。

**验收**：见 Phase 5 的日志确认步骤。

---

### Phase 3 — Hook 实现

**目标**：新建唯一一个 Hook 文件，去掉 DouBao 版本里所有与状态广播相关的代码。

新建 `app/src/main/java/io/github/kanou/reny/RenyVoiceHook.kt`。相对 DouBao 版本的变化：

| 变化 | 说明 |
|------|------|
| 包名/类名 | `io.github.kanou.reny.RenyVoiceHook` |
| `APP_PACKAGE` | `io.github.kanou.reny` |
| 新增 `VOICE_IME_OPTION` 校验 | `editorInfo.privateImeOptions == VOICE_IME_OPTION`，与包名同时成立才触发 |
| **删除** `hookImeService` | 不再需要 `Context` |
| **删除** `registerCommandReceiver` | 不做状态回传 |
| **删除** `sendStatus` / `ACTION_PING` / `ACTION_STATUS` | 同上 |
| **删除** `imeContext` / `isHooked` 字段 | 同上 |
| 保留 `TARGET_PACKAGE` / `IME_SERVICE` / `KEYBOARD_JNI` / `TRIGGER_SOURCE="tool"` / `TOOLBAR_START_ASR=6` / `AUTO_TRIGGER_DELAY_MS=500L` | 已验证的关键常量，一个都不改 |
| 保留 `autoTriggerArmed` / `autoTriggerGeneration` | 防重复触发与过期任务 |
| 异常捕获改 `runCatching` | detekt 的 `TooGenericExceptionCaught` 会拦 `catch (Throwable)` |
| 新增一行诊断日志 | 仅当包名命中时打印 `privateImeOptions`，用于首次真机确认标记是否送达 |

`handleLoadPackage` 的最终形态（示意）：

```kotlin
override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
    if (lpparam.packageName != TARGET_PACKAGE || lpparam.processName != TARGET_PACKAGE) return
    XposedBridge.log("$TAG loading in ${lpparam.processName}")
    runCatching { hookInputViewLifecycle(lpparam) }
        .onFailure { XposedBridge.log("$TAG hook failed: ${it.message}") }
}
```

触发判定最终形态（示意）：

```kotlin
private fun maybeAutoStartForEditor(editorInfo: EditorInfo?) {
    val packageName = editorInfo?.packageName ?: return
    if (packageName != APP_PACKAGE) return
    XposedBridge.log("$TAG privateImeOptions=${editorInfo.privateImeOptions}")
    if (editorInfo.privateImeOptions != VOICE_IME_OPTION) return
    if (!autoTriggerArmed) return

    autoTriggerArmed = false
    autoTriggerGeneration++
    val generation = autoTriggerGeneration

    Handler(Looper.getMainLooper()).postDelayed({
        if (generation == autoTriggerGeneration) {
            startVoiceRecognition()
        }
    }, AUTO_TRIGGER_DELAY_MS)
}
```

**风格约束**（必须一次过 ktlint + detekt）：
- ktlint 的 `function-naming` 只对 `@Composable` 放宽，`RenyVoiceHook` 里方法名照常小驼峰。
- detekt `LongMethod.allowedLines = 150`，本文件远低于此。
- 不写 `catch (e: Throwable)`；用 `runCatching`。
- 长行按现有代码风格折到约 100 列。

---

### Phase 4 — R8 keep 规则（**容易被忽略，但缺了模块必然静默失效**）

Reny 的 release 构建开了 `isMinifyEnabled = true`，R8 会把 `RenyVoiceHook` 改名，
于是 LSPosed 从 `assets/xposed_init` 读到 `io.github.kanou.reny.RenyVoiceHook` 却找不到这个类，
**模块会毫无提示地不生效**。

1. 新建 `app/proguard-rules.pro`：
   ```proguard
   # LSPosed 通过 assets/xposed_init 里的全限定名反射入口类，不能被混淆
   -keep class io.github.kanou.reny.RenyVoiceHook { *; }

   # Xposed API 是 compileOnly，运行时由框架提供
   -dontwarn de.robv.android.xposed.**
   ```

2. `app/build.gradle.kts` 的 `release` 块补 `proguardFiles`：
   ```kotlin
   release {
       isMinifyEnabled = true
       isShrinkResources = true
       proguardFiles(
           getDefaultProguardFile("proguard-android-optimize.txt"),
           "proguard-rules.pro",
       )
       signingConfig = signingConfigs.findByName("release")
   }
   ```

**验收**：构建后反查 APK 里确实留有该类名与 `xposed_init` 资产。

---

### Phase 5 — 云端构建与真机验证

1. 本地只做静态检查（本机无 Android 环境）：
   `./gradlew ktlintCheck detekt` 若无法运行，则完全依赖 GitHub Actions。
2. 推送后由 `.github/workflows/release.yml` 自动跑
   `ktlintCheck detekt` → `assembleRelease` → 发布到 `latest` Release。
3. 真机验证清单：
   - 装上豆包输入法 1.4.5，设为系统输入法，授予麦克风权限
   - 在 LSPosed 里启用 **Reny**，作用域勾选「豆包输入法」
   - 强制停止豆包输入法（或重启手机），让 Hook 重新注入
   - 打开 Reny 发送栏 → 键盘弹起 → **约 0.5 秒后自动出现原生语音波纹**
   - 打开 Reny 设置页 → 点 Termux 路径输入框 → **键盘弹起但不应触发语音**（验证标记生效）
   - 随便打开一个别的 App 的输入框 → 豆包键盘正常打字，**不触发语音**
4. 用 LSPosed 日志 / logcat 过滤 `[RenyVoice]` 观察：
   - `privateImeOptions=io.github.kanou.reny.voice` → 标记送达（关键确认点）
   - `DoFunctionKey(6, "tool") invoked` → 触发成功

---

## 4. 文件改动总清单

### 新增

| 文件 | 内容 |
|------|------|
| `Plan.md` | 本文件 |
| `app/src/main/assets/xposed_init` | 一行入口类名 |
| `app/src/main/res/values/arrays.xml` | `xposedscope` 数组 |
| `app/src/main/java/io/github/kanou/reny/VoiceImeOption.kt` | 私有 IME 标记常量 |
| `app/src/main/java/io/github/kanou/reny/RenyVoiceHook.kt` | Hook 全部逻辑 |
| `app/proguard-rules.pro` | R8 keep 规则 |

### 修改

| 文件 | 改动 |
|------|------|
| `settings.gradle.kts` | 加 `api.xposed.info` 仓库 |
| `gradle/libs.versions.toml` | 加 `xposedApi = "82"` 与 `xposed-api` 库 |
| `app/build.gradle.kts` | 加 `compileOnly(libs.xposed.api)`；release 加 `proguardFiles` |
| `app/src/main/AndroidManifest.xml` | 加 4 个 Xposed `meta-data` |
| `app/src/main/java/io/github/kanou/reny/InputActivity.kt` | `BasicTextField` 加 `keyboardOptions` + 2 个 import |
| `AGENTS.md` | 补上新文件说明与「Reny 同时是 LSPosed 模块」这一事实 |

### 不动

`SettingsActivity.kt`、`TermuxSettingsActivity.kt`、`TermuxRunner.kt`、`RenyConfig.kt`、
`SendBehavior.kt`、`TermuxExecutionMode.kt`、`ui/theme/*`、`.github/workflows/release.yml`、
以及所有现有字符串资源。

---

## 5. 风险点

| 风险 | 等级 | 说明与应对 |
|------|------|-----------|
| `privateImeOptions` 未送达 | 低（已验证） | 已反编译 Compose 字节码确认 `PlatformImeOptions` 会被写入 `EditorInfo.privateImeOptions`。真机上若日志显示 `privateImeOptions=null`，则退回到「只按包名判定」（接受设置页输入框也会触发语音，或给设置页输入框单独挂一个空标记） |
| R8 混淆掉入口类 | **中** | Phase 4 已覆盖。若忘记，症状是「模块显示已启用但完全没反应」，极易误判成 LSPosed 问题 |
| 豆包输入法升级后混淆名变化 | 中 | `ImeService` / `KeyboardJni` / `DoFunctionKey` 都是 v1.4.5 的名字。升级后若失效，按 `reverse-analysis/xposed-hook` 的「方法签名匹配」思路重新定位 |
| 触发时输入法工具栏状态不符 | 低 | `DoFunctionKey(6, "tool")` 走的是原生路径，DouBao 项目已验证可用；Reny 只是换了触发时机判定 |
| Reny 面板自身生命周期 | 低 | `InputActivity` 带 `noHistory` + `excludeFromRecents`；若用户点空白处关闭面板，语音随键盘收起而停止，属预期行为 |
| ktlint / detekt 未过 | 低 | 按 Phase 3 的风格约束编写；CI 会直接拦住 |
| 云编译首次失败 | 低 | 改动集中在 Gradle 与 manifest，出错信息可读；后续按 CI 日志迭代 |

---

## 6. 明确不做的事

- ❌ 不引入 AIDL / 广播 / PING 状态检测
- ❌ 不拦截 `Y.a` / `C1184w` 识别结果
- ❌ 不做音频注入（DouBao 文档里的「路径 B」）
- ❌ 不在 Reny 设置页加语音开关或状态行
- ❌ 不新建独立 Gradle 模块、不保留「豆包语音桥」APK
- ❌ 不改 Reny 现有的 compileSdk / minSdk / AGP / Kotlin 版本
- ❌ 不保留任何兼容分支或「备用方案」代码路径

---

## 7. 实施顺序一览

```
Phase 1  构建骨架        settings.gradle.kts / libs.versions.toml / build.gradle.kts
                         assets/xposed_init / arrays.xml / AndroidManifest.xml
   ↓
Phase 2  打标记          VoiceImeOption.kt（新增）/ InputActivity.kt（改 1 处）
   ↓
Phase 3  Hook            RenyVoiceHook.kt（新增，单文件）
   ↓
Phase 4  R8 keep         proguard-rules.pro（新增）/ build.gradle.kts（补 proguardFiles）
   ↓
Phase 5  云端 + 真机     GitHub Actions → 装 APK → LSPosed 启用 → 实测三场景
   ↓
收尾     文档            AGENTS.md 补文件说明
```
