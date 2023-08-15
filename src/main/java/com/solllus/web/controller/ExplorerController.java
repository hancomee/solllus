package com.solllus.web.controller;


import com.boosteel.util.explorer.DirInfo;
import com.boosteel.util.explorer.IOManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("explorer")
public class ExplorerController {

    @RequestMapping()
    public String html() {
        return "src/explorer.html";
    }

    public static class RequestDirConfig {
        Path root;
        DirInfo dirInfo;
        public String path;
        public boolean files;
        public boolean directories;
        public boolean sub;
        public String body;


        boolean isOK() {
            try {
                Path root = Paths.get(path + "/");
                if (Files.exists(root) && Files.isDirectory(root)) {
                    dirInfo = new DirInfo(this.root = root);
                    return true;
                }
            } catch (Exception e) {
            }
            return false;
        }
    }


    @RequestMapping(value = "log")
    @ResponseBody
    public Object log(@RequestParam("src") String src) throws IOException {
        return "<pre>" + String.join("\n", Files.readAllLines(Paths.get(src))) + "</pre>";
    }


    @RequestMapping(value = "read", method = RequestMethod.POST)
    @ResponseBody
    public Object read(@RequestBody RequestDirConfig config) throws IOException {
        DirInfo result = null;

        if (config.isOK()) {
            result = config.dirInfo;
            if (config.sub) result.checkSub();
            if (config.files) result.readFiles();
            if (config.directories) result.readDir();
        }
        return new Object[]{result};
    }

    @RequestMapping(value = "reader", method = RequestMethod.POST)
    @ResponseBody
    public String reader(@RequestBody RequestDirConfig config) throws IOException {
        Path path = Paths.get(config.path);
        if (Files.exists(path)) {
            return String.join("\n", Files.readAllLines(path));
        }
        return "";
    }

    @RequestMapping(value = "writer", method = RequestMethod.POST)
    @ResponseBody
    public void writer(@RequestBody RequestDirConfig config) throws IOException {
        Path path = Paths.get(config.path);
        Files.write(path, Arrays.asList(config.body), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @RequestMapping(value = "create", method = RequestMethod.POST)
    @ResponseBody
    public int create(@RequestBody RequestDirConfig config) throws Exception {
        try {
            Path target = Paths.get(config.path);
            if (!Files.exists(target)) {
                Files.createDirectory(Paths.get(config.path));
                return 1;
            }
        } catch (IOException e) {
        }
        return 0;
    }

    // 파일 업로드
    @RequestMapping(value = "upload", method = RequestMethod.PUT)
    @ResponseBody
    public int upload(@RequestPart("config") RequestDirConfig config, @RequestPart("files") List<MultipartFile> files) throws Exception {
        int count = 0;
        if (config.isOK()) {
            for (MultipartFile file : files) {
                if (config.dirInfo.copy(file.getInputStream(), file.getOriginalFilename()) > 0) count++;
            }
        }
        return count;
    }

    @RequestMapping(value = "exists", method = RequestMethod.POST)
    @ResponseBody
    public boolean exists(@RequestBody RequestDirConfig config) throws Exception {
        return Files.exists(Paths.get(config.path));
    }

    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @ResponseBody
    public Object delete(@RequestBody RequestDirConfig config) throws Exception {
        try {
            return IOManager.deleteAll(Paths.get(config.path));
        } catch (IOException e) {
            return 0;
        }
    }

    public static final <T> T out(T obj) {
        System.out.println(obj);
        return obj;
    }
}
