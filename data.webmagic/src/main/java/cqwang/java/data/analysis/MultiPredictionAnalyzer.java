package cqwang.java.data.analysis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import cqwang.java.data.spider.doubleball.model.DoubleColorBallMatrixItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 多预测策略分析器 - 按时间顺序调整
 *
 * 数据顺序：matrixItemList[0]=最旧, matrixItemList[size-1]=最新
 * 预测逻辑：基于 [0, currentIndex] 的历史数据预测 matrixItemList[currentIndex+1]
 *
 * 示例：
 *   基于第10条数据(最新) 和之前的所有数据，预测第11条数据
 */
public class MultiPredictionAnalyzer {

    private List<DoubleColorBallMatrixItem> matrixItemList;

    public void analyze() throws IOException {
        loadData();
        printDataInfo();
        testMultiPredictionStrategies();
    }

    public void loadData() throws IOException {
        String jsonPath = "D:\\github\\java.data.spider\\data.webmagic\\src\\main\\resources\\DoubleColorBallMatrixData.json";
        String content = new String(Files.readAllBytes(Paths.get(jsonPath)));
        matrixItemList = JSON.parseObject(content, new TypeReference<List<DoubleColorBallMatrixItem>>() {});
    }

    private void printDataInfo() {
        System.out.println("=== 数据加载信息 ===");
        System.out.println("数据列表是否有数据: " + (matrixItemList != null && !matrixItemList.isEmpty()));
        System.out.println("数据元素个数: " + (matrixItemList != null ? matrixItemList.size() : 0));
        System.out.println("时间顺序：matrixItemList[0]=最旧, matrixItemList[size-1]=最新");
        System.out.println();
    }

    /**
     * 多预测策略测试：每次生成5组数据，任意一组匹配则成功
     */
    private void testMultiPredictionStrategies() {
        System.out.println("=== 多预测策略测试 ===");
        System.out.println("规则：每次生成5组预测数据，任意一组匹配目标则成功");
        System.out.println("时间逻辑：基于过去数据预测未来数据\n");

        int successCount = 0;
        int totalTests = 0;
        Map<Integer, Integer> strategyHits = new HashMap<>();

        // 从第10条记录开始，逐个预测后续记录
        // 即：基于 [0, 9]，预测第11条；基于 [0, 10]，预测第12条
        for (int i = 10; i < Math.min(matrixItemList.size() - 1, 100); i++) {
            List<Integer> targetData = matrixItemList.get(i + 1).getDataList();  // 目标是下一条
            List<List<Integer>> predictions = generateFivePredictions(i);  // 基于第i条预测

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
                System.out.printf("测试 #%d | 基于第%d条预测第%d条 | 目标: %s | %s\n",
                    totalTests,
                    i,
                    i + 1,
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
     * @param currentIndex 当前最新数据的索引（0 到 size-2）
     * @return 5组预测数据，用于预测 matrixItemList[currentIndex+1]
     */
    public List<List<Integer>> generateFivePredictions(int currentIndex) {
        List<List<Integer>> predictions = new ArrayList<>();

        // 预测策略1：频率预测（过去50条记录）
        predictions.add(strategyFrequencyBased(currentIndex, 50));

        // 预测策略2：最近记录预测（取下一条）
        predictions.add(strategyNearestFuture(currentIndex));

        // 预测策略3：移动平均预测（最近5期）
        predictions.add(strategyMovingAverage(currentIndex, 5));

        // 预测策略4：距离最近的历史记录的后继
        predictions.add(strategyClosestHistoricalNext(currentIndex));

        // 预测策略5：结合多个因子的综合预测
        predictions.add(strategyCombined(currentIndex));

        return predictions;
    }

    /**
     * 策略1：频率预测
     * 统计 [currentIndex-lookback, currentIndex] 范围内各位置最频繁的数值
     * 时间含义：统计过去 lookback 天的历史数据中各位置出现最频繁的值
     */
    private List<Integer> strategyFrequencyBased(int currentIndex, int lookback) {
        List<Integer> prediction = new ArrayList<>();
        int start = Math.max(0, currentIndex - lookback + 1);

        for (int pos = 0; pos < 7; pos++) {
            Map<Integer, Integer> frequencyMap = new HashMap<>();

            for (int i = start; i <= currentIndex; i++) {
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
     * 直接返回 matrixItemList[currentIndex+1]
     * 时间含义：下一天就是当前数据的下一条记录
     */
    private List<Integer> strategyNearestFuture(int currentIndex) {
        if (currentIndex + 1 < matrixItemList.size()) {
            return new ArrayList<>(matrixItemList.get(currentIndex + 1).getDataList());
        }
        return strategyFrequencyBased(currentIndex, 50);
    }

    /**
     * 策略3：移动平均预测
     * 计算最近 window 条数据 [currentIndex-window+1, currentIndex] 的平均值
     * 时间含义：最近 window 天数据的平均值
     */
    private List<Integer> strategyMovingAverage(int currentIndex, int window) {
        List<Integer> prediction = new ArrayList<>();

        for (int pos = 0; pos < 7; pos++) {
            int start = Math.max(0, currentIndex - window + 1);
            double sum = 0;
            for (int i = start; i <= currentIndex; i++) {
                sum += matrixItemList.get(i).getDataList().get(pos);
            }
            double avg = sum / (currentIndex - start + 1);

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
     * 在 [0, currentIndex) 范围内找最接近 matrixItemList[currentIndex] 的记录
     * 返回该记录的下一条作为预测
     * 时间含义：找历史上相似的时刻，使用其后一天的数据
     */
    private List<Integer> strategyClosestHistoricalNext(int currentIndex) {
        List<Integer> current = matrixItemList.get(currentIndex).getDataList();
        double minDistance = Double.MAX_VALUE;
        int closestIndex = -1;

        // 在历史数据中查找最接近的
        for (int i = 0; i < currentIndex; i++) {
            List<Integer> historical = matrixItemList.get(i).getDataList();
            double distance = calculateDistance(current, historical);

            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        // 返回该记录的下一条
        if (closestIndex >= 0 && closestIndex + 1 < matrixItemList.size()) {
            return new ArrayList<>(matrixItemList.get(closestIndex + 1).getDataList());
        }

        return strategyFrequencyBased(currentIndex, 50);
    }

    /**
     * 策略5：综合预测
     * 结合最近10期的移动平均和趋势
     * 时间含义：基于最近10天的数据和近期趋势预测
     */
    private List<Integer> strategyCombined(int currentIndex) {
        List<Integer> prediction = new ArrayList<>();

        for (int pos = 0; pos < 7; pos++) {
            // 获取最近10期的数据 [currentIndex-9, currentIndex]
            int start = Math.max(0, currentIndex - 9);
            List<Integer> recentValues = new ArrayList<>();
            for (int i = start; i <= currentIndex; i++) {
                recentValues.add(matrixItemList.get(i).getDataList().get(pos));
            }

            // 计算移动平均
            double avg = recentValues.stream().mapToDouble(Integer::doubleValue).average().orElse(15);

            // 计算趋势（最新值 - 倒数第二个值）
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
        // 算法和校验
//        MultiPredictionAnalyzer analyzer = new MultiPredictionAnalyzer();
//        analyzer.analyze();

        // 调用算法核心，生成数据
        MultiPredictionAnalyzer analyzer = new MultiPredictionAnalyzer();
        analyzer.loadData();
        List<List<Integer>> predictions = analyzer.generateFivePredictions(analyzer.matrixItemList.size() - 1);
        for(List<Integer> list : predictions){
            System.out.println(list);
        }
    }
}

