package org.example.mvc.controller;

import org.example.mvc.bean.User;
import org.example.mvc.framework.GetMapping;
import org.example.mvc.framework.ModelAndView;

import javax.servlet.http.HttpSession;

public class IndexController {

    @GetMapping("/")
    public ModelAndView index(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return new ModelAndView("index", "user", user);
    }

    @GetMapping("/hello")
    public ModelAndView hello(String name) {
        if (name == null) {
            name = "World";
        }
        return new ModelAndView("hello", "name", name);
    }
}
