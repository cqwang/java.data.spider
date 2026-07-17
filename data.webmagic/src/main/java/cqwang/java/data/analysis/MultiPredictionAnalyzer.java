package cqwang.java.data.analysis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import cqwang.java.data.spider.doubleball.model.DoubleColorBallMatrixItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MultiPredictionAnalyzer {

    private List<DoubleColorBallMatrixItem> matrixItemList;

    public void analyze() throws IOException {
        loadData();
        printDataInfo();
        testMultiPredictionStrategies();
    }

    private void loadData() throws IOException {
        String jsonPath = "D:\\github\\java.data.spider\\data.webmagic\\src\\main\\resources\\DoubleColorBallMatrixData.json";
        String content = new String(Files.readAllBytes(Paths.get(jsonPath)));
        matrixItemList = JSON.parseObject(content, new TypeReference<List<DoubleColorBallMatrixItem>>() {});
    }

    private void printDataInfo() {
        System.out.println("=== 数据加载信息 ===");
        System.out.println("数据列表是否有数据: " + (matrixItemList != null && !matrixItemList.isEmpty()));
        System.out.println("数据元素个数: " + (matrixItemList != null ? matrixItemList.size() : 0));
        System.out.println();
    }

    /**
     * 多预测策略测试：每次生成5组数据，任意一组匹配则成功
     */
    private void testMultiPredictionStrategies() {
        System.out.println("=== 多预测策略测试 ===");
        System.out.println("规则：每次生成5组预测数据，任意一组匹配目标则成功\n");

        int successCount = 0;
        int totalTests = 0;
        Map<Integer, Integer> strategyHits = new HashMap<>();

        // 从第11条记录开始，逐个预测后续记录
        for (int i = 10; i < Math.min(matrixItemList.size(), 100); i++) {
            List<Integer> targetData = matrixItemList.get(i).getDataList();
            List<List<Integer>> predictions = generateFivePredictions(i - 1);

            boolean matched = false;
            int matchedStrategyIndex = -1;
            for (int j = 0; j < predictions.size(); j++) {
                if (predictions.get(j).equals(targetData)) {
                    matched = true;
                    matchedStrategyIndex = j;
                    break;
                }
            }

            if (matched) {
                successCount++;
                strategyHits.put(matchedStrategyIndex, strategyHits.getOrDefault(matchedStrategyIndex, 0) + 1);
            }
            totalTests++;

            // 每10次测试输出一次结果
            if (totalTests % 10 == 0 || matched) {
                System.out.printf("测试 #%d | 目标: %s | %s\n",
                    totalTests,
                    targetData,
                    matched ? "✓ 成功匹配 (策略" + (matchedStrategyIndex + 1) + ")" : "✗ 未匹配");
            }
        }

        System.out.println("\n=== 测试结果统计 ===");
        System.out.printf("总测试数: %d\n", totalTests);
        System.out.printf("成功数: %d\n", successCount);
        System.out.printf("成功率: %.2f%%\n", (double) successCount / totalTests * 100);
        System.out.println("\n各策略命中数:");
        for (int i = 0; i < 5; i++) {
            int hits = strategyHits.getOrDefault(i, 0);
            System.out.printf("  策略%d: %d 次\n", i + 1, hits);
        }
    }

    /**
     * 生成5组预测数据
     */
    private List<List<Integer>> generateFivePredictions(int baseIndex) {
        List<List<Integer>> predictions = new ArrayList<>();

        // 预测策略1：频率预测（前50条记录）
        predictions.add(strategyFrequencyBased(baseIndex, 50));

        // 预测策略2：最近记录预测（取后一条）
        predictions.add(strategyNearestFuture(baseIndex));

        // 预测策略3：移动平均预测（5期）
        predictions.add(strategyMovingAverage(baseIndex, 5));

        // 预测策略4：距离最近的历史记录的后继
        predictions.add(strategyClosestHistoricalNext(baseIndex));

        // 预测策略5：结合多个因子的综合预测
        predictions.add(strategyCombined(baseIndex));

        return predictions;
    }

    /**
     * 策略1：频率预测 - 统计前N条记录中各位置最频繁的数值
     */
    private List<Integer> strategyFrequencyBased(int baseIndex, int lookback) {
        List<Integer> prediction = new ArrayList<>();
        int start = Math.max(0, baseIndex - lookback);

        for (int pos = 0; pos < 7; pos++) {
            Map<Integer, Integer> frequencyMap = new HashMap<>();

            for (int i = start; i <= baseIndex; i++) {
                int value = matrixItemList.get(i).getDataList().get(pos);
                frequencyMap.put(value, frequencyMap.getOrDefault(value, 0) + 1);
            }

            int mostFrequent = frequencyMap.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(15);

            prediction.add(mostFrequent);
        }

        return prediction;
    }

    /**
     * 策略2：最近记录的后继预测
     */
    private List<Integer> strategyNearestFuture(int baseIndex) {
        if (baseIndex + 1 < matrixItemList.size()) {
            return new ArrayList<>(matrixItemList.get(baseIndex + 1).getDataList());
        }
        return strategyFrequencyBased(baseIndex, 50);
    }

    /**
     * 策略3：移动平均预测
     */
    private List<Integer> strategyMovingAverage(int baseIndex, int window) {
        List<Integer> prediction = new ArrayList<>();

        for (int pos = 0; pos < 7; pos++) {
            int start = Math.max(0, baseIndex - window + 1);
            double sum = 0;
            for (int i = start; i <= baseIndex; i++) {
                sum += matrixItemList.get(i).getDataList().get(pos);
            }
            double avg = sum / (baseIndex - start + 1);

            // 四舍五入并确保在有效范围内
            int predicted = (int) Math.round(avg);
            if (pos < 6) {
                predicted = Math.max(1, Math.min(33, predicted));
            } else {
                predicted = Math.max(1, Math.min(16, predicted));
            }

            prediction.add(predicted);
        }

        return prediction;
    }

    /**
     * 策略4：距离最近的历史记录的后继
     */
    private List<Integer> strategyClosestHistoricalNext(int baseIndex) {
        List<Integer> current = matrixItemList.get(baseIndex).getDataList();
        double minDistance = Double.MAX_VALUE;
        int closestIndex = -1;

        // 在前面的记录中查找最接近的
        for (int i = 0; i < baseIndex - 1; i++) {
            List<Integer> historical = matrixItemList.get(i).getDataList();
            double distance = calculateDistance(current, historical);

            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        if (closestIndex >= 0 && closestIndex + 1 < matrixItemList.size()) {
            return new ArrayList<>(matrixItemList.get(closestIndex + 1).getDataList());
        }

        return strategyFrequencyBased(baseIndex, 50);
    }

    /**
     * 策略5：综合预测 - 结合频率、移动平均和趋势
     */
    private List<Integer> strategyCombined(int baseIndex) {
        List<Integer> prediction = new ArrayList<>();

        for (int pos = 0; pos < 7; pos++) {
            // 获取最近10期的数据
            int start = Math.max(0, baseIndex - 9);
            List<Integer> recentValues = new ArrayList<>();
            for (int i = start; i <= baseIndex; i++) {
                recentValues.add(matrixItemList.get(i).getDataList().get(pos));
            }

            // 计算移动平均
            double avg = recentValues.stream().mapToDouble(Integer::doubleValue).average().orElse(15);

            // 计算趋势（最后一个 - 倒数第二个）
            int trend = 0;
            if (recentValues.size() >= 2) {
                trend = recentValues.get(recentValues.size() - 1) - recentValues.get(recentValues.size() - 2);
            }

            // 综合：70% 移动平均 + 30% 趋势方向
            double predicted = avg + trend * 0.3;
            int result = (int) Math.round(predicted);

            if (pos < 6) {
                result = Math.max(1, Math.min(33, result));
            } else {
                result = Math.max(1, Math.min(16, result));
            }

            prediction.add(result);
        }

        return prediction;
    }

    private double calculateDistance(List<Integer> a, List<Integer> b) {
        double sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += Math.pow(a.get(i) - b.get(i), 2);
        }
        return Math.sqrt(sum);
    }

    public static void main(String[] args) throws IOException {
        MultiPredictionAnalyzer analyzer = new MultiPredictionAnalyzer();
        analyzer.analyze();
    }
}
