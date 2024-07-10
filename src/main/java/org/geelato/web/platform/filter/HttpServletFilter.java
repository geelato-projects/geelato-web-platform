package org.geelato.web.platform.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class HttpServletFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        boolean cacheOption = Boolean.parseBoolean(httpServletRequest.getHeader("cache"));
        if (cacheOption) {
            CustomHttpServletRequest customHttpServletRequest = new CustomHttpServletRequest((HttpServletRequest) servletRequest);
            CustomHttpServletResponse customHttpServletResponse = new CustomHttpServletResponse((HttpServletResponse) servletResponse);
            filterChain.doFilter(customHttpServletRequest, customHttpServletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }


    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
