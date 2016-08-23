package org.unizin.cmp.lti;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.collect.Maps;

import net.oauth.OAuthConsumer;
import net.oauth.signature.RSA_SHA1;


public interface LTIConsumerRegistry {
	OAuthConsumer get(String consumerKey);
	void save(OAuthConsumer consumer) throws IOException;


	static final String SCHEMA = "ltiConsumer";
	static final String DIRECTORY = SCHEMA + "s";
	static final String CONSUMER_KEY_PROP = "consumerKey";
	static final String SECRET_PROP = "consumerSecret";
	static final String RSA_PROP = "RSAPublicKey";
	static final String CERT_PROP = "X509Certificate";


	public static final class Implementation
		extends DefaultComponent implements LTIConsumerRegistry {

		private DirectoryService directoryService;

		@Override
		public void activate(ComponentContext context) {
			super.activate(context);
			directoryService = Framework.getService(DirectoryService.class);
		}

		@Override
		public OAuthConsumer get(final String consumerKey) {
			try (final Session session = directoryService.open(DIRECTORY)) {
				final DocumentModel doc = session.getEntry(consumerKey);
				if (doc == null) {
					return null;
				}

				final Function<String, String> get = (prop) -> {
					return (String)doc.getProperty(SCHEMA, prop);
				};
				final OAuthConsumer consumer = new OAuthConsumer("about:blank",
						consumerKey, get.apply(SECRET_PROP), null);
				consumer.setProperty(RSA_SHA1.PUBLIC_KEY, get.apply(RSA_PROP));
				consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, get.apply(CERT_PROP));
				return consumer;
			}
		}

		private DocumentModel getOrCreate(final Session session, final String consumerKey) {
			DocumentModel doc = session.getEntry(consumerKey);
			if (doc == null) {
				final Map<String, Object> map = Maps.newHashMap();
				map.put(CONSUMER_KEY_PROP, consumerKey);
				doc = session.createEntry(map);
			}
			return doc;
		}


		@Override
		public void save(final OAuthConsumer consumer) throws IOException {
			final String publicKey = (String)consumer.getProperty(RSA_SHA1.PUBLIC_KEY);
			final String cert = (String)consumer.getProperty(RSA_SHA1.X509_CERTIFICATE);

			try (final Session session = directoryService.open(DIRECTORY)) {
				final DocumentModel doc = getOrCreate(session, consumer.consumerKey);
				final BiConsumer<String, String> set = (name, value) -> {
					doc.setProperty(SCHEMA, name, value);
				};
				set.accept(SECRET_PROP, consumer.consumerSecret);
				set.accept(RSA_PROP, publicKey);
				set.accept(CERT_PROP, cert);
				session.updateEntry(doc);
			}
		}
	}
}
