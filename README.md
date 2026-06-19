# 四象限待办 APK

这是一个本地离线优先的 Android 待办应用工程，核心功能包括：

- 文本录入待办
- 键盘听写或系统语音识别录入待办
- 设置截止时间和提醒时间
- 提醒方式可选择普通通知、震动、闹铃声、闹铃声+震动
- 勾选完成、删除待办
- 按“重要/紧急”自动归入四象限
- 中文/英文多语言，跟随系统语言
- 桌面小组件显示未完成任务，支持刷新、打开添加、点圆圈完成、系统缩放
- 本地 Room 数据库存储
- AlarmManager 本地提醒通知
- 开机后自动恢复未来提醒

## 技术栈

- Kotlin
- Jetpack Compose
- Room
- AlarmManager
- AndroidX NotificationCompat
- Android App Widget / RemoteViews

## 本机环境要求

推荐安装：

1. Android Studio
2. Android SDK，建议安装 API 37
3. JDK 17 或更新版本

安装 Android Studio 后，用 Android Studio 打开本目录：

```text
/Users/gaohuirong/Documents/Codex/TodoQuadrantAndroid
```

首次打开时让 Android Studio 同步 Gradle 依赖。

## 构建 APK

Android Studio 同步完成后，可以在终端运行：

```bash
./gradlew assembleDebug
```

Debug APK 生成位置通常是：

```text
app/build/outputs/apk/debug/app-debug.apk
```

如果项目还没有 Gradle wrapper，可以先在 Android Studio 里同步项目，或安装 Gradle 后运行：

```bash
gradle wrapper --gradle-version 9.6.0
./gradlew assembleDebug
```

## 当前本机测试方式

本项目在当前机器上已经使用项目内工具目录完成过一次真实模拟器测试。工具位于 `.local-tools/`，不会提交到 git。

进入项目目录：

```bash
cd /Users/gaohuirong/Documents/Codex/TodoQuadrantAndroid
```

构建 Debug APK：

```bash
JAVA_HOME="$PWD/.local-tools/jdk/Contents/Home" \
ANDROID_HOME="$PWD/.local-tools/android-sdk" \
GRADLE_USER_HOME="$PWD/.gradle-user-home" \
"$PWD/.local-tools/gradle/gradle-9.6.0/bin/gradle" assembleDebug
```

启动可见的本地 Android 模拟器：

```bash
ANDROID_HOME="$PWD/.local-tools/android-sdk" \
"$PWD/.local-tools/android-sdk/emulator/emulator" \
  @TodoQuadrantApi36 \
  -no-audio \
  -gpu host \
  -no-snapshot-load
```

另开一个终端，等待模拟器启动、安装并启动 App。如果 `adb devices` 显示多个模拟器，请把下面命令里的 `emulator-5556` 改成你看到的设备 id。

```bash
ANDROID_HOME="$PWD/.local-tools/android-sdk" \
"$PWD/.local-tools/android-sdk/platform-tools/adb" devices

ANDROID_HOME="$PWD/.local-tools/android-sdk" \
"$PWD/.local-tools/android-sdk/platform-tools/adb" -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk

ANDROID_HOME="$PWD/.local-tools/android-sdk" \
"$PWD/.local-tools/android-sdk/platform-tools/adb" -s emulator-5556 shell am start -n com.example.todoquadrant/.MainActivity
```

截图：

```bash
mkdir -p screenshots

ANDROID_HOME="$PWD/.local-tools/android-sdk" \
"$PWD/.local-tools/android-sdk/platform-tools/adb" exec-out screencap -p > screenshots/home.png
```

关闭模拟器：

```bash
ANDROID_HOME="$PWD/.local-tools/android-sdk" \
"$PWD/.local-tools/android-sdk/platform-tools/adb" emu kill
```

已验证 v0.1.0：

- Debug APK 构建成功。
- APK 能安装到 Android 36 arm64 模拟器。
- App 能启动并显示四象限首页。
- 文本待办能添加到“重要不紧急”象限。
- 勾选完成后，计数从“未完成 1 / 已完成 0”更新为“未完成 0 / 已完成 1”。

## v0.3.0 更新

