package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-bad-date-test-module",
	displayName = "Payments Consents API bad date test module",
	summary = "Payments Consents API bad date test module",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)

public class PaymentsConsentsApiPastDateTestModule extends AbstractClientCredentialsGrantFunctionalTestModule{

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}
	@Override
	protected void runTests() {
		runInBlock("Validate payment initiation consent", () -> {
			callAndStopOnFailure(SetConsentDataInPast.class);
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(new SignedPaymentConsentSequence()
				.replace
					(EnsureHttpStatusCodeIs201.class,
						condition(EnsureConsentResponseCodeWas422.class)
					)
			);
		});
	}
}
