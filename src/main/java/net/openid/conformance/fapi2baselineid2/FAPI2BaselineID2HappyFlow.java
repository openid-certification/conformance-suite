package net.openid.conformance.fapi2baselineid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.FAPIBrazilCheckDirectoryKeystore;
import net.openid.conformance.condition.client.SetApplicationJwtCharsetUtf8AcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtCharsetUtf8ContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetUtf8JsonAcceptHeadersForResourceEndpointRequest;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2ID2OPProfile;

@PublishTestModule(
		testName = "fapi2-baseline-id2-happy-flow",
		displayName = "FAPI2-Baseline-ID2: Happy flow",
		summary = "Tests primarily 'happy' flows, using two different OAuth2 clients (and hence authenticating the user twice), and uses different variations on request objects, registered redirect uri (both redirect uris must be pre-registered as shown in the instructions). It also tests that sender constrained access tokens (required by the FAPI spec) are correctly implemented.",
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
public class FAPI2BaselineID2HappyFlow extends AbstractFAPI2BaselineID2MultipleClient {

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

	protected void performAdditionalResourceEndpointTests() {
		updateResourceRequest();
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-4");

		updateResourceRequest();
		callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");
		if (getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.CONSUMERDATARIGHT_AU) {
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
		callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		if (brazilPayments) {
			validateBrazilPaymentInitiationSignedResponse();
		}

		updateResourceRequest();
		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
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
			performAdditionalResourceEndpointTests();
		}
	}
}
