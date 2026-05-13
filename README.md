# 智能图片处理器（YOLO11 + ONNX Runtime）

基于 **YOLO11 ONNX** 与 **ONNX Runtime（CPU）** 的本地智能主体检测与按比例裁切。提供 **Swing 桌面批处理**、**可打包 Windows 绿色版 EXE**。

## 功能概览

- 批量扫描文件夹，在同级目录下写入 `output`（可在配置中改名）中的裁切结果。
- 支持常见位图格式：`jpg`、`jpeg`、`png`、`bmp`、`webp`、`gif`。
- 检测失败时回退为**中心裁切**，仍遵守目标比例。
- 模型路径可通过 `modelConfig.json` 或环境变量配置（详见下文）。

## 架构与模块

| 包 / 模块 | 说明 |
|-----------|------|
| `detector` | `YOLO11n.onnx` + ONNX Runtime 推理（支持动态输入） |
| `cropper` | 主体评分、多人/多目标合并、人物特殊扩边、比例适配 |
| `model` | `BoundingBox`、`DetectionResult`、`CropResult` 等结构化数据 |
| `util` | 图片预处理（letterbox）与 NMS |
| `config` | 检测与裁剪策略参数 |
| `core` | 批处理、模型配置加载、`AiSubjectCropper` 编排 |
| `app` | 桌面入口 `MainApp`；可选 API 入口 `SmartCropApiApplication` |
| `controller` | 示例接口 `SmartCropController` |

## 环境要求

- **JDK 21**
- **Maven 3.8+**
- **YOLO ONNX 模型**（默认期望路径见 `modelConfig.json`，例如 `models/YOLO11/yolo11n.onnx`）

### 主要依赖（`pom.xml`）

- `onnxruntime` 1.19.2  
- `spring-boot-starter-web` 3.3.5（仅用于示例 API）  
- `webp-imageio`、`jackson-databind`、`slf4j-simple`

## 模型配置

程序优先从**安装目录**读取 `modelConfig.json`；若不存在则使用 **JAR 内置** `src/main/resources/modelConfig.json`。

仓库根目录的 `modelConfig.json` 示例（与 `build-exe.ps1` 打包逻辑一致）：

```json
{
  "models": [
    {
      "name": "YOLO11n ONNX (CPU)",
      "modelUrl": "${configDir}/models/YOLO11/yolo11n.onnx",
      "engine": "ONNXRuntime"
    }
  ]
}
```

内置默认配置使用 `${appDir}` 解析模型路径，便于随应用目录分发。

**占位符：**

- `${configDir}`：`modelConfig.json` 所在目录  
- `${appDir}`：程序运行目录（工作目录 / 安装目录）

> **EXE / 绿色版**：将 `modelConfig.json` 与 `models/YOLO11/`（内含 `yolo11n.onnx`）放在发布目录即可；`build-exe.ps1` 会在打包时尝试一并复制。

## 运行桌面版（Swing）

```bash
mvn clean package
mvn exec:java -Dexec.mainClass="com.aipichandler.app.MainApp"
```

也可直接运行 shade 生成的 fat JAR（主类同为 `MainApp`）：

```bash
java -jar target/ai-pic-handler-1.0.0-SNAPSHOT-all.jar
```

桌面版在选定目录下创建 **`output`** 子目录存放结果（与 `ProcessingConfig` 中输出文件夹名一致）。

## 示例 API（Spring Boot）

入口类：`com.aipichandler.app.SmartCropApiApplication`  
未提供 `application.properties` 时，默认监听 **8080**。

启动：

```bash
mvn exec:java -Dexec.mainClass="com.aipichandler.app.SmartCropApiApplication"
```

**接口：** `POST /api/crop/smart`

| 参数 | 类型 | 说明 |
|------|------|------|
| `file` | `multipart/form-data` | 待裁切图片 |
| `ratio` | 可选 query，默认 `1:1` | 目标比例，见下表 |

支持的比例字符串：

- `1:1`、`4:3`、`16:9`、`9:16`

其他或无法识别的 `ratio` 会**按 `1:1` 处理**（与 `CropAspectRatio.fromText` 行为一致）。

**环境变量（示例控制器）：**

- `AIPICHANDLER_YOLO_MODEL`：ONNX 模型绝对路径。未设置时默认为当前工作目录下的 `models/yolo11n.onnx`（与桌面版 `modelConfig` 中的 `YOLO11` 子目录路径可能不同，部署 API 时请显式设置）。

**调用示例（curl）：**

```bash
curl -sS -X POST "http://localhost:8080/api/crop/smart?ratio=1:1" \
  -F "file=@/path/to/photo.jpg" \
  -o smart-crop.jpg
```

在 **Windows CMD** 中可将续行符 `\` 改为 `^`；在 **PowerShell** 中也可使用 `curl.exe`（避免被 `Invoke-WebRequest` 别名占用）执行同上命令。

响应为 **JPEG** 图片字节流，`Content-Type: image/jpeg`。

## 打包绿色版 EXE（Windows，免安装）

基于 `jpackage --type app-image`，**不需要 WiX**。

```powershell
.\build-exe.ps1
```

可选指定 JDK：

```powershell
.\build-exe.ps1 -JdkHome "D:\JetBrains\JDK21"
```

**输出：**

- `dist\PicCrop\`：绿色版目录，内含 `PicCrop.exe`
- `dist\PicCrop-portable.zip`：压缩包，解压即用

脚本会在打包输入中尽量包含：

- 项目根目录的 `modelConfig.json`
- `models\YOLO11\` 默认模型目录

若缺失会给出警告，应用将回退到内置配置或需用户自行放置模型文件。

## 裁切参数说明（与 UI 一致）

### 比例模式

- **区间限制（跟随原图）**：先取原图宽高比，再限制到允许区间内；适合尽量保留原图构图。
- **严格基准（固定比例）**：以基准比例（如 `1:1`、`4:3`）为主，再受允许区间约束；适合统一产出比例。

### 上下误差计算

- **上限误差**、**下限误差** 都支持留空，留空按 `0` 处理。
- 计算公式：
  - `上限因子 = 1 + 上限误差`
  - `下限因子 = 1 - 下限误差`
  - `最小比例 = 基准比例 × 下限因子`
  - `最大比例 = 基准比例 × 上限因子`
- 输入约束：
  - 两个误差都不能小于 `0`
  - `下限误差 < 1`（否则比例下界无效）

### 主体居中与可见率阈值

- 主体居中策略默认启用。
- **启用自定义可见率阈值** 勾选后，使用输入框中的阈值。
- 未勾选时，使用默认阈值 `0.92`。
- 阈值越大，算法越保守，越不容易截断主体。

### 最终图片产出逻辑（优先级）

1. 先由 **输出比例 + 比例模式 + 上下误差** 计算目标比例（最高优先级）。
2. 再执行主体检测与智能裁切，裁切框贴合目标比例。
3. 主体可见率阈值只在不突破比例约束前提下生效。
4. 检测失败时回退为中心裁切，并继续遵守目标比例。

### 其他 UI 选项

- **1:1 裁切保持原图宽度（仅调整高度，只对竖图生效）**：在 1:1 输出时保留源图宽度方向的像素范围，仅通过高度适配（竖图场景）。

## 开发与构建提示

- **编码**：UTF-8（`pom.xml` 中已配置）。
- **打包**：`maven-shade-plugin` 生成 `ai-pic-handler-1.0.0-SNAPSHOT-all.jar`，合并依赖并指定 `MainApp` 为默认主类。

## 许可证

### 本仓库源代码

本仓库中的 **Java 应用源代码** 在 [**Apache License 2.0**](https://www.apache.org/licenses/LICENSE-2.0) 下发布，全文见根目录 [`LICENSE`](LICENSE)。

### 与 YOLO11 / Ultralytics 的关系（重要）

Ultralytics 的 YOLO11 **训练代码、官方权重与导出模型**通常适用其 [**AGPL-3.0**](https://www.gnu.org/licenses/agpl-3.0.html)（或另行购买的企业许可），这与「把本项目标成 Apache-2.0」**并不矛盾**：Apache-2.0 描述的是**你写的这份程序**；**ONNX 等模型文件**若来自 Ultralytics 且受其条款约束，你仍需**单独遵守**其对模型分发与使用的要求（常见路径包括：按 AGPL 提供对应源码/网络服务条款、购买企业许可，或改用其他许可证下的检测模型）。

因此并不是「只能用 GPL」这一种说法：AGPL 与 GPL 相关但不同（AGPL 对通过网络提供服务的场景更严格）。**是否可将整体闭源商用打包**，取决于你如何获取与许可模型及 Ultralytics 条款；请以 [Ultralytics 官方许可说明](https://www.ultralytics.com/license) 为准，必要时咨询专业律师。

### 其他依赖

预编译依赖（如 ONNX Runtime、Spring Boot、WebP ImageIO 等）受其各自许可证约束，使用时请一并遵守。
