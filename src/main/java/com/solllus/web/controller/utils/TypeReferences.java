package com.solllus.web.controller.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.solllus.web.controller.domain.Display;
import com.solllus.web.security.UserDetail;

import java.util.List;
import java.util.Map;

public class TypeReferences {

    public static final TypeReference<Map<String, String>> map_str_str = new TypeReference<Map<String, String>>() {
    };
    public static final TypeReference<Map<String, Object>> map_str_obj = new TypeReference<Map<String, Object>>() {
    };
    public static final TypeReference<List<Display>> list_display = new TypeReference<List<Display>>() {
    };
    public static final TypeReference<List<String>> list_string = new TypeReference<List<String>>() {
    };
    public static final TypeReference<UserDetail> userDetail = new TypeReference<UserDetail>() {
    };
}
