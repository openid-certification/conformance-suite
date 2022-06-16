package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
@PublishTestModule(
	testName = "patch-consents-api-pixscheduling-patch-consents-incorrect-status",
	displayName = "Patch Consents API Test Module",
	summary = "This test is an unhappy path PATCH consents test module.\n\n" +
		"Flow:\n" +
		"Creates a payment consent scheduled for today + 1 day, re-direct the user to authorize the consent, POST a payment, attempts to PATCH the consent with status AUTHORISED, the test is expecting a 400 error being returned \n\n" +
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
		"resource.brazilPaymentConsent",
		"resource.brazilPatchPaymentConsent",
		"resource.brazilOrganizationId"
	}
)
public class PixSchedulingPatchConsentsIncorrectStatusTestModule extends AbstractFunctionalTestModule {
	protected ClientAuthType clientAuthType;

	@Override
	protected void setupResourceEndpoint() {
		callAndStopOnFailure(AddResourceUrlToConfig.class);
		super.setupResourceEndpoint();
	}

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddBrazilPixPaymentToTheResource.class);
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		clientAuthType = getVariant(ClientAuthType.class);
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock("POST Payment");
		ConditionSequence pixSequence = new CallPixPaymentsEndpointSequence()
			.replace(CreatePaymentRequestEntityClaims.class, condition(GeneratePaymentRequestEntityClaims.class))
			.skip(EnsureResponseCodeWas201.class, "Skipping 201 check");
		call(pixSequence);
		eventLog.startBlock("Attempting to PATCH consents");
		callAndStopOnFailure(PaymentConsentIdExtractor.class);
		callAndStopOnFailure(PrepareToPatchConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilGeneratePatchPaymentConsentRequest.class);
		callAndStopOnFailure(SetPatchConsentsStatusToAuthorised.class);
		call(new SignedPaymentConsentSequence()
			.replace(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, condition(FAPIPatchConsentsRequest.class))
			.replace(AddAudAsPaymentConsentUriToRequestObject.class, condition(AddAudToPatchConsentRequest.class))
			.skip(FAPIBrazilGetKeystoreJwksUri.class, "Skipping as expecting 400 response which is JSON")
			.skip(ExtractSignedJwtFromResourceResponse.class, "Skipping as expecting 400 response which is JSON")
			.skip(FAPIBrazilValidateResourceResponseTyp.class, "Skipping as expecting 400 response which is JSON")
			.skip(FAPIBrazilValidateResourceResponseSigningAlg.class, "Skipping as expecting 400 response which is JSON")
			.skip(EnsureContentTypeApplicationJwt.class, "Skipping as expecting 400 response which is JSON")
			.skip(FetchServerKeys.class, "Skipping as expecting 400 response which is JSON")
			.skip(ValidateResourceResponseSignature.class, "Skipping as expecting 400 response which is JSON")
			.skip(ValidateResourceResponseJwtClaims.class, "Skipping as expecting 400 response which is JSON")
			.replace(EnsureHttpStatusCodeIs201.class,condition(EnsureConsentHttpStatusCodeIs400.class))
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
	protected void validateResponse() {}
}
