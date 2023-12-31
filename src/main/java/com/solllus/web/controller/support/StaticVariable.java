package com.solllus.web.controller.support;

import com.solllus.web.controller.domain.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StaticVariable {


    public static Path ROOT;
    public static Path USERS_DIR;

    public static final Map<String, DisplayKey> DISPLAY_KEYS = new HashMap<>();
    public static final Map<String, ContentMap_users> USERS = new HashMap<>();
    public static Map<String, ContentMap_apps> APPS;

    public static final Map<String, Display> EMPTY_DISPLAY_MAP = new HashMap<>();
    public static final Map<String, Object> EMPTY_MAP = new HashMap<>();
    public static final Object[] EMPTY_ARRAY = new Object[0];


    public static final Set<Content> APP_ALL() {
        Map<String, ContentMap_apps> map = PROVIDER("apps");
        Set<Content> contents = new HashSet<>();
        for (ContentMap_apps t : map.values()) contents.addAll(t.contents.values());
        return contents;
    }


    public static final <T extends ContentMap> Map<String, T> PROVIDER(String root) {
        return root.equals("apps") ? (Map<String, T>) APPS : (Map<String, T>) USERS;
    }

    public static final <T extends ContentMap> T PROVIDER(String root, String path) {
        Map<String, T> map = PROVIDER(root);
        T contentMap = map.get(path);

        // apps의 경우 새로운 카테고리가 생긴 경우 map을 생성한다.
        if(contentMap == null) {
            Path rootPath = ROOT.resolve(root).resolve(path);
            if(Files.exists(rootPath) && root.equals("apps")) {
                ContentMap_apps app = new ContentMap_apps(rootPath, root, path);
                app.readAll();
                APPS.put(path, app);
                return (T) app;
            }
        }

        return contentMap;
    }


}
