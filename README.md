# 大肥鱼课表 · 开源 Android 课程表 App

一个**本地优先**的 Android 课表应用（课程表 / 上课时间表）。支持正方、强智、URP 等常见教务系统一键导入，可设上课提醒与桌面小组件。不注册账号，不上传数据，课表只存在你自己的手机里。

**下载**：https://newmaterialsdu.github.io/dafeiyu/

> 关键词：大肥鱼课表 / 安卓课程表 / 课表 App / 教务系统导入 / 正方教务 / 强智教务 / 大学课表 / 开源课表

> 本项目基于作者 [YZune](https://github.com/YZune) 的开源项目 WakeupSchedule_Kotlin（Apache-2.0）修改而来。
> 原仓库已不再公开，特此说明出处；与其作者名下的任何商业产品无关。详见 [NOTICE](NOTICE)。

## 功能

**课表**
- 一周七天 × 多节次网格，支持单双周、非本周课程淡化显示
- 上午 / 下午 / 晚上分段（1-4 / 5-8 / 9-12），改一节时间整段自动顺延
- 格子高度、圆角、透明度、分隔线、配色方案可调
- 自带 8 套配色，一键换掉强调色 + 课程文字色 + 边框 + 背景
- 多课表管理，每张课表独立配置

**导入**
- 教务系统网页导入（内置常见教务类型）
- 网页源码导入（不依赖教务类型判断，最稳）
- Excel / CSV 导入（按模板填）
- ICS 日历导入导出

**其他**
- 上课提醒（可设提前量）
- 桌面小组件：周视图 + 当日课程
- 导入导出、课表分享

## 两个发行版

同一份代码，两个 flavor，差别只在素材与赞赏入口：

| | `store`（应用商店） | `github`（GitHub 发行） |
|---|---|---|
| 内置壁纸 | 纯色 / 几何（无第三方素材） | 含社区二创插画 |
| 赞赏入口 | 无 | 有 |
| 二创署名 | 不展示 | 在「关于」页展示 |
| 用途 | 上架商店 | 自用 / 分享 |

构建命令：

```bash
./gradlew assembleStoreRelease    # 商店版
./gradlew assembleGithubRelease   # GitHub 版
./gradlew assembleStoreDebug      # 调试
```

签名配置读根目录 `keystore.properties`（**不入库**）：

```properties
storeFile=app/your.jks
storePassword=******
keyAlias=******
keyPassword=******
```

没有该文件时会退回内置调试密钥，保证 clone 下来能直接编译。

## 环境

- JDK 17
- Android SDK 35（compileSdk / targetSdk 35，minSdk 24）
- Gradle 8.9

## 隐私

不收集任何个人信息，不需要账号，数据全部存本地。见 [PRIVACY.md](PRIVACY.md)。

## 许可

代码遵循 [Apache License 2.0](LICENSE)。

`github` flavor 中包含的社区二创插画遵循 **CC BY-NC-SA 4.0**，仅限非商业使用，版权归原作者（见 [NOTICE](NOTICE)）。**`store` flavor 不包含任何二创素材**，可安全用于商店分发。
