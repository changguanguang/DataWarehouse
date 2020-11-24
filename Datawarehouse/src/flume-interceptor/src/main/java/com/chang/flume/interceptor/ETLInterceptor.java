package com.chang.flume.interceptor;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

public class ETLInterceptor implements Interceptor {

    @Override
    public void initialize() {

    }

    /**
     *  清除 格式不对的数据 （不是合法json字符串的数据 || 损坏的数据）
     * @param event flume管道中的数据
     * @return
     */
    @Override
    public Event intercept(Event event) {
        byte[] body = event.getBody();
        String log = new String(body, StandardCharsets.UTF_8);

        if(JSONUtils.isJSONValidate(log)){
            return event;
        }else{
            return null;
        }
    }


    @Override
    public List<Event> intercept(List<Event> list) {

        Iterator<Event> iterator = list.iterator();

        while(iterator.hasNext()){
            Event event = iterator.next();
            Event bol = intercept(event);
            if(bol == null ){
                iterator.remove();
            }
        }

        return list;
    }

    @Override
    public void close() {

    }
    public static class Builder implements Interceptor.Builder{
        @Override
        public Interceptor build() {
            return new ETLInterceptor();
        }

        @Override
        public void configure(Context context) {

        }
    }
}
