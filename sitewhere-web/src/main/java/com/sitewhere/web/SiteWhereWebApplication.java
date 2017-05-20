/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web;

import java.util.Arrays;

import org.apache.catalina.Context;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import com.hazelcast.core.HazelcastInstance;
import com.sitewhere.SiteWhere;
import com.sitewhere.SiteWhereApplication;
import com.sitewhere.hazelcast.NoShutdownHazelcastInstance;
import com.sitewhere.web.filters.JsonpFilter;
import com.sitewhere.web.filters.MethodOverrideFilter;
import com.sitewhere.web.filters.NoCacheFilter;
import com.sitewhere.web.filters.ResponseTimerFilter;
import com.sitewhere.web.mvc.MvcConfiguration;
import com.sitewhere.web.rest.RestMvcConfiguration;
import com.sitewhere.web.swagger.SiteWhereSwaggerConfig;

/**
 * Spring Boot application that loads SiteWhere with embedded Tomcat container,
 * REST services, and administrative application.
 * 
 * @author Derek
 */
@Configuration
@EnableHazelcastHttpSession
@Import({ SiteWhereSecurity.class })
public class SiteWhereWebApplication extends SiteWhereApplication {

    /** Static logger instance */
    @SuppressWarnings("unused")
    private static Logger LOGGER = LogManager.getLogger();

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
	TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory() {
	    protected void postProcessContext(Context context) {
		final int cacheSize = 100 * 1024 * 1024;
		StandardRoot standardRoot = new StandardRoot(context);
		standardRoot.setCacheMaxSize(cacheSize);
		standardRoot.setCacheObjectMaxSize(Integer.MAX_VALUE / 1024);
		context.setResources(standardRoot);
	    }
	};
	tomcat.setContextPath("/sitewhere");
	tomcat.setPort(8080);
	return tomcat;
    }

    @Bean
    public ServletRegistrationBean sitewhereRestInterface() {
	DispatcherServlet dispatcherServlet = new DispatcherServlet();
	AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
	applicationContext.register(RestMvcConfiguration.class, SiteWhereSwaggerConfig.class);
	dispatcherServlet.setApplicationContext(applicationContext);
	ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet,
		RestMvcConfiguration.REST_API_MATCHER);
	registration.setName("sitewhereRestInterface");
	registration.setLoadOnStartup(1);
	return registration;
    }

    @Bean
    public HazelcastInstance sitewhereHazelcast() {
	return new NoShutdownHazelcastInstance(
		SiteWhere.getServer().getHazelcastConfiguration().getHazelcastInstance());
    }

    @Bean
    @Order(value = 0)
    public FilterRegistrationBean sessionRepositoryFilterRegistration(
	    SessionRepositoryFilter<?> springSessionRepositoryFilter) {
	FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
	filterRegistrationBean.setFilter(new DelegatingFilterProxy(springSessionRepositoryFilter));
	filterRegistrationBean.setUrlPatterns(Arrays.asList("/*"));
	return filterRegistrationBean;
    }

    @Bean
    public CorsFilter corsFilter() {
	UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	CorsConfiguration config = new CorsConfiguration();
	config.addAllowedOrigin("*");
	config.addAllowedHeader("*");
	config.addAllowedMethod("*");
	config.addExposedHeader("Authorization");
	config.addExposedHeader("Content-Type");
	source.registerCorsConfiguration("/api/**", config);
	return new CorsFilter(source);
    }

    @Bean
    public ServletRegistrationBean sitewhereAdminInterface() {
	DispatcherServlet dispatcherServlet = new DispatcherServlet();
	AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
	applicationContext.register(MvcConfiguration.class);
	dispatcherServlet.setApplicationContext(applicationContext);
	ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet, "/admin/*");
	registration.setName("sitewhereAdminInterface");
	registration.setLoadOnStartup(2);
	return registration;
    }

    @Bean
    public ServletRegistrationBean redirectServlet() {
	RedirectServlet redirect = new RedirectServlet();
	ServletRegistrationBean registration = new ServletRegistrationBean(redirect, "/admin");
	registration.setName("sitewhereRedirect");
	registration.setLoadOnStartup(3);
	return registration;
    }

    @Bean
    public FilterRegistrationBean methodOverrideFilter() {
	MethodOverrideFilter filter = new MethodOverrideFilter();
	FilterRegistrationBean registration = new FilterRegistrationBean();
	registration.setFilter(filter);
	registration.addUrlPatterns(RestMvcConfiguration.REST_API_MATCHER);
	return registration;
    }

    @Bean
    public FilterRegistrationBean responseTimerFilter() {
	ResponseTimerFilter filter = new ResponseTimerFilter();
	FilterRegistrationBean registration = new FilterRegistrationBean();
	registration.setFilter(filter);
	registration.addUrlPatterns(RestMvcConfiguration.REST_API_MATCHER);
	return registration;
    }

    @Bean
    public FilterRegistrationBean noCacheFilter() {
	NoCacheFilter filter = new NoCacheFilter();
	FilterRegistrationBean registration = new FilterRegistrationBean();
	registration.setFilter(filter);
	registration.addUrlPatterns(RestMvcConfiguration.REST_API_MATCHER);
	return registration;
    }

    @Bean
    public FilterRegistrationBean jsonpFilter() {
	JsonpFilter filter = new JsonpFilter();
	FilterRegistrationBean registration = new FilterRegistrationBean();
	registration.setFilter(filter);
	registration.addUrlPatterns(RestMvcConfiguration.REST_API_MATCHER);
	return registration;
    }

    /**
     * Acts on shutdown hook to gracefully shut down SiteWhere server
     * components.
     * 
     * @return
     */
    @Bean
    public ShutdownListener shutdownListener() {
	return new ShutdownListener();
    }

    public static void main(String[] args) {
	SpringApplication.run(SiteWhereWebApplication.class, args);
    }
}