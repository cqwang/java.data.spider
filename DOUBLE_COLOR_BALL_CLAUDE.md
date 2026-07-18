## 随机数生成预测规则分析

### 获取有序数据列表：
1.通过反序列化获取所有有序数据，List<DoubleColorBallMatrixItem> matrixItemList = JSON.parseObject("/DoubleColorBallMatrixData.json", new TypeReference<List<DoubleColorBallMatrixItem>>() {});

2.输出matrixItemList是否有数据，以及元素个数。

3.matrixItemList中第一个DoubleColorBallMatrixItem时间最旧，最后一个DoubleColorBallMatrixItem时间最新，预测算法要以次时间顺序为准。


### 对列表中每个元素DoubleColorBallMatrixItem，这里解释其dataList中的数值规则：
1.包含7个数值

2.每个数值都是整数

3.前6个数值的区间都是[1,33]，数值不会重复，数值只能按照位序变大。

4.第7个元素的数值区间是[1,16]


### 预测分析算法 如何判断匹配成功
1.算法一次预测5组数据，每组数据包含7个数值。

2.任意一组的7个数值，能完全匹配dataList中的7个数值，则认为算法预测成功。

### 预测分析算法的设计思路：
遍历matrixItemList，对每个数据项，分析dataList中的7个数值，有几种方向：

1.尝试基于matrixItemList中的前序数据，预测后续数据，如果算法匹配成功则输出算法说明。

2.尝试基于最近距离算法，给出预测算法，如果算法匹配成功则输出算法说明。

3.尝试基于随机算法，如果算法匹配成功则输出算法说明。

