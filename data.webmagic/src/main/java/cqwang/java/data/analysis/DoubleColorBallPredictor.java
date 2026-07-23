package cqwang.java.data.analysis;

import cqwang.java.data.serialize.FileProvider;
import cqwang.java.data.spider.doubleball.model.DoubleColorBallMatrixItem;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 双色球预测器 V4 - 完全修复版本
 *
 * 改进点：
 * 1. 修复所有8种算法 - 确保返回正确的数据格式 [红1-6, 蓝]
 * 2. 完整的迭代去重和验证机制 - 直到所有约束都满足
 * 3. 严格的约束检查和日志
 */
public class DoubleColorBallPredictor {

    private static final int RED_BALL_MAX = 33;
    private static final int BLUE_BALL_MAX = 16;
    private static final int RED_BALL_COUNT = 6;
    private static final int TOTAL_COUNT = 7;
    private static final Random random = new Random();
    private static final int MAX_ITERATIONS = 10;
    private static final int MAX_REGENERATION_ATTEMPTS = 50;

    @Data
    public static class PredictionResult {
        private String algorithmName;
        private List<Integer> predictedData;

        public PredictionResult(String name, List<Integer> data) {
            this.algorithmName = name;
            this.predictedData = new ArrayList<>(data);
        }

        @Override
        public String toString() {
            return algorithmName + " → " + predictedData;
        }
    }

    @Data
    public static class PredictionPlan {
        private List<Integer> sampleData;
        private int sampleSize;
        private List<PredictionResult> predictions;
        private List<String> processingLog;
        private int iterationCount;
        private boolean allConstraintsMet;

        public PredictionPlan(int sampleSize) {
            this.sampleSize = sampleSize;
            this.predictions = new ArrayList<>();
            this.processingLog = new ArrayList<>();
            this.iterationCount = 0;
            this.allConstraintsMet = false;
        }

        public void printReport() {
            System.out.println("\n========== 双色球预测结果 (V4 - 完全修复版本) ==========\n");
            System.out.println("采样大小: " + sampleSize);
            System.out.println("迭代轮数: " + iterationCount);
            System.out.println("约束满足: " + (allConstraintsMet ? "✅ 是" : "❌ 否"));

            if (!processingLog.isEmpty()) {
                System.out.println("\n📋 处理日志:");
                for (String log : processingLog) {
                    System.out.println("  " + log);
                }
            }

            System.out.println("\n8组最终预测方案:");
            for (int i = 0; i < predictions.size(); i++) {
                PredictionResult pred = predictions.get(i);
                System.out.println((i + 1) + ". " + pred);
            }

            // 最终验证
            System.out.println("\n✅ 最终约束验证:");
            verifyAllConstraints(predictions);

            System.out.println("\n====================================================\n");
        }
    }

    /**
     * 主入口：执行完整的预测和约束处理
     */
    public static PredictionPlan predict(int sampleSize) {
        List<DoubleColorBallMatrixItem> matrixItemList = loadMatrixData();
        if (matrixItemList == null || matrixItemList.size() < sampleSize) {
            System.err.println("数据不足，需要至少 " + sampleSize + " 条数据");
            return null;
        }

        return predictWithData(matrixItemList, sampleSize);
    }

    /**
     * 完整的预测流程：包含迭代去重和验证
     */
    public static PredictionPlan predictWithData(List<DoubleColorBallMatrixItem> matrixItemList, int sampleSize) {
        PredictionPlan plan = new PredictionPlan(sampleSize);

        // 提取最后N个数据作为样本
        int startIndex = Math.max(0, matrixItemList.size() - sampleSize);
        List<Integer> sampleData = flattenDataList(
                matrixItemList.subList(startIndex, matrixItemList.size())
        );
        plan.setSampleData(sampleData);

        // 第一步：执行8种算法，生成初始预测
        plan.predictions.addAll(runAllAlgorithms(sampleData));
        plan.processingLog.add("第1步: 执行8种算法，生成初始预测");

        // 第二步：迭代处理直到所有约束都满足或达到最大迭代数
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            plan.iterationCount = iteration + 1;

            // 检查所有约束
            List<String> violations = checkAllConstraints(plan.predictions);

            if (violations.isEmpty()) {
                plan.processingLog.add("第" + (iteration + 2) + "步: ✅ 所有约束已满足！");
                plan.allConstraintsMet = true;
                break;
            }

            // 有约束违反
            plan.processingLog.add("第" + (iteration + 2) + "步: 发现 " + violations.size() + " 项约束违规");

            // 处理约束违规
            boolean improved = improveConstraints(plan, sampleData);

            if (!improved) {
                plan.processingLog.add("  警告: 无法进一步改进约束");
                break;
            }
        }

