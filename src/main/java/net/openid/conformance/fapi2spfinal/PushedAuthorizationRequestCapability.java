package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.AddPARToServerConfiguration;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.endpoint.Channel;
import net.openid.conformance.testmodule.endpoint.Endpoint;
import net.openid.conformance.testmodule.endpoint.EndpointRequest;

/**
 * Pushed Authorization Requests (RFC 9126) as a capability of the emulated authorization server:
 * everything PAR adds to the server is declared here.
 * <ul>
 *   <li>the PAR endpoint, on the regular and the mTLS channel;</li>
 *   <li>its server metadata ({@link #serverConfigurationSteps()});</li>
 *   <li>the checks the authorization endpoint applies because the request was pushed
 *       ({@link #authorizationRequestChecks()}).</li>
 * </ul>
 *
 * <p>The module registers the endpoints only when {@link #isEnabled()}. Whether PAR is enabled comes
 * from the profile, never from what the client does, so a profile that mandates PAR fails a client
 * that skips it rather than skipping the checks.
 */
public class PushedAuthorizationRequestCapability {

	protected final AbstractFAPI2SPFinalClientTest module;

	public PushedAuthorizationRequestCapability(AbstractFAPI2SPFinalClientTest module) {
		this.module = module;
	}

	public boolean isEnabled() {
		return module.profileBehavior.shouldRegisterPAREndpoint();
	}

	/**
	 * Adds {@code pushed_authorization_request_endpoint} and {@code require_pushed_authorization_requests}
	 * to the server configuration.
	 */
	public Class<? extends ConditionSequence> serverConfigurationSteps() {
		return AddPARToServerConfiguration.class;
	}

	/**
	 * Checks the authorization endpoint applies to a request whose parameters were pushed: the front-channel
	 * request carries only the client_id and request_uri.
	 */
	public ConditionSequence authorizationRequestChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(stopOnFailure(EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR.class));
				call(continueOnFailure(EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR.class)
					.requirements("PAR-4", "FAPI2-SP-FINAL-5.3.3.2-6"));
			}
		};
	}

	@RefusedAfterShutdown
	@Endpoint(path = "par")
	public Object par(EndpointRequest request) {
		if (module.parEndpointRequiresMtls()) {
			throw new TestFailureException(module.getId(), "The PAR endpoint must be called over an mTLS secured connection " +
				"using the pushed_authorization_request_endpoint found in mtls_endpoint_aliases.");
		}
		if (module.clientMustUseMtlsAlias("pushed_authorization_request_endpoint")) {
			throw new TestFailureException(module.getId(), "invalid_client",
				"The PAR endpoint must be called over an mTLS secured connection because use_mtls_endpoint_aliases is set in the client configuration.");
		}
		return module.parEndpoint(request.requestId());
	}

	@Endpoint(path = "par", channels = Channel.MTLS)
	public Object parOverMtls(EndpointRequest request) {
		if (!module.parEndpointRequiresMtls() && !module.clientRequestsMtlsEndpointAliases()) {
			throw new TestFailureException(module.getId(), "invalid_client",
				"The PAR endpoint was called over an mTLS secured connection, but this is not " +
				"expected when using " + module.clientAuthType + " client authentication without " +
				"use_mtls_endpoint_aliases. The regular (non-mTLS) " +
				"pushed_authorization_request_endpoint must be used.");
		}
		return module.parEndpoint(request.requestId());
	}
}
