package cqwang.java.data.spider.doubleball;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;

import java.text.MessageFormat;

public class DoubleColorBallDataBatchService {
    public void execute() {
        String urlPattern = "https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=&issueStart=&issueEnd=&dayStart=&dayEnd=&pageNo={0}&pageSize={1}&week=&systemType=PC";
        int pageSize = 30;
        int pageNo = 1;
        while (true) {
            String url = MessageFormat.format(urlPattern, pageNo, pageSize);
            Request request = new Request();
            request.setMethod("GET");
            request.setUrl(url);
            Spider.create(new DoubleColorBallProcessor())
                    .addRequest(request)
                    .addPipeline(new DoubleColorBallPipeline())
                    .thread(1)
                    .start();

            pageNo++;
            if (pageNo > 70) {
                break;
            }

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        DoubleColorBallContext.write();
    }
}
