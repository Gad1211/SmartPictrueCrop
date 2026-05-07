package com.aipichandler.app;

import com.aipichandler.core.ImageBatchProcessor;
import com.aipichandler.core.ModelConfigLoader;
import com.aipichandler.core.ProcessingConfig;

import javax.swing.DefaultComboBoxModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MainApp {

    private static final double DEFAULT_TOLERANCE = 0.15;
    private static final float DEFAULT_JPEG_QUALITY = 0.9f;
    private static final double MIN_VISIBLE_RATIO_RECOMMENDED_MIN = 0.85;
    private static final double MIN_VISIBLE_RATIO_RECOMMENDED_MAX = 0.98;

    private final JTextField folderField = new JTextField(48);
    private final JComboBox<String> ratioCombo = new JComboBox<>(new String[]{"1:1", "4:3", "16:9", "9:16", "自定义"});
    private final JTextField customRatioField = new JTextField("1:1", 6);
    private final JComboBox<String> ratioModeCombo = new JComboBox<>(new String[]{"严格基准（固定比例）", "区间限制（跟随原图）"});
    private final JTextField ratioUpperErrorField = new JTextField("", 5);
    private final JTextField ratioLowerErrorField = new JTextField("", 5);
    private final JLabel ratioErrorHintLabel = new JLabel();
    private final JCheckBox customVisibleRatioCheckBox = new JCheckBox("启用自定义可见率阈值", false);
    private final JLabel minVisibleRatioLabel = new JLabel("主体可见率阈值:");
    private final JLabel minVisibleRatioHintLabel = new JLabel();
    private final JTextField minVisibleRatioField = new JTextField(
            String.valueOf(ProcessingConfig.DEFAULT_MIN_SUBJECT_VISIBLE_RATIO),
            5
    );
    private final JComboBox<ModelConfigLoader.ModelOption> modelCombo = new JComboBox<ModelConfigLoader.ModelOption>();
    private final JButton reloadModelButton = new JButton("重载模型配置");
    private final JTextArea logArea = new JTextArea(20, 80);
    private final JButton startButton = new JButton("开始处理");
    private final JButton stopButton = new JButton("结束处理");
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private JFrame frame;
    private Thread workerThread;

    private void createAndShow() {
        frame = new JFrame("图片主体裁切处理器(适用于识别图片主体按比例裁切)");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        folderField.setEditable(false);
        JButton chooseButton = new JButton("选择文件夹");
        chooseButton.addActionListener(e -> chooseFolder());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        JPanel baseSection = new JPanel(new GridLayout(0, 1, 0, 6));
        baseSection.setBorder(BorderFactory.createTitledBorder("基础设置"));

        JPanel folderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        folderPanel.add(new JLabel("目录:"));
        folderPanel.add(folderField);
        folderPanel.add(chooseButton);
        baseSection.add(folderPanel);

        JPanel modelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modelPanel.add(new JLabel("模型:"));
        modelCombo.setPrototypeDisplayValue(new ModelConfigLoader.ModelOption("请选择模型", "placeholder", "ONNXRuntime"));
        modelPanel.add(modelCombo);
        reloadModelButton.addActionListener(e -> loadModelConfig(true));
        modelPanel.add(reloadModelButton);
        baseSection.add(modelPanel);
        formPanel.add(baseSection);

        JPanel cropSection = new JPanel(new GridLayout(0, 1, 0, 6));
        cropSection.setBorder(BorderFactory.createTitledBorder("裁切设置"));

        JPanel cropPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cropPanel.add(new JLabel("输出比例:"));
        ratioCombo.setSelectedItem("1:1");
        ratioCombo.addActionListener(e -> customRatioField.setEnabled("自定义".equals(ratioCombo.getSelectedItem())));
        cropPanel.add(ratioCombo);
        customRatioField.setEnabled(false);
        cropPanel.add(customRatioField);
        cropPanel.add(new JLabel("比例模式:"));
        ratioModeCombo.setSelectedIndex(0);
        cropPanel.add(ratioModeCombo);
        cropPanel.add(new JLabel("上限误差:"));
        cropPanel.add(ratioUpperErrorField);
        cropPanel.add(new JLabel("下限误差:"));
        cropPanel.add(ratioLowerErrorField);
        cropSection.add(cropPanel);
        ratioErrorHintLabel.setText("说明：最终比例范围 = 基准比例 x [1-下限误差, 1+上限误差]；空值按 0 处理。");
        Font ratioHintBaseFont = ratioErrorHintLabel.getFont();
        ratioErrorHintLabel.setFont(ratioHintBaseFont.deriveFont(ratioHintBaseFont.getSize2D() - 1f));
        ratioErrorHintLabel.setForeground(Color.DARK_GRAY);
        JPanel ratioHintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ratioHintPanel.add(ratioErrorHintLabel);
        cropSection.add(ratioHintPanel);
        JLabel ratioModeHintLabelA = new JLabel("模式说明：区间限制（跟随原图）会优先保持原图宽高比，仅在超出误差区间时收敛。");
        Font ratioModeHintBaseFont = ratioModeHintLabelA.getFont();
        ratioModeHintLabelA.setFont(ratioModeHintBaseFont.deriveFont(ratioModeHintBaseFont.getSize2D() - 1f));
        ratioModeHintLabelA.setForeground(Color.DARK_GRAY);
        JPanel ratioModeHintPanelA = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ratioModeHintPanelA.add(ratioModeHintLabelA);
        cropSection.add(ratioModeHintPanelA);
        JLabel ratioModeHintLabelB = new JLabel("模式说明：严格基准（固定比例）会以基准比例为准，再按误差区间做边界限制。");
        ratioModeHintLabelB.setFont(ratioModeHintBaseFont.deriveFont(ratioModeHintBaseFont.getSize2D() - 1f));
        ratioModeHintLabelB.setForeground(Color.DARK_GRAY);
        JPanel ratioModeHintPanelB = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ratioModeHintPanelB.add(ratioModeHintLabelB);
        cropSection.add(ratioModeHintPanelB);
        formPanel.add(cropSection);

        JPanel centeringSection = new JPanel(new GridLayout(0, 1, 0, 4));
        centeringSection.setBorder(BorderFactory.createTitledBorder("主体居中设置"));
        JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionPanel.add(customVisibleRatioCheckBox);
        customVisibleRatioCheckBox.addActionListener(e -> updateVisibleRatioControlsState());
        centeringSection.add(optionPanel);
        JPanel visibleRatioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        visibleRatioPanel.add(minVisibleRatioLabel);
        visibleRatioPanel.add(minVisibleRatioField);
        centeringSection.add(visibleRatioPanel);
        minVisibleRatioHintLabel.setText(String.format(
                Locale.ROOT,
                "建议范围：%.2f ~ %.2f（值越大越保守，主体越不容易被截断）",
                MIN_VISIBLE_RATIO_RECOMMENDED_MIN,
                MIN_VISIBLE_RATIO_RECOMMENDED_MAX
        ));
        Font baseFont = minVisibleRatioHintLabel.getFont();
        minVisibleRatioHintLabel.setFont(baseFont.deriveFont(baseFont.getSize2D() - 1f));
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hintPanel.add(minVisibleRatioHintLabel);
        centeringSection.add(hintPanel);
        formPanel.add(centeringSection);

        JPanel outputLogicSection = new JPanel(new GridLayout(0, 1, 0, 4));
        outputLogicSection.setBorder(BorderFactory.createTitledBorder("最终图片产出逻辑"));
        JLabel outputLogicLabel1 = new JLabel("1) 先根据输出比例、比例模式、上下误差得到目标比例区间（裁切设置最高优先级）。");
        JLabel outputLogicLabel2 = new JLabel("2) 基于检测主体进行智能裁切并贴合目标比例，主体设置不会突破该比例限制。");
        JLabel outputLogicLabel3 = new JLabel("3) 勾选自定义可见率阈值后，按你输入值保留主体；未勾选时使用默认阈值。");
        JLabel outputLogicLabel4 = new JLabel("4) 检测失败时自动回退到中心裁切，并继续遵守目标比例。");
        Font outputHintBaseFont = outputLogicLabel1.getFont();
        outputLogicLabel1.setFont(outputHintBaseFont.deriveFont(outputHintBaseFont.getSize2D() - 1f));
        outputLogicLabel2.setFont(outputHintBaseFont.deriveFont(outputHintBaseFont.getSize2D() - 1f));
        outputLogicLabel3.setFont(outputHintBaseFont.deriveFont(outputHintBaseFont.getSize2D() - 1f));
        outputLogicLabel4.setFont(outputHintBaseFont.deriveFont(outputHintBaseFont.getSize2D() - 1f));
        outputLogicLabel1.setForeground(Color.DARK_GRAY);
        outputLogicLabel2.setForeground(Color.DARK_GRAY);
        outputLogicLabel3.setForeground(Color.DARK_GRAY);
        outputLogicLabel4.setForeground(Color.DARK_GRAY);
        JPanel outputLogicPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outputLogicPanel1.add(outputLogicLabel1);
        JPanel outputLogicPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outputLogicPanel2.add(outputLogicLabel2);
        JPanel outputLogicPanel3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outputLogicPanel3.add(outputLogicLabel3);
        JPanel outputLogicPanel4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outputLogicPanel4.add(outputLogicLabel4);
        outputLogicSection.add(outputLogicPanel1);
        outputLogicSection.add(outputLogicPanel2);
        outputLogicSection.add(outputLogicPanel3);
        outputLogicSection.add(outputLogicPanel4);
        formPanel.add(outputLogicSection);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        startButton.addActionListener(e -> startProcessing());
        stopButton.addActionListener(e -> stopProcessing());
        stopButton.setEnabled(false);
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actionPanel.add(startButton);
        actionPanel.add(stopButton);

        frame.add(formPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);
        frame.add(actionPanel, BorderLayout.SOUTH);
        updateVisibleRatioControlsState();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        loadModelConfig(false);
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择图片目录");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        String current = folderField.getText().trim();
        if (!current.isEmpty()) {
            chooser.setCurrentDirectory(new File(current));
        }
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            folderField.setText(selected.getAbsolutePath());
            appendLog("已选择目录: " + selected.getAbsolutePath());
        }
    }

    private void startProcessing() {
        final String folder = folderField.getText().trim();
        final String ratioText = resolveSelectedRatioText();
        final double baseRatio;
        final double upperError;
        final double lowerError;
        final double minVisibleRatio;
        final ProcessingConfig.AspectRatioMode ratioMode;
        try {
            baseRatio = parseRatioText(ratioText);
        } catch (IllegalArgumentException ex) {
            appendLog("输出比例格式错误: " + ex.getMessage());
            return;
        }
        boolean useCustomVisibleRatio = customVisibleRatioCheckBox.isSelected();
        try {
            upperError = parseOptionalNonNegativeNumber(ratioUpperErrorField.getText(), "上限误差");
            lowerError = parseOptionalNonNegativeNumber(ratioLowerErrorField.getText(), "下限误差");
            if (lowerError >= 1.0) {
                throw new IllegalArgumentException("下限误差必须小于 1，否则会导致目标比例下界无效。");
            }
            ratioMode = resolveSelectedRatioMode();
            if (useCustomVisibleRatio) {
                minVisibleRatio = parseRatioInRange(
                        minVisibleRatioField.getText(),
                        "主体可见率阈值",
                        0.0,
                        1.0
                );
            } else {
                minVisibleRatio = ProcessingConfig.DEFAULT_MIN_SUBJECT_VISIBLE_RATIO;
            }
        } catch (IllegalArgumentException ex) {
            appendLog("参数输入错误: " + ex.getMessage());
            return;
        }
        final ModelConfigLoader.ModelOption selectedModel = (ModelConfigLoader.ModelOption) modelCombo.getSelectedItem();
        if (folder.isEmpty()) {
            appendLog("请先选择文件夹。");
            return;
        }
        if (selectedModel == null) {
            appendLog("请先加载 modelConfig.json 并选择模型。");
            return;
        }
        if (workerThread != null && workerThread.isAlive()) {
            appendLog("任务正在进行中，请先结束当前任务。");
            return;
        }

        stopRequested.set(false);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        appendLog("开始处理...");

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                ProcessingConfig config = new ProcessingConfig(
                        DEFAULT_TOLERANCE,
                        baseRatio,
                        1.0 + upperError,
                        1.0 - lowerError,
                        "output",
                        DEFAULT_JPEG_QUALITY,
                        selectedModel.name(),
                        selectedModel.modelUrl(),
                        selectedModel.engine(),
                        ProcessingConfig.DEFAULT_SUBJECT_PROMPT,
                        ratioMode,
                        true,
                        minVisibleRatio
                );
                Consumer<String> logger = new Consumer<String>() {
                    @Override
                    public void accept(String text) {
                        appendLog(text);
                    }
                };
                try {
                    appendLog("当前模型: " + config.modelName());
                    appendLog("当前模型地址: " + config.modelUrl());
                    appendLog("当前推理引擎: " + config.modelEngine());
                    appendLog(String.format(Locale.ROOT,
                            "输出比例设置: %.4f，误差(上限/下限)=+%.4f/-%.4f",
                            config.outputAspectRatio(),
                            upperError,
                            lowerError));
                    appendLog("比例模式: " + (config.aspectRatioMode() == ProcessingConfig.AspectRatioMode.STRICT_BASE_RATIO
                            ? "严格基准（固定比例）"
                            : "区间限制（跟随原图）"));
                    appendLog("主体居中策略: 默认启用");
                    appendLog("自定义可见率阈值: " + (useCustomVisibleRatio ? "已启用" : "未启用(使用默认值)"));
                    appendLog(String.format(Locale.ROOT, "生效主体可见率阈值: %.3f", config.minSubjectVisibleRatio()));
                    ImageBatchProcessor processor = new ImageBatchProcessor(config, logger, stopRequested::get);
                    processor.processFolder(Paths.get(folder));
                    if (stopRequested.get()) {
                        appendLog("处理已结束。");
                    } else {
                        appendLog("全部处理完成。");
                    }
                } catch (Exception ex) {
                    appendLog("处理失败: " + ex.getMessage());
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);
                            workerThread = null;
                        }
                    });
                }
            }
        }, "image-processor");
        worker.setDaemon(true);
        workerThread = worker;
        worker.start();
    }

    private void stopProcessing() {
        stopRequested.set(true);
        if (workerThread != null) {
            workerThread.interrupt();
        }
        appendLog("已发送停止指令，当前图片处理完成后会终止。");
        stopButton.setEnabled(false);
    }

    private void appendLog(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                logArea.append(message);
                logArea.append(System.lineSeparator());
            }
        });
    }

    private void loadModelConfig(boolean showSuccessDialog) {
        Path defaultConfig = ModelConfigLoader.resolveDefaultConfigPath();
        try {
            List<ModelConfigLoader.ModelOption> models = ModelConfigLoader.load(defaultConfig);
            DefaultComboBoxModel<ModelConfigLoader.ModelOption> comboModel = new DefaultComboBoxModel<ModelConfigLoader.ModelOption>();
            for (ModelConfigLoader.ModelOption model : models) {
                comboModel.addElement(model);
            }
            modelCombo.setModel(comboModel);
            if (comboModel.getSize() > 0) {
                modelCombo.setSelectedIndex(0);
            }
            appendLog("模型配置加载成功: " + defaultConfig + "，可选模型数: " + models.size());
            if (showSuccessDialog) {
                JOptionPane.showMessageDialog(frame, "模型配置加载成功。", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            modelCombo.setModel(new DefaultComboBoxModel<ModelConfigLoader.ModelOption>());
            appendLog("模型配置加载失败: " + defaultConfig + "，原因: " + ex.getMessage());
            appendLog("请检查安装目录 modelConfig.json，或使用内置默认配置。");
            if (showSuccessDialog) {
                JOptionPane.showMessageDialog(frame, "模型配置加载失败:\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String resolveSelectedRatioText() {
        Object selected = ratioCombo.getSelectedItem();
        String ratio = selected == null ? "1:1" : selected.toString();
        if ("自定义".equals(ratio)) {
            return customRatioField.getText().trim();
        }
        return ratio;
    }

    private double parseRatioText(String ratioText) {
        if (ratioText == null || ratioText.isBlank()) {
            throw new IllegalArgumentException("请输入比例，格式示例 1:1 或 4:3。");
        }
        String[] parts = ratioText.trim().split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("比例需为 A:B 格式。");
        }
        double width = parsePositiveNumber(parts[0], "比例宽度");
        double height = parsePositiveNumber(parts[1], "比例高度");
        return width / height;
    }

    private double parsePositiveNumber(String text, String fieldName) {
        try {
            double value = Double.parseDouble(text.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + "必须大于 0。");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + "不是有效数字。");
        }
    }

    private double parseOptionalNonNegativeNumber(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        try {
            double value = Double.parseDouble(text.trim());
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + "不能小于 0。");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + "不是有效数字。");
        }
    }

    private double parseRatioInRange(String text, String fieldName, double minExclusive, double maxInclusive) {
        if (text == null || text.isBlank()) {
            return ProcessingConfig.DEFAULT_MIN_SUBJECT_VISIBLE_RATIO;
        }
        try {
            double value = Double.parseDouble(text.trim());
            if (value <= minExclusive || value > maxInclusive) {
                throw new IllegalArgumentException(String.format(
                        Locale.ROOT,
                        "%s必须在(%.2f, %.2f]区间内。",
                        fieldName,
                        minExclusive,
                        maxInclusive
                ));
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + "不是有效数字。");
        }
    }

    private ProcessingConfig.AspectRatioMode resolveSelectedRatioMode() {
        Object selected = ratioModeCombo.getSelectedItem();
        String mode = selected == null ? "" : selected.toString();
        if (mode.startsWith("严格基准")) {
            return ProcessingConfig.AspectRatioMode.STRICT_BASE_RATIO;
        }
        return ProcessingConfig.AspectRatioMode.CLAMP_SOURCE_RATIO;
    }

    private void updateVisibleRatioControlsState() {
        boolean enabled = customVisibleRatioCheckBox.isSelected();
        minVisibleRatioField.setEnabled(enabled);
        minVisibleRatioLabel.setEnabled(enabled);
        minVisibleRatioHintLabel.setEnabled(enabled);
        minVisibleRatioHintLabel.setForeground(enabled ? Color.DARK_GRAY : Color.GRAY);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainApp().createAndShow();
            }
        });
    }
}
