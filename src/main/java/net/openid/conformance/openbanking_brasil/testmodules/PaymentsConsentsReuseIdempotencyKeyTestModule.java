package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-idempotency-test",
	displayName = "Payments API test module for re-using idempotency keys",
	summary = "Payments API test module for re-using idempotency keys" +
		"Flow:" +
		"Makes a good consent flow - expects success. Makes a bad consent flow with a reused idempotency key - expects 422." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Resource url pointing at the base url. The test appends on the required payment endpoints",
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
public class PaymentsConsentsReuseIdempotencyKeyTestModule extends AbstractOBBrasilFunctionalTestModule {

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
		callAndStopOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndStopOnFailure(AddBrazilPixPaymentToTheResource.class);
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(SanitiseQrCodeConfig.class);
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void validateResponse() {
		eventLog.startBlock("Making first PIX request");
		callPix();
		//Make same request again with same idempotency key but with same payload - Expects a 200 or 201
		eventLog.startBlock("Making identical PIX request");
		callPix();
		//Make request again but using different ISS and expect a 403 response
		eventLog.startBlock("Making PIX request with incorrect ISS");
		callPixWrongIss();
		//Makes request using same idempotency key but using a different payload - Expects a 422
		eventLog.startBlock("Making PIX request with with same idempotency key but different payload");
		callAndStopOnFailure(ModifyPixPaymentValue.class);
		callPixChangedPayload();
	}

	public void callPixChangedPayload() {

		callPixFirstBlock();

		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

		callPixSecondBlock();

		callAndStopOnFailure(EnsureResponseCodeWas422.class);
		callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);
	}

	public void callPix() {

		callPixFirstBlock();

		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

		callPixSecondBlock();

		callAndStopOnFailure(EnsureResponseCodeWas201or200.class);
	}

	public void callPixWrongIss(){
		callPixFirstBlock();

		callAndStopOnFailure(InjectWrongISSToJWT.class);

		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

		callPixSecondBlock();

		callAndStopOnFailure(EnsureResourceResponseReturnedJsonContentType.class);

		callAndContinueOnFailure(EnsureResponseCodeWas403or400.class);

		callAndStopOnFailure(ResourceErrorMetaValidator.class);

		if (env.getString("warning_message") != null){
			callAndContinueOnFailure(ChuckWarning.class, Condition.ConditionResult.WARNING);
		}
	}

	private void callPixFirstBlock(){
		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");

		callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");

		callAndStopOnFailure(AddIdempotencyKeyHeader.class);
		callAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(CreatePaymentRequestEntityClaims.class);

		call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));

		callAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");

		callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

		callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
	}

	private void callPixSecondBlock(){
		call(exec().unmapKey("request_object_claims"));

		callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);

		// TODO port to using CallProtectedResource
//		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeadersOptionalError.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
		callAndStopOnFailure(CallProtectedResource.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
	}

}
