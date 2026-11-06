package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.*;
@Configuration
public class JornadasWebConfig implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler) throws java.io.IOException {
                res.setStatus(410);res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"Este módulo foi unificado em Jornadas. Use /jornadas.\"}");return false;
            }
        }).addPathPatterns("/tarefas/**","/metas/**","/materias/**","/estudos/**");
    }
}
