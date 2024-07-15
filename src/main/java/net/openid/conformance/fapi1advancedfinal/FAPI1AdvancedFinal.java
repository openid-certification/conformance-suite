package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainNonRequestedClaims;
import net.openid.conformance.condition.client.FAPIBrazilCheckDirectoryKeystore;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointScopesSupportedForNonPayments;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointScopesSupportedForPayments;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentInitiationRequest;
import net.openid.conformance.condition.client.SetApplicationJwtCharsetUtf8AcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtCharsetUtf8ContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetUtf8JsonAcceptHeadersForResourceEndpointRequest;
import net.openid.conformance.condition.common.CheckForBCP195InsecureFAPICiphers;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

@PublishTestModule(
		testName = "fapi1-advanced-final",
		displayName = "FAPI1-Advanced-Final",
		summary = "Tests primarily 'happy' flows, using two different OAuth2 clients (and hence authenticating the user twice), and uses different variations on request objects, registered redirect uri (both redirect uris must be pre-registered as shown in the instructions). It also tests that TLS Certificate-Bound access tokens (required by the FAPI-RW spec) are correctly implemented.",
		profile = "FAPI1-Advanced-Final",
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
public class FAPI1AdvancedFinal extends AbstractFAPI1AdvancedFinalMultipleClient {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		if (isBrazil.isTrue()) {
			if (brazilPayments.isTrue()) {
				callAndContinueOnFailure(FAPIBrazilCheckDirectoryKeystore.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointScopesSupportedForPayments.class, Condition.ConditionResult.FAILURE);
			} else {
				callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointScopesSupportedForNonPayments.class, Condition.ConditionResult.FAILURE);
			}
		}
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
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

	@Override
	protected void onPostAuthorizationFlowComplete() {

		if (!isSecondClient()) {
			callAndContinueOnFailure(EnsureIdTokenDoesNotContainNonRequestedClaims.class, Condition.ConditionResult.WARNING);
		}

		super.onPostAuthorizationFlowComplete();
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
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-8.5-1");
		callAndContinueOnFailure(CheckForBCP195InsecureFAPICiphers.class, Condition.ConditionResult.WARNING, "FAPI1-ADV-8.5", "RFC9325A-A", "RFC9325-4.2");
	}

	protected void updateResourceRequest() {
		if (brazilPayments.isTrue()) {
			// we use the idempotency header to allow us to make a request more than once; however it is required
			// that a new jwt is sent in each retry, so update jti/iat & resign
			call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));
			callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
			callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");
			call(exec().unmapKey("request_object_claims"));
			callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);

		}
	}

	protected void verifyAccessTokenWithResourceEndpoint() {
		updateResourceRequest();
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-4");


		updateResourceRequest();
		callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");
		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
			// CDR requires this header when the x-fapi-customer-ip-address header is present
			callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
		}
		// try different, valid accept headers to verify server accepts them
		if (brazilPayments.isTrue()) {
			callAndStopOnFailure(SetApplicationJwtCharsetUtf8ContentTypeHeaderForResourceEndpointRequest.class);
			callAndStopOnFailure(SetApplicationJwtCharsetUtf8AcceptHeaderForResourceEndpointRequest.class);
		} else {
			callAndStopOnFailure(SetUtf8JsonAcceptHeadersForResourceEndpointRequest.class);
		}
		callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		if (brazilPayments.isTrue()) {
			validateBrazilPaymentInitiationSignedResponse();
		}

		updateResourceRequest();
		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		if (brazilPayments.isTrue()) {
			validateBrazilPaymentInitiationSignedResponse();
		}

		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);
	}

	@Override
	protected void requestProtectedResource() {
		if (!isSecondClient()) {
			checkAccountRequestEndpointTLS();
			checkAccountResourceEndpointTLS();
		}

		super.requestProtectedResource();

		if (!isSecondClient()) {
			verifyAccessTokenWithResourceEndpoint();
		}
	}
}
