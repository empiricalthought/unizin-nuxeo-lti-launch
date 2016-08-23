package org.unzin.cmp.lti;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.unizin.cmp.lti.LTIConsumerRegistry;

import net.oauth.OAuthConsumer;
import net.oauth.signature.RSA_SHA1;


@Features(LTIFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@RunWith(FeaturesRunner.class)
public final class LTIConsumerRegistryTest {

	@Test
	public void testDirectoryExists() {
		final DirectoryService service = Framework.getService(DirectoryService.class);
		try (final Session session = service.open(LTIConsumerRegistry.DIRECTORY)) {
		}
	}

	@Test
	public void testServiceExists() {
		final LTIConsumerRegistry reg = Framework.getService(LTIConsumerRegistry.class);
		assertTrue(reg != null);
		assertTrue(reg instanceof LTIConsumerRegistry.Implementation);
	}

	@Test
	public void testSaveAndRetrieve() throws IOException {
		final String consumerKey = "12345";
		final String consumerSecret = "secret";
		final String cert = "x509";
		final LTIConsumerRegistry reg = Framework.getService(LTIConsumerRegistry.class);

		// Initial save.
		OAuthConsumer consumer = new OAuthConsumer("", consumerKey, consumerSecret, null);
		reg.save(consumer);
		// Get it back
		consumer = reg.get(consumerKey);
		assertEquals(consumerKey, consumer.consumerKey);
		assertEquals(consumerSecret, consumer.consumerSecret);
		assertNull(consumer.getProperty(RSA_SHA1.X509_CERTIFICATE));
		// Add a certificate, save again.
		consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, cert);
		reg.save(consumer);
		// Check stuff.
		consumer = reg.get(consumerKey);
		assertEquals(consumerKey, consumer.consumerKey);
		assertEquals(consumerSecret, consumer.consumerSecret);
		assertEquals(cert, consumer.getProperty(RSA_SHA1.X509_CERTIFICATE));
	}
}
