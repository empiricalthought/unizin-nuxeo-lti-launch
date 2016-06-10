# unizin-nuxeo-lti-launch

###Nuxeo Authenticator Plugin and User Mapper

When the request is coming to the server (by order), it goes:

- To a filter: by default [NuxeoAuthenticationFilter](https://github.com/nuxeo/nuxeo/blob/master/nuxeo-services/nuxeo-platform-web-common/src/main/java/org/nuxeo/ecm/platform/ui/web/auth/NuxeoAuthenticationFilter.java) which has to be kept cause its responsible to trigger all the auth contributions. But FYI you can create yours like `org.unizin.cmp.login.LtiWebFilter` contributed in `deployment-fragment.xml` (this one last file let you define a filter and define a URL mapping).

- The `authenticationChain` contributions (in `lti-login-contrib.xml`):

The `NuxeoAuthenticationFilter` will use this chain to trigger the login prompt. When authentication is needed, the Filter will first call the `handleRetrieveIdentity` method on all the plugins in the order of the authentication chain. Then, if the authentication could not be achieved, the filter will call the `handleLoginPrompt` method in the same order on all the plugins. The aim is to have as much automatic authentications as possible. That's why all the manual authentications (those needing a prompt) are done in a second round.

- `org.unizin.cmp.login.LtiAuthenticator`: Main auth configuration. This is where all the business auth logic should be implemented but if you need another guard, there is

-> A login Plugin:

- `org.unizin.cmp.login.LtiLoginPlugin` can be used instead of `Trusting_LM` module to validate a user afterward.

- `org.unizin.cmp.login.LtiUserMapper`: It's used in case you would like to have a hook on the other authentication configuration (Basic, OAuth etc...). If you don't have to hook on another auth plugin than your own, the implementation of the user mapper can be done inside the LtiAuthenticator.

Don't hesitate again to read [Authentication and User Management > Core Implementation](https://doc.nuxeo.com/display/NXDOC/Authentication+and+User+Management), with the schemas giving you the big picture (in complement with the above explanations).