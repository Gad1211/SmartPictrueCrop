# AI 图片处理器（YOLO11 + ONNX Runtime）

本地 CPU 推理的智能主体裁剪架构：

- `detector`：`YOLO11n.onnx` + ONNX Runtime 推理（支持动态输入）
- `cropper`：主体评分、多人/多目标合并、人物特殊扩边、比例适配
- `model`：`BoundingBox`、`DetectionResult`、`CropResult` 等结构化数据
- `util`：图片预处理（letterbox）与 NMS
- `config`：检测与裁剪策略参数

同时保留原有桌面批处理和 EXE 输出能力（`MainApp` + `build-exe.ps1`）。

## 环境要求

- JDK 21（本机建议：`D:\JetBrains\JDK21`）
- Maven 3.8+
- YOLO 模型文件（例如：`models/yolo11n.onnx`）

## 模型配置

程序优先从安装目录读取 `modelConfig.json`，如果找不到会回退到 JAR 内置默认配置：

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

> 支持占位符：`${configDir}`（配置文件所在目录）与 `${appDir}`（程序目录）。
>  
> 例如 EXE 场景默认模型可放在 `models/YOLO11/yolo11n.onnx`，并随安装包一起发布。

## 运行桌面版（Swing）

```bash
mvn clean package
mvn exec:java -Dexec.mainClass="com.aipichandler.app.MainApp"
```

## 示例 API（Spring Boot）

项目内提供了示例入口：`com.aipichandler.app.SmartCropApiApplication`  
示例接口：`POST /api/crop/smart?ratio=1:1`，支持比例：

- `1:1`
- `4:3`
- `16:9`
- `9:16`

## 打包绿色版 EXE（Windows，免安装）

该方式基于 `jpackage --type app-image`，无需 WiX。

```powershell
.\build-exe.ps1
```

可选指定 JDK：

```powershell
.\build-exe.ps1 -JdkHome "D:\JetBrains\JDK21"
```

输出目录：

- `dist\PicCrop\`（绿色版目录，含 `PicCrop.exe`）
- `dist\PicCrop-portable.zip`（可直接发给别人，解压即用）

打包脚本会自动尝试将以下内容一起打包到绿色版目录：

- `modelConfig.json`
- `models\YOLO11\`（默认 YOLO11 模型目录）

## 裁切参数说明（与 UI 一致）

### 比例模式

- `区间限制（跟随原图）`：先取原图宽高比，再限制到允许区间内；适合尽量保留原图构图。
- `严格基准（固定比例）`：以基准比例（如 `1:1`、`4:3`）为主，再受允许区间约束；适合统一产出比例。

### 上下误差计算

- `上限误差`、`下限误差` 都支持留空，留空按 `0` 处理。
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
- `启用自定义可见率阈值` 勾选后，使用输入框中的阈值。
- 未勾选时，使用默认阈值 `0.92`。
- 阈值越大，算法越保守，越不容易截断主体。

### 最终图片产出逻辑（优先级）

1. 先由 `输出比例 + 比例模式 + 上下误差` 计算目标比例（最高优先级）。
2. 再执行主体检测与智能裁切，裁切框贴合目标比例。
3. 主体可见率阈值只在不突破比例约束前提下生效。
4. 检测失败时回退为中心裁切，并继续遵守目标比例。