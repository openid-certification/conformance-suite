package net.openid.conformance.fapi2baselineid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.FAPIBrazilCheckDirectoryKeystore;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentInitiationRequest;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetUtf8JsonAcceptHeadersForResourceEndpointRequest;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

@PublishTestModule(
		testName = "fapi2-baseline-id2",
		displayName = "FAPI2-Baseline-ID2",
		summary = "Tests primarily 'happy' flows, using two different OAuth2 clients (and hence authenticating the user twice), and uses different variations on request objects, registered redirect uri (both redirect uris must be pre-registered as shown in the instructions). It also tests that TLS Certificate-Bound access tokens (required by the FAPI-RW spec) are correctly implemented.",
		profile = "FAPI2-Baseline-ID2",
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
public class FAPI2BaselineID2 extends AbstractFAPI2BaselineID2MultipleClient {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		if (isBrazil) {
			if (brazilPayments) {
				callAndContinueOnFailure(FAPIBrazilCheckDirectoryKeystore.class, Condition.ConditionResult.FAILURE);
			}
		}
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
	}

	protected void updateResourceRequest() {
		if (brazilPayments) {
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
		if (brazilPayments) {
			callAndStopOnFailure(SetApplicationJwtCharsetUtf8ContentTypeHeaderForResourceEndpointRequest.class);
			callAndStopOnFailure(SetApplicationJwtCharsetUtf8AcceptHeaderForResourceEndpointRequest.class);
		} else {
			callAndStopOnFailure(SetUtf8JsonAcceptHeadersForResourceEndpointRequest.class);
		}
		callAndStopOnFailure(CallProtectedResourceWithBearerToken.class, "RFC7231-5.3.2");
		if (brazilPayments) {
			validateBrazilPaymentInitiationSignedResponse();
		}

		updateResourceRequest();
		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);
		callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");
		if (brazilPayments) {
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
