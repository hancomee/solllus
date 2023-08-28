package com.solllus.web.controller.domain;

import com.boosteel.util.explorer.IOManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.solllus.web.controller.support.StaticVariable.*;


/*
 * Display는 users/name/_AppData/{...}에 해당한다.
 * 이 객체(폴더)는 오직 app에 대한 데이터만 가진다.
 */
public class Display {

    // JSON변환 프로퍼티
    Path PATH;

    public String user;
    public String index;

    // 현재 디스플레이에 송출되어야 하는 컨텐츠 정보 :: root/path/name
    public String root;
    public String path;
    public String name;

    public String text;

    public int command;
    public long refreshTime;        // 새로고침 명령 :: 브라우저가 저장하고 있는 serverTime과 비교해 새로고침을 실행한다.
    public long serverTime;         // 브라우저에게 데이터 전송시 타임 (비교목적)

    public long updateTime;         // 변경시간
    public Map<Long, Object> request = new LinkedHashMap<>();       // 시간과 함께 명령어를 전달한다.

    public Set<String> contents = new HashSet<>();

    public Display(String index, String user) {
        this.user = user;
        this.index = index;
        PATH = USERS_DIR.resolve(user).resolve("_AppData").resolve(index);
        serverTime = new Date().getTime();
    }

    public String jsonText() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("root", root);
        obj.put("path", path);
        obj.put("name", name);
        obj.put("text", text);
        return obj.toString();
    }

    public Display addRequest(String request, long serverTime) {
        for (Long time : this.request.keySet())
            if (serverTime > time) this.request.remove(time);

        this.request.put(serverTime, request);
        this.updateTime = serverTime;
        return this;
    }


    /* ****************** ▼ 컨텐츠 관리 ▼ ****************** */
    public void clearData(String root, String path, String name) {
        String filename = String.join("_", root, path, name);
        try {
            Path file = PATH.resolve(filename + ".txt");
            if (Files.exists(file)) Files.delete(file);
            file = PATH.resolve(filename);
            if (Files.exists(file)) IOManager.deleteAll(file);
        } catch (Exception e) {
        }
    }
    /* ****************** ▲ 컨텐츠 관리 ▲ ****************** */

    public Display setContent(String root, String path, String name) {
        this.root = root;
        this.path = path;
        this.name = name;
        return this.update();
    }

    public Display setServerTime(long serverTime) {
        this.serverTime = serverTime;
        return this;
    }

    public Display setCommand(int command) {
        this.command = command;
        return this;
    }

    public boolean checkContent(String root, String path, String name) {
        return this.root.equals(root) && this.path.equals(path) && this.name.equals(name);
    }

    public Content getContent() {
        if(root != null && contents.contains(String.join("/", root, path, name))) {
            ContentMap contentMap = PROVIDER(root, path);
            if (contentMap != null) {
                Content content = contentMap.contents.get(name);
                if (content != null) {
                    return content;
                }
            }
        }
        return null;
    }

    public Display setText(String text) {
        this.text = text;
        return this.update();
    }

    public Display update() {
        updateTime = new Date().getTime();
        return this;
    }


    @Override
    public String toString() {
        return "[" + index + "] " + path + "/" + name;
    }

}
