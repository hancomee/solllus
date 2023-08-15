package com.solllus.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping()
public class ViewController {


    @RequestMapping
    public String intro() {
        return "index.html";
    }

    @RequestMapping(value = "templates/**")
    public String $template(HttpServletRequest req) {
        return req.getServletPath().substring(10) + ".html";
    }

    // view Page
    @RequestMapping(value = "certify/*/*")
    public String certify() {
        return "certify.html";
    }

    @RequestMapping(value = "v/*/*")
    public String view() {
        return "main.html";
    }

    @RequestMapping(value = "v2/*/*")
    public String view2() {
        return "main.html";
    }

    @RequestMapping(value = "v3/*/*")
    public String view3() {
        return "main.html";
    }

    @RequestMapping(value = "appstore")
    public String apps() {
        return "appstore.html";
    }

    // admin/* 은 관리자모드에서 모든 계정을 살펴보기 위함
    @RequestMapping(value = {"admin", "admin/*"})
    public String admin() {
        return "admin.html";
    }

    // admin/* 은 관리자모드에서 모든 계정을 살펴보기 위함
    @RequestMapping(value = {"admin-register/*"})
    public String adminRegister() {
        return "admin-register.html";
    }

    @RequestMapping(value = {"dashboard",})
    public String dashboard() {
        return "dashboard.html";
    }


    @RequestMapping(value = {"master",})
    public String master() {
        return "master.html";
    }

    @RequestMapping(value = "app/**")
    public String appAdmin() {
        return "src/app-admin.html";
    }


    /* *************************** ▲ Template ▲ *************************** */

    private <T> T out(T obj) {
        System.out.println(obj);
        return obj;
    }
}
