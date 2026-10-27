package com.voyagego.traingo.common.config;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.HttpSessionMutexListener;

/**
 * Gives every session its own lock object ({@code WebUtils.getSessionMutex}).
 * The payment synchronizes on it, so two requests of one session, such as a
 * button pressed twice, pay one after the other and never at the same time.
 */
@Configuration
public class SessionMutexConfig {

	@Bean
	ServletListenerRegistrationBean<HttpSessionMutexListener> httpSessionMutexListener() {
		return new ServletListenerRegistrationBean<>(new HttpSessionMutexListener());
	}

}
