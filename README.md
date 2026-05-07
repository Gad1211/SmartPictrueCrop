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

## 打包 EXE（Windows）

```powershell
.\build-exe.ps1
```

可选指定 JDK：

```powershell
.\build-exe.ps1 -JdkHome "D:\JetBrains\JDK21"
```

输出目录：`dist\AIPicHandler-*.exe`

打包脚本会自动尝试将以下内容一起打包到 EXE 安装包：

- `modelConfig.json`
- `models\YOLO11\`（默认 YOLO11 模型目录）

## 上下限计算：

### 两个独立输入框：
- `上限误差`
- `下限误差`
两个输入框都支持留空，留空时默认按 0 处理。

### 误差参与计算方式：
- `上限因子 = 1 + 上限误差`
- `下限因子 = 1 - 下限误差`

### 额外校验：
两个误差都不能小于 0
下限误差必须 < 1（否则下限比例会无效）