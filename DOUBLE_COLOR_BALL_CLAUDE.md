## 随机数生成预测规则分析

### 获取有序数据列表：
1.通过反序列化获取所有有序数据，List<DoubleColorBallMatrixItem> matrixItemList = JSON.parseObject("/DoubleColorBallMatrixData.json", new TypeReference<List<DoubleColorBallMatrixItem>>() {});

2.输出matrixItemList是否有数据，以及元素个数。

3.matrixItemList中第一个DoubleColorBallMatrixItem时间最旧，最后一个DoubleColorBallMatrixItem时间最新，预测算法要以次时间顺序为准。


### 数值规则约束
对列表中每个元素DoubleColorBallMatrixItem，这里解释其dataList中的数值规则
1.包含7个数值

2.每个数值都是整数

3.前6个数值的区间都是[1,33]，数值不会重复，数值只能按照位序变大。

4.第7个元素的数值区间是[1,16]


### 算法匹配成功的约束
1.算法一次预测5组数据，每组数据包含7个数值。

2.每一组数据，尽可能匹配dataList中的7个数值，具体为：要求至少有5个位序相同的数值相同

3.要求任意一组的7个数值，满足“数值规则约束”。如果不满足，则优化算法。

### 预测分析算法的设计思路：
遍历matrixItemList，对每个数据项，分析dataList中的7个数值的生成方案，有几种方向：

1.分析matrixItemList中的数据特征，包括数据分布、概率分布、最近距离算法、随机算法等。
2.不要基于前序数据来预测后续数据，而应该关注于数据生成规则和概率。
3.如果满足“算法匹配成功的约束”则输出算法说明。

### 关于算法的补充验证
1.要求所有算法的生成结果，必需满足“数值规则约束”和“算法匹配成功的约束”。如果不满足，则继续优化算法
