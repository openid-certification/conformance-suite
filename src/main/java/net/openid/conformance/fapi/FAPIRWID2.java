package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
		testName = "fapi-rw-id2",
		displayName = "FAPI-RW-ID2",
		summary = "Tests primarily 'happy' flows, using two different OAuth2 clients (and hence authenticating the user twice), and uses different variations on request objects, registered redirect uri (both redirect uris must be pre-registered as shown in the instructions). It also tests that TLS Certificate-Bound access tokens (required by the FAPI-RW spec) are correctly implemented.",
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
public class FAPIRWID2 extends AbstractFAPIRWID2MultipleClient {

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
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-1");
	}

	protected void verifyAccessTokenWithResourceEndpoint() {
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-4");
		callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI-R-6.2.2-4");
		callAndStopOnFailure(SetPlainJsonAcceptHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "RFC7231-5.3.2");
		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);
		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");
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
