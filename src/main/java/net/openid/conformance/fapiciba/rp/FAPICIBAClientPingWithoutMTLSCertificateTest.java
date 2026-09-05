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
		"The client must reject the notification and must not redeem the auth_req_id or call a protected resource.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = { "poll" })
@VariantNotApplicable(parameter = FAPICIBAProfile.class,
	values = { "plain_fapi", "openbanking_uk", "connectid_au" })
public class FAPICIBAClientPingWithoutMTLSCertificateTest
	extends AbstractFAPICIBAClientPingWithInvalidNotificationTest {

	@Override
	protected Class<? extends Condition> getPingNotificationCondition() {
		return PingClientNotificationEndpointWithoutMTLS.class;
	}

	@Override
	protected String[] getPingNotificationRequirements() {
		return new String[] { "CIBA-10.2", "BrazilCIBA-6.3.4" };
	}

	@Override
	protected void verifyPingResponse() {
		callAndContinueOnFailure(WarnIfNotificationRejectionWithoutMTLSIsUncertain.class,
			Condition.ConditionResult.WARNING, "BrazilCIBA-6.3.4");
	}
}
