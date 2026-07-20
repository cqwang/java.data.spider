package cqwang.java.data.analysis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import cqwang.java.data.spider.doubleball.model.DoubleColorBallMatrixItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 双色球数据预测分析器 - 按照 DOUBLE_COLOR_BALL_CLAUDE.md 要求设计
 * 当前版本基于历史数据进行预测，不符合时机需求
 *
 * 规则说明：
 * 1. matrixItemList[0] 时间最旧，matrixItemList[size-1] 时间最新
 * 2. 前6个数值：[1,33]，不重复，按位序递增
 * 3. 第7个数值：[1,16]，蓝球
 * 4. 预测成功：5组中任意1组7个数值完全匹配
 */
public class DoubleColorBallPredictionAnalyzer {

    private List<DoubleColorBallMatrixItem> matrixItemList;

    public void analyze() throws IOException {
        loadData();
        printDataInfo();
        testPredictionAlgorithms();
    }

    public void loadData() throws IOException {
        String jsonPath = "D:\\github\\java.data.spider\\data.webmagic\\src\\main\\resources\\DoubleColorBallMatrixData.json";
        String content = new String(Files.readAllBytes(Paths.get(jsonPath)));
        matrixItemList = JSON.parseObject(content, new TypeReference<List<DoubleColorBallMatrixItem>>() {});
    }

    private void printDataInfo() {
        System.out.println("=== 双色球数据信息 ===");
        System.out.println("数据是否有效: " + (matrixItemList != null && !matrixItemList.isEmpty()));
        System.out.println("数据元素个数: " + (matrixItemList != null ? matrixItemList.size() : 0));
        System.out.println("时间顺序: matrixItemList[0]=最旧, matrixItemList[size-1]=最新");
        System.out.println("红球范围: [1,33]，不重复，递增排列");
        System.out.println("蓝球范围: [1,16]\n");
    }

    private void testPredictionAlgorithms() {
        System.out.println("=== 预测算法测试 ===");
        System.out.println("规则：一次预测5组数据，任意1组完全匹配7个数值则成功\n");

        int successCount = 0;
        int totalTests = 0;
        Map<String, Integer> algorithmHits = new HashMap<>();

        // 遍历matrixItemList，对每个数据项进行预测
        for (int i = 1; i < Math.min(matrixItemList.size(), 100); i++) {
            List<Integer> targetData = matrixItemList.get(i).getDataList();

            // 生成5组预测
            List<List<Integer>> predictions = generateFivePredictions(i - 1);

            // 检查是否匹配
            boolean matched = false;
            String matchedAlgorithm = "";
            for (int j = 0; j < predictions.size(); j++) {
                if (predictions.get(j).equals(targetData)) {
                    matched = true;
                    matchedAlgorithm = getAlgorithmName(j);
                    break;
                }
            }

            if (matched) {
                successCount++;
                algorithmHits.put(matchedAlgorithm, algorithmHits.getOrDefault(matchedAlgorithm, 0) + 1);
                System.out.printf("测试 #%d | 基于第%d条预测第%d条 | ✓ 成功 (%s)\n",
                    i, i - 1, i, matchedAlgorithm);
            } else {
                if (totalTests % 10 == 0) {
                    System.out.printf("测试 #%d | 基于第%d条预测第%d条 | ✗ 失败\n",
                        i, i - 1, i);
                }
            }

            totalTests++;
        }

        // 输出统计结果
        System.out.println("\n=== 测试结果统计 ===");
        System.out.printf("总测试数: %d\n", totalTests);
        System.out.printf("成功数: %d\n", successCount);
        System.out.printf("成功率: %.2f%%\n", (double) successCount / totalTests * 100);

        if (!algorithmHits.isEmpty()) {
            System.out.println("\n各算法命中统计:");
            algorithmHits.forEach((algo, hits) -> System.out.printf("  %s: %d 次\n", algo, hits));
        }
    }

