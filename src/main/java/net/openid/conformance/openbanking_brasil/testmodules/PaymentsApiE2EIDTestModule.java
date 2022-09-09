package net.openid.conformance.openbanking_brasil.testmodules;


import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.paymentInitiation.EnsureEndToEndIdIsEqual;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

@PublishTestModule(
	testName = "payments-api-e2eid-test",
	displayName = "Payments API E2EID test module",
	summary = "Validate that the server is correctly validating if a correct E2EID field is being sent before accepting the PIX Payments requests\n" +
		"\u2022 Call the POST Payments Consent API with DICT initiation type using a valid e-mail proxy key\n" +
		"\u2022 Expects a success - 201\n" +
		"\u2022 Redirect the user to authorize the Consent\n" +
		"\u2022 Calls the POST PIX Payments API without sending an E2EID\n" +
		"\u2022 Expects a failure with either a 400 in JSON or a 422 JWT. For a 422 returned code must be set to \"PARAMETRO_NAO_INFORMADO\"\n" +
		"\u2022 Calls the POST PIX Payments API with a bad E2EID which sets the month field to 13\n" +
		"\u2022 Expects a failure with either a 400 in JSON or a 422 JWT. For a 422 returned code must be set to \"PARAMETRO_INVALIDO\"\n" +
		"\u2022 Calls the POST PIX Payments API with a valid E2EID\n" +
		"\u2022 Expects a success 201\n" +
		"\u2022 Pool the GET PIX Payments API until payment leaves a PNDG or PART states\n" +
		"\u2022 Test should finish once payment reaches ACCC/ACSP/ACSC status and matching E2EID",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl"
	}
)
public class PaymentsApiE2EIDTestModule extends AbstractOBBrasilFunctionalTestModule {

	private boolean secondTest = false;
	private boolean finalTest = false;

	@Override
	protected void setupResourceEndpoint() {
		callAndStopOnFailure(AddResourceUrlToConfig.class);
		super.setupResourceEndpoint();
	}

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		callAndStopOnFailure(SanitiseQrCodeConfig.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndStopOnFailure(AddBrazilPixPaymentToTheResource.class);
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		eventLog.startBlock("Validating consent and payment request are the same");
		callAndStopOnFailure(ValidatePaymentAndConsentHaveSameProperties.class);
		eventLog.startBlock("Storing authorisation endpoint");
		callAndStopOnFailure(StoreScope.class);
		eventLog.startBlock("Preparing consent request, setting payment request to incorrect currency type");
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(RemoveE2EIDFromPayment.class);
	}

	@Override
	protected void validateResponse() {}

	protected void fireSecondTest() {
		eventLog.startBlock("Generating new endToEndId");
		callAndStopOnFailure(GenerateNewE2EID.class);
		eventLog.startBlock("Setting E2EID to and invalid value");
		callAndStopOnFailure(MakeE2EIDInvalid.class);
		eventLog.startBlock("Resetting authorisation endpoint");
		callAndStopOnFailure(SetScope.class);
		secondTest = true;
		performAuthorizationFlow();
	}
	protected void fireThirdTest() {
		eventLog.startBlock("Generating new endToEndId");
		callAndStopOnFailure(GenerateNewE2EID.class);
		eventLog.startBlock("Resetting authorisation endpoint");
		callAndStopOnFailure(SetScope.class);
		finalTest = true;
		performAuthorizationFlow();
	}

