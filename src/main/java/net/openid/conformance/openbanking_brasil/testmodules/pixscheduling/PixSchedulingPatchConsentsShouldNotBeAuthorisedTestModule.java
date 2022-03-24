package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.FAPIBrazilCallPaymentConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationConsentValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "patch-consents-api-pixscheduling-revoke-awaitingauth",
	displayName = "Patch Consents API Test Module",
	summary = "This test is an unhappy path PATCH consents test module.\n\n" +
		"Flow:\n" +
		"Creates a payment consent scheduled for today + 1 day, does not re-direct the user, attempts to PATCH the consent with status REVOKED and revokedBy TPP, the test is expecting a 422 error being returned with the code OPERATION_NOT_ALLOWED_BY_STATUS \n\n" +
		"Required:\n" +
		"Consent url pointing at the consent endpoint.\n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"client.org_jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment",
		"resource.brazilOrganizationId"
	}
)
public class PixSchedulingPatchConsentsShouldNotBeAuthorisedTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void runTests() {
		runInBlock("Validate payment initiation consent", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilGeneratePaymentConsentRequest.class);
			callAndStopOnFailure(RemovePaymentDateFromConsentRequest.class);
			callAndStopOnFailure(EnsureScheduledPaymentDateIsTomorrow.class);

			call(sequence(SignedPaymentConsentSequence.class));

			callAndStopOnFailure(PaymentInitiationConsentValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(new ValidateSelfEndpointPaymentConsent());
		});

		runInBlock("Validate payment payment PATCH consent", () -> {
			callAndStopOnFailure(PaymentConsentIdExtractor.class);
			callAndStopOnFailure(PrepareToPatchConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilGeneratePatchPaymentConsentRequest.class);
			callAndStopOnFailure(SetPatchConsentsRevokedAndRevokedByTPP.class);
			call(new SignedPaymentConsentSequence()
				.replace(EnsureHttpStatusCodeIs201.class,condition(EnsureConsentResponseCodeWas422.class))
				.replace(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, condition(FAPIPatchConsentsRequest.class))
				.insertBefore(EnsureHttpStatusCodeIs201.class,condition(EnsurePatchPayment422ResponseCodeIsOperationNotAllowed.class))
			);
		});
	}
}
