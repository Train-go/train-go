package com.voyagego.traingo.common.config;

import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sends a failed login back to the login page and keeps the typed email for
 * that one page view, so the visitor only retypes the password. The email goes
 * through the session, not the URL, and nothing else from the request is kept:
 * never the password. The login page reads the value once and removes it.
 */
public class EmailRetainingLoginFailureHandler implements AuthenticationFailureHandler {
	public static final String LOGIN_EMAIL_ATTRIBUTE = "AUTH_LOGIN_EMAIL";
	private static final int EMAIL_MAX_LENGTH = 255;

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		String email = request.getParameter("email");
		if (email != null && !email.isBlank()) {
			String kept = email.length() > EMAIL_MAX_LENGTH ? email.substring(0, EMAIL_MAX_LENGTH) : email;
			request.getSession().setAttribute(LOGIN_EMAIL_ATTRIBUTE, kept);
		} else {
			var session = request.getSession(false);
			if (session != null) {
				session.removeAttribute(LOGIN_EMAIL_ATTRIBUTE);
			}
		}
		redirectStrategy.sendRedirect(request, response, "/login?error");
	}
}
