package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-ping-without-mtls-certificate-test",
	displayName = "FAPI-CIBA-ID1: Client test - missing mutual TLS certificate in client notification request",
	summary = "The client receives a ping notification without a mutual TLS client certificate. " +
		"The client must reject the notification at the TLS or HTTP layer. An ambiguous connection closure produces a warning. " +
		"After notification rejection is validated, further token polls receive invalid_grant and are not logged. " +
		"Controlled poll fallback is tested separately by the ping-mode poll-fallback test.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = { "poll" })
@VariantNotApplicable(parameter = FAPICIBAProfile.class,
	values = { "plain_fapi", "openbanking_uk", "connectid_au" })
public class FAPICIBAClientPingWithoutMTLSCertificateTest extends AbstractFAPICIBAClientTest {

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session,
		JsonObject requestParts) {
		if (path.equals("token")) {
			if (setStatusRunningIfWaiting()) {
				if (clientPingResponseValidated()) {
					setStatus(Status.WAITING);
					return completedTokenResponse();
				}
				return handleMtlsRequest(path, requestParts);
			}
			if (getStatus() == Status.FINISHED) {
				return completedTokenResponse();
			}
		}
		return super.handleHttpMtls(path, req, res, session, requestParts);
	}

	@Override
	protected Object tokenEndpoint(String requestId) {
		// Finalization can also run after the incoming TLS checks release the test lock.
		if (setStatusRunningIfWaiting()) {
			if (clientPingResponseValidated()) {
				setStatus(Status.WAITING);
				return completedTokenResponse();
			}
			return processTokenEndpointRequest(requestId);
		}
		if (getStatus() == Status.FINISHED) {
			return completedTokenResponse();
		}
		return super.tokenEndpoint(requestId);
	}

	private ResponseEntity<JsonObject> completedTokenResponse() {
		JsonObject body = new JsonObject();
		body.addProperty("error", "invalid_grant");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Cache-Control", "no-store")
			.header("Pragma", "no-cache")
			.body(body);
	}

	@Override
	protected void sendPingRequestAndVerifyResponse() {
		// BrazilCIBA-6.3.4.1 permits controlled polling after notification delivery failure.
		// This test checks transport rejection, not the client's fallback policy.
		callAndStopOnFailure(PingClientNotificationEndpointWithoutMTLS.class,
			Condition.ConditionResult.FAILURE, "CIBA-10.2", "BrazilCIBA-6.3.4");
		verifyPingResponse();
	}

	protected void verifyPingResponse() {
		callAndContinueOnFailure(WarnIfNotificationRejectionWithoutMTLSIsUncertain.class,
			Condition.ConditionResult.WARNING, "BrazilCIBA-6.3.4");
	}

	@Override
	protected void pingRequestComplete() {
		markPingResponseValidated();
		fireTestFinished();
	}
}
