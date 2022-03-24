package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.EnsureNoRejectionReasonIFStatusIsNotRJCT;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

@PublishTestModule(
	testName = "payments-api-negative-tests",
	displayName = "Payments API basic negative test modules",
	summary = "This test is a basic negative test that checks scenarios where the consent obtained and the payment created mismatch." +
		"Flow:" +
		"Makes a payment with currency not matching the consent - expects a 422." +
		"Makes a payment with payment amount not matching the consent - expects a 422." +
		"Makes a good payment flow - expects success." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Resource url pointing at the base url. The test appends on the required payment endpoints" +
		"Config: The test changes the provided payment data to bad values throughout the test",
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
		"resource.resourceUrl"
	}
)
public class PaymentsApiNegativeTestModule extends AbstractOBBrasilFunctionalTestModule {

	private boolean finalAuth = false;
	private boolean secondTest = false;

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		callAndStopOnFailure(SanitiseQrCodeConfig.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		eventLog.startBlock("Validating consent and payment request are the same");
		callAndStopOnFailure(ValidatePaymentAndConsentHaveSameProperties.class);
		eventLog.startBlock("Storing authorisation endpoint");
		callAndStopOnFailure(StoreScope.class);
		eventLog.startBlock("Preparing consent request, setting payment request to incorrect currency type");
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(SetIncorrectCurrencyPayment.class);
	}

	@Override
	protected void validateResponse() {
	}

	protected void fireSecondTest() {
		eventLog.startBlock("Resetting payment config");
		callAndStopOnFailure(ResetPaymentRequest.class);
		eventLog.startBlock("Setting amount to be incorrect");
		callAndStopOnFailure(SetIncorrectAmountInPayment.class);
		eventLog.startBlock("Resetting authorisation endpoint");
		callAndStopOnFailure(SetScope.class);
		secondTest = true;
		performAuthorizationFlow();
	}
	protected void fireThirdTest() {
		eventLog.startBlock("Resetting payment config");
		callAndStopOnFailure(ResetPaymentRequest.class);
		eventLog.startBlock("Resetting authorisation endpoint");
		callAndStopOnFailure(SetScope.class);
		finalAuth = true;
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
			if (brazilPayments) {
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
		if(!finalAuth && !secondTest) {
			callAndStopOnFailure(CallProtectedResource.class);
			callAndStopOnFailure(EnsureResponseCodeWas422.class);
			callAndStopOnFailure(EnsureResponseWasJwt.class);
			callAndContinueOnFailure(Ensure422ResponseCodeWasPAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO_OR_NAO_INFORMADO.class, Condition.ConditionResult.FAILURE);
		} else if (!finalAuth) {
			// TODO stop using that
			callAndStopOnFailure(CallProtectedResource.class);
			callAndStopOnFailure(EnsureResponseCodeWas422.class);
			callAndStopOnFailure(EnsureResponseWasJwt.class);
			callAndContinueOnFailure(EnsurePaymentCodeIsCorrect.class, Condition.ConditionResult.FAILURE);
		}
		else {
			callAndStopOnFailure(CallProtectedResource.class);
			callAndContinueOnFailure(EnsureResponseCodeWas201.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureNoRejectionReasonIFStatusIsNotRJCT.class, Condition.ConditionResult.FAILURE);

			callAndStopOnFailure(EnsureResponseWasJwt.class);
		}
		callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);



		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		if (!isSecondClient()) {
			callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");
		}
		eventLog.endBlock();
		if(!secondTest) {
			fireSecondTest();
		} else if(!finalAuth){
			fireThirdTest();
		}
	}

	@Override
	protected void handleSuccessfulAuthorizationEndpointResponse() {

		if (!jarm) {
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

		performPostAuthorizationFlow(finalAuth);
	}

	protected void performPostAuthorizationFlow(boolean finalAuth) {
		eventLog.startBlock(currentClientString() + "Call token endpoint");

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();
		requestAuthorizationCode();
		requestProtectedResource();
		if(finalAuth) {
			onPostAuthorizationFlowComplete();
		}
	}
}
