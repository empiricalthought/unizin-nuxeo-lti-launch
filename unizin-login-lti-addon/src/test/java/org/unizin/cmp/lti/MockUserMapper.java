package org.unizin.cmp.lti;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.extension.UserMapper;

public final class MockUserMapper implements UserMapper {
    private UserManager userManager;

    public static final String USERNAME = "Administrator";

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(final Object userObject) {
        return getOrCreateAndUpdateNuxeoPrincipal(userObject, true, true, null);
    }

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(
            final Object userObject, final boolean createIfNeeded,
            final boolean update, final Map<String, Serializable> params) {
        return userManager.getPrincipal(USERNAME);
    }

    @Override
    public void init(final Map<String, String> params) throws Exception {
        userManager = Framework.getService(UserManager.class);
    }

    @Override
    public Object wrapNuxeoPrincipal(final NuxeoPrincipal principal,
            final Object nativePrincipal,
            final Map<String, Serializable> params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release() {
    }
}
