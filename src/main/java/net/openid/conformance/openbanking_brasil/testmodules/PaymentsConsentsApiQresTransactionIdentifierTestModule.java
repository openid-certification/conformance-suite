package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-qres-with-transaction-identifier-test",
	displayName = "Ensures that a payment consent created with a QRES and a transaction identifier is rejected",
	summary = "Ensures that a payment consent created with a QRES and a transaction identifier is rejected.",
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
	protected void requestProtectedResource() {
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(AddTransactionIdentification.class);
		call(new CallPixPaymentsEndpointSequence()
			.replace(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, condition(CallProtectedResourceWithBearerTokenAndCustomHeadersOptionalError.class))
			.skip(EnsureHttpStatusCodeIs201.class, "Expecting error here")
		);
		validateResponse();
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(EnsureResponseWasJwt.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResponseCodeWas422.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(Ensure422ResponseCodeWasNAO_INFORMADO.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResourceResponseReturnedJwtContentType.class, Condition.ConditionResult.FAILURE);
	}
}
