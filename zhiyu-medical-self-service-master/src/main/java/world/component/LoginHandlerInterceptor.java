package world.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import world.entity.User;



/**
 * 登录拦截器
 *
 */
public class LoginHandlerInterceptor implements HandlerInterceptor {

    /**
     * 在目标方式执行之前执行
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {
        User user = (User) request.getSession().getAttribute("loginUser");
        if (user == null) {
            // 未登录,返回登录页面
            response.sendRedirect("/");
            return false;
        } else {
            // 已登录,放行
            return true;
        }
    }
}
