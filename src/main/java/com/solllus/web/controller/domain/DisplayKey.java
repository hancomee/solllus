package com.solllus.web.controller.domain;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DisplayKey {
    public long createTime;
    public String name;
    public String index;
    public String nickname = "";
    public String value;


    public DisplayKey createTime() {
        this.createTime = new Date().getTime();
        return this;
    }

    public String datetime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createTime);
    }

    public DisplayKey extend(DisplayKey other) {
        name = other.name;
        index = other.index;
        nickname = other.nickname;
        value = other.value;
        return this;
    }

    public boolean equals(String name, String index) {
        return index.equals(this.index) && name.equals(this.name);
    }

    public boolean equals(DisplayKey key) {
        return value.equals(key.value) && index.equals(key.index) && name.equals(key.name);
    }

    @Override
    public String toString() {
        return value + ":" + name + "/" + index;
    }

}
