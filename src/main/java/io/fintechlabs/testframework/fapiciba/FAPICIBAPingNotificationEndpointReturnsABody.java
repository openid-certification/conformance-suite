package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus200;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-ping-backchannel-notification-endpoint-response-has-body",
	displayName = "FAPI-CIBA: Ping mode - backchannel notification-endpoint returns HTTP 200 OK response with a body",
	summary = "The client's backchannel_notification_endpoint returns a HTTP 200 OK response with a body, the token endpoint should then return successfully as normal. If the token endpoint does not return success, this test will fail with a warning.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
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
	},
	notApplicableForVariants = {
		FAPICIBA.variant_poll_mtls,
		FAPICIBA.variant_poll_privatekeyjwt,
		FAPICIBA.variant_openbankinguk_poll_mtls,
		FAPICIBA.variant_openbankinguk_poll_privatekeyjwt
	}
)
// FIXME: mark test as Ping specific
public class FAPICIBAPingNotificationEndpointReturnsABody extends AbstractFAPICIBA {

	@Variant(name = variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		super.setupPingPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() { super.setupOpenBankingUkPingMTLS(); }

	@Variant(name = variant_openbankinguk_ping_privatekeyjwt)
	public void setupOpenBankingUkPingPrivateKeyJwt() {
		super.setupOpenBankingUkPingPrivateKeyJwt();
	}

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {

		super.handlePingCallback(requestParts);
		return new ResponseEntity<Object>("Backchannel Notification Endpoint returns a HTTP 200 OK response with a body.", HttpStatus.OK);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {

		processPingNotificationCallback(requestParts);

		int httpStatus = env.getInteger("token_endpoint_response_http_status");
		if (httpStatus == 200) {
			handleSuccessfulTokenEndpointResponse();
		} else {
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus200.class, Condition.ConditionResult.WARNING, "CIBA-10.2");
			cleanUpPingTestResources();
			fireTestFinished();
		}

	}

}
