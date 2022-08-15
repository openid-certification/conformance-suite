package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.ConditionSequenceRepeater;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-pixscheduling-happy-contenttype-jwt",
	displayName = "Scheduled Payment API basic test module",
	summary = "This test is a variation of the core happy path scheduled payments test where we check that the server is correctly dealing with the accept header on the API Calls.\n\n" +
		" \u2022 Creates a payment consent scheduled for today + 350 days \n" +
		" \u2022 POSTs to the payment endpoint - Both POST Requests will have accept headers that are not application/jwt \n" +
		" \u2022 Validates the payment returned in the self-object can be reached  \n" +
		" \u2022 Validates that the Payment will arrive at a scheduled payment confirmed status \n" +
		" \u2022 Perform 2 Requests to the GET Consents API with different accept header and check that it return a response of type application/jwt  \n" +
		" \u2022 Perform 2 Requests to the GET Payments API with different accept header and check that it return a response of type application/jwt  \n",
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
		"resource.brazilOrganizationId"
	}
)
public class PixScheduledPaymentContentTypeJwtTestModule extends AbstractFunctionalTestModule {

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
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(EnforcePresenceOfDebtorAccount.class);

		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void requestProtectedResource() {
		if(!validationStarted) {
			validationStarted = true;
			ConditionSequence pixSequence = new CallPixPaymentsEndpointSequence()
				.replace(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class, condition(AddNoAcceptHeaderRequest.class))
				.replace(CreatePaymentRequestEntityClaims.class, condition(GeneratePaymentRequestEntityClaims.class))
				.skip(EnsureResponseCodeWas201.class, "Skipping 201 check");
			call(pixSequence);
			eventLog.startBlock(currentClientString() + "Validate response");
			validateResponse();

			eventLog.startBlock("Get Payments API with no accept header");
			ConditionSequence GetPaymentNoAcceptSequence = new CallGetPaymentEndpointSequence()
				.replace(AddJWTAcceptHeaderRequest.class, condition(AddNoAcceptHeaderRequest.class))
				.replace(EnsureResponseCodeWas200.class, condition(OptionallyAllow200or406.class).onFail(Condition.ConditionResult.FAILURE).dontStopOnFailure());

			call(GetPaymentNoAcceptSequence);

			eventLog.startBlock("Get Payments API with application/json accept header");
			ConditionSequence GetPaymentJsonAcceptSequence = new CallGetPaymentEndpointSequence()
				.replace(AddJWTAcceptHeaderRequest.class, condition(AddJsonAcceptHeaderRequest.class))
				.replace(EnsureResponseCodeWas200.class, condition(OptionallyAllow200or406.class).onFail(Condition.ConditionResult.FAILURE).dontStopOnFailure());
			call(GetPaymentJsonAcceptSequence);

			eventLog.startBlock("Get Consents API with no accept header");
			callAndStopOnFailure(LoadOldValues.class);
			ConditionSequence GetConsentNoAcceptSequence = new CallGetPaymentEndpointSequence()
				.replace(AddJWTAcceptHeaderRequest.class, condition(AddNoAcceptHeaderRequest.class))
				.replace(EnsureResponseCodeWas200.class, condition(OptionallyAllow200or406.class).onFail(Condition.ConditionResult.FAILURE).dontStopOnFailure());
			call(GetConsentNoAcceptSequence);

			eventLog.startBlock("Get Consents API with application/json accept header");
			callAndStopOnFailure(LoadOldValues.class);
			ConditionSequence GetConsentJsonAcceptSequence = new CallGetPaymentEndpointSequence()
				.replace(AddJWTAcceptHeaderRequest.class, condition(AddJsonAcceptHeaderRequest.class))
				.replace(EnsureResponseCodeWas200.class, condition(OptionallyAllow200or406.class).onFail(Condition.ConditionResult.FAILURE).dontStopOnFailure());
			call(GetConsentJsonAcceptSequence);

			eventLog.endBlock();
		}
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication)
			.replace(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, condition(FAPIBrazilCallPaymentConsentEndpointWithBearerTokenNoAcceptField.class))
			.replace(OptionallyAllow201Or422.class, condition(EnsureConsentResponseCodeWas201.class))
			.replace(FAPIBrazilCreatePaymentConsentRequest.class, paymentConsentEditingSequence())
			.insertAfter(FAPIBrazilAddConsentIdToClientScope.class, condition(SaveOldValues.class));
		return preauthSteps;
	}

	private ConditionSequence paymentConsentEditingSequence() {
		return sequenceOf(
			condition(FAPIBrazilGeneratePaymentConsentRequest.class),
			condition(EnsureScheduledPaymentDateIsTodayPlus350.class)
		);
	}
	@Override
	protected void validateResponse() {
		ConditionSequenceRepeater repeatSequence = repeatSequence(() -> new PollForScheduledPaymentChangeSequence())
			.untilTrue("payment_accepted")
			.times(10)
			.onTimeout(sequenceOf(
					condition(TestTimedOut.class),
					condition(ChuckWarning.class)));

		repeatSequence.setProperties(env, getId(),
			eventLog,testInfo, getTestExecutionManager());
		repeatSequence.run();
	}

}
