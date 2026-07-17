package cqwang.java.data.spider.doubleball;

import cqwang.java.data.serialize.JSON;
import cqwang.java.data.spider.doubleball.model.DoubleColorBallResponse;
import org.apache.commons.collections4.CollectionUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

public class DoubleColorBallProcessor implements PageProcessor {
    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setTimeOut(10000);


    @Override
    public void process(Page page) {
        String text = page.getRawText();
        DoubleColorBallResponse response = JSON.parseObject(text, DoubleColorBallResponse.class);
        if(response == null || CollectionUtils.isEmpty(response.getResult())){
            return;
        }
        page.putField("result", response.getResult());
    }

    @Override
    public Site getSite() {
        return site;
    }
}
