package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "patch-consents-api-pixscheduling-missing-loggeduser",
	displayName = "Patch Consents API Test Module",
	summary = "This test is an unhappy path PATCH consents test module.\n\n" +
		"Flow:\n" +
		"Creates a payment consent scheduled for today + 1 day, re-direct the user to authorize the consent, POST a payment with the consent, attempts to PATCH the consent with status REVOKED, revokedBy USER, loggedUser is removed, the test is expecting a 422 error being returned with the code INFORMACAO_USUARIO_REQUERIDA \n\n" +
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
		"resource.brazilPatchPaymentConsent",
		"resource.brazilOrganizationId"
	}
)
public class PixSchedulingPatchConsentsMissingLoggedUserTestModule extends AbstractFunctionalTestModule {


	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock("POST Payment");
		ConditionSequence pixSequence = new CallPixPaymentsEndpointSequence()
			.replace(CreatePaymentRequestEntityClaims.class, condition(GeneratePaymentRequestEntityClaims.class));
		call(pixSequence);
		eventLog.startBlock("Attempting to PATCH consents");
		callAndStopOnFailure(PaymentConsentIdExtractor.class);
		callAndStopOnFailure(PrepareToPatchConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilGeneratePatchPaymentConsentRequest.class);
		callAndStopOnFailure(RemovePatchConsentsLoggedUser.class);
		call(new SignedPaymentConsentSequence()
			.replace(EnsureHttpStatusCodeIs201.class,condition(EnsureConsentResponseCodeWas422.class))
			.replace(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, condition(FAPIPatchConsentsRequest.class))
			.replace(AddAudAsPaymentConsentUriToRequestObject.class, condition(AddAudToPatchConsentRequest.class))
			.insertBefore(EnsureHttpStatusCodeIs201.class,condition(Ensure422PatchErrorIsUserInfoRequired.class))
		);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication)
			.replace(OptionallyAllow201Or422.class, condition(EnsureConsentResponseCodeWas201.class))
			.replace(FAPIBrazilCreatePaymentConsentRequest.class, paymentConsentEditingSequence());
		return preauthSteps;
	}

	private ConditionSequence paymentConsentEditingSequence() {
		return sequenceOf(
			condition(FAPIBrazilGeneratePaymentConsentRequest.class),
			condition(RemovePaymentDateFromConsentRequest.class),
			condition(EnsureScheduledPaymentDateIsTomorrow.class)
		);
	}




	@Override
	protected void validateResponse() { }
}
