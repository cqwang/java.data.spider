package cqwang.java.data.spider.doubleball;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import cqwang.java.data.serialize.JSON;
import cqwang.java.data.spider.doubleball.model.DoubleColorBallItem;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DoubleColorBallPipeline implements Pipeline {



    @Override
    public void process(ResultItems resultItems, Task task) {
        List<DoubleColorBallItem> dataList = resultItems.get("result");
        for(DoubleColorBallItem data : dataList){
            data.formatData();
        }

        DoubleColorBallContext.allData.addAll(dataList);
    }
}