	@Override
	protected void requestProtectedResource() {
		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		if (isSecondClient()) {
			if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
				// CDR requires this header for all authenticated resource server endpoints
				callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3", "CDR-http-headers");
			}
		} else {
			// these are optional; only add them for the first client
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");

			callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");
			if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
				// CDR requires this header when the x-fapi-customer-ip-address header is present
				callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
			}

			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");
		}

		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.CONSUMERDATARIGHT_AU) {
			callAndStopOnFailure(AddCdrXvToResourceEndpointRequest.class, "CDR-http-headers");
		}

		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			if (brazilPayments.isTrue()) {
				// setup to call the payments initiation API, which requires a signed jwt request body
				call(sequenceOf(condition(CreateIdempotencyKey.class), condition(AddIdempotencyKeyHeader.class)));
				callAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetResourceMethodToPost.class);
				callAndStopOnFailure(CreatePaymentRequestEntityClaims.class);

				// we reuse the request object conditions to add various jwt claims; it would perhaps make sense to make
				// these more generic.
				call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));

				// aud (in the JWT request): the Resource Provider (eg the institution holding the account) must validate if the value of the aud field matches the endpoint being triggered;
				callAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");

				//iss (in the JWT request and in the JWT response): the receiver of the message shall validate if the value of the iss field matches the organisationId of the sender;
				callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

				//jti (in the JWT request and in the JWT response): the value of the jti field shall be filled with the UUID defined by the institution according to [RFC4122] version 4;
				callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");

				//iat (in the JWT request and in the JWT response): the iat field shall be filled with the message generation time and according to the standard established in [RFC7519](https:// datatracker.ietf.org/doc/html/rfc7519#section-2) to the NumericDate format.
				callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

				call(exec().unmapKey("request_object_claims"));

				callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
			}
		}
		if(!finalTest && !secondTest) {
			callAndStopOnFailure(CallProtectedResource.class);
			callAndStopOnFailure(EnsureResponseCodeWas400or422.class);
			if (env.getInteger("resource_endpoint_response_full", "status") == 400) {
				callAndStopOnFailure(EnsureResponseWasJson.class);
				callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			} else {
				callAndStopOnFailure(EnsureResponseWasJwt.class);
				callAndContinueOnFailure(Ensure422ErrorIsParameterNotInformed.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);
			}
		} else if (!finalTest) {
			callAndStopOnFailure(CallProtectedResource.class);
			callAndStopOnFailure(EnsureResponseCodeWas400or422.class);
			if (env.getInteger("resource_endpoint_response_full", "status") == 400) {
				callAndStopOnFailure(EnsureResponseWasJson.class);
				callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			} else {
				callAndStopOnFailure(EnsureResponseWasJwt.class);
				callAndContinueOnFailure(Ensure422ErrorIsInvalidParameter.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);
			}
		}
		else {
			callAndStopOnFailure(CallProtectedResource.class);
			callAndContinueOnFailure(EnsureResponseCodeWas201.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseWasJwt.class);
			validateBrazilPaymentInitiationSignedResponse();
			callAndStopOnFailure(PaymentInitiationPixPaymentsValidator.class);
			callAndStopOnFailure(EnsureEndToEndIdIsEqual.class);
			callAndStopOnFailure(EnsureNoRejectionReasonIFStatusIsNotRJCT.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);

			repeatSequence(PollForAcceptedPaymentSequence::new)
				.untilTrue("payment_accepted")
				.trailingPause(30)
				.times(10)
				.onTimeout(sequenceOf(
					condition(TestTimedOut.class),
					condition(ChuckWarning.class)))
				.run();
			callAndStopOnFailure(PollPaymentAcceptedResultCheck.class);
		}

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		if (!isSecondClient()) {
			callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");
		}
		eventLog.endBlock();
		if(!secondTest) {
			fireSecondTest();
		} else if(!finalTest){
			fireThirdTest();
		}
	}

	@Override
	protected void handleSuccessfulAuthorizationEndpointResponse() {

		if (!jarm.isTrue()) {
			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI1-ADV-5.2.2.1-4");

			// save the id_token returned from the authorization endpoint
			env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));
			performIdTokenValidation();

			callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");

			skipIfMissing(new String[]{"s_hash"}, null, Condition.ConditionResult.INFO,
				ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");

			callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			skipIfMissing(new String[]{"c_hash"}, null, Condition.ConditionResult.INFO,
				ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		}

		performPostAuthorizationFlow(finalTest);
	}

	protected void performPostAuthorizationFlow(boolean finalTest) {
		eventLog.startBlock(currentClientString() + "Call token endpoint");

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();
		requestAuthorizationCode();
		requestProtectedResource();
		if(finalTest) {
			onPostAuthorizationFlowComplete();
		}
	}
}
