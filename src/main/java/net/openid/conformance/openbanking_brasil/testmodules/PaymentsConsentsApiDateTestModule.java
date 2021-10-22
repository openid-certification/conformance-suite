package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "payments-consents-api-payment-date-test",
	displayName = "Payments Consents API payment Date module",
	summary = "Payments Consents API payment Date module" +
		"Flow:" +
		"Makes a good consent flow with a good date value - expects 201. Then makes a bad consent flow with a bad date value - expects 422" +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Config: We manually play around with the date value for this test",
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
public class PaymentsConsentsApiDateTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void runTests() {
		eventLog.startBlock("Ensure the payment date is correct before beginning");
		callAndStopOnFailure(EnsurePaymentDateIsCorrect.class);
		eventLog.startBlock("Prepare to post the consent request");
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);
		eventLog.startBlock("Post payment consent request - Expected to pass");
		call(sequence(SignedPaymentConsentSequence.class));

		eventLog.startBlock("Set consent date in the past and prepare the FAPI Brazil ");
		callAndStopOnFailure(SetConsentDateInPast.class);
		eventLog.startBlock("Prepare to post the consent request");
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);


		eventLog.startBlock("Post payment consent request - Expected to fail based on date");
		call(new SignedPaymentConsentSequence()
			.replace
				(EnsureHttpStatusCodeIs201.class,
					condition(EnsureConsentResponseCodeWas422.class)
				)
		);




	}
}
