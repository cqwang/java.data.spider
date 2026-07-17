package cqwang.java.data.serialize;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileProvider {
    public static <T> T readFile(String file,Class<T> clazz) {
        String json = readFile(file);
        return JSON.parseObject(json, clazz);
    }

    public static <T> T readFile(String file, TypeReference<T> typeReference) {
        String json = readFile(file);
        return JSON.parseObject(json, typeReference);
    }

    public static String readFile(String file){
        InputStream resourceAsStream = FileProvider.class.getClassLoader().getResourceAsStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(resourceAsStream));
        StringBuffer buffer = new StringBuffer();
        String line = "";
        try {
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                in.close();
                resourceAsStream.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        return buffer.toString();
    }
}
