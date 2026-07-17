package cqwang.java.data.spider.doubleball.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DoubleColorBallMatrixItem {
    private List<Integer> dataList;

    public DoubleColorBallMatrixItem(){
        this.dataList = new ArrayList<>(7); // 六个红色球，1个蓝色球
    }
}
