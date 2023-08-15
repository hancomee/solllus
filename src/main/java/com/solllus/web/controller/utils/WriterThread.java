package com.solllus.web.controller.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Supplier;

public class WriterThread implements Runnable {

    private Path root;
    public boolean active;
    int i = 10;
    Map<String, Supplier<List<String>>> handlers = new HashMap<>();
    Queue<String> queue = new LinkedList<>();


    public WriterThread(Path root) {
        this.root = root;
    }

    public boolean exists(String path) {
        return queue.contains(path);
    }

    public synchronized void awake() {
        if (!active) notify();
    }

    public WriterThread add(String path, Supplier<List<String>> handler) {
        synchronized (queue) {
            if (!queue.contains(path)) {
                queue.add(path);
                handlers.put(path, handler);
            }
        }
        return this;
    }

    @Override
    public synchronized void run() {
        while (true) {

            try {
                this.active = false;
                wait();
            } catch (Exception e) {
                System.out.println(e);
            }
            this.active = true;

            try {
                write();
            } catch (IOException e) {

            }

        }
    }

    private void write() throws IOException {
        String path;
        while (!queue.isEmpty()) {
            synchronized (queue) {
                path = queue.poll();
            }

            Supplier<List<String>> handler = handlers.get(path);
            Path file = root.resolve("config").resolve(path);
            if (!Files.exists(file)) Files.createFile(file);
            List<String> lines = handler.get();
            if (lines != null)  // 에러시 null을 반환하게 하고, 작성을 취소한다.
                Files.write(file, handler.get(), StandardOpenOption.TRUNCATE_EXISTING);
        }
    }


    /*
    private void write() throws IOException {
        UserMap map;
        while(!queue.isEmpty()) {
            synchronized (queue) {
                map = queue.poll();
            }

            Path file = Files.createDirectories(root.resolve("config").resolve(map.name));
            file = file.resolve("display.txt");
            if(!Files.exists(file)) Files.createFile(file);

            List<Map<String, Object>> list = new ArrayList<>();
            map.displayInfo.values().stream().forEach(v -> v.toJSON(list));
            String json = new ObjectMapper().writeValueAsString(list);

            Files.write(file, Arrays.asList(json), StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
    */
}
