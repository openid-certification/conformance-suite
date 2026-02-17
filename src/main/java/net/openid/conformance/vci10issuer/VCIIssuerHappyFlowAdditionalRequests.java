package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetUtf8JsonAcceptHeadersForResourceEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-happy-flow-additional-requests",
	displayName = "OID4VCI 1.0: Issuer happy flow with additional requests",
	summary = """
	This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID
	for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by retrieving metadata from both the
	Credential Issuer and the OAuth 2.0 Authorization Server. An authorization request is initiated using
	Pushed Authorization Requests (PAR), and an access token is obtained.

	The test then retrieves a nonce from the Credential Endpoint, constructs a DPoP proof JWT bound to the nonce,
	and successfully requests a credential from the Credential Endpoint.

	Once the initial credential has been obtained, additional requests are sent to the credential endpoint.
	""",
	profile = "OID4VCI-1_0",
	configurationFields = {
		"vci.credential_issuer_url",
		"client.client_id",
		"client.jwks",
		"client2.client_id",
		"client2.jwks",
		"vci.credential_configuration_id",
		"vci.credential_proof_type_hint",
		"vci.key_attestation_jwks",
		"vci.authorization_server",
	}
)
public class VCIIssuerHappyFlowAdditionalRequests extends VCIIssuerHappyFlow {

	@Override
	protected void performAdditionalResourceEndpointTests() {

		eventLog.startBlock("Attempt to credential resource endpoint IPv6 customer IP-Address");
		refreshCredentialRequest();
		updateResourceRequest();
		callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");
//		if (getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.CONSUMERDATARIGHT_AU) {
//			// CDR requires this header when the x-fapi-customer-ip-address header is present
//			callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
//		}

		callAndStopOnFailure(SetUtf8JsonAcceptHeadersForResourceEndpointRequest.class);

		if (isDpop()) {
			requestProtectedResourceUsingDpop();
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		eventLog.startBlock("Attempt to credential resource endpoint with permissive accept header");
		refreshCredentialRequest();
		updateResourceRequest();
		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);
		if (isDpop()) {
			requestProtectedResourceUsingDpop();
		} else {
			callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
		}
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);
	}
}
