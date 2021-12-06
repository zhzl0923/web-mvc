package org.example.mvc.framework;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@WebServlet(urlPatterns = "/")
public class DispatcherServlet extends HttpServlet {

    private final Map<String, GetDispatcher> getMappings = new HashMap<>();
    private final Map<String, PostDispatcher> postMappings = new HashMap<>();
    private static final Set<Class<?>> supportedGetParameterTypes = Set.of(int.class, long.class, boolean.class,
            String.class, HttpServletRequest.class, HttpServletResponse.class, HttpSession.class);

    private static final Set<Class<?>> supportedPostParameterTypes = Set.of(HttpServletRequest.class,
            HttpServletResponse.class, HttpSession.class);
    private ViewEngine viewEngine;

    @Override
    public void init() throws ServletException {
        String controllerPath = "/WEB-INF/classes/org/example/mvc/controller";
        File dir = new File(this.getServletContext().getRealPath(controllerPath));
        File[] controllerFiles = dir.listFiles();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (controllerFiles != null) {
            for (File controllerFile : controllerFiles) {
                if (controllerFile.getName().contains("$1.class")){
                    continue;
                }
                try {
                    String className = controllerFile.getName().split("\\.")[0];
                    Class<?> controllerClass = Class.forName("org.example.mvc.controller." + className);
                    Constructor<?> constructor = controllerClass.getDeclaredConstructor();
                    Object controllerInstance = constructor.newInstance();
                    Method[] methods = controllerClass.getMethods();
                    for (Method method : methods) {
                        if (method.getAnnotation(GetMapping.class) != null) {
                            if (method.getReturnType() != ModelAndView.class && method.getReturnType() != void.class) {
                                throw new UnsupportedOperationException(
                                        "Unsupported return type: " + method.getReturnType() + " for method: " + method);
                            }
                            for (Class<?> parameterClass : method.getParameterTypes()) {
                                if (!supportedGetParameterTypes.contains(parameterClass)) {
                                    throw new UnsupportedOperationException(
                                            "Unsupported parameter type: " + parameterClass + " for method: " + method);
                                }
                            }
                            String[] parameterNames = Arrays.stream(method.getParameters()).map(Parameter::getName)
                                    .toArray(String[]::new);
                            String path = method.getAnnotation(GetMapping.class).value();
                            this.getMappings.put(path, new GetDispatcher(controllerInstance, method, parameterNames,
                                    method.getParameterTypes()));
                        } else if (method.getAnnotation(PostMapping.class) != null) {
                            // 处理@Post:
                            if (method.getReturnType() != ModelAndView.class && method.getReturnType() != void.class) {
                                throw new UnsupportedOperationException(
                                        "Unsupported return type: " + method.getReturnType() + " for method: " + method);
                            }
                            Class<?> requestBodyClass = null;
                            for (Class<?> parameterClass : method.getParameterTypes()) {
                                if (!supportedPostParameterTypes.contains(parameterClass)) {
                                    if (requestBodyClass == null) {
                                        requestBodyClass = parameterClass;
                                    } else {
                                        throw new UnsupportedOperationException("Unsupported duplicate request body type: "
                                                + parameterClass + " for method: " + method);
                                    }
                                }
                            }
                            String path = method.getAnnotation(PostMapping.class).value();
                            this.postMappings.put(path, new PostDispatcher(controllerInstance, method,
                                    method.getParameterTypes(), objectMapper));
                        }
                    }
                }catch (ReflectiveOperationException e){
                    throw new ServletException(e);
                }

            }
        }
        this.viewEngine = new ViewEngine();
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp, postMappings);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        process(req, resp, getMappings);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp,
                         Map<String, ? extends AbstractDispatcher> dispatcherMap)
            throws IOException, ServletException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        String path = req.getRequestURI().substring(req.getContextPath().length());
        // 根据路径查找GetDispatcher:

        AbstractDispatcher dispatcher = dispatcherMap.get(path);
        if (dispatcher == null) {
            // 未找到返回404:
//            resp.sendError(404);

            return;
        }
        // 调用Controller方法获得返回值:
        ModelAndView mv;
        try {
            mv = dispatcher.invoke(req, resp);
        } catch (ReflectiveOperationException e) {
            throw new ServletException(e);
        }
        // 允许返回null:
        if (mv == null) {
            return;
        }
        // 允许返回`redirect:`开头的view表示重定向:
        if (mv.view.startsWith("redirect:")) {
            resp.sendRedirect(mv.view.substring(9));
            return;
        }
        // 将模板引擎渲染的内容写入响应:
        this.viewEngine.render(req, resp, mv);
    }
}

abstract class AbstractDispatcher {
    public abstract ModelAndView invoke(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ReflectiveOperationException;
}


class GetDispatcher extends AbstractDispatcher {

    final Object instance;
    final Method method;
    final String[] parameterNames;
    final Class<?>[] parameterClasses;

    public GetDispatcher(Object instance, Method method, String[] parameterNames, Class<?>[] parameterClasses) {
        this.instance = instance;
        this.method = method;
        this.parameterNames = parameterNames;
        this.parameterClasses = parameterClasses;
    }

    @Override
    public ModelAndView invoke(HttpServletRequest request, HttpServletResponse response)
            throws ReflectiveOperationException {
        Object[] arguments = new Object[parameterClasses.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            String parameterName = parameterNames[i];
            Class<?> parameterClass = parameterClasses[i];
            if (parameterClass == HttpServletRequest.class) {
                arguments[i] = request;
            } else if (parameterClass == HttpServletResponse.class) {
                arguments[i] = response;
            } else if (parameterClass == HttpSession.class) {
                arguments[i] = request.getSession();
            } else if (parameterClass == int.class) {
                arguments[i] = Integer.valueOf(getOrDefault(request, parameterName, "0"));
            } else if (parameterClass == long.class) {
                arguments[i] = Long.valueOf(getOrDefault(request, parameterName, "0"));
            } else if (parameterClass == boolean.class) {
                arguments[i] = Boolean.valueOf(getOrDefault(request, parameterName, "false"));
            } else if (parameterClass == String.class) {
                arguments[i] = getOrDefault(request, parameterName, "");
            } else {
                throw new RuntimeException("Missing handler for type: " + parameterClass);
            }
        }
        return (ModelAndView) this.method.invoke(this.instance, arguments);
    }

    private String getOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String s = request.getParameter(name);
        return s == null ? defaultValue : s;
    }
}

class PostDispatcher extends AbstractDispatcher {

    final Object instance;
    final Method method;
    final Class<?>[] parameterClasses;
    final ObjectMapper objectMapper;

    public PostDispatcher(Object instance, Method method, Class<?>[] parameterClasses, ObjectMapper objectMapper) {
        this.instance = instance;
        this.method = method;
        this.parameterClasses = parameterClasses;
        this.objectMapper = objectMapper;
    }

    @Override
    public ModelAndView invoke(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ReflectiveOperationException {
        Object[] arguments = new Object[parameterClasses.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            Class<?> parameterClass = parameterClasses[i];
            if (parameterClass == HttpServletRequest.class) {
                arguments[i] = request;
            } else if (parameterClass == HttpServletResponse.class) {
                arguments[i] = response;
            } else if (parameterClass == HttpSession.class) {
                arguments[i] = request.getSession();
            } else {
                BufferedReader reader = request.getReader();
                arguments[i] = this.objectMapper.readValue(reader, parameterClass);
            }
        }
        return (ModelAndView) this.method.invoke(instance, arguments);
    }
}