package simplerag.utils;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;
import java.util.List;

public class StringOrListDeserializer implements ObjectReader<List<String>> {
    @Override
    public List<String> readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        if (jsonReader.isString()) {
            return List.of(jsonReader.readString());
        } else if (jsonReader.isArray()) {
            //noinspection unchecked
            return jsonReader.readArray(String.class);
        }
        throw new JSONException("Invalid tags format");
    }

}
