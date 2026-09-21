# AGENTS.md

### 用户特质

用户只具有少量的编程知识，你应该多询问用户，来了解用户的需求。

### 本机没有安卓环境

全部编译都放在 GitHub 云端

### gh 命令行工具

当前的环境变量：`GH_TOKEN` 已经包含可信的凭据
推送到云端仓库时，优先使用 gh 命令行工具

### 非常重要、需要遵循的原则

遵循 Go 语言"少就是多" 的哲学
1. 有且仅有一种方法把事情做好做对。
例如，最优的命令若因权限失败，不退回其他方案；我们应该给它权限，让命令跑通。
写代码遇到多方案，先问用户保留哪个，并说明各自优点。只有用户明确要多方案，才保留。

遵循Arch Linux哲学
1. 永远不考虑兼容性，像Arch Linux一样，只追求最新稳定的方案。

遵循"只留关键，不堆兼容"的原则
比如番茄炒蛋，用户提出“不够咸”，AI便改成“加盐版”，后来又提出多蛋、少番茄、加点醋；
若最终写成“番茄炒蛋（多蛋、少番茄、一点醋、加盐版）”，就是在堆兼容记录。
改代码也一样：直接简明地改。
非常重要的是：不必保留这类兼容代码和备注！

### 项目目录说明

- `app/src/main/java/io/github/kanou/reny/InputActivity.kt`：透明输入面板、输入框和发送按钮。
- `app/src/main/java/io/github/kanou/reny/SettingsActivity.kt`：设置页，包含主题和发送行为。
- `app/src/main/java/io/github/kanou/reny/SendBehavior.kt`：读取和保存发送行为偏好。
- `app/src/main/java/io/github/kanou/reny/TermuxRunner.kt`：检查 Termux 和权限，通过 `RUN_COMMAND` 调用 `~/reny.sh`。
- `app/src/main/java/io/github/kanou/reny/ui/theme/`：主题配色和主题偏好。
- `app/src/main/res/`：Android 字符串、颜色、主题和图标资源。
- `app/src/main/AndroidManifest.xml`：Activity、Termux 权限和包可见性声明。
- `app/build.gradle.kts`：Android 模块构建配置。
- `gradle/libs.versions.toml`：依赖和插件版本。
- `.github/workflows/release.yml`：云端 Release 构建和 APK 发布。