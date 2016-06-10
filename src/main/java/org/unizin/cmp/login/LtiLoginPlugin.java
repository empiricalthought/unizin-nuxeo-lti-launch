package org.unizin.cmp.login;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.login.BaseLoginModule;

public class LtiLoginPlugin extends BaseLoginModule {

    @Override
    public Boolean initLoginModule() {
        return Boolean.TRUE;
    }

    @Override
    public String validatedUserIdentity(UserIdentificationInfo userIdent) {
        // Validate the user identity
        // return userIdent.getUserName();
        return null;
    }

    // @Override
    // public String getLoginPage() {
    // return super.getLoginPage();
    // }
    //
    // @Override
    // public void setLoginPage(String loginPage) {
    // super.setLoginPage(loginPage);
    // }
    //
    // @Override
    // public Map<String, String> getParameters() {
    // return super.getParameters();
    // }
    //
    // @Override
    // public void setParameters(Map<String, String> parameters) {
    // super.setParameters(parameters);
    // }
    //
    // @Override
    // public String getName() {
    // return super.getName();
    // }
    //
    // @Override
    // public void setName(String pluginName) {
    // super.setName(pluginName);
    // }
    //
    // @Override
    // public String getParameter(String parameterName) {
    // return super.getParameter(parameterName);
    // }
}