        if (!plan.allConstraintsMet) {
            List<String> remaining = checkAllConstraints(plan.predictions);

            // 最后一次尝试：强制去除剩余的重复
            if (!remaining.isEmpty()) {
                plan.processingLog.add("执行最终去重处理...");
                forceRemoveDuplicates(plan, sampleData);

                List<String> afterFinal = checkAllConstraints(plan.predictions);
                if (afterFinal.isEmpty()) {
                    plan.processingLog.add("✅ 最终去重成功！所有约束已满足");
                    plan.allConstraintsMet = true;
                } else {
                    plan.processingLog.add("最终: 仍存在 " + afterFinal.size() + " 项约束违规");
                }
            }
        }

        return plan;
    }

    /**
     * 检查所有约束
     */
    private static List<String> checkAllConstraints(List<PredictionResult> predictions) {
        List<String> violations = new ArrayList<>();

        // 约束3: 数值规则
        for (int i = 0; i < predictions.size(); i++) {
            List<Integer> pred = predictions.get(i).predictedData;

            // 检查长度
            if (pred.size() != 7) {
                violations.add("预测" + (i + 1) + ": 长度 " + pred.size() + " != 7");
                continue;
            }

            // 只检查前6个红球的升序（不检查蓝球）
            for (int j = 0; j < 5; j++) {
                if (pred.get(j) >= pred.get(j + 1)) {
                    violations.add("预测" + (i + 1) + ": 红球不升序 pos" + j + "→" + (j + 1) + ": " + pred.get(j) + ">=" + pred.get(j + 1));
                    break;  // 只报告第一个问题
                }
            }

            // 检查无重复（只检查前6个红球）
            Set<Integer> reds = new HashSet<>(pred.subList(0, 6));
            if (reds.size() != 6) {
                violations.add("预测" + (i + 1) + ": 红球有重复");
            }

            // 检查范围
            for (int j = 0; j < 6; j++) {
                int num = pred.get(j);
                if (num < 1 || num > RED_BALL_MAX) {
                    violations.add("预测" + (i + 1) + ": 红球超出范围 pos" + j + "=" + num);
                    break;
                }
            }

            int blue = pred.get(6);
            if (blue < 1 || blue > BLUE_BALL_MAX) {
                violations.add("预测" + (i + 1) + ": 蓝球超出范围 " + blue);
            }
        }

        // 约束4: 第一个号码不重复
        Map<Integer, List<Integer>> firstNumMap = new HashMap<>();
        for (int i = 0; i < predictions.size(); i++) {
            int firstNum = predictions.get(i).predictedData.get(0);
            firstNumMap.computeIfAbsent(firstNum, k -> new ArrayList<>()).add(i + 1);
        }

        for (Integer num : firstNumMap.keySet()) {
            List<Integer> indices = firstNumMap.get(num);
            if (indices.size() > 1) {
                violations.add("约束4: 第一个号码 " + num + " 重复在第" + indices + "组");
            }
        }

        // 约束5: 最后一个号码不重复
        Map<Integer, List<Integer>> lastNumMap = new HashMap<>();
        for (int i = 0; i < predictions.size(); i++) {
            int lastNum = predictions.get(i).predictedData.get(6);
            lastNumMap.computeIfAbsent(lastNum, k -> new ArrayList<>()).add(i + 1);
        }

        for (Integer num : lastNumMap.keySet()) {
            List<Integer> indices = lastNumMap.get(num);
            if (indices.size() > 1) {
                violations.add("约束5: 最后一个号码 " + num + " 重复在第" + indices + "组");
            }
        }

        return violations;
    }

    /**
     * 最后的强制去重 - 处理最顽固的重复
     */
    private static void forceRemoveDuplicates(PredictionPlan plan, List<Integer> sampleData) {
        int maxAttempts = 3;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<String> violations = checkAllConstraints(plan.predictions);

            if (violations.isEmpty()) {
                return;  // 所有约束都满足了
            }

            // 找出所有重复
            for (String violation : violations) {
                if (violation.contains("约束4") || violation.contains("约束5")) {
                    // 提取重复的组索引
                    Set<Integer> indexesToRegenerate = extractIndicesFromViolation(violation, plan.predictions);

                    // 确定是第几个位置重复
                    int position = violation.contains("约束4") ? 0 : 6;

                    // 获取该位置所有现存的值
                    Set<Integer> existSet = new HashSet<>();
                    for (int i = 0; i < plan.predictions.size(); i++) {
                        if (!indexesToRegenerate.contains(i)) {
                            existSet.add(plan.predictions.get(i).predictedData.get(position));
                        }
                    }

                    // 重新生成所有重复的索引
                    for (int idx : indexesToRegenerate) {
                        List<Integer> newData = regeneratePredictionAggressive(sampleData,
                                plan.predictions.get(idx).algorithmName, existSet, position);

                        if (newData != null && isValidPrediction(newData)) {
                            plan.predictions.set(idx, new PredictionResult(
                                    plan.predictions.get(idx).algorithmName, newData));
                            existSet.add(newData.get(position));
                        }
                    }
                }
            }
        }
    }

    /**
     * 从约束违规信息中提取需要重新生成的索引
     */
    private static Set<Integer> extractIndicesFromViolation(String violation, List<PredictionResult> predictions) {
        Set<Integer> indices = new HashSet<>();

        // 格式: "约束4: 第一个号码 1 重复在第[1, 6, 8]组" 或 "约束5: 最后一个号码 10 重复在第[1, 5]组"
        String pattern = "\\[(.*?)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(violation);

        if (m.find()) {
            String[] parts = m.group(1).split(",");
            for (String part : parts) {
                try {
                    int idx = Integer.parseInt(part.trim()) - 1;  // 转换为0-based索引
                    if (idx >= 0 && idx < predictions.size()) {
                        indices.add(idx);
                    }
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }

        return indices;
    }

    /**
     * 改进约束（处理一轮）- 增强版：更激进的去重
     */
    private static boolean improveConstraints(PredictionPlan plan, List<Integer> sampleData) {
        boolean improved = false;

        // 处理第一个号码重复 - 激进策略：全部重新生成
        Map<Integer, List<Integer>> firstNumMap = new HashMap<>();
        for (int i = 0; i < plan.predictions.size(); i++) {
            int firstNum = plan.predictions.get(i).predictedData.get(0);
            firstNumMap.computeIfAbsent(firstNum, k -> new ArrayList<>()).add(i);
        }

        for (Integer num : new ArrayList<>(firstNumMap.keySet())) {
            List<Integer> indices = firstNumMap.get(num);
            if (indices.size() > 1) {
                // 激进策略：对所有重复的都尝试重新生成
                Set<Integer> existSet = collectExistingValues(plan.predictions, 0, indices);
                for (int idx : indices) {
                    List<Integer> newData = regeneratePredictionAggressive(sampleData,
                            plan.predictions.get(idx).algorithmName, existSet, 0);
                    if (newData != null && isValidPrediction(newData)) {
                        plan.predictions.set(idx, new PredictionResult(
                                plan.predictions.get(idx).algorithmName, newData));
                        improved = true;
                        existSet.add(newData.get(0));  // 更新 existSet
                    }
                }
            }
        }

        // 处理最后一个号码重复 - 激进策略：全部重新生成
        Map<Integer, List<Integer>> lastNumMap = new HashMap<>();
        for (int i = 0; i < plan.predictions.size(); i++) {
            int lastNum = plan.predictions.get(i).predictedData.get(6);
            lastNumMap.computeIfAbsent(lastNum, k -> new ArrayList<>()).add(i);
        }

        for (Integer num : new ArrayList<>(lastNumMap.keySet())) {
            List<Integer> indices = lastNumMap.get(num);
            if (indices.size() > 1) {
                Set<Integer> existSet = collectExistingValues(plan.predictions, 6, indices);
                for (int idx : indices) {
                    List<Integer> newData = regeneratePredictionAggressive(sampleData,
                            plan.predictions.get(idx).algorithmName, existSet, 6);
                    if (newData != null && isValidPrediction(newData)) {
                        plan.predictions.set(idx, new PredictionResult(
                                plan.predictions.get(idx).algorithmName, newData));
                        improved = true;
                        existSet.add(newData.get(6));  // 更新 existSet
                    }
                }
            }
        }

        return improved;
    }

    /**
     * 收集现存的值（不包括指定的索引）
     */
    private static Set<Integer> collectExistingValues(List<PredictionResult> predictions, int position, List<Integer> excludeIndices) {
        Set<Integer> existSet = new HashSet<>();
        Set<Integer> excludeSet = new HashSet<>(excludeIndices);

        for (int i = 0; i < predictions.size(); i++) {
            if (!excludeSet.contains(i)) {
                existSet.add(predictions.get(i).predictedData.get(position));
            }
        }

        return existSet;
    }

    /**
     * 执行所有8种算法
     */
    private static List<PredictionResult> runAllAlgorithms(List<Integer> sampleData) {
        List<PredictionResult> results = new ArrayList<>();

        results.add(new PredictionResult("频率分布算法", frequencyBasedPrediction(sampleData)));
        results.add(new PredictionResult("移动平均算法", movingAveragePrediction(sampleData)));
        results.add(new PredictionResult("区间分布算法", intervalDistributionPrediction()));
        results.add(new PredictionResult("差值模式算法", differencePatternPrediction(sampleData)));
        results.add(new PredictionResult("最近邻算法", nearestNeighborPrediction(sampleData)));
        results.add(new PredictionResult("随机加权算法", randomWeightedPrediction(sampleData)));
        results.add(new PredictionResult("周期性算法", periodicalPrediction(sampleData)));
        results.add(new PredictionResult("混合启发式算法", hybridHeuristicPrediction(sampleData)));

        return results;
    }

    /**
     * 激进的重新生成 - 尝试更多次
     */
    private static List<Integer> regeneratePredictionAggressive(List<Integer> sampleData, String algorithmName,
                                                                 Set<Integer> existSet, int position) {
        // 尝试 100 次来找到有效的替代数据
        for (int attempt = 0; attempt < 100; attempt++) {
            List<Integer> generated;

            switch (algorithmName) {
                case "频率分布算法": generated = frequencyBasedPrediction(sampleData); break;
                case "移动平均算法": generated = movingAveragePrediction(sampleData); break;
                case "区间分布算法": generated = intervalDistributionPrediction(); break;
                case "差值模式算法": generated = differencePatternPrediction(sampleData); break;
                case "最近邻算法": generated = nearestNeighborPrediction(sampleData); break;
                case "随机加权算法": generated = randomWeightedPrediction(sampleData); break;
                case "周期性算法": generated = periodicalPrediction(sampleData); break;
                case "混合启发式算法": generated = hybridHeuristicPrediction(sampleData); break;
                default: generated = null;
            }

            if (generated != null && !existSet.contains(generated.get(position)) && isValidPrediction(generated)) {
                return generated;
            }
        }

        return null;
    }

    /**
     * 重新生成预测数据（尝试更多次）
     */
    private static List<Integer> regeneratePrediction(List<Integer> sampleData, String algorithmName,
                                                       Set<Integer> existSet, int position) {
        for (int attempt = 0; attempt < MAX_REGENERATION_ATTEMPTS; attempt++) {
            List<Integer> generated;

            switch (algorithmName) {
                case "频率分布算法": generated = frequencyBasedPrediction(sampleData); break;
                case "移动平均算法": generated = movingAveragePrediction(sampleData); break;
                case "区间分布算法": generated = intervalDistributionPrediction(); break;
                case "差值模式算法": generated = differencePatternPrediction(sampleData); break;
                case "最近邻算法": generated = nearestNeighborPrediction(sampleData); break;
                case "随机加权算法": generated = randomWeightedPrediction(sampleData); break;
                case "周期性算法": generated = periodicalPrediction(sampleData); break;
                case "混合启发式算法": generated = hybridHeuristicPrediction(sampleData); break;
                default: generated = null;
            }

            if (generated != null && !existSet.contains(generated.get(position)) && isValidPrediction(generated)) {
                return generated;
            }
        }

        return null;
    }

    /**
     * 验证预测数据是否合法
     */
    private static boolean isValidPrediction(List<Integer> data) {
        if (data == null || data.size() != 7) return false;

        // 前6个红球升序
        for (int i = 0; i < 5; i++) {
            if (data.get(i) >= data.get(i + 1)) return false;
        }

        // 前6个红球无重复且在范围内
        Set<Integer> reds = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            int num = data.get(i);
            if (num < 1 || num > RED_BALL_MAX || !reds.add(num)) return false;
        }

        // 蓝球在范围内
        int blue = data.get(6);
        if (blue < 1 || blue > BLUE_BALL_MAX) return false;

        return true;
    }

    /**
     * 最终验证所有约束
     */
    private static void verifyAllConstraints(List<PredictionResult> predictions) {
        List<String> violations = checkAllConstraints(predictions);

        if (violations.isEmpty()) {
            System.out.println("  ✅ 约束3 (数值规则): 通过 - 所有数据升序无重复");
            System.out.println("  ✅ 约束4 (第一个号码): 通过 - 所有第一个号码都不重复");
            System.out.println("  ✅ 约束5 (最后一个号码): 通过 - 所有最后一个号码都不重复");
            System.out.println("  🎉 所有约束都已满足！");
        } else {
            System.out.println("  ⚠️ 仍存在约束违规 (" + violations.size() + "项):");
            for (String v : violations) {
                System.out.println("    - " + v);
            }
        }
    }

    // ==================== 8种预测算法 - 已修复 ====================

    /**
     * 1. 频率分布算法 - 修复：确保返回 [红1-6, 蓝]
     */
    private static List<Integer> frequencyBasedPrediction(List<Integer> sampleData) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < sampleData.size() - 1; i += 7) {
            for (int j = 0; j < 6 && i + j < sampleData.size(); j++) {
                freq.merge(sampleData.get(i + j), 1, Integer::sum);
            }
        }

        List<Integer> reds = freq.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(6)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        while (reds.size() < 6) {
            int num = random.nextInt(RED_BALL_MAX) + 1;
            if (!reds.contains(num)) reds.add(num);
        }

        reds.sort(null);
        reds = reds.stream().limit(6).collect(Collectors.toList());

        // ✅ 分别处理红球和蓝球
        List<Integer> result = new ArrayList<>(reds);
        result.add(random.nextInt(BLUE_BALL_MAX) + 1);

        return result;
    }

    /**
     * 2. 移动平均算法
     */
    private static List<Integer> movingAveragePrediction(List<Integer> sampleData) {
        Map<Integer, Double> weights = new HashMap<>();
        int totalItems = Math.max(1, sampleData.size() / 7);

        for (int i = 0; i < sampleData.size() - 1; i += 7) {
            double weight = 1.0 / (totalItems - i / 7 + 1);
            for (int j = 0; j < 6 && i + j < sampleData.size(); j++) {
                weights.merge(sampleData.get(i + j), weight, Double::sum);
            }
        }

        List<Integer> reds = weights.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(6)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        while (reds.size() < 6) {
            int num = random.nextInt(RED_BALL_MAX) + 1;
            if (!reds.contains(num)) reds.add(num);
        }

        reds.sort(null);
        reds = reds.stream().limit(6).collect(Collectors.toList());

        List<Integer> result = new ArrayList<>(reds);
        result.add(random.nextInt(BLUE_BALL_MAX) + 1);
        return result;
    }

    /**
     * 3. 区间分布算法
     */
    private static List<Integer> intervalDistributionPrediction() {
        Set<Integer> reds = new TreeSet<>();

        // 将1-33分成6个区间，从每个区间选一个
        for (int i = 0; i < 6; i++) {
            int start = i * 5 + 1;
            int end = Math.min(33, (i + 1) * 5 + 3);
            reds.add(start + random.nextInt(end - start + 1));
        }

        List<Integer> result = new ArrayList<>(reds);
        result.sort(null);
        result = result.stream().limit(6).collect(Collectors.toList());

        while (result.size() < 6) {
            int num = random.nextInt(RED_BALL_MAX) + 1;
            if (!result.contains(num)) result.add(num);
        }

        result.sort(null);
        result = result.stream().limit(6).collect(Collectors.toList());

        result.add(random.nextInt(BLUE_BALL_MAX) + 1);
        return result;
    }

    /**
     * 4. 差值模式算法
     */
    private static List<Integer> differencePatternPrediction(List<Integer> sampleData) {
        Set<Integer> reds = new TreeSet<>();

        if (sampleData.size() >= 14) {
            List<Integer> prev = sampleData.subList(sampleData.size() - 14, sampleData.size() - 7);
            List<Integer> curr = sampleData.subList(sampleData.size() - 7, sampleData.size());

            for (int i = 0; i < 6; i++) {
                int diff = curr.get(i) - prev.get(i);
                int next = Math.max(1, Math.min(RED_BALL_MAX, curr.get(i) + diff / 2));
                reds.add(next);
            }
        }

        while (reds.size() < 6) {
            reds.add(random.nextInt(RED_BALL_MAX) + 1);
        }

        List<Integer> result = new ArrayList<>(reds);
        result.sort(null);
        result = result.stream().limit(6).collect(Collectors.toList());

        result.add(random.nextInt(BLUE_BALL_MAX) + 1);
        return result;
    }

    /**
     * 5. 最近邻算法
     */
    private static List<Integer> nearestNeighborPrediction(List<Integer> sampleData) {
        Set<Integer> reds = new TreeSet<>();

        if (sampleData.size() >= 7) {
            List<Integer> lastDraw = sampleData.subList(sampleData.size() - 7, sampleData.size());
            for (int i = 0; i < 6; i++) {
                int num = lastDraw.get(i);
                int near = Math.max(1, Math.min(RED_BALL_MAX, num + random.nextInt(3) - 1));
                reds.add(near);
            }
        }

        while (reds.size() < 6) {
            reds.add(random.nextInt(RED_BALL_MAX) + 1);
        }

        List<Integer> result = new ArrayList<>(reds);
        result.sort(null);
        result = result.stream().limit(6).collect(Collectors.toList());

        result.add(random.nextInt(BLUE_BALL_MAX) + 1);
        return result;
    }

    /**
     * 6. 随机加权算法
     */
    private static List<Integer> randomWeightedPrediction(List<Integer> sampleData) {
        Map<Integer, Double> weights = new HashMap<>();
        int totalItems = Math.max(1, sampleData.size() / 7);

        for (int i = 0; i < sampleData.size() - 1; i += 7) {
            double weight = Math.pow(0.95, totalItems - i / 7);
            for (int j = 0; j < 6 && i + j < sampleData.size(); j++) {
                weights.merge(sampleData.get(i + j), weight, Double::sum);
            }
        }

        List<Integer> reds = weights.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(6)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        while (reds.size() < 6) {
            int num = random.nextInt(RED_BALL_MAX) + 1;
            if (!reds.contains(num)) reds.add(num);
        }

        reds.sort(null);
        reds = reds.stream().limit(6).collect(Collectors.toList());

        List<Integer> result = new ArrayList<>(reds);
        result.add(random.nextInt(BLUE_BALL_MAX) + 1);
        return result;
    }

    /**
     * 7. 周期性算法
     */
    private static List<Integer> periodicalPrediction(List<Integer> sampleData) {
        Set<Integer> reds = new TreeSet<>();

        if (sampleData.size() >= 14) {
            List<Integer> curr = sampleData.subList(sampleData.size() - 7, sampleData.size());
            List<Integer> prev = sampleData.subList(sampleData.size() - 14, sampleData.size() - 7);

            for (int i = 0; i < 6; i++) {
                int predicted = (curr.get(i) + prev.get(i)) / 2;
                reds.add(Math.max(1, Math.min(RED_BALL_MAX, predicted)));
            }
        }

        while (reds.size() < 6) {
            reds.add(random.nextInt(RED_BALL_MAX) + 1);
        }

        List<Integer> result = new ArrayList<>(reds);
        result.sort(null);
        result = result.stream().limit(6).collect(Collectors.toList());

        result.add(random.nextInt(BLUE_BALL_MAX) + 1);
        return result;
    }

    /**
     * 8. 混合启发式算法
     */
    private static List<Integer> hybridHeuristicPrediction(List<Integer> sampleData) {
        Set<Integer> combined = new TreeSet<>();

        List<Integer> freq = frequencyBasedPrediction(sampleData).subList(0, 3);
        List<Integer> trend = movingAveragePrediction(sampleData).subList(0, 3);

        combined.addAll(freq);
        combined.addAll(trend);

        while (combined.size() < 6) {
            combined.add(random.nextInt(RED_BALL_MAX) + 1);
        }

        List<Integer> result = new ArrayList<>(combined);
        result.sort(null);
        result = result.stream().limit(6).collect(Collectors.toList());

        result.add(random.nextInt(BLUE_BALL_MAX) + 1);
        return result;
    }

    // ==================== 辅助方法 ====================

    private static List<DoubleColorBallMatrixItem> loadMatrixData() {
        try {
            return FileProvider.readFile("DoubleColorBallMatrixData.json",
                    new TypeReference<List<DoubleColorBallMatrixItem>>() {});
        } catch (Exception e) {
            System.err.println("加载矩阵数据失败: " + e.getMessage());
            return null;
        }
    }

    private static List<Integer> flattenDataList(List<DoubleColorBallMatrixItem> items) {
        List<Integer> result = new ArrayList<>();
        for (DoubleColorBallMatrixItem item : items) {
            result.addAll(item.getDataList());
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println("DoubleColorBallPredictorV4 - 完全修复版本");
        System.out.println("包含: 算法修复 + 完整去重 + 约束验证\n");

        PredictionPlan plan = predict(100);
        if (plan != null) {
            plan.printReport();
        }
    }
}
