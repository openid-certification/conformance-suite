package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.condition.client.EnsureResourceResponseReturnedJsonContentType;
import net.openid.conformance.condition.client.SetResourceMethodToPost;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "payments-api-test-invalid-cnpj",
	displayName = "Payments API invalid CNPJ test module",
	summary = "Payments API invalid CNPJ test module",
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
		"resource.brazilCnpj",
		"resource.brazilOrganizationId",
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment",
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
		callAndStopOnFailure(ReplaceInitiatorCnpjWithBadValue.class);

	}

	@Override
	protected void requestProtectedResource() {
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		call(new CallPixPaymentsEndpointSequence().replace(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, condition(CallProtectedResourceAndExpectFailure.class)));
		validateResponse();
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(EnsureResponseWasJwt.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResponseCodeWas422.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResourceResponseReturnedJsonContentType.class, Condition.ConditionResult.FAILURE);
	}

}
