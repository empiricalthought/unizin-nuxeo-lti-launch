package org.unizin.cmp.auth;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.google.common.net.MediaType;

import net.oauth.OAuth.Problems;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;


final class LTIRequests {
	private static boolean isFormPost(final HttpServletRequest request) {
		return "POST".equalsIgnoreCase(request.getMethod()) &&
				MediaType.FORM_DATA.toString().equalsIgnoreCase(request.getContentType());
	}

	private static boolean isOAuthSigned(final HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.contains("OAuth")) {
			return true;
		}
		return request.getParameter("oauth_signature") != null;
	}

	static boolean isLTILaunch(final HttpServletRequest request) {
		// LTI launches are OAuth 1.0 signed post requests.
		// https://www.imsglobal.org/wiki/step-1-lti-launch-request
		return isFormPost(request) && isOAuthSigned(request) &&
				// Ignore other required things for now. Maybe later.
				"basic-lti-launch-request".equals(
						request.getParameter("lti_message_type"));
	}

	// Adapted from NuxeoOAuthFilter.
	static String requestURL(final HttpServletRequest request) {
		final String url = request.getRequestURI();
		final String proto = request.getHeader("X-Forwarded-Proto");
		if (proto == null || url.startsWith(proto)) {
			return url;
		}
		return url.replaceFirst("^.*://", proto + "://");
	}

	static void validate(final OAuthValidator validator, final OAuthMessage message,
			final OAuthConsumer consumer, final Logger logger) throws OAuthProblemException {
		try {
			validator.validateMessage(message, new OAuthAccessor(consumer));
		} catch (final OAuthProblemException e) {
			throw e;
		} catch (final URISyntaxException | IOException | OAuthException e) {
			logger.error("Could not verify signature.", e);
			throw new OAuthProblemException(Problems.SIGNATURE_INVALID);
		}
	}
}
