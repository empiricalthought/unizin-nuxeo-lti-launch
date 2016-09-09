package org.unizin.cmp.lti;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.api.client.util.Maps;
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
    private static final String CONSUMER_KEY = "12345";
    private static final String TOOL_CONSUMER_GUID = "wharblegarbleguid";
    private static final Map<String, String> PARAMETERS;
    static {
        final Map<String, String> m = Maps.newHashMap();
        m.put("oauth_version", "1.0");
        m.put("oauth_consumer_key", CONSUMER_KEY);
        m.put("oauth_nonce", "whatever");
        m.put("lti_message_type", "basic-lti-launch-request");
        m.put("tool_consumer_instance_guid", TOOL_CONSUMER_GUID);
        m.put("user_id", "userID");
        m.put("lis_person_name_given", "Ani");
        m.put("lis_person_name_family", "DiFranco");
        m.put("lis_person_contact_email_primary", "info@righteousbabe.com");
        m.put("roles", "Instructor,Learner");
        PARAMETERS = Collections.unmodifiableMap(m);
    }

    public static final String EXPECTED_USERNAME = "wharblegarbleguserID";

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


    @Inject
    DirectoryService directoryService;

    private OAuthLTIFilter filter;
    private MockHttpSession session;
    private MockFilterChain chain;
    private EnhancedMockHttpServletRequest req;
    private EnhancedMockHttpServletResponse resp;

    @Before
    public void setUp() {
        MockServletContext context = new MockServletContext();
        final String sessionID = UUID.randomUUID().toString();
        session = new MockHttpSession(context) {
            @Override
            public String getId() {
                return sessionID;
            }
        };
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
        assertNull(chain.principal);
    }


    private void testAuth(final String signatureMethod, final KeyPair keyPair)
            throws Exception {
        try {
            final String consumerSecret = "secret";
            final String requestURI = "http://whatever.com";
            final OAuthConsumer consumer = new OAuthConsumer("", CONSUMER_KEY,
                    consumerSecret, null);
            if (keyPair != null) {
                consumer.setProperty(RSA_SHA1.PRIVATE_KEY,
                        keyPair.getPrivate().getEncoded());
                consumer.setProperty(RSA_SHA1.PUBLIC_KEY,
                        keyPair.getPublic().getEncoded());
            }
            Framework.getService(LTIConsumerRegistry.class).save(consumer);
            req.setContentType(MediaType.FORM_DATA.toString());
            req.addHeader("X-Forwarded-Proto", "https");
            req.setRequestURI(requestURI);
            final Map<String, String> requestParams = new TreeMap<>(PARAMETERS);
            requestParams.put("oauth_timestamp",
                    String.valueOf(System.currentTimeMillis() / 1000));
            requestParams.put("oauth_signature_method", signatureMethod);
            for (final Map.Entry<String, String> entry: requestParams.entrySet()) {
                req.addParameter(entry.getKey(), entry.getValue());
            }

            final OAuthMessage message = OAuthServlet.getMessage(req,
                    requestURI.replaceFirst("http:", "https:"));
            message.sign(new OAuthAccessor(consumer));
            // Need to add signature AFTER signing.
            req.addParameter("oauth_signature", message.getSignature());
            requestParams.put("oauth_signature", message.getSignature());

            filter.doFilter(req, resp, chain);
            assertEquals(resp.getStatusMessage(), HttpServletResponse.SC_OK,
                    resp.getStatus());
            assertEquals("Expected one call to filter chain.", 1,
                    chain.numCalls);
            assertEquals("Expected one authorized call to filter chain.", 1,
                    chain.authCalls);
            assertEquals(EXPECTED_USERNAME, chain.principal.getName());
            assertEquals(requestParams,
                    session.getAttribute(OAuthLTIFilter.LAUNCH_PARAMS_ATTRIB));

            final UserManager um = Framework.getService(UserManager.class);
            final DocumentModel userDoc = um.getUserModel(EXPECTED_USERNAME);
            assertNotNull(userDoc);
            assertEquals(TOOL_CONSUMER_GUID,
                    userDoc.getPropertyValue("user:toolConsumerInstanceGuid"));
        } finally {
            Framework.login();
        }
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
