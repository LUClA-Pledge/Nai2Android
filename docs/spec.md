# Nai2Android 规格说明

## 目标

构建一个可以直接用 Android Studio 打开的原生 Android 应用，调用 `https://nai.sta1n.cn/` 提供的 Nai2API 接口生成图片。生成成功后，图片默认写入系统图库中的 `Pictures/Nai2API`，并在应用内保存可检索的生成记录。

## 默认假设

1. 第一版使用 Kotlin + Jetpack Compose，最低支持 Android 10（API 29），以便通过 MediaStore 无需存储权限地保存图片。
2. 用户使用的是该站点的 `STA1N-...` 访问密钥；应用不接收 NovelAI 密码、Cookie 或第三方账号登录信息。
3. 站点接口合同以当前公开前端为准：`GET /api/settings`、`GET /api/me`、`POST /api/jobs`、`GET /api/jobs/{id}`，任务完成后返回 `imageUrl`。
4. “tag”分为两层：发送给服务的 NAI 提示词 tag，以及本地图库的归档 tag。归档 tag 可以在生成前填写，也可以在图片详情中修改；默认从当前预设名称或提示词首个非空 tag 推导。
5. 第一版只做单密钥、单图/批量四图生成，不实现账号注册、充值、密钥融合或站点管理后台。

## 用户故事与验收标准

### 生成与保存

- 用户可以在设置中输入并保存站点地址和 `STA1N` 访问密钥。
- 用户可以填写 prompt/tag、artist、反向提示词、采样器、尺寸、步数、Scale、CFG，并提交生成任务。
- 应用能轮询任务状态，显示排队、生成中、成功和失败状态。
- 生成成功后，图片自动保存到 `Pictures/Nai2API`，同时写入本地图库记录；网络图片未完整落盘前不显示为已完成。

### 图库

- 图库以缩略图网格展示本地图片，默认按生成时间倒序排列。
- 用户可以切换正序/倒序、只看收藏，并通过归档 tag 筛选。
- 用户可以打开详情查看原图、prompt、artist、反向提示词和归档 tag。
- 用户可以收藏/取消收藏，并编辑归档 tag；记录不会因离线状态丢失。

### 预设

- 用户可以新增、编辑、删除预设。
- 每个预设至少包含名称、tag/prompt、artist、反向提示词；字段支持追加内容，不强制覆盖已有内容。
- 在生成页选中预设后，字段可以继续手工修改。

## 技术栈

- Kotlin 2.0.x
- Android Gradle Plugin 8.x
- Jetpack Compose Material 3
- AndroidX ViewModel / Lifecycle
- Android Keystore + SharedPreferences：加密保存访问密钥
- SQLiteOpenHelper：保存图片索引和预设，避免图库功能依赖云端
- MediaStore：保存图片到系统图库
- `HttpURLConnection` + `org.json`：调用当前站点公开接口，减少不必要的网络依赖

## 数据模型

### ImageRecord

`id`、`localUri`、`createdAt`、`prompt`、`archiveTags`、`artist`、`negativePrompt`、`presetName`、`favorite`。

### Preset

`id`、`name`、`tag`、`artist`、`negativePrompt`、`createdAt`、`updatedAt`。

## 安全与边界

- 始终使用 HTTPS；默认服务地址固定为 `https://nai.sta1n.cn/`，用户可以改为兼容部署地址。
- 访问密钥通过 Android Keystore 加密后落盘，网络请求时才解密放入请求体或查询参数。
- 不记录访问密钥到日志、截图、测试数据或 git。
- 不自动实现“融合另一个密钥额度”，避免对余额造成不可逆操作。
- 站点返回的图片地址和错误信息视为不可信输入；应用只按 HTTP 客户端规则处理，不执行其中的脚本内容。

## 测试策略

- 纯 Kotlin 单元测试：tag 规范化、图库筛选/排序、请求 JSON 编解码、生成成本计算。
- 本地数据库测试：图片收藏、tag 更新、预设增删改。
- 手工设备检查：生成任务、断网错误、图库权限与系统相册显示、旋转屏幕后状态恢复。

## 构建命令

```text
gradlew.bat test
gradlew.bat assembleDebug
```

当前工作环境未发现 Android SDK，因此本次实现会先保证工程结构、源码和单元测试完整；如果本机后续补齐 Android SDK 与 Gradle 依赖，即可执行 APK 构建。

## GitHub Actions 构建

`.github/workflows/android-build.yml` 在推送、Pull Request 或手动触发时运行。工作流使用 Java 17、Android SDK 35 和 Gradle 8.9，先执行 `:app:testDebugUnitTest`，再执行 `:app:assembleDebug`，最后上传 `app-debug.apk`。访问密钥只在应用运行时由用户输入，CI 不需要任何站点密钥。

## 暂不包含

- 账号注册和支付
- 站点的密钥融合功能
- 云端同步图库
- AI 自动打标
- 删除系统图库文件的批量管理
