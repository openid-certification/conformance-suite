package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddDpopJktToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointReorderedParams;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainNonRequestedClaims;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromOBResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetUtf8JsonAcceptHeadersForResourceEndpointRequest;
import net.openid.conformance.condition.common.CheckForBCP195InsecureFAPICiphers;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12RequireBCP195Ciphers;
import net.openid.conformance.condition.common.RequireOnlyBCP195RecommendedCiphersForTLS12;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;

@PublishTestModule(
		testName = "fapi2-security-profile-final-happy-flow",
		displayName = "FAPI2-Security-Profile-Final: Happy flow",
		summary = "Tests primarily 'happy' flows, using two different OAuth2 clients (and hence authenticating the user twice), and uses different variations on request objects, registered redirect uri (both redirect uris must be pre-registered as shown in the instructions). It also tests that sender constrained access tokens (required by the FAPI spec) are correctly implemented.",
		profile = "FAPI2-Security-Profile-Final"
	)
public class FAPI2SPFinalHappyFlow extends AbstractFAPI2SPFinalMultipleClient {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		call(profileBehavior.onConfigure());
		call(profileBehavior.validateDiscoveryEndpointScopes());
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		ConditionSequence sequence;

		if (isOpenId) {
			Command cmd = new Command();

			if (isSecondClient()) {
				cmd.putInteger("requested_nonce_length", 43);
			}
			else {
				cmd.removeNativeValue("requested_nonce_length");
			}

			sequence = super.makeCreateAuthorizationRequestSteps()
				.insertBefore(CreateRandomNonceValue.class, cmd);
		} else {
			sequence = super.makeCreateAuthorizationRequestSteps();
		}

		// On the second client, exercise RFC 9449 §10.1's dpop_jkt-only PAR shape
		// (no DPoP header, just the form param) — not otherwise covered in the happy flow.
		if (isSecondClient() && isDpop()) {
			sequence = sequence
				.then(condition(GenerateDpopKey.class))
				.then(condition(AddDpopJktToAuthorizationEndpointRequest.class));
		}

		return sequence;
	}

	protected void checkResourceEndpointTLS() {
		eventLog.startBlock("Resource endpoint TLS test");
		env.mapKey("tls", "resource_endpoint_tls");
		checkEndpointTLS();
		env.unmapKey("tls");
		eventLog.endBlock();
	}

	protected void checkAccountRequestEndpointTLS() {
		eventLog.startBlock("Accounts request endpoint TLS test");
		env.mapKey("tls", "accounts_request_endpoint_tls");
		checkEndpointTLS();
		env.unmapKey("tls");
		eventLog.endBlock();
	}

	protected void checkAccountResourceEndpointTLS() {
		eventLog.startBlock("Accounts resource endpoint TLS test");
		env.mapKey("tls", "accounts_resource_endpoint_tls");
		checkEndpointTLS();
		env.unmapKey("tls");
		eventLog.endBlock();
	}

	protected void checkEndpointTLS() {
		callAndContinueOnFailure(EnsureTLS12RequireBCP195Ciphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.3-2", "FAPI-ISSUES-847");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
		callAndContinueOnFailure(RequireOnlyBCP195RecommendedCiphersForTLS12.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.2", "FAPI-ISSUES-847");
		callAndContinueOnFailure(CheckForBCP195InsecureFAPICiphers.class, Condition.ConditionResult.WARNING, "FAPI2-SP-FINAL-5.2.2", "RFC9325A-A", "RFC9325-4.2");
	}

	protected void performAdditionalResourceEndpointTests() {
		updateResourceRequest();
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.4-2");

		updateResourceRequest();
		callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");
		if (getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.CONSUMERDATARIGHT_AU) {
			// CDR requires this header when the x-fapi-customer-ip-address header is present
			callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
		}
		// try different, valid accept headers to verify server accepts them
		ConditionSequence alternateHeaders = profileBehavior.setAlternateResourceEndpointContentHeaders();
		if (alternateHeaders != null) {
			call(alternateHeaders);
		} else {
			callAndStopOnFailure(SetUtf8JsonAcceptHeadersForResourceEndpointRequest.class);
		}

		if (isDpop() ) {
			requestProtectedResourceUsingDpop();
		} else  {
			callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		call(profileBehavior.validateResourceEndpointResponse());

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
		call(profileBehavior.validateResourceEndpointResponse());

		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);
	}

	@Override
	protected void requestProtectedResource() {

		if (!isSecondClient()) {
			if (getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.OPENBANKING_UK ||
				getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.OPENBANKING_BRAZIL) {
				callAndStopOnFailure(ExtractTLSTestValuesFromOBResourceConfiguration.class);
				checkAccountRequestEndpointTLS();
				checkAccountResourceEndpointTLS();
			} else {
				callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
				checkResourceEndpointTLS();
			}
		}

		super.requestProtectedResource();

		if (!isSecondClient()) {
			performAdditionalResourceEndpointTests();
		}
	}

	@Override
	protected void performPARRedirectWithRequestUri() {
		if (isSecondClient()) {
			eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
			callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointReorderedParams.class, "PAR-4");
			performRedirect();
		} else {
			super.performPARRedirectWithRequestUri();
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {

		if (isOpenId && !isSecondClient()) {
			callAndContinueOnFailure(EnsureIdTokenDoesNotContainNonRequestedClaims.class, Condition.ConditionResult.WARNING);
		}

		super.onPostAuthorizationFlowComplete();
	}
}
