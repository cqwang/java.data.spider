package cqwang.java.data.spider.doubleball;

import com.fasterxml.jackson.core.type.TypeReference;
import cqwang.java.data.serialize.FileProvider;
import cqwang.java.data.serialize.JSON;
import cqwang.java.data.spider.doubleball.model.DoubleColorBallItem;
import cqwang.java.data.spider.doubleball.model.DoubleColorBallMatrixItem;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.util.ArrayList;
import java.util.List;

public class DoubleColorBallDataPreparePipeline implements Pipeline {
    @Override
    public void process(ResultItems resultItems, Task task) {
        List<DoubleColorBallItem> allData = FileProvider.readFile("DoubleColorBallData.json", new TypeReference<List<DoubleColorBallItem>>() {
        });

        // 逆序转正序
        List<DoubleColorBallMatrixItem> matrixItemList = new ArrayList<>(allData.size());
        for (int i = allData.size() - 1; i >= 0; i--) {
            DoubleColorBallItem current = allData.get(i);

            DoubleColorBallMatrixItem matrixItem = new DoubleColorBallMatrixItem();
            matrixItem.getDataList().addAll(current.getRedValueList());
            matrixItem.getDataList().add(current.getBlueValue());
            matrixItemList.add(matrixItem);
        }

        // 序列化，落地文件
        String json = JSON.toJSONString(matrixItemList);
//        matrixItemList = JSON.parseObject("DoubleColorBallMatrixData.json", new TypeReference<List<DoubleColorBallMatrixItem>>() {
//        });
        System.out.println();
    }
}
