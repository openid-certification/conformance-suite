package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-client-id-in-token-endpoint",
	displayName = "FAPI-RW-ID2: ensure client_id in token endpoint",
	summary = "Send client_id for the second client to the token endpoint, which should result in the token endpoint returning an error message that the client is invalid. Note that you must configure the second client to use client credentials that are not equivalent to those for the first client - e.g. if using tls_client_auth_subject_dn, the certificates must have different subject distinguished names.",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPIRWID2EnsureClientIdInTokenEndpoint extends AbstractFAPIRWID2PerformTokenEndpoint {

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {

		// Switch to client 2 client
		eventLog.startBlock("Swapping to Client2");
		env.mapKey("client", "client2");

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class, "FAPI-R-5.2.2-19");

		// For this test, we explicitly add the client ID - so don't do it twice
		if (getVariant(ClientAuthType.class) != ClientAuthType.MTLS) {
			super.addClientAuthenticationToTokenEndpointRequest();
		}
	}

	@Override
	protected void processTokenEndpointResponse() {
		/* This test ends up using an authorization code for client1.
		 * For MTLS, it passes the client_id for client2 but the tls cert for client 1.
		 * For private_key_jwt, it passes the client_id and a client_assertion for client 2, but signed
		 * using client1's jwk.
		 *
		 * If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error (assuming the client authentication was checked first)
		 * - It must be a 'invalid_grant' error (assuming the check for the client_id matching the authorization code is performed first).
		 * The specs don't appear to define an order for these two checks. It may have been preferable for this test to only trigger one possible error.
		 */
		callAndContinueOnFailure(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");

		fireTestFinished();
	}
}
