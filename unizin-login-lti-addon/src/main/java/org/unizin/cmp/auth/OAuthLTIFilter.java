package org.unizin.cmp.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;

/**
 * Authentication pre-filter used to replace the one defined by the OAuth addon.
 *
 * @since 1.0.17
 */
public class OAuthLTIFilter implements NuxeoAuthPreFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }

}
