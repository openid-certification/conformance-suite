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
	testName = "patch-consents-api-pixscheduling-revoke-tpp",
	displayName = "Patch Consents API Test Module",
	summary = "PATCH Payments happy path test that will revoke a created consent with revokedBy set for the TPP\n\n" +
		"	\u2022 Creates a payment consent scheduled for today + 1 day \n" +
		"	\u2022 Re-directs the user to authorize the consent \n" +
		"	\u2022 POST a payment with the consent \n" +
		"	\u2022 Attempts to PATCH the consent with status REVOKED, revokedBy TPP without LoggedUser \n" +
		"	\u2022 Expects a 200 - OK, with status set to Revoked \n" +
		"	\u2022 Performs a GET on the created Payment \n" +
		"	\u2022 Expects a 200 - OK, with status set to RJCT \n",
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
public class PixSchedulingPatchHappyPathRevokedByTppTestModule extends AbstractFunctionalTestModule {

	//Setup PaymentScope
	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	//Setup POST Method in env object
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
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
	protected void requestProtectedResource() {
		eventLog.startBlock("POST Payment Request");
		ConditionSequence pixSequence = new CallPixPaymentsEndpointSequence()
			.replace(CreatePaymentRequestEntityClaims.class, condition(GeneratePaymentRequestEntityClaims.class));
		call(pixSequence);
		//Make sure that the self links are correct and point to the created payment
		call(new ValidateSelfEndpointPaymentConsent());

		callAndStopOnFailure(SaveOldValues.class);

		eventLog.startBlock("Attempting to PATCH consents");
		callAndStopOnFailure(PaymentConsentIdExtractor.class);
		callAndStopOnFailure(PrepareToPatchConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilGeneratePatchPaymentConsentRequest.class);
		//Set Status to REVOKED
		callAndStopOnFailure(SetPatchConsentsRevoked.class);
		//Set RevokedBy to TPP
		callAndStopOnFailure(SetPatchConsentsRevokedByTpp.class);
		//Remove LoggedUser
		callAndStopOnFailure(RemovePatchConsentsLoggedUser.class);

		//Call PATCH consents request
		call(new SignedPaymentConsentSequence()
			.replace(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, condition(FAPIPatchConsentsRequest.class))
			.replace(AddAudAsPaymentConsentUriToRequestObject.class, condition(AddAudToPatchConsentRequest.class))
			.replace(EnsureHttpStatusCodeIs201.class, condition(EnsureHttpStatusCodeIs200.class))
		);

		//Check if Status is REVOKED
		callAndStopOnFailure(EnsureStatusRevoked.class);

		eventLog.startBlock("GET Payments Request");
		callAndStopOnFailure(LoadOldValues.class);
		ConditionSequence GetPaymentSequence = new CallGetPaymentEndpointSequence();
		call(GetPaymentSequence);

		//Check if Status is RJCT
		callAndStopOnFailure(EnsureStatusRJCT.class);
	}


	@Override
	protected void validateResponse() { }
}
