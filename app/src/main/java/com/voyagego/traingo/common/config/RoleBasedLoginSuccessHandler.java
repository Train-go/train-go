package com.voyagego.traingo.common.config;

import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class RoleBasedLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
	private final RequestCache requestCache;

	public RoleBasedLoginSuccessHandler(RequestCache requestCache) {
		this.requestCache = requestCache;
		setRequestCache(requestCache);
		setDefaultTargetUrl("/");
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		// An earlier failed attempt must not reappear on the login page after a success.
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(EmailRetainingLoginFailureHandler.LOGIN_EMAIL_ATTRIBUTE);
		}
		boolean admin = authentication.getAuthorities().stream()
			.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
		if (admin) {
			// Never change shared handler settings based on an individual login.
			requestCache.removeRequest(request, response);
			clearAuthenticationAttributes(request);
			getRedirectStrategy().sendRedirect(request, response, "/admin");
			return;
		}
		super.onAuthenticationSuccess(request, response, authentication);
	}
}
