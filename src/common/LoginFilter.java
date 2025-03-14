import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        System.out.println("LoginFilter: " + httpRequest.getRequestURI());

        // Check if this URL is allowed to access without logging in
        if (this.isUrlAllowedWithoutLogin(httpRequest.getRequestURI())) {
            // Keep default action: pass along the filter chain
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession();
        String role = (session != null) ? (String) session.getAttribute("role") : null;

        // Redirect to login page if the "user" attribute doesn't exist in session
        if (httpRequest.getSession().getAttribute("user") == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
            return;
        }

        if ("customer".equals(role)
                && httpRequest.getRequestURI().contains("/_dashboard/")
                && !httpRequest.getRequestURI().endsWith("/_dashboard/login.html")) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/_dashboard/login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
    /*
     Setup your own rules here to allow accessing some resources without logging in
     Always allow your own login-related requests (HTML, JS, servlet, etc.).
     You might also want to allow some CSS files, etc.
    */
        return allowedURIs.stream().anyMatch(requestURI::endsWith);
    }

    public void init(FilterConfig fConfig) {
        allowedURIs.add("login.html");
        allowedURIs.add("login.js");
        allowedURIs.add("api/login");
        allowedURIs.add("logo.png");
        allowedURIs.add("https://www.google.com/recaptcha/api/siteverify");
        allowedURIs.add("_dashboard/login.html");
        allowedURIs.add("api/employeeLogin");
    }

    public void destroy() {
        // ignored.
    }

}
