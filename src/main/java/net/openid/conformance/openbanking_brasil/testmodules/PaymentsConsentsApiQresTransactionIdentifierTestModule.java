package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.EnsureHttpStatusIs422;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-qres-with-transaction-identifier-test",
	displayName = "Ensures that a payment consent created with a QRES and a transaction identifier is rejected",
	summary = "Payments API Test that will add a transaction identification on the payload despite the local instrument being different from INIC. Test will set a Payload using the creditor account details related to the cliente-a00001@pix.bcb.gov.br and the debtor account details will fetched from the configuration field set by the instituion. Test will create a consent but when doing the payment for this created consent it will add a transactionidentification field on the request Payload. Test will expect a failure on the POST Payments - 422 NAO_INFORMADO",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.resourceUrl",
		"resource.brazilCpf"
	}
)
public class PaymentsConsentsApiQresTransactionIdentifierTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		return super.createOBBPreauthSteps()
			.insertBefore(FAPIBrazilValidateResourceResponseTyp.class, condition(EnsureConsentStatusIsAwaitingAuthorisation.class));
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndContinueOnFailure(SelectQRESCodeLocalInstrument.class);
		callAndContinueOnFailure(SelectQRESCodePixLocalInstrument.class);
		callAndContinueOnFailure(RemoveQRCodeFromConfig.class);
		callAndContinueOnFailure(InjectQRCodeWithTransactionIdentifierIntoConfig.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPaymentConsent.class);
		callAndContinueOnFailure(InjectRealCreditorAccountToPayment.class);
		callAndContinueOnFailure(SetProxyToRealEmailAddressOnPayment.class);
		callAndContinueOnFailure(SetProxyToRealEmailAddressOnPaymentConsent.class);
	}

	@Override
	@CallProtectedResource.FixMe
	protected void requestProtectedResource() {
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(AddTransactionIdentification.class);
		call(new CallPixPaymentsEndpointSequence()
			.replace(EnsureHttpStatusCodeIs201.class, condition(EnsureHttpStatusIs422.class))
		);
		validateResponse();
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(EnsureResponseWasJwt.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(Ensure422ResponseCodeWasNAO_INFORMADO.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResourceResponseReturnedJwtContentType.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);
	}
}
