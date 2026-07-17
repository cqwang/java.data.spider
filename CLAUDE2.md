## 随机数生成预测规则分析

### 获取有序数据列表：
1.通过反序列化获取所有有序数据，List<DoubleColorBallMatrixItem> matrixItemList = JSON.parseObject("/DoubleColorBallMatrixData.json", new TypeReference<List<DoubleColorBallMatrixItem>>() {});
2.输出matrixItemList是否有数据，以及元素个数。
3.matrixItemList中第一个DoubleColorBallMatrixItem时间最旧，最后一个DoubleColorBallMatrixItem时间最新，预测算法要以次时间顺序为准。

### 对列表中每个元素DoubleColorBallMatrixItem字段dataList的解释：
dataList有7个元素，每个元素都是整数。

### 预测算法 匹配成功说明
1.算法一次预测5组数据，每组数据包含7个数值。
2.任意一组的7个数值，能完全匹配dataList中的7个数值，则认为算法预测成功。

### 随机数分析思路：
遍历matrixItemList，对每个数据项，分析dataList中的7个数值，有几种方向：
1.尝试基于matrixItemList中的前序数据，预测后续数据，如果预测算法匹配成功则输出算法说明。
2.尝试基于最近距离算法，给出预测算法，如果预测算法匹配成功则输出算法说明。
3.尝试基于随机算法，如果预测算法匹配成功则输出算法说明。
