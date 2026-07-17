package cqwang.java.data.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarDeserializer extends JsonDeserializer<Calendar> {
    public static final int TIME_IN_MILLIS_LENGTH = 12;
    private static final Pattern PATTERN = Pattern.compile("[\\+]?\\d+");

    private static final Cache<String, DateFormat> DATE_FORMAT_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(1)
            .maximumSize(10)
            .concurrencyLevel(Runtime.getRuntime().availableProcessors()).build();

    static {
        register("yyyy-MM-dd HH:mm:ss");
        register("yyyy-MM-dd");
    }

    public static void register(String format) {
        if (StringUtils.isEmpty(format)) {
            return;
        }
        DateFormat dateFormat = new SimpleDateFormat(format);
        if (Objects.isNull(dateFormat)) {
            return;
        }
        DATE_FORMAT_CACHE.put(format, dateFormat);
    }

    @Override
    public Calendar deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = p.getCodec().readTree(p);
        if (Objects.isNull(jsonNode)) {
            return null;
        }

        String json = jsonNode.asText();
        Calendar calendar = tryParseTimestamp(json);
        if (Objects.nonNull(calendar)) {
            return calendar;
        }

        return tryParseFormat(json);
    }

    private Calendar tryParseTimestamp(String json) {
        Calendar calendar = null;
        Matcher matcher = PATTERN.matcher(json);
        List<String> matchers = getMatcherResult(matcher);
        if (matchers.size() >= 1 && matchers.get(0).length() >= TIME_IN_MILLIS_LENGTH) {
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.valueOf(matchers.get(0)));
            if (matchers.size() == 2) {
                ZoneId zoneId = ZoneOffset.of(matchers.get(1)).normalized();
                calendar.setTimeZone(TimeZone.getTimeZone(zoneId));
            }
        }
        return calendar;
    }

    private List<String> getMatcherResult(Matcher matcher){
        List<String> matchers = new ArrayList<>();
        while (matcher.find()){
            matchers.add(matcher.group());
        }
        return matchers;
    }


    private Calendar tryParseFormat(String json) {
        if (DATE_FORMAT_CACHE.size() == 0) {
            return null;
        }

        Date date = tryParseDate(json);
        if (Objects.isNull(date)) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    private Date tryParseDate(String json) {
        ConcurrentMap<String, DateFormat> map = DATE_FORMAT_CACHE.asMap();
        for (Map.Entry<String, DateFormat> entry : map.entrySet()) {
            try {
                Date date = entry.getValue().parse(json);
                return date;
            } catch (ParseException e) {
            }
        }
        return null;
    }
}