v0.3.0 新增：

- 提醒方式：普通通知、震动、闹铃声、闹铃声+震动。
- 多语言：默认中文，系统语言为英文时自动显示英文界面。
- 桌面小组件：默认 4 x 3，可在桌面长按后拉伸，显示更多未完成任务。
- 小组件联动：App 内新增、编辑、完成、删除任务后会刷新小组件；小组件左侧圆圈可直接完成任务；刷新按钮可重新读取数据库；加号可打开 App 添加任务。

v0.3.0 模拟器验证：

- Debug APK 构建成功。
- APK 安装到 Android 36 arm64 模拟器成功。
- App 启动成功，英文系统下自动显示英文界面。
- 通过主界面添加 4 条测试任务成功。
- 系统包信息确认 `TodoWidgetProvider` 已注册为 `APPWIDGET_UPDATE` 小组件 Provider。
- Launcher 小组件列表可搜索到 `Quadrant Todo`，预览尺寸显示为 `4 x 3`。
- 小组件刷新广播执行成功，无 App 崩溃。
- 小组件完成任务广播可把任务写为已完成，App 主界面计数同步为 `Open 3 / Done 1`。

> 当前模拟器已验证 Launcher 可搜索并预览小组件、Provider 注册和数据联动；不同手机 Launcher 的拖放和缩放交互略有差异，请按下方真机步骤再做一次桌面缩放确认。

## 语音输入建议

v0.2.0 之后提供两个入口：

- `键盘听写`：推荐在真机上使用。它会聚焦待办标题并打开当前输入法，你可以使用 Typeless、Wispr Flow、Gboard、Samsung Keyboard 或手机厂商输入法自带的麦克风听写。
- `系统语音`：调用 Android 系统语音识别服务。这个入口通常会落到 Google、厂商或设备默认语音识别服务上，模拟器里经常不可用。

真机测试时，如果你想用 Typeless 或其他第三方语音输入，请先在手机系统里把它启用为输入法，然后在 App 里点 `键盘听写`。

v0.2.0 之后还改进了添加体验：

- 语音识别结果会先填入标题，不会立刻添加。
- `添加` 按钮变成全宽按钮，更容易点击。
- 输入键盘的完成/回车动作也可以直接添加待办。

## 真机安装 APK

Debug APK 位于：

```text
/Users/gaohuirong/Documents/Codex/TodoQuadrantAndroid/app/build/outputs/apk/debug/app-debug.apk
```

v0.3.0 也会复制到：

```text
/Users/gaohuirong/Documents/Codex/TodoQuadrantAndroid/dist/TodoQuadrant-v0.3.0-debug.apk
```

把这个文件发送到安卓手机后，手机上打开它并允许“安装未知来源应用”即可安装测试。

如果通过 GitHub Release 发布，把这个 APK 作为 v0.3.0 Release 附件上传，手机浏览器打开 Release 页面即可下载。

## 真机小组件测试

安装 v0.3.0 后，在真实安卓手机上按下面步骤测试：

1. 打开 App，添加 3 到 5 条未完成任务。
2. 回到手机桌面，长按空白区域。
3. 选择“小组件”。
4. 搜索或找到“四象限待办 / Quadrant Todo”。
5. 长按 `4 x 3` 小组件并拖到桌面。
6. 确认小组件显示未完成任务列表。
7. 长按桌面上的小组件，拖动边框放大或缩小，确认显示任务数量随高度变化。
8. 点小组件左侧圆圈完成一条任务。
9. 回到 App，确认该任务已进入已完成，计数同步更新。
10. 在 App 里新增或删除任务，回到桌面确认小组件刷新。

## 权限说明

- `RECORD_AUDIO`：用于语音录入。
- `POST_NOTIFICATIONS`：Android 13 及以上用于提醒通知。
- `RECEIVE_BOOT_COMPLETED`：手机重启后恢复未来提醒。
- `VIBRATE`：用于震动提醒。

当前版本使用非精确提醒，避免一开始申请精确闹钟特殊权限。闹铃声模式会使用系统默认闹钟声音，但具体声音、震动强度仍可能受手机系统通知渠道设置影响。
