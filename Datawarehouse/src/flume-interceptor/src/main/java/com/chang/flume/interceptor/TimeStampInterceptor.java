package com.chang.flume.interceptor;

import com.alibaba.fastjson.JSONObject;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TimeStampInterceptor implements Interceptor {
    private ArrayList<Event> events = new ArrayList<>();
    @Override
    public void initialize() {

    }

    @Override
    // 从body中获得timestamp，放到header中
    //      body中的timestamp是日志的产生时间，header中的ts是接收到消息的时间
    //      我们使用的是原始日志的产生时间
    public Event intercept(Event event) {
        Map<String, String> headers = event.getHeaders();
        String log = new String(event.getBody(), StandardCharsets.UTF_8);

        // 将json转化为json对象，找到对象中的属性ts值
        JSONObject jsonObject = JSONObject.parseObject(log);
        String ts = jsonObject.getString("ts");
        headers.put("timestamp",ts);
        return event;
    }

    @Override
    public List<Event> intercept(List<Event> list) {
        events.clear();
        for (Event event : list) {
            events.add(intercept(event));
        }

        return events;
    }

    @Override
    public void close() {

    }

    public static class Builder implements Interceptor.Builder{

        @Override
        public Interceptor build() {

            return  new TimeStampInterceptor();
        }

        @Override
        public void configure(Context context) {

        }
    }
}
