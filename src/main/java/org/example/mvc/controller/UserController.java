package org.example.mvc.controller;

import org.example.mvc.bean.SignInBean;
import org.example.mvc.framework.GetMapping;
import org.example.mvc.framework.ModelAndView;
import org.example.mvc.framework.PostMapping;

import javax.servlet.http.HttpSession;

public class UserController {
    @GetMapping("/sign_in")
    public ModelAndView signIn() {
        return new ModelAndView();
    }

    @PostMapping("/sign_in")
    public ModelAndView doSignIn(SignInBean bean) {
        return new ModelAndView();
    }

    @GetMapping("/sign_out")
    public ModelAndView signOut(HttpSession session) {
        return new ModelAndView();
    }
}
