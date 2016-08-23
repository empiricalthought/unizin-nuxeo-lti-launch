package org.unzin.cmp.lti;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.MockHttpSession;
import org.jboss.seam.mock.MockServletContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.unizin.cmp.lti.LTIConsumerRegistry;
import org.unizin.cmp.lti.OAuthLTIFilter;

import com.google.common.net.MediaType;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;

@RunWith(FeaturesRunner.class)
@Features(LTIFeature.class)
public final class OAuthLTIFilterTest {
	private static final class MockFilterChain implements FilterChain {
		int numCalls = 0;
		int authCalls = 0;
		int unauthCalls = 0;

		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response)
				throws IOException, ServletException {
			numCalls++;
			if (request instanceof NuxeoSecuredRequestWrapper) {
				authCalls++;
			} else {
				unauthCalls++;
			}
		}
	}

	private OAuthLTIFilter filter;
	private MockHttpSession session;
	private MockFilterChain chain;
	private EnhancedMockHttpServletRequest req;
	private EnhancedMockHttpServletResponse resp;

	@Before
	public void setUp() {
		MockServletContext context = new MockServletContext();
	    session = new MockHttpSession(context);
		filter = new OAuthLTIFilter();
		chain = new MockFilterChain();
		req = new EnhancedMockHttpServletRequest(
                session, null, null, null, "POST");
		resp = new EnhancedMockHttpServletResponse();
	}

	@Test
	public void testUnauth() throws IOException, ServletException {
		filter.doFilter(req, resp, chain);
		assertEquals(1, chain.numCalls);
		assertEquals(1, chain.unauthCalls);
	}

	@Test
	public void testAuth() throws Exception {
		final String consumerKey = "12345";
		final String consumerSecret = "secret";
		final String requestURI = "http://whatever.com";
		final OAuthConsumer consumer = new OAuthConsumer("", consumerKey, consumerSecret, null);
		Framework.getService(LTIConsumerRegistry.class).save(consumer);
		req.setContentType(MediaType.FORM_DATA.toString());
		req.addHeader("X-Forwarded-Proto", "https");
		req.addParameter("oauth_version", "1.0");
		req.addParameter("oauth_consumer_key", consumerKey);
		req.addParameter("oauth_nonce", "whatever");
		req.addParameter("oauth_signature_method", "HMAC-SHA1");
		req.addParameter("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
		req.addParameter("lti_message_type", "basic-lti-launch-request");
		req.setRequestURI(requestURI);
		final OAuthMessage message = OAuthServlet.getMessage(req, requestURI.replaceFirst("http:", "https:"));
		message.sign(new OAuthAccessor(consumer));
		req.addParameter("oauth_signature", message.getSignature());
		filter.doFilter(req, resp, chain);
		assertEquals(resp.getStatusMessage(), HttpServletResponse.SC_OK, resp.getStatus());
		assertEquals("Expected one call to filter chain.", 1, chain.numCalls);
		assertEquals("Expected one authorized call to filter chain.", 1, chain.authCalls);
	}
}
