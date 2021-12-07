package org.example.mvc.controller;


import org.example.mvc.bean.SignInBean;
import org.example.mvc.bean.User;
import org.example.mvc.framework.GetMapping;
import org.example.mvc.framework.ModelAndView;
import org.example.mvc.framework.PostMapping;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserController {
    private final Map<String, User> userDatabase = new HashMap<>() {
        {
            List<User> users = List.of(
                    new User("bob@example.com", "bob123", "Bob", "This is bob."),
                    new User("tom@example.com", "tomcat", "Tom", "This is tom.")
            );
            users.forEach(user -> {
                put(user.email, user);
            });
        }
    };

    @GetMapping("/sign_in")
    public ModelAndView signIn() {
        return new ModelAndView("signIn");
    }

    @PostMapping("/sign_in")
    public ModelAndView doSignIn(SignInBean bean, HttpServletResponse response, HttpSession session)
            throws IOException {
        User user = userDatabase.get(bean.email);
        if (user == null || !user.password.equals(bean.password)) {
            response.setContentType("application/json");
            PrintWriter pw = response.getWriter();
            pw.write("{\"error\":\"Bad email or password\"}");
            pw.flush();
        } else {
            session.setAttribute("user", user);
            response.setContentType("application/json");
            PrintWriter pw = response.getWriter();
            pw.write("{\"result\":true}");
            pw.flush();
        }
        return null;
    }

    @GetMapping("/sign_out")
    public ModelAndView signOut(HttpSession session) {
        session.removeAttribute("user");
        return new ModelAndView("redirect:/");
    }

    @GetMapping("/user/profile")
    public ModelAndView profile(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return new ModelAndView("redirect:/sign_in");
        }
        return new ModelAndView("profile", "user", user);
    }
}
