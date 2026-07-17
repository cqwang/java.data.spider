package cqwang.java.data.spider.doubleball;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;

public class DoubleColorBallDataService {
    public void execute(){
        Request request = new Request();
        request.setMethod("GET");
        request.setUrl("https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=&issueStart=&issueEnd=&dayStart=&dayEnd=&pageNo=1&pageSize=30&week=&systemType=PC");
        Spider.create(new DoubleColorBallProcessor())
                .addRequest(request)
                .addPipeline(new DoubleColorBallPipeline())
                .thread(1)
                .start();
    }
}
