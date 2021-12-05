package org.example.mvc.framework;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class ViewEngine {

    public void render(HttpServletRequest req, HttpServletResponse resp, ModelAndView mv) throws IOException {
        String view = mv.view;
        Map<String, Object> model = mv.model;
        // 根据view找到模板文件:

        TemplateEngine template = getTemplateByPath(req.getServletContext(), view);
        // 渲染并写入Writer:
        WebContext ctx =
                new WebContext(req, resp, req.getServletContext(), req.getLocale());
        ctx.setVariables(model);
        template.process(view, ctx, resp.getWriter());
    }

    private TemplateEngine getTemplateByPath(ServletContext servletContext, String view) {
        TemplateEngine templateEngine = new TemplateEngine();

        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/WEB-INF/templates/");
        templateResolver.setSuffix(".html");
        servletContext.getResourceAsStream("/WEB-INF/templates/" + view);
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}