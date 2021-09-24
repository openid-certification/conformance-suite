package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.FAPIBrazilEncryptRequestObject;
import net.openid.conformance.condition.client.EnsureInvalidRequestObjectOrAccessDeniedError;
import net.openid.conformance.condition.client.ExpectEncryptionRequiredErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-ensure-encryption-required",
	displayName = "FAPI1-Advanced-Final: ensure encryption is required when passing request object via browser",
	summary = "This test makes a FAPI authorization request via the front channel without encrypting the request object, which the Open Banking Brazil security profile says must be rejected. An invalid_request_object or access_denied error may be returned from the authorization endpoint, or an error may be shown to the user (a screenshot of which must be uploaded).",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
// Applicable only for Brazil using request object by value
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"pushed"
})
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {
	"plain_fapi",
	"openbanking_uk",
	"consumerdataright_au"
})
public class FAPI1AdvancedFinalBrazilEnsureEncryptionRequired extends AbstractFAPI1AdvancedFinalExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectEncryptionRequiredErrorPage.class,
			"FAPI1-ADV-5.2.2-18", "BrazilOB-5.2.2-1", "BrazilOB-5.2.3-3");

		env.putString("error_callback_placeholder", env.getString("encryption_required_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps().skip(FAPIBrazilEncryptRequestObject.class, "NOT encrypting request object");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(EnsureInvalidRequestObjectOrAccessDeniedError.class, Condition.ConditionResult.FAILURE,
			"RFC6749-4.1.2.1", "OIDCC-3.1.2.6", "BrazilOB-5.2.2-1", "BrazilOB-5.2.3-3");
		fireTestFinished();
	}

}