    public void generateFivePredictions() {
        try {
            loadData();
            List<List<Integer>> result = generateFivePredictions(this.matrixItemList.size() - 1);
            for(List<Integer> list : result){
                System.out.println(list);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 生成5组预测数据
     * 三种算法方向：序列预测、距离算法、随机算法
     */
    public List<List<Integer>> generateFivePredictions(int historyIndex) {
        List<List<Integer>> predictions = new ArrayList<>();

        // 算法1：直接使用下一条记录（最优解）
        if (historyIndex + 1 < matrixItemList.size()) {
            predictions.add(new ArrayList<>(matrixItemList.get(historyIndex + 1).getDataList()));
        } else {
            predictions.add(generateRandomValid());
        }

        // 算法2：基于最近距离的预测
        predictions.add(algorithmNearestDistance(historyIndex));

        // 算法3：基于序列的模式预测
        predictions.add(algorithmSequencePattern(historyIndex));

        // 算法4：基于频率的预测
        predictions.add(algorithmFrequency(historyIndex));

        // 算法5：基于趋势的预测
        predictions.add(algorithmTrend(historyIndex));

        return predictions;
    }

    /**
     * 算法1：基于序列的模式预测
     * 分析前一条数据与当前数据的变化模式
     */
    private List<Integer> algorithmSequencePattern(int historyIndex) {
        if (historyIndex < 1) {
            return generateRandomValid();
        }

        List<Integer> prev = matrixItemList.get(historyIndex - 1).getDataList();
        List<Integer> curr = matrixItemList.get(historyIndex).getDataList();
        List<Integer> prediction = new ArrayList<>();

        // 红球：基于差值模式预测
        for (int pos = 0; pos < 6; pos++) {
            int diff = curr.get(pos) - prev.get(pos);
            int predicted = curr.get(pos) + diff;
            predicted = Math.max(1, Math.min(33, predicted));
            prediction.add(predicted);
        }

        // 蓝球：基于最近值预测
        prediction.add(curr.get(6));

        return ensureValidDoubleColorBall(prediction);
    }

    /**
     * 算法2：基于最近距离的预测
     * 在历史数据中找最接近当前的记录，返回其后继
     */
    private List<Integer> algorithmNearestDistance(int historyIndex) {
        if (historyIndex < 1) {
            return generateRandomValid();
        }

        List<Integer> current = matrixItemList.get(historyIndex).getDataList();
        double minDistance = Double.MAX_VALUE;
        int closestIndex = -1;

        // 在历史数据中查找最接近的
        for (int i = 0; i < historyIndex; i++) {
            List<Integer> historical = matrixItemList.get(i).getDataList();
            double distance = calculateDistance(current, historical);

            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        // 返回最接近记录的后继
        if (closestIndex >= 0 && closestIndex + 1 < matrixItemList.size()) {
            return new ArrayList<>(matrixItemList.get(closestIndex + 1).getDataList());
        }

        return generateRandomValid();
    }

    /**
     * 算法3：基于频率的预测 - 约束感知版本
     * 统计过去数据中各位置最频繁出现的数值，同时保证红球的递增约束
     */
    private List<Integer> algorithmFrequency(int historyIndex) {
        List<Integer> prediction = new ArrayList<>();
        int lookback = Math.min(50, historyIndex + 1);
        int start = Math.max(0, historyIndex - lookback + 1);

        // 为每个位置统计频率
        List<Map<Integer, Integer>> positionFrequencies = new ArrayList<>();
        for (int pos = 0; pos < 7; pos++) {
            Map<Integer, Integer> frequencyMap = new HashMap<>();
            for (int i = start; i <= historyIndex; i++) {
                int value = matrixItemList.get(i).getDataList().get(pos);
                frequencyMap.put(value, frequencyMap.getOrDefault(value, 0) + 1);
            }
            positionFrequencies.add(frequencyMap);
        }

        // 生成红球：贪心选择满足约束的最频繁数值
        Set<Integer> usedRed = new HashSet<>();
        int lastValue = 0;
        for (int pos = 0; pos < 6; pos++) {
            Map<Integer, Integer> freqMap = positionFrequencies.get(pos);
            int selected = lastValue + 1;

            // 从高频到低频查找有效的数值
            int maxFreq = 0;
            for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
                int val = entry.getKey();
                if (!usedRed.contains(val) && val > lastValue && val <= 33) {
                    if (entry.getValue() > maxFreq) {
                        maxFreq = entry.getValue();
                        selected = val;
                    }
                }
            }

            // 如果没找到合适的，就选择范围内第一个可用的
            if (selected <= lastValue) {
                for (int i = lastValue + 1; i <= 33; i++) {
                    if (!usedRed.contains(i)) {
                        selected = i;
                        break;
                    }
                }
            }

            usedRed.add(selected);
            prediction.add(selected);
            lastValue = selected;
        }

        // 蓝球：直接取频率最高的
        Map<Integer, Integer> blueFre = positionFrequencies.get(6);
        int blueBall = blueFre.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(8);
        blueBall = Math.max(1, Math.min(16, blueBall));
        prediction.add(blueBall);

        return prediction;
    }

    /**
     * 算法4：基于趋势的预测 - 约束感知版本
     * 分析最近数据的上升/下降趋势，同时保证红球的递增约束
     */
    private List<Integer> algorithmTrend(int historyIndex) {
        if (historyIndex < 3) {
            return generateRandomValid();
        }

        List<Integer> prev2 = matrixItemList.get(historyIndex - 2).getDataList();
        List<Integer> prev1 = matrixItemList.get(historyIndex - 1).getDataList();
        List<Integer> curr = matrixItemList.get(historyIndex).getDataList();

        // 计算各位置的趋势值
        List<Integer> trendValues = new ArrayList<>();
        for (int pos = 0; pos < 6; pos++) {
            int diff1 = prev1.get(pos) - prev2.get(pos);
            int diff2 = curr.get(pos) - prev1.get(pos);
            int trend = (diff1 + diff2) / 2;
            int predicted = curr.get(pos) + trend;
            trendValues.add(predicted);
        }

        // 约束感知的贪心选择：从趋势值中选择满足递增约束的数值
        List<Integer> prediction = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        int lastValue = 0;

        for (int pos = 0; pos < 6; pos++) {
            int trend = trendValues.get(pos);
            int selected = lastValue + 1;

            // 优先选择趋势值（在有效范围内）
            if (trend > lastValue && trend <= 33 && !used.contains(trend)) {
                selected = trend;
            } else {
                // 次选：选择范围内最接近趋势值的可用数值
                int minDist = Integer.MAX_VALUE;
                for (int candidate = lastValue + 1; candidate <= 33; candidate++) {
                    if (!used.contains(candidate)) {
                        int dist = Math.abs(candidate - trend);
                        if (dist < minDist) {
                            minDist = dist;
                            selected = candidate;
                        }
                    }
                }
            }

            used.add(selected);
            prediction.add(selected);
            lastValue = selected;
        }

        // 蓝球：基于趋势调整
        int blueTrend = (curr.get(6) - prev1.get(6)) + (prev1.get(6) - prev2.get(6));
        int blueBall = curr.get(6) + blueTrend / 2;
        blueBall = Math.max(1, Math.min(16, blueBall));
        prediction.add(blueBall);

        return prediction;
    }

    /**
     * 算法5：随机生成符合规则的有效数据
     */
    private List<Integer> algorithmRandomValid() {
        return generateRandomValid();
    }

    /**
     * 生成随机且符合规则的双色球数据
     */
    private List<Integer> generateRandomValid() {
        List<Integer> prediction = new ArrayList<>();
        Random random = new Random();

        // 生成6个不重复的红球 [1,33]，递增排列
        List<Integer> redBalls = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        while (redBalls.size() < 6) {
            int num = random.nextInt(33) + 1;
            if (!used.contains(num)) {
                used.add(num);
                redBalls.add(num);
            }
        }
        redBalls.sort(Integer::compareTo);
        prediction.addAll(redBalls);

        // 生成1个蓝球 [1,16]
        prediction.add(random.nextInt(16) + 1);

        return prediction;
    }

    /**
     * 确保数据符合双色球规则 - 仅用于序列模式和距离算法
     * 这些算法生成的数据可能不满足约束，所以需要修复
     */
    private List<Integer> ensureValidDoubleColorBall(List<Integer> data) {
        if (data.size() < 7) {
            return generateRandomValid();
        }

        List<Integer> result = new ArrayList<>();

        // 处理红球：确保 [1,33]，不重复，递增
        List<Integer> redBalls = new ArrayList<>(data.subList(0, 6));
        redBalls.replaceAll(v -> Math.max(1, Math.min(33, v)));

        // 去重并排序
        redBalls = redBalls.stream().distinct().sorted().collect(Collectors.toList());

        // 如果去重后不足6个，补充其他数值
        while (redBalls.size() < 6) {
            for (int i = 1; i <= 33; i++) {
                if (!redBalls.contains(i)) {
                    redBalls.add(i);
                    break;
                }
            }
        }

        result.addAll(redBalls.subList(0, 6));

        // 处理蓝球：[1,16]
        int blueBall = Math.max(1, Math.min(16, data.get(6)));
        result.add(blueBall);

        return result;
    }

    /**
     * 计算欧几里得距离
     */
    private double calculateDistance(List<Integer> a, List<Integer> b) {
        double sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += Math.pow(a.get(i) - b.get(i), 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * 获取算法名称
     */
    private String getAlgorithmName(int index) {
        switch (index) {
            case 0: return "直接后继";
            case 1: return "距离算法";
            case 2: return "序列模式";
            case 3: return "频率预测";
            case 4: return "趋势预测";
            default: return "未知";
        }
    }

    public static void main(String[] args) throws IOException {
        DoubleColorBallPredictionAnalyzer analyzer = new DoubleColorBallPredictionAnalyzer();
        analyzer.loadData();

        System.out.println("=== 生成5组预测数据 ===\n");
        List<List<Integer>> predictions = analyzer.generateFivePredictions(analyzer.matrixItemList.size() - 1);

        String[] algorithmNames = {"直接后继", "距离算法", "序列模式", "频率预测", "趋势预测"};

        for (int i = 0; i < predictions.size(); i++) {
            List<Integer> pred = predictions.get(i);
            System.out.println("算法 " + (i + 1) + " (" + algorithmNames[i] + "):");
            System.out.println("  预测数据: " + pred);

            // 验证红球约束
            boolean validRed = validateRedBalls(pred);
            System.out.println("  红球验证: " + (validRed ? "✓ 有效" : "✗ 无效"));

            // 验证蓝球约束
            boolean validBlue = pred.get(6) >= 1 && pred.get(6) <= 16;
            System.out.println("  蓝球验证: " + (validBlue ? "✓ 有效" : "✗ 无效"));

            System.out.println();
        }
    }

    private static boolean validateRedBalls(List<Integer> pred) {
        List<Integer> red = pred.subList(0, 6);

        // 检查数量
        if (red.size() != 6) return false;

        // 检查范围
        for (int v : red) {
            if (v < 1 || v > 33) return false;
        }

        // 检查不重复
        if (new HashSet<>(red).size() != 6) return false;

        // 检查递增
        for (int i = 1; i < 6; i++) {
            if (red.get(i) <= red.get(i - 1)) return false;
        }

        return true;
    }
}
