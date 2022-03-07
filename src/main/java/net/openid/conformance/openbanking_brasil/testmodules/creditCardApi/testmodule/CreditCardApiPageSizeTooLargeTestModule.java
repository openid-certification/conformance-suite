package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "credit-card-api-page-size-too-big-test",
	displayName = "Validate that a request for Credit Card Accounts with page-size > 1000 returns HTTP 422",
	summary = "Validate that a request for Credit Card Accounts with page-size > 1000 returns HTTP 422",
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
public class CreditCardApiPageSizeTooLargeTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddCreditCardScopes.class);
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(ClearRequestObjectFromEnvironment.class);
		callAndStopOnFailure(SetProtectedResourceUrlPageSize1001.class);
		callAndStopOnFailure(SetResourceMethodToGet.class);
		callAndStopOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndStopOnFailure(EnsureResponseCodeWas422.class);

	}

}
