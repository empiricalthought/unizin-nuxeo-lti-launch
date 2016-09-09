package org.unizin.cmp.lti;

import static org.unizin.cmp.lti.LTIRequests.isLTILaunch;
import static org.unizin.cmp.lti.LTIRequests.validate;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.usermapper.service.UserMapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import net.oauth.OAuth.Problems;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

/**
 * Authentication pre-filter used to replace the one defined by the OAuth addon.
 *
 * @since 1.0.17
 */
public class OAuthLTIFilter implements NuxeoAuthPreFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthLTIFilter.class);
    private static final OAuthValidator VALIDATOR = new SimpleOAuthValidator();

    /**
     * Name of the session attribute containing the LTI launch parameters.
     * These are stored as a map of parameter key-value pairs.
     */
    public static final String LAUNCH_PARAMS_ATTRIB = "ltiLaunchParams";


    @Override
    public void doFilter(final ServletRequest req,
            final ServletResponse resp,
            final FilterChain chain)
                    throws IOException, ServletException {
        boolean lti = false;
        if (req instanceof HttpServletRequest &&
                resp instanceof HttpServletResponse) {
            final HttpServletRequest request = (HttpServletRequest)req;
            final HttpServletResponse response = (HttpServletResponse)resp;
            if (isLTILaunch(request)) {
                lti = true;
                try {
                    final Principal principal = doFilter(request, response);
                    chain.doFilter(new NuxeoSecuredRequestWrapper(request, principal), resp);
                } catch (final OAuthProblemException e) {
                    response.sendError(Problems.TO_HTTP_CODE.get(e.getProblem()), e.getMessage());
                }
            }
        }
        // Not an LTI launch request. Let something else try to handle it.
        if (! lti) {
            chain.doFilter(req, resp);
        }
    }


    private Principal doFilter(final HttpServletRequest request,
            final HttpServletResponse response)
                    throws IOException, OAuthProblemException {
        final String url = LTIRequests.requestURL(request);
        final OAuthMessage message = OAuthServlet.getMessage(request, url);
        LOGGER.debug("OAuth message received: {}", message);

        // Accesses database to look up OAuth secrets and keys, and possibly to
        // find or create users, so needs a transaction.
        boolean startedTx = false;
        if (!TransactionHelper.isTransactionActive()) {
            startedTx = TransactionHelper.startTransaction();
        }
        boolean done = false;
        done = true;
        try {
            return doFilter(message, request, response);
        } finally {
            if (startedTx) {
                if (done == false) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }


    private static Principal principal(final Map<String, String> userObject) {
        final UserMapperService ums = Framework.getService(UserMapperService.class);
        return ums.getOrCreateAndUpdateNuxeoPrincipal("LTI", userObject);
    }

    private static Map<String, String> parameters(final HttpServletRequest request) {
        final Map<String, String> m = Maps.newTreeMap(); // Ordered => easier debugging.
        for (final String param : Collections.list(request.getParameterNames())) {
            m.put(param, request.getParameter(param));
        }
        return m;
    }

    private Principal doFilter(final OAuthMessage message,
            final HttpServletRequest request,
            final HttpServletResponse response)
                    throws IOException, OAuthProblemException {
        // message methods are documented to throw IOException but never will.
        final String consumerKey = message.getConsumerKey();
        final OAuthConsumer consumer = Framework.getService(
                LTIConsumerRegistry.class).get(consumerKey);
        if (consumer == null) {
            LOGGER.debug("Consumer key {} unknown.", consumerKey);
            throw new OAuthProblemException(Problems.CONSUMER_KEY_UNKNOWN);
        }
        validate(VALIDATOR, message, consumer, LOGGER);
        final Map<String, String> parameters = parameters(request);
        final Principal principal = principal(parameters);
        if (principal == null) {
            throw new OAuthProblemException(Problems.USER_REFUSED);
        }
        try {
            NuxeoAuthenticationFilter.loginAs(principal.getName());
            final HttpSession session = request.getSession();
            session.setAttribute(LAUNCH_PARAMS_ATTRIB, parameters);
            return principal;
        } catch (final LoginException e) {
            // Login failed. Follow NuxeoOAuthFilter's lead and send this back.
            throw new OAuthProblemException(Problems.SIGNATURE_INVALID);
        }
    }
}
