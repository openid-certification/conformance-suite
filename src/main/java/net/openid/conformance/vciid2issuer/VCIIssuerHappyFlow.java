package net.openid.conformance.vciid2issuer;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainNonRequestedClaims;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromOBResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetUtf8JsonAcceptHeadersForResourceEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2ID2OPProfile;

@PublishTestModule(
		testName = "oid4vci-id2-issuer-happy-flow",
		displayName = "OID4VCIID2: Issuer happy flow",
		summary = "This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by retrieving metadata from both the Credential Issuer and the OAuth 2.0 Authorization Server. An authorization request is initiated using Pushed Authorization Requests (PAR), and an access token is obtained. The test then retrieves a nonce from the Credential Endpoint, constructs a DPoP proof JWT bound to the nonce, and successfully requests a credential from the Credential Endpoint.",
		profile = "OID4VCI-ID2",
		configurationFields = {
			"server.discoveryIssuer",
			"client.client_id",
			"client.jwks",
			"mtls.key",
			"mtls.cert",
			"mtls.ca",
			"client2.client_id",
			"client2.jwks",
			"mtls2.key",
			"mtls2.cert",
			"mtls2.ca",
			"vci.credential_configuration_id",
			"vci.authorization_server",
			"client_attestation.issuer"
		}
	)
public class VCIIssuerHappyFlow extends AbstractVCIIssuerMultipleClient {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		if (isOpenId) {
			Command cmd = new Command();

			if (isSecondClient()) {
				cmd.putInteger("requested_nonce_length", 43);
			}
			else {
				cmd.removeNativeValue("requested_nonce_length");
			}

			ConditionSequence conditionSequence = super.makeCreateAuthorizationRequestSteps()
				.insertBefore(CreateRandomNonceValue.class, cmd);

			return conditionSequence;
		}

		return super.makeCreateAuthorizationRequestSteps();
	}

	@Override
	protected void performAuthorizationFlowWithSecondClient() {
		// NOOP
	}

	protected void performAdditionalResourceEndpointTests() {
		updateResourceRequest();
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.4-2");

		updateResourceRequest();
		callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");
		if (getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.CONSUMERDATARIGHT_AU) {
			// CDR requires this header when the x-fapi-customer-ip-address header is present
			callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
		}

		callAndStopOnFailure(SetUtf8JsonAcceptHeadersForResourceEndpointRequest.class);


		if (isDpop() ) {
			requestProtectedResourceUsingDpop();
		} else  {
			callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		updateResourceRequest();
		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);
		if (isDpop() ) {
			requestProtectedResourceUsingDpop();
		} else  {
			callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
		}
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);
	}

	@Override
	protected void requestProtectedResource() {

		if (!isSecondClient()) {
			if (getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.OPENBANKING_UK ||
				getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
				callAndStopOnFailure(ExtractTLSTestValuesFromOBResourceConfiguration.class);
			} else {
				callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
			}
		}

		super.requestProtectedResource();

		fireTestFinished();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {

		if (isOpenId && !isSecondClient()) {
			callAndContinueOnFailure(EnsureIdTokenDoesNotContainNonRequestedClaims.class, Condition.ConditionResult.WARNING);
		}

		super.onPostAuthorizationFlowComplete();
	}
}
