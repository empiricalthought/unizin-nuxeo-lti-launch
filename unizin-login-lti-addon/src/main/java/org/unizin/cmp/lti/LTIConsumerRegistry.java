package org.unizin.cmp.lti;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
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
        public void activate(final ComponentContext context) {
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
                final Function<String, byte[]> bytes = (str) -> {
                    return (str == null) ? null : Base64.getDecoder().decode(str);
                };

                final OAuthConsumer consumer = new OAuthConsumer("about:blank",
                        consumerKey, get.apply(SECRET_PROP), null);
                consumer.setProperty(RSA_SHA1.PUBLIC_KEY,
                        bytes.apply(get.apply(RSA_PROP)));
                consumer.setProperty(RSA_SHA1.X509_CERTIFICATE,
                        bytes.apply(get.apply(CERT_PROP)));
                return consumer;
            }
        }


        private static DocumentModel getOrCreate(final Session session,
                final String consumerKey) {
            DocumentModel doc = session.getEntry(consumerKey);
            if (doc == null) {
                final Map<String, Object> map = Maps.newHashMap();
                map.put(CONSUMER_KEY_PROP, consumerKey);
                doc = session.createEntry(map);
            }
            return doc;
        }


        private static String convertKey(final Object publicKey) throws IOException {
            if (publicKey == null) {
                return null;
            }
            final Function<byte[], String> str = (b) -> Base64.getEncoder().encodeToString(b);

            if (publicKey instanceof PublicKey) {
                return str.apply(((PublicKey)publicKey).getEncoded());
            }
            if (publicKey instanceof byte[]) {
                return str.apply((byte[])publicKey);
            }
            if (publicKey instanceof String) {
                return (String)publicKey;
            }
            throw new IOException("Invalid public key type: " + publicKey.getClass());
        }


        @Override
        public void save(final OAuthConsumer consumer) throws IOException {
            final Object publicKey = consumer.getProperty(RSA_SHA1.PUBLIC_KEY);
            final Object cert = consumer.getProperty(RSA_SHA1.X509_CERTIFICATE);

            try (final Session session = directoryService.open(DIRECTORY)) {
                final DocumentModel doc = getOrCreate(session, consumer.consumerKey);
                final BiConsumer<String, String> set = (name, value) -> {
                    doc.setProperty(SCHEMA, name, value);
                };
                set.accept(SECRET_PROP, consumer.consumerSecret);
                set.accept(RSA_PROP, convertKey(publicKey));
                set.accept(CERT_PROP, convertKey(cert));
                session.updateEntry(doc);
            }
        }
    }
}
