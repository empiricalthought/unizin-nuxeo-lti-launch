package org.unizin.cmp.lti;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.usermapper.extension.AbstractUserMapper;

import java.io.Serializable;
import java.util.Map;

public class LtiUserMapper extends AbstractUserMapper {

    @Override
    protected void resolveAttributes(Object userObject, Map<String, Serializable> searchAttributes, Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes) {

    }

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object nativePrincipal, Map<String, Serializable> params) {
        return null;
    }

    @Override
    public void init(Map<String, String> params) throws Exception {

    }

    @Override
    public void release() {

    }
}
