package com.solllus.web.controller.domain;

import com.boosteel.util.explorer.IOManager;
import com.solllus.web.security.UserDetail;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.solllus.web.controller.support.StaticVariable.*;

/*
 *  리소스(이미지, 동영상)의 파일명이 Primary Key가 된다.
 *  ※다시 말해 파일명은 절대 중복될 수 없다.
 */
public class ContentMap_users extends ContentMap {


    public UserDetail userDetail;
    public Map<String, Display> displayMap;  // 디스플레이 정보

    // root : "users"  /  name : "hancomee"
    public ContentMap_users(Path rootPath, String root, String name) {
        super(rootPath, root, name);
    }

    public ContentMap_users setUserDetail(UserDetail userDetail) {
        this.userDetail = userDetail.setUsername(name);
        return this;
    }

    public Display getDisplay(String displayNum) {
        return displayMap.get(displayNum);
    }

    public ContentMap_users addDisplay(Display display) throws IOException {
        removeDisplay(display.index);
        displayMap.put(display.index, display);
        return this;
    }

    // @::2023-08-09
    public ContentMap_users removeDisplay(String index) throws IOException {
        Path appData = USERS_DIR.resolve(this.name).resolve("_AppData");

        if(Files.exists(appData)) {
            List<Path>
                    files = new ArrayList<>(),
                    dirs = new ArrayList<>();

            // _AppData에 있는 디스플레이 파일/폴더를 모두 삭제한다.
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(appData)) {
                for (Path dir : stream) {
                    try (DirectoryStream<Path> stream1 = Files.newDirectoryStream(dir)) {
                        for (Path file : stream1) {
                            if (Files.isDirectory(file)) {
                                if (file.getFileName().toString().equals(index)) dirs.add(file);
                            } else {
                                String filename = file.getFileName().toString();
                                filename = filename.substring(0, filename.lastIndexOf("."));
                                if (filename.equals(index)) {
                                    files.add(file);
                                }
                            }
                        }
                    }
                }
            }

            for (Path file : files)
                Files.delete(file);

            for (Path file : dirs)
                IOManager.deleteAll(file);
        }

        displayMap.remove(index);

        return this;
    }



    // 모든 컨텐츠 반환 :: 유저 어드민 페이지에서 활용
    // [유저앱 전체리스트, 유저컨텐츠 리스트, 등록된 앱 리스트]
    /*public Object[] contents(String index) {
        // 앱 정보는 따로 반환
        List<Content> appList = new ArrayList<>();
        for (String appName : apps) {
            String[] values = appName.split("/");
            ContentMap map = APPS.get(values[0]);
            if (map != null && map.contents.containsKey(values[1])) {
                appList.add(map.contents.get(values[1]));
            }
        }
        return new Object[]{contents.values(), appList};
    }

    // 컨텐츠 반환
    public Content getContent(String index, String root, String path, String name) {
        ContentMap contentMap = PROVIDER(root, path);
        if (contentMap != null) {
            Content content = contentMap.contents.get(name);
            if (content != null) {
                // 어플인 경우
                if (content.mediaType.contains("html")) {
                    Set<String> list = apps.get(index);
                    if (list != null && list.contains(content.root + "/" + content.path + "/" + content.name))
                        return content;
                } else
                    return content;
            }
        }
        return null;
    }*/

}
