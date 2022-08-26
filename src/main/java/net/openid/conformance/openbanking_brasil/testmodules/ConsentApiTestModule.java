package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckItemCountHasMin1;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.consent.v1.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.consent.v1.CreateNewConsentValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "consent-api-test",
	displayName = "Validate the structure of all consent API resources",
	summary = "Validates the structure of all consent API resources\n" +
		"\u2022 Creates a Consent with all of the existing permissions.\n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n",
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
		"consent.productType"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class ConsentApiTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {

		runInBlock("Validating create consent response", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(AddConsentScope.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CreateNewConsentValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(CheckItemCountHasMin1.class);
		});

		runInBlock("Validating get consent response", () -> {
			callAndStopOnFailure(ConsentIdExtractor.class);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ConsentDetailsIdentifiedByConsentIdValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
		});

		runInBlock("Deleting consent", () -> {
			callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndStopOnFailure(IgnoreResponseError.class);
			callAndStopOnFailure(SetResponseBodyOptional.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);

			callAndStopOnFailure(ConsentWasRejectedOrDeleted.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.WARNING);
		});

	}


}
