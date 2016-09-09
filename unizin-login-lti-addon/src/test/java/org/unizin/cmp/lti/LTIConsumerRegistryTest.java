package org.unizin.cmp.lti;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.inject.Inject;
import javax.security.auth.login.LoginContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import net.oauth.OAuthConsumer;
import net.oauth.signature.RSA_SHA1;


@Features({LTIFeature.class,
    // Necessary to make login work reasonably -- permissions test fails w/o this.
    WebEngineFeature.class,
})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@RunWith(FeaturesRunner.class)
public final class LTIConsumerRegistryTest {
    private static final String CONSUMER_KEY = "12345";
    private static final String USERNAME = "LTI_USER";

    @Inject
    DirectoryService directoryService;

    @Inject
    LTIConsumerRegistry ltiRegistry;

    @Before
    public void loginAsAdministrator() throws Exception {
        Framework.login();
    }

    @Test
    public void testDirectoryExists() {
        try (final Session session = directoryService.open(LTIConsumerRegistry.DIRECTORY)) {
        }
    }

    @Test
    public void testServiceExists() {
        assertTrue(ltiRegistry instanceof LTIConsumerRegistry.Implementation);
    }

    @Test
    public void testSaveAndRetrieve() throws Exception {
        final String consumerSecret = "secret";
        final byte[] cert = "x509".getBytes();
        // Initial save.
        OAuthConsumer consumer = new OAuthConsumer("", CONSUMER_KEY, consumerSecret, null);
        ltiRegistry.save(consumer);
        // Get it back
        consumer = ltiRegistry.get(CONSUMER_KEY);
        assertNotNull(consumer);
        assertEquals(CONSUMER_KEY, consumer.consumerKey);
        assertEquals(consumerSecret, consumer.consumerSecret);
        assertNull(consumer.getProperty(RSA_SHA1.X509_CERTIFICATE));
        // Add a certificate, save again.
        consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, cert);
        ltiRegistry.save(consumer);
        // Check stuff.
        consumer = ltiRegistry.get(CONSUMER_KEY);
        assertEquals(CONSUMER_KEY, consumer.consumerKey);
        assertEquals(consumerSecret, consumer.consumerSecret);
        final Object certProp = consumer.getProperty(RSA_SHA1.X509_CERTIFICATE);
        assertTrue(certProp instanceof byte[]);
        assertArrayEquals(cert, (byte[])certProp);
    }

    private void permissionsTest(final boolean isAdministrator) throws Exception {
        final UserManager um = Framework.getService(UserManager.class);
        DocumentModel user = um.getBareUserModel();
        final String schema = um.getUserSchemaName();
        user.setProperty(schema, "username", USERNAME);
        user.setProperty(schema, "password", USERNAME);
        if (isAdministrator) {
            user.setProperty(schema, "groups", um.getAdministratorsGroups());
        }
        user = um.createUser(user);
        try {
            final LoginContext context = Framework.loginAsUser(USERNAME);
            try (final Session session = directoryService.open(LTIConsumerRegistry.DIRECTORY)) {
                final DocumentModelList results = session.query(
                        Collections.singletonMap(LTIConsumerRegistry.CONSUMER_KEY_PROP,
                                CONSUMER_KEY));
                if (isAdministrator) {
                    assertEquals("administrative user should see LTIConsumerRegistry contents",
                            1, results.size());
                } else {
                    assertEquals("non-administrative user should not see LTIConsumerRegistry contents",
                            0, results.size());
                }
            } finally {
                context.logout();
            }
        } finally {
            um.deleteUser(user.getId());
        }
    }

    @Test
    public void testPermissions() throws Exception {
        // Create an entry, then see if users can see it or not.
        final OAuthConsumer consumer = new OAuthConsumer("", CONSUMER_KEY, "secret", null);
        ltiRegistry.save(consumer);
        permissionsTest(true); // Admin user should.
        permissionsTest(false); // Non-admin user should not.
    }
}
