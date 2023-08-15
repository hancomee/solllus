package com.solllus.web.controller.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class Content {

    private Path parentPath;

    // root/path/filename
    public String root;
    public String path;
    public String name;
    public long time;           // lastModifiedTime :: 이 값을 기준으로 브라우저에서 새로고침을 판단한다.
    public long createTime;
    public String mediaType;

    public boolean hasAdmin;
    public String meta = "";        // 그밖의 데이터


    Content(Path parentPath, String root) {
        this.parentPath = parentPath;
        this.root = root;
        this.path = parentPath.getFileName().toString();
    }

    Content set(String name, long time, String mediaType) {
        this.name = name;
        this.time = time;
        this.mediaType = mediaType.replaceAll("jpeg", "jpg");
        return this;
    }

    Content setCreateTime(long time) {
        this.createTime = time;
        return this;
    }

    Content setHasAdmin(boolean hasAdmin) {
        this.hasAdmin = hasAdmin;
        return this;
    }

    public static void register(Path contentPath, Map<String, Content> list) throws IOException {
        String filename = contentPath.getFileName().toString();

        // 앱(폴더)이든 미디어(파일)든 없으면 리스트에서 삭제한다.
        if (!Files.exists(contentPath)) {
            list.remove(filename);
            return;
        }

        Path parentPath = contentPath.getParent(),
                rootPath = parentPath.getParent(),
                path;

        String root = rootPath.getFileName().toString();
        Content content = list.get(filename);
        BasicFileAttributes attrs = Files.readAttributes(contentPath, BasicFileAttributes.class);

        if (content == null) {
            content = new Content(parentPath, root)
                    .setCreateTime(attrs.creationTime().toMillis());
        }

        if (Files.isDirectory(contentPath)) {
            // index.html이 존재하는 폴더만 대상으로 한다.
            if (!filename.startsWith("$") && !filename.startsWith("_") && Files.exists(contentPath.resolve("index.html"))) {
                content.set(filename, attrs.lastModifiedTime().toMillis(), "text/html");
                content.setHasAdmin(Files.exists(contentPath.resolve("admin.html")));
                // 앱 메타 파일
                if (Files.exists(path = contentPath.resolve("$meta.txt")))
                    content.meta = String.join("\n", Files.readAllLines(path));
                list.put(content.name, content);
            } else list.remove(filename);
        }
        // 일반 미디어파일
        else {
            String mediaType = Files.probeContentType(contentPath);
            if (mediaType != null) {
                content.set(contentPath.getFileName().toString(), attrs.lastModifiedTime().toMillis(), mediaType);
                list.put(content.name, content);
            }
        }

    }

    private String key() {
        return path + "/" + name;
    }

    @Override
    public int hashCode() {
        return key().hashCode();
    }

    @Override
    public boolean equals(Object target) {
        return key().equals(((Content) target).key());
    }

    @Override
    public String toString() {
        return "[" + mediaType + "] " + name + "\n";
    }
}
