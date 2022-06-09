package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "payments-api-test-invalid-cnpj",
	displayName = "Payments API invalid CNPJ test module",
	summary = "Payments API invalid CNPJ test module" +
		"Flow:" +
		"Makes a bad consent flow with an invalid cnpj value - expects 422." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Resource url pointing at the base url. The test appends on the required payment endpoints" +
		"Config: We manually set the cnpj initiator value for consent to a bad value.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilOrganizationId",
		"resource.brazilPaymentConsent",
		"directory.keystore"
	}
)
public class PaymentsApiInvalidCnpjTestModule extends AbstractOBBrasilFunctionalTestModule {

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
		callAndStopOnFailure(SanitiseQrCodeConfig.class);
		callAndStopOnFailure(ReplaceInitiatorCnpjWithBadValue.class);
		eventLog.endBlock();
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock("Initiating payment");
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		call(new CallPixPaymentsEndpointSequence()
			.replace(EnsureResponseCodeWas201.class, condition(EnsureResponseCodeWas422.class))
			.insertAfter(EnsureResponseCodeWas201.class, condition(ValidateErrorAndMetaFieldNames.class))
			.skip(ExtractSignedJwtFromResourceResponse.class, "Signed jwt will be decoded elsewhere")
		);
		eventLog.endBlock();
		eventLog.startBlock("Validating payment response");
		validateResponse();
		eventLog.endBlock();
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(EnsureResponseWasJwt.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResponseCodeWas422.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResourceResponseReturnedJwtContentType.class, Condition.ConditionResult.FAILURE);
	}

}
