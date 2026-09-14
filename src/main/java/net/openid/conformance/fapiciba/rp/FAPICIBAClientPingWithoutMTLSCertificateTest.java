package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-ping-without-mtls-certificate-test",
	displayName = "FAPI-CIBA-ID1: Client test - missing mutual TLS certificate in client notification request",
	summary = "The client receives a ping notification without a mutual TLS client certificate. " +
		"The client must reject the notification at the TLS or HTTP layer. An ambiguous connection closure produces a warning. " +
		"Controlled poll fallback is tested separately by the ping-mode poll-fallback test.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = { "poll" })
@VariantNotApplicable(parameter = FAPICIBAProfile.class,
	values = { "plain_fapi", "openbanking_uk", "connectid_au" })
public class FAPICIBAClientPingWithoutMTLSCertificateTest extends AbstractFAPICIBAClientTest {

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
