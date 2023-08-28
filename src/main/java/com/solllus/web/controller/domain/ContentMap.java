package com.solllus.web.controller.domain;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ContentMap {

    public Path rootPath;
    public String root;
    public String name;
    public Map<String, Content> contents;               // 미디어, html 파일 정보


    public ContentMap(Path rootPath, String root, String name) {
        this.rootPath = rootPath;
        this.root = root;
        this.name = name;
    }

    public Collection<Content> values() {
        return contents.values();
    }

    // 폴더 읽어오기
    public <T extends ContentMap> T readAll() {
        Map<String, Content> list = new HashMap<>();
        if (Files.exists(rootPath))
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
                for (Path f : stream) Content.register(f, list);
            } catch (IOException e) {

            }
        contents = list;
        return (T) this;
    }



    @Override
    public String toString() {
        return getClass() + " / " + root + "/" + name;
    }
}
