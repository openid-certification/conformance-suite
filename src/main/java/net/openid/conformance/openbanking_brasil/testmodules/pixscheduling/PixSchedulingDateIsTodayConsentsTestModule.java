package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureConsentResponseCodeWas422;
import net.openid.conformance.openbanking_brasil.testmodules.support.ObtainPaymentsAccessTokenWithClientCredentials;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToPostConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.SignedPaymentConsentSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-pixscheduling-today-date",
	displayName = "Pix scheduled for today",
	summary = "Attempts to create a payment consent scheduled for today, and expects a 422 response with the error AGENDAMENTO_INVALIDO",
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
public class PixSchedulingDateIsTodayConsentsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

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
			callAndStopOnFailure(EnsureScheduledPaymentDateIsToday.class);

			call(new SignedPaymentConsentSequence()
				.replace(EnsureHttpStatusCodeIs201.class, condition(EnsureConsentResponseCodeWas422.class)));

			callAndStopOnFailure(EnsureConsentErrorWasAgendamantoInvalido.class);
		});

	}

}
