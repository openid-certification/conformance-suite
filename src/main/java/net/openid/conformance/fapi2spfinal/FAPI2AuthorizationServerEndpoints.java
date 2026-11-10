package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.endpoint.Channel;
import net.openid.conformance.testmodule.endpoint.Endpoint;
import net.openid.conformance.testmodule.endpoint.EndpointRequest;
import net.openid.conformance.testmodule.endpoint.WellKnown;
import net.openid.conformance.variant.ClientAuthType;

/**
 * The core endpoints of the authorization server and resource server that
 * {@link AbstractFAPI2SPFinalClientTest} emulates. Optional features such as PAR are separate
 * capabilities ({@link PushedAuthorizationRequestCapability}) registered next to this class.
 *
 * <p>Each handler checks that the endpoint was called over the channel the test configuration
 * expects, then delegates to the module's endpoint method, so test modules that override those
 * methods keep working.
 */
public class FAPI2AuthorizationServerEndpoints {

	protected final AbstractFAPI2SPFinalClientTest module;

	public FAPI2AuthorizationServerEndpoints(AbstractFAPI2SPFinalClientTest module) {
		this.module = module;
	}

	@RefusedAfterShutdown
	@Endpoint(path = "authorize")
	public Object authorize(EndpointRequest request) {
		return module.authorizationEndpoint(request.requestId());
	}

	@RefusedAfterShutdown
	@Endpoint(path = "token")
	public Object token(EndpointRequest request) {
		if (module.tokenEndpointRequiresMtls()) {
			throw new TestFailureException(module.getId(), "invalid_client",
				"The token endpoint must be called over an mTLS secured connection using the token_endpoint found in mtls_endpoint_aliases.");
		}
		if (module.clientMustUseMtlsAlias("token_endpoint")) {
			throw new TestFailureException(module.getId(), "invalid_client",
				"The token endpoint must be called over an mTLS secured connection because use_mtls_endpoint_aliases is set in the client configuration.");
		}
		return module.tokenEndpoint(request.requestId());
	}

	@Endpoint(path = "token", channels = Channel.MTLS)
	public Object tokenOverMtls(EndpointRequest request) {
		if (module.clientAuthType != ClientAuthType.MTLS && !module.isMTLSConstrain() && !module.profileRequiresMtlsEverywhere
				&& !module.clientRequestsMtlsEndpointAliases()) {
			throw new TestFailureException(module.getId(), "invalid_client",
				"The token endpoint was called over an mTLS secured connection, but this is not " +
				"expected when using " + module.clientAuthType + " client authentication with " +
				module.fapi2SenderConstrainMethod + " sender-constraining and without " +
				"use_mtls_endpoint_aliases. The regular (non-mTLS) token_endpoint must be used.");
		}
		return module.tokenEndpoint(request.requestId());
	}

	@Endpoint(path = "jwks")
	public Object jwks() {
		return module.jwksEndpoint();
	}

	@RefusedAfterShutdown
	@Endpoint(path = "userinfo")
	public Object userinfo(EndpointRequest request) {
		if (module.userinfoEndpointRequiresMtls()) {
			throw new TestFailureException(module.getId(), "The userinfo endpoint must be called over an mTLS secured connection.");
		}
		return module.userinfoEndpoint(request.requestId());
	}

	@Endpoint(path = "userinfo", channels = Channel.MTLS)
	public Object userinfoOverMtls(EndpointRequest request) {
		return module.userinfoEndpoint(request.requestId());
	}

	@WellKnown("oauth-authorization-server")
	@Endpoint(path = ".well-known/openid-configuration")
	public Object discovery() {
		return module.discoveryEndpoint();
	}

	@RefusedAfterShutdown
	@Endpoint(path = AbstractFAPI2SPFinalClientTest.ACCOUNTS_PATH)
	public Object accounts(EndpointRequest request) {
		if (module.accountsEndpointRequiresMtls()) {
			throw new TestFailureException(module.getId(), "The accounts endpoint must be called over an mTLS secured connection.");
		}
		return module.accountsEndpoint(request.requestId());
	}

	@Endpoint(path = AbstractFAPI2SPFinalClientTest.ACCOUNTS_PATH, channels = Channel.MTLS)
	public Object accountsOverMtls(EndpointRequest request) {
		return accountsOverMtlsChecked(request);
	}

	@Endpoint(path = FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH, channels = Channel.MTLS)
	public Object brazilAccountsOverMtls(EndpointRequest request) {
		return accountsOverMtlsChecked(request);
	}

	private Object accountsOverMtlsChecked(EndpointRequest request) {
		if (!module.accountsEndpointRequiresMtls()) {
			throw new TestFailureException(module.getId(), "The accounts endpoint must not be called over an mTLS secured connection.");
		}
		return module.accountsEndpoint(request.requestId());
	}
}
