package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetUtf8JsonAcceptHeadersForResourceEndpointRequest;
import net.openid.conformance.condition.common.CheckForBCP195InsecureFAPICiphers;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
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

		Once the initial credential has been obtained, additional requests with FAPI specific parameters are sent to the credential endpoint.
		""",
	profile = "OID4VCI-1_0",
	configurationFields = {
		"vci.credential_issuer_url",
		"client.client_id",
		"client.jwks",
		"vci.credential_configuration_id",
		"vci.credential_proof_type_hint",
		"vci.key_attestation_jwks",
		"vci.authorization_server",
	}
)
public class VCIIssuerHappyFlowAdditionalRequests extends AbstractVCIIssuerTestModule {

	protected void checkResourceEndpointTLS() {
		eventLog.startBlock("Resource endpoint TLS test");
		env.mapKey("tls", "resource_endpoint_tls");
		checkEndpointTLS();
		env.unmapKey("tls");
		eventLog.endBlock();
	}

	protected void checkEndpointTLS() {
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.3-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.2.1");
		callAndContinueOnFailure(CheckForBCP195InsecureFAPICiphers.class, Condition.ConditionResult.WARNING, "FAPI1-ADV-8.5", "RFC9325A-A", "RFC9325-4.2");
	}

	@Override
	protected void requestProtectedResource() {

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
		checkResourceEndpointTLS();

		super.requestProtectedResource();

		performAdditionalResourceEndpointTests();

		fireTestFinished();
	}

	protected void performAdditionalResourceEndpointTests() {

		eventLog.startBlock("Attempt to credential resource endpoint IPv6 customer IP-Address");
		refreshCredentialRequest();
		updateResourceRequest();
		callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");

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
