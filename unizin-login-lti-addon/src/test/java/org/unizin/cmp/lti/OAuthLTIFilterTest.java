package org.unizin.cmp.lti;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.MockHttpSession;
import org.jboss.seam.mock.MockServletContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.common.net.MediaType;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;
import net.oauth.signature.RSA_SHA1;

@RunWith(FeaturesRunner.class)
@Features(LTIFeature.class)
public final class OAuthLTIFilterTest {

    @Inject
    DirectoryService directoryService;

    public static final String EXPECTED_USERNAME = "wharblegarbleg12345";
    private static final class MockFilterChain implements FilterChain {
        int numCalls = 0;
        int authCalls = 0;
        int unauthCalls = 0;
        Principal principal;

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response)
                throws IOException, ServletException {
            numCalls++;
            if (request instanceof NuxeoSecuredRequestWrapper) {
                authCalls++;
                principal = ((NuxeoSecuredRequestWrapper) request).getUserPrincipal();
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

    @After
    public void tearDown() throws Exception {
        NuxeoAuthenticationFilter.loginAs("Administrator");
    }

    @Test
    public void testUnauth() throws IOException, ServletException {
        filter.doFilter(req, resp, chain);
        assertEquals(1, chain.numCalls);
        assertEquals(1, chain.unauthCalls);
        assertNull(chain.principal);
    }

    private void testAuth(final String signatureMethod, final KeyPair keyPair)
            throws Exception {
        final String consumerKey = "12345";
        final String consumerSecret = "secret";
        final String requestURI = "http://whatever.com";
        final String toolConsumerGuid = "wharblegarbleguid";
        final OAuthConsumer consumer = new OAuthConsumer("", consumerKey, consumerSecret, null);
        if (keyPair != null) {
            consumer.setProperty(RSA_SHA1.PRIVATE_KEY, keyPair.getPrivate().getEncoded());
            consumer.setProperty(RSA_SHA1.PUBLIC_KEY, keyPair.getPublic().getEncoded());
        }
        Framework.getService(LTIConsumerRegistry.class).save(consumer);
        req.setContentType(MediaType.FORM_DATA.toString());
        req.addHeader("X-Forwarded-Proto", "https");
        req.addParameter("oauth_version", "1.0");
        req.addParameter("oauth_consumer_key", consumerKey);
        req.addParameter("oauth_nonce", "whatever");
        req.addParameter("oauth_signature_method", signatureMethod);
        req.addParameter("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        req.addParameter("lti_message_type", "basic-lti-launch-request");
        req.addParameter("tool_consumer_instance_guid", toolConsumerGuid);
        req.addParameter("user_id", "12345");
        req.addParameter("lis_person_name_given", "Ani");
        req.addParameter("lis_person_name_family", "DiFranco");
        req.addParameter("lis_person_contact_email_primary", "info@righteousbabe.com");
        req.addParameter("roles", "Instructor,Learner");
        req.setRequestURI(requestURI);
        final OAuthMessage message = OAuthServlet.getMessage(req, requestURI.replaceFirst("http:", "https:"));
        message.sign(new OAuthAccessor(consumer));
        req.addParameter("oauth_signature", message.getSignature());
        filter.doFilter(req, resp, chain);
        assertEquals(resp.getStatusMessage(), HttpServletResponse.SC_OK, resp.getStatus());
        assertEquals("Expected one call to filter chain.", 1, chain.numCalls);
        assertEquals("Expected one authorized call to filter chain.", 1, chain.authCalls);
        assertEquals(EXPECTED_USERNAME, chain.principal.getName());
        final UserManager um = Framework.getService(UserManager.class);
        final DocumentModel userDoc = um.getUserModel(EXPECTED_USERNAME);
        assertNotNull(userDoc);
        assertEquals(toolConsumerGuid, userDoc.getPropertyValue("user:toolConsumerInstanceGuid"));
    }

    @Test
    public void testHMAC() throws Exception {
        testAuth(OAuth.HMAC_SHA1, null);
    }

    @Test
    public void testRSA() throws Exception {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        final KeyPair keyPair = keyGen.generateKeyPair();
        testAuth(OAuth.RSA_SHA1, keyPair);
    }
}
