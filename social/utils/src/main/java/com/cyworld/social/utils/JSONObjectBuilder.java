package com.cyworld.social.utils;


import com.alibaba.fastjson.JSONObject;

public class JSONObjectBuilder {
    private JSONObject obj = new JSONObject();

    public static JSONObjectBuilder new_builder() {
        return new JSONObjectBuilder();
    }

    public JSONObjectBuilder put(String key, Object value) {
        obj.put(key, value);
        return this;
    }

    public JSONObject build() {
        return obj;
    }
}
