package com.solllus.web;

import com.boosteel.nativedb.NativeDB;
import com.solllus.web.controller.support.StaticVariable;
import com.solllus.web.security.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

@ComponentScan({"com.solllus"})
@RequestMapping
@SpringBootApplication
public class SolllusApplication extends SpringBootServletInitializer implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(SolllusApplication.class, args);
    }

    // WEB-INF 배포를 위해서 반드시 필요함
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SolllusApplication.class);
    }

    Properties properties;

    private Path ROOT = Paths.get("E:/solllus");
    private String LOCATION;

    @PostConstruct
    public void before() throws Exception {


        if (!Files.exists(ROOT)) ROOT = Paths.get("D:/solllus");
        if (!Files.exists(ROOT)) ROOT = Paths.get("E:/solllus");

        // cafe24 용
        if (!Files.exists(ROOT)) {
            ROOT = Paths.get("/home/hosting_users/boosteel/solllus");
            LOCATION = "file:/";
        }

        if (!Files.exists(ROOT)) {
            ROOT = Paths.get("/web/solllus");
            LOCATION = "file:/";
        }

        properties = new Properties();

        if (Files.exists(ROOT.resolve("config.properties")))
            properties.load(Files.newInputStream(ROOT.resolve("config.properties")));

        StaticVariable.ROOT = ROOT;
        StaticVariable.USERS_DIR = ROOT.resolve("users");
    }

    @Bean("rootPath")
    public Path rootPath() {
        return ROOT;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestInterceptor())
                .addPathPatterns("/data/user/**");
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String location = ROOT.toString();

        if (!location.startsWith("file")) location = "file:" + location;
        if (!location.endsWith("/")) location = location + "/";

        CacheControl cacheControl = CacheControl.maxAge(Duration.ofDays(365));

        registry.addResourceHandler("/apps/**")
                .addResourceLocations(location + "apps/")
                .setCachePeriod(Integer.MAX_VALUE)
//                .setCacheControl(cacheControl)
                .resourceChain(true);

        registry.addResourceHandler("/users/**")
                .addResourceLocations(location + "users/")
                .setCachePeriod(Integer.MAX_VALUE)
//                .setCacheControl(cacheControl)
                .resourceChain(true);

        registry.addResourceHandler("/resources/**")
                .addResourceLocations(location + "resources/")
                .setCachePeriod(Integer.MAX_VALUE)
//                .setCacheControl(cacheControl)
                .resourceChain(true);

        if (LOCATION == null) {
            for (String CHAR : "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split(""))
                registry.addResourceHandler("/disc/" + CHAR + ":/**").addResourceLocations("file:/" + CHAR + ":/");
        } else {
            registry.addResourceHandler("/disc/**").addResourceLocations(LOCATION);
        }
    }


    @Bean
    public DataSource dataSource(@Autowired NativeDB nativeDB) {
        return nativeDB.dataSource;
    }

    @Bean
    public NativeDB db() {

        String
                DB_HOST = "localhost",
                DB_NAME = "hellofunc",
                DB_USER = "root",
                DB_PASSWORD = "ko9984";

        if (properties.containsKey("db.host")) DB_HOST = properties.getProperty("db.host");
        if (properties.containsKey("db.name")) DB_NAME = properties.getProperty("db.name");
        if (properties.containsKey("db.user")) DB_USER = properties.getProperty("db.user");
        if (properties.containsKey("db.password")) DB_PASSWORD = properties.getProperty("db.password");

        return new NativeDB("jdbc:mariadb://" + DB_HOST + ":3306/" + DB_NAME + "?useOldAliasMetadataBehavior=true",
                DB_USER, DB_PASSWORD);
    }

}
