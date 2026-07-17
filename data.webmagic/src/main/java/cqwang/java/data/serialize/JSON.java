package cqwang.java.data.serialize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

public class JSON {
    public static String toJSONString(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }

        ObjectMapper objectMapper = getObjectMapper();
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Throwable throwable) {
            System.out.println(throwable);
            return null;
        }
    }

    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        ObjectMapper objectMapper = getIbuObjectMapper();
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (Throwable throwable) {
            System.out.println(throwable);
            return null;
        }
    }

    public static <T> T parseObject(String jsonString, TypeReference<T> typeReference) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        ObjectMapper objectMapper = getIbuObjectMapper();
        try {
            return objectMapper.readValue(jsonString, typeReference);
        } catch (Throwable throwable) {
            System.out.println(throwable);
            return null;
        }
    }


    public static ObjectMapper getIbuObjectMapper() {
        ObjectMapper objectMapper = getObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(Calendar.class, new CalendarDeserializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.setTimeZone(TimeZone.getDefault());
        return objectMapper;
    }
}
