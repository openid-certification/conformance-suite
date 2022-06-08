package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-pixscheduling-date-schedulefuture",
	displayName = "Pix scheduled payment which also sends data.date field",
	summary = "Attempts to create a payment consent scheduled with the date element present together with schedule.single.date, and expects a 422 response with the error NAO_INFORMADO",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilPaymentConsent",
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
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilGeneratePaymentConsentRequest.class);
			callAndStopOnFailure(EnsurePaymentDateIsTomorrow.class);
			callAndStopOnFailure(EnsureScheduledPaymentDateIsTomorrow.class);

			call(new SignedPaymentConsentSequence()
				.replace(EnsureHttpStatusCodeIs201.class, condition(EnsureConsentResponseCodeWas422.class)));

			callAndStopOnFailure(EnsureConsentErrorWasNaoInformadio.class);
		});

	}

}
