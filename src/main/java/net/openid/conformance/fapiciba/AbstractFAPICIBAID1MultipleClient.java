package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;

public abstract class AbstractFAPICIBAID1MultipleClient extends AbstractFAPICIBAID1 {

	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("client_public_jwks", "client_public_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("client_public_jwks");
		env.unmapKey("mutual_tls_authentication");
	}

	/** Return which client is in use, for use in block identifiers */
	protected String currentClientString() {
		if (isSecondClient()) {
			return "Second client: ";
		}
		return "";
	}

	protected void verifyAccessTokenWithResourceEndpointDifferentAcceptHeader() {
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-4");

		callAndStopOnFailure(SetPlainJsonAcceptHeaderForResourceEndpointRequest.class);

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "RFC7231-5.3.2");

		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);

		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");

		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);
	}

	@Override
	protected void configClient() {
		setupClient1();

		setupClient2();
	}

	@Override
	protected void cleanUpPingTestResources() {
		unregisterClient1();

		unregisterClient2();
	}
}
