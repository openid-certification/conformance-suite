package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-pixscheduling-data-scheduled",
	displayName = "Pix schedule",
	summary = "Attempts to create a payment consent scheduled for a day in the future, and expects a 422 response with the error NAO_INFORMADIO",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment",
		"resource.brazilOrganizationId"
	}
)
public class PixSchedulingDateInFutureConsentsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndContinueOnFailure(SanitiseQrCodeConfig.class);
	}

	@Override
	protected void runTests() {
		runInBlock("Validate payment initiation consent", () -> {
			callAndStopOnFailure(EnsurePaymentDateIsTomorrow.class);
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilScheduleConsentRequest.class);
			callAndStopOnFailure(EnsureScheduledPaymentDateIsTomorrow.class);

			call(new SignedPaymentConsentSequence()
				.replace(EnsureHttpStatusCodeIs201.class, condition(EnsureConsentResponseCodeWas422.class)));

			callAndStopOnFailure(EnsureConsentErrorWasNaoInformadio.class);
		});

	}

}
