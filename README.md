# 🎂 生日提醒

[![License: MIT](https://img.shields.io/badge/license-MIT-e2545c.svg)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3ddc84.svg)](#安装)
[![APK size](https://img.shields.io/badge/APK-1.2%20MB-blue.svg)](https://github.com/haining6/BirthdayReminder/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7f52ff.svg)](#技术细节)

**一款小而干净的安卓生日 / 纪念日提醒应用。** 公历、农历都支持，到日子前用通知、来电式响铃、短信或邮件叫你，不会再忘记爸妈、朋友和重要的日子。

安装包只有 **1.2 MB**，没有广告，没有统计，没有任何第三方 SDK，数据只存在你自己的手机里，代码完全公开可查。

> 开发者：海宁 · [下载最新版 APK](https://github.com/haining6/BirthdayReminder/releases/latest)

---

## ✨ 特色

### 🌙 农历是一等公民
很多长辈过的是农历生日，而多数提醒应用对农历的支持很勉强。这里：

- 内置 1900–2100 年农历表，**完全离线换算**，不联网
- 正确处理**闰月**（如闰六月）、小月没有"三十"（自动取廿九）、2 月 29 日出生（平年取 2 月 28 日）
- 添加农历日期时，会同时显示对应的公历日期；列表里每一项都标明"农历八月廿八"这样的日期
- 年份可以不填："不记得年份"也能记，填了则自动算出"58 岁生日""10 周年"

### 🔔 四种提醒方式，每条记录可单独选
| 方式 | 说明 |
|---|---|
| **通知** | 一定会发。当天的通知上带"打电话""发短信祝福"按钮，一点就拨号 |
| **来电式响铃** | 像来电一样持续响铃，锁屏时亮屏弹出全屏界面，最长 1 分钟，可"10 分钟后再提醒" |
| **短信** | 本机自动发短信给你指定的号码。**双卡手机可以选哪张卡发**，比如卡 1 发给卡 2 |
| **邮件** | 用你自己的邮箱（QQ / 163 / 126 / 139 / Gmail）给自己发提醒邮件 |

> 为什么"来电式响铃"而不是真的打电话？普通双卡手机是双卡单通，自己打给自己接不到，而且"拨打电话"权限很容易被安全软件当成风险。所以用一个像来电的全屏响铃代替，效果相近，不需要这个权限。

### 📅 想提前多久都行
当天、提前 1 / 2 / 3 / 5 / 7 / 10 / 15 / 30 天，**可多选**。比如妈妈的生日同时选"当天 + 提前 7 天"，一周前先提醒你买礼物，当天再提醒你打电话。

### 🪶 又小又省电
- 不常驻后台、不轮询、没有常驻服务。只登记**一个**系统闹钟，每天到点唤醒一次，查完立刻退出
- 开机、改系统时间后自动重新登记；到点时手机恰好关机的话，开机或打开 App 时会**补发**当天的提醒
- 邮件交给系统的任务调度器：有网才发，网络不好自动退避重试，最多 6 小时

### 🔐 看得见的安全
- 没有广告、统计、崩溃上报，只用 Google 官方 AndroidX 库
- 发布版**不混淆代码**，用 jadx 等工具反编译就能读懂全部逻辑
- `allowBackup=false`，数据不会被备份到任何云端
- 联网只连**你自己填的**邮件服务器，TLS 加密并校验证书；邮件客户端是约 150 行的自写代码（[`Smtp.kt`](app/src/main/java/com/haining/birthday/remind/Smtp.kt)），没引入任何邮件库
- 邮箱授权码用 Android Keystore 的硬件密钥加密保存，导出备份时**不包含**
- 短信只发给你自己填写的号码，不读取任何短信

### 🎨 简洁好看
Material 3 设计，暖色主题，跟随系统深色模式。首页大卡片显示"下一个重要日子"和倒计时，下面按"30 天内 / 以后"分组，支持搜索。

---

## 📲 安装

1. 到 [Releases](https://github.com/haining6/BirthdayReminder/releases/latest) 下载 `生日提醒-v1.0.0.apk`
2. 传到手机（微信、QQ、数据线均可），点开安装，按提示允许"安装未知来源应用"
3. 要求 **Android 8.0 及以上**

下载后可校验文件完整性（Windows PowerShell）：

```powershell
Get-FileHash 生日提醒-v1.0.0.apk -Algorithm SHA256
```

SHA-256 应为 `B8FE1FC8469CA222B065597EB94D026DF9A12BCE7CA6AC0C33F8ED9BDF122D28`。

## 🚀 使用

### 1. 添加
点首页右下角「添加」：选类型（生日 / 纪念日 / 忌日 / 其他）→ 填名字 → 选公历或农历 → 滚轮选日期 → 选提前几天、用什么方式提醒 → 保存。

### 2. 开启你要的提醒方式（进入「设置」）
- **每天提醒时间**：默认 09:00，可改
- **短信**：选发送卡、填接收号码，点「发送测试短信」确认能收到。部分手机会弹出"是否允许发送短信"，请选**始终允许**
- **邮件**：点你的邮箱服务商预设 → 填邮箱账号和**授权码** → 点「发送测试邮件」。授权码需要在网页版邮箱的"设置 → POP3/SMTP 服务"里开启并生成，**不是登录密码**
- **来电式响铃**：点「试一下」看效果

### 3. ⚠️ 国产手机必做一步，否则后台提醒可能被系统拦掉
在「设置 → 让提醒更准时」里，App 会检查各项状态并提供跳转。再点「打开应用详情」：

1. 允许「自启动」
2. 省电策略选「无限制」/ 允许后台活动
3. 通知里打开「锁屏通知」「悬浮通知 / 横幅」
4. 小米 / 红米：其他权限里允许「后台弹出界面」「锁屏显示」（来电式提醒用）
5. 华为 / 荣耀：应用启动管理里改为手动管理，三项全开

### 4. 备份与换机
「设置 → 备份」可导出 / 导入 JSON 文件。换手机时导出，在新手机导入即可；邮箱授权码需要在新手机重新填写。

出了问题先看「设置 → 提醒记录」，每次提醒、短信、邮件的结果和失败原因都记在这里。

---

## 🔒 权限说明

| 权限 | 用途 | 何时申请 |
|---|---|---|
| 通知 | 弹出提醒 | 首次打开 |
| 发送短信 | 短信提醒 | 选择"短信"方式并发送时 |
| 读取手机状态 | **仅**用于列出双卡，让你选用哪张卡发短信 | 点"发送卡"时 |
| 网络 | 邮件提醒 | 普通权限，无需申请 |
| 开机启动 / 精确闹钟 / 全屏通知 / 振动 | 准时提醒、来电式响铃 | 普通权限，无需申请 |

**关于"报毒"**：应用带有"发送短信"权限，部分手机自带的安全扫描可能会提示"含短信权限"之类的风险。这是按权限给出的通用提示，并非检出病毒。你可以把 APK 上传到 [VirusTotal](https://www.virustotal.com) 用几十家引擎复查，或直接阅读本仓库的全部源码。

---

## 🛠 技术细节

- **语言 / 界面**：Kotlin + Jetpack Compose + Material 3；min SDK 26，target SDK 35
- **依赖**：仅 AndroidX（Compose、Activity）与 kotlinx-coroutines，无网络库、无图片库、无 DI 框架
- **体积优化**：R8 裁剪 + 资源裁剪，只保留中英文资源，不打包依赖元数据
- **测试**：15 个自动测试 —— 农历换算对 1900–2100 **每一天**做公历↔农历往返校验、校验历年春节日期；日期与岁数计算；用 Robolectric 在电脑上模拟运行界面，走完"添加 → 编辑 → 设置"和各通道提醒触发流程

```
app/src/main/java/com/haining/birthday/
├─ core/     Lunar.kt 农历换算    Dates.kt 下次日期、岁数、提醒文案
├─ data/     Event 数据模型  Repo 存储  Prefs 设置  Secret 加密  EventLog 提醒记录
├─ remind/   Scheduler 每日闹钟  Reminder 触发提醒  Notifier 通知/来电式响铃
│            SmsSender 短信  Smtp + EmailJob 邮件  RingActivity 来电界面
└─ ui/       MainActivity  HomeScreen 首页  EditScreen 编辑
             SettingsScreen 设置  DateDialog 日期滚轮
```

### 自己编译

需要 JDK 17 和 Android SDK（platform 35）。

```bash
# 运行全部测试
./gradlew :app:testDebugUnitTest

# 编译 debug 版（无需签名密钥）
./gradlew :app:assembleDebug
```

要编译正式签名版，在项目根目录放一个 `keystore.properties`（已被 `.gitignore` 排除，**不要上传**）：

```properties
storeFile=keystore/your-release.jks
storePassword=你的密码
keyAlias=你的别名
keyPassword=你的密码
```

然后 `./gradlew :app:assembleRelease`。Windows 上本作者的一键脚本见 [`build-apk.bat`](build-apk.bat)（里面的 JDK 路径需按你的环境修改）。

> 注意：你自己编译的 APK 签名与官方发布版不同，无法覆盖安装官方版，需要先卸载。

## 🤝 参与

欢迎提 [Issue](https://github.com/haining6/BirthdayReminder/issues) 反馈问题或建议功能，也欢迎 Pull Request。

## 📄 许可证

[MIT](LICENSE) © 海宁
