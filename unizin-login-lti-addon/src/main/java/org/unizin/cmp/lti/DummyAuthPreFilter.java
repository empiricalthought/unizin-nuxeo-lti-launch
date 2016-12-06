package org.unizin.cmp.lti;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;

public class DummyAuthPreFilter implements NuxeoAuthPreFilter {
    @Override
    public void doFilter(final ServletRequest req,
                         final ServletResponse resp,
                         final FilterChain chain)
        throws IOException, ServletException {
        chain.doFilter(req, resp);
    }

}
