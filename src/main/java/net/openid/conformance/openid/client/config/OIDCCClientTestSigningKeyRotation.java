package net.openid.conformance.openid.client.config;

import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientRegistration;

import jakarta.servlet.http.HttpServletResponse;


@PublishTestModule(
	testName = "oidcc-client-test-signing-key-rotation",
	displayName = "OIDCC: Relying party signing key rotation test",
	summary = "The client is expected to request an ID token and verify its signature by" +
		" fetching keys from the jwks endpoint. " +
		" Then make a new authentication request and retrieve another ID Token and verify its signature." +
		" Keys will be rotated after the first ID token is issued so the client needs to refetch the jwks to validate " +
		"the second ID token." +
		"Corresponds to rp-key-rotation-op-sign-key test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestSigningKeyRotation extends AbstractOIDCCClientTest {
	protected boolean receivedSecondJwksRequest;
	protected boolean receivedSecondAuthorizationRequest;
	protected boolean receivedSecondTokenRequest;
	protected boolean receivedSecondUserinfoRequest;

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		boolean fireTestFinishedCalled = false;
		switch (responseType) {
			case CODE:
				if(receivedSecondUserinfoRequest && receivedSecondJwksRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case CODE_ID_TOKEN:
				if(receivedSecondUserinfoRequest && receivedSecondJwksRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case ID_TOKEN:
				if(receivedSecondAuthorizationRequest && receivedSecondJwksRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case CODE_TOKEN:
				if(receivedSecondUserinfoRequest && receivedSecondJwksRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case CODE_ID_TOKEN_TOKEN:
				if(receivedSecondUserinfoRequest && receivedSecondJwksRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case ID_TOKEN_TOKEN:
				if(receivedSecondUserinfoRequest && receivedSecondJwksRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
		}
		return fireTestFinishedCalled;
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse) {
		if (path.equals("authorize")) {
			if(receivedAuthorizationRequest) {
				env.removeObject("client_authentication");
				receivedSecondAuthorizationRequest = true;
				super.configureServerJWKS();
			} else {
				receivedAuthorizationRequest = true;
			}
			return handleAuthorizationEndpointRequest(requestId);

		} else if (path.equals("token")) {
			if(receivedTokenRequest) {
				receivedSecondTokenRequest = true;
			} else {
				receivedTokenRequest = true;
			}
			return handleTokenEndpointRequest(requestId);

		} else if (path.equals(getJwksPath())) {
			if(receivedJwksRequest) {
				receivedSecondJwksRequest = true;
			} else {
				receivedJwksRequest = true;
			}
			return handleJwksEndpointRequest();

		} else if (path.equals("userinfo")) {
			if(receivedUserinfoRequest) {
				receivedSecondUserinfoRequest = true;
			} else {
				receivedUserinfoRequest = true;
			}
			return handleUserinfoEndpointRequest(requestId);

		} else if (path.equals("register") && clientRegistrationType == ClientRegistration.DYNAMIC_CLIENT) {
			receivedRegistrationRequest = true;
			return handleRegistrationEndpointRequest(requestId);

		} else if (path.equals(".well-known/openid-configuration")) {
			receivedDiscoveryRequest = true;
			return handleDiscoveryEndpointRequest();

		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
	}

	@Override
	protected void signIdToken() {
		super.signIdToken();
	}

	@Override
	protected String getAuthorizationEndpointBlockText() {
		if(receivedSecondAuthorizationRequest) {
			return "Second Authorization Request";
		}
		return super.getAuthorizationEndpointBlockText();
	}
}
