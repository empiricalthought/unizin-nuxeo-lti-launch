# unizin-nuxeo-lti-launch

### Nuxeo Authenticator Plugin and User Mapper

When the request is coming to the server (by order), it goes:

- To a filter: by default [NuxeoAuthenticationFilter](https://github.com/nuxeo/nuxeo/blob/master/nuxeo-services/nuxeo-platform-web-common/src/main/java/org/nuxeo/ecm/platform/ui/web/auth/NuxeoAuthenticationFilter.java) which has to be kept cause its responsible to trigger all the auth contributions. But FYI you can create yours like `org.unizin.cmp.login.LtiWebFilter` contributed in `deployment-fragment.xml` (this one last file let you define a filter and define a URL mapping).

- The `authenticationChain` contributions (in `lti-login-contrib.xml`):

The pre-filters defined for the OAuth authenticator have been disabled and a custom one has been defined [OAuthLTIFilter](https://github.com/unizin/unizin-nuxeo-lti-launch/blob/master/unizin-login-lti-addon/src/main/java/org/unizin/cmp/lti/OAuthLTIFilter.java). It must be completed to provide the require behavior.

The `NuxeoAuthenticationFilter` will use this chain to trigger the login prompt. When authentication is needed, the Filter will first call the `handleRetrieveIdentity` method on all the plugins in the order of the authentication chain. Then, if the authentication could not be achieved, the filter will call the `handleLoginPrompt` method in the same order on all the plugins. The aim is to have as much automatic authentications as possible. That's why all the manual authentications (those needing a prompt) are done in a second round.

See [Authentication and User Management > Core Implementation](https://doc.nuxeo.com/display/NXDOC/Authentication+and+User+Management).


### Open Source

This software is written for and maintained by Unizin. We offer it without
guarantees because it may be useful to your projects. All proposed contributions
to this repository are reviewed by Unizin.
