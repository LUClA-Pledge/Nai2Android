# Nai2Android

一个面向 `https://nai.sta1n.cn/` Nai2API 的原生 Android 客户端。它把生成流程和本地图库放在一起，图片成功下载后先保存到应用图库，需要时可多次导出到系统图库的 `Pictures/Nai2API` 目录。

## 已实现功能

- `STA1N` 访问密钥和服务地址设置；密钥使用 Android Keystore 加密保存。
- 提示词/tag、artist、反向提示词、采样器、画幅、步数、Scale、CFG。
- 生成任务提交、排队/生成状态轮询和额度刷新。
- 图片默认写入系统图库，并建立本地索引。
- 图库按时间正序或倒序查看、收藏筛选、按归档 tag 筛选。
- 图片详情查看和归档 tag 修改。
- 自定义预设：名称、tag、artist、反向提示词均可编辑和追加。
- 单次最多并发生成 4 张图片，并分别展示排队、进度、保存或失败状态。
- 创作页直接预览本次最新结果，图片仍先保存在应用图库。
- 图片详情保留完整生成参数；同一图片可多次导出并显示累计导出次数。
- 切换预设时，图库归档 tag 会同步为预设名称。

## 打开与构建

用 Android Studio 打开 `Nai2Android` 目录，等待 Gradle 同步，然后运行 `app` 的 debug 变体。

项目已经附带 GitHub Actions 工作流。将工程推送到 GitHub 后，进入仓库的 **Actions** 页面，运行 `Android build`，或者向 `main` / `master` 分支推送一次代码。工作流会准备 Java 17、Android SDK 和 Gradle 8.9，完成测试后上传 `nai2android-debug-apk` 构建产物。

如果本机已安装 Gradle，也可以使用：

```powershell
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

当前工作环境没有检测到 Android SDK，因而本机暂时不能承诺已生成 APK；GitHub Actions 工作流已经把所需的云端构建环境补齐。

## 使用说明

1. 打开“设置”，填入站点地址和 `STA1N-...` 访问密钥。
2. 回到“创作”，选择预设或直接编辑 prompt、artist、反向提示词。
3. 选择并发张数后生成；图片完成后先保存到应用图库并在创作页预览。
4. 在“图库”中按时间、收藏和 tag 查找；打开图片详情可以修改归档 tag。

应用没有实现站点的“密钥融合”操作，避免误合并额度造成不可逆影响。

