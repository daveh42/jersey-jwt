package com.cassiomolin.example.security.api.filter;

import com.cassiomolin.example.security.api.AuthenticatedUserDetails;
import com.cassiomolin.example.security.api.AuthenticationTokenDetails;
import com.cassiomolin.example.security.api.TokenBasedSecurityContext;
import com.cassiomolin.example.security.service.AuthenticationTokenService;
import com.cassiomolin.example.user.domain.User;
import com.cassiomolin.example.user.service.UserService;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * JWT authentication filter.
 *
 * @author cassiomolin
 *
 * In this example, non-annotated methods require authentication.
 *
 * Both AuthenticationFilter and AuthorizationFilter are global and will be executed for each resource method:
 *
 * The AuthenticationFilter will attempt to authenticate the user for each request. This filter will just tell who the user 
 * is (can be an anonymous user, for example). Then such filter will set a SecurityContext for the request (see the code).
 *
 * Then the AuthorizationFilter will authorize the request. As part of the authorization process, anonymous users cannot 
 * execute non-annotated methods (see the code).
 *
 */
@Provider
@Dependent
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @Inject
    private UserService userService;

    @Inject
    private AuthenticationTokenService authenticationTokenService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String authenticationToken = authorizationHeader.substring(7);
            handleTokenBasedAuthentication(authenticationToken, requestContext);
            return;
        }

        // Other authentication schemes (such as Basic) could be supported
    }

    private void handleTokenBasedAuthentication(String authenticationToken, ContainerRequestContext requestContext) {

        AuthenticationTokenDetails authenticationTokenDetails = authenticationTokenService.parseToken(authenticationToken);
        User user = userService.findByUsernameOrEmail(authenticationTokenDetails.getUsername());
        AuthenticatedUserDetails authenticatedUserDetails = new AuthenticatedUserDetails(user.getUsername(), user.getAuthorities());

        boolean isSecure = requestContext.getSecurityContext().isSecure();
        SecurityContext securityContext = new TokenBasedSecurityContext(authenticatedUserDetails, authenticationTokenDetails, isSecure);
        requestContext.setSecurityContext(securityContext);
    }
}
