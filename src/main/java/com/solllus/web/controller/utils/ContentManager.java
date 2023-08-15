package com.solllus.web.controller.utils;

import com.boosteel.util.explorer.IOManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class ContentManager {

    static List<String> EMPTY_LIST = new ArrayList<>();

    // 실제 파일주소
    public String[] paths;

    // Content를 찾아가기 위한 Key값
    public String root;
    public String path;
    public String name;

    public String user;
    public String body;
    public Object data;

    public String index;    // 디스플레이 넘버

    // 임시파일 삭제용 프로퍼티
    public String prefix;
    public String safe;
    public List<String> safeFiles = EMPTY_LIST;

    public ContentManager() {
    }

    public Path resolve(Path root) {
        return root.resolve(String.join("/", paths));
    }

    public String path() {
        return path("/");
    }
    public String path(String split) {
        return String.join(split, root, path, name);
    }

    public int delete(Path root) throws IOException {
        return IOManager.deleteAll(resolve(root)).size();
    }

    public long copy(Path root, InputStream io, String filename) throws IOException {
        Path rootPath = resolve(root);
        if (!Files.exists(rootPath)) Files.createDirectories(rootPath);
        return Files.copy(io, rootPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    }

    public void createDirectories(Path root) throws IOException {
        Files.createDirectories(resolve(root));
    }

    public List<Object[]> directories(Path root) throws IOException {
        List<Object[]> list = new ArrayList<>();
        tour(resolve(root), (isDirectory, file, type, attrs) -> {
            if (Files.isDirectory(file)) {
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                list.add(new Object[]{file.getFileName().toString(), attributes.creationTime().toMillis()});
            }
        });
        return list;
    }

    public List<Object[]> files(Path root) throws IOException {
        List<Object[]> list = new ArrayList<>();
        tour(resolve(root), ((isDirectory, file, type, attrs) -> {
            list.add(new Object[]{
                    type,
                    file.getFileName().toString(),
                    attrs.creationTime().toMillis(),
                    attrs.lastModifiedTime().toMillis(),
                    Files.size(file)});
        }));
        return list;
    }

    public Object[] removeTempFiles(Path root) throws IOException {
        int success = 0, fail = 0;
        root = resolve(root);
        if (Files.exists(root)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path p : stream) {
                    String filename = p.getFileName().toString();
                    if (!safeFiles.contains(filename) && filename.startsWith(prefix) && !Files.isDirectory(p)) {
                        try {
                            Files.delete(p);
                            success++;
                        } catch (Exception e) {
                            fail++;
                        }
                    }
                }
            }
        }
        return new Object[]{success, fail};
    }

    public static void tour(Path target, TOUR tour) throws IOException {
        if (Files.exists(target) && Files.isDirectory(target)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
                for (Path file : stream) {
                    tour.accept(Files.isDirectory(file), file, Files.probeContentType(file), Files.readAttributes(file, BasicFileAttributes.class));
                }
            }
        }
    }


    public interface TOUR {
        void accept(boolean isDirectory, Path path, String mediaType, BasicFileAttributes attributes) throws IOException;
    }
}
