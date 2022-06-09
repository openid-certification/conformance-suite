package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-pixscheduling-future-date",
	displayName = "Pix scheduled payment date next year",
	summary = "Attempts to create a payment consent scheduled for a day too far in the future, with a payment date of today + 370 days, and expects a 422 response with the error INVALID_SCHEDULE",
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
public class PixScheduledPaymentDateTooFarInFutureConsentsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

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
			eventLog.startBlock("Setting payment schedule date to be in the past");
			callAndStopOnFailure(EnsurePixScheduleDateIsTooFarInFuture.class);
			callAndStopOnFailure(RemovePaymentDateFromConsentRequest.class);

			call(new SignedPaymentConsentSequence()
				.replace(EnsureHttpStatusCodeIs201.class,
					sequenceOf(
						condition(EnsureConsentResponseCodeWas422.class),
						condition(ValidateErrorAndMetaFieldNames.class),
						condition(EnsureConsentErrorWasInvalid_Schedule.class)
					))
			);
		});

	}
}
