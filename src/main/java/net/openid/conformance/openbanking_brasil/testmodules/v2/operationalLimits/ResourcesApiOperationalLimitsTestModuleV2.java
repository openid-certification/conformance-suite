package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.ResourcesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-operational-limits",
	displayName = "Test will make sure that the server has not implemented any type of operational limits for the Resources API.",
	summary = "This test will generate three different consent requests and call the resources API 450 times for each created consent\n" +
		"\u2022 Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test has been provided\n" +
		"\u2022 Using the client_id for OL and the CPF/CNPJ for OL create a Consent Request sending either business or customer permissions, depending on what has been provided on the test plan configuration - Expect Server to return a 201 - Save ConsentID (1)\n" +
		"\u2022 Return a Success if Consent Response is a 201 containing all permissions required on the scope of the test. Return a Warning and end the test if the consent request returns either a 422 or a 201 without Permission for this specific test.\n" +
		"\u2022 With the authorized consent id (1) , call the GET Resources API 450 Times - Expect a 200 on all requests\n" +
		"\u2022 Using the client_id for OL and the CPF/CNPJ for OL create a Consent Request sending either business or customer permissions, depending on what has been provided on the test plan configuration - Expect Server to return a 201 - Save ConsentID (2)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (2) , call the GET Resources API 450 Times - Expect a 200\n" +
		"\u2022 Using the regular client_id provided and the regular CPF/CNPJ for OL create a Consent Request sending either business or customer permissions, depending on what has been provided on the test plan configuration - Expect Server to return a 201 - Save ConsentID (3)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (3), call the GET Resources API 450 Times - Expect a 200 on all request",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.client_id_operational_limits",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.brazilCnpj",
		"resource.brazilCpfOperationalPersonal",
		"resource.brazilCpfOperationalBusiness",
		"resource.brazilCnpjOperationalBusiness",
		"consent.productType"
	}
)

public class ResourcesApiOperationalLimitsTestModuleV2 extends AbstractOperationalLimitsTestModule {

	private int currentBatch = 1;
	private static final int NUMBER_OF_EXECUTIONS = 450;

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllResourceRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddResourcesScope.class);
		switchToSecondClient();
		callAndStopOnFailure(AddResourcesScope.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void requestProtectedResource() {

		for (int i = 0; i < NUMBER_OF_EXECUTIONS; i++) {
			preCallProtectedResource(String.format("[%d] Calling Resources Endpoint with consent_id_%d", i + 1, currentBatch));

			if (i == 0) {
				validateResponse();
			}
			if (i % 100 == 0) {
				//Get a new access token every 100 iterations
				refreshAccessToken();
			}
		}

	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		expose("consent_id_" + currentBatch, env.getString("consent_id"));
		enableLogging();
		if (currentBatch == 3) {
			fireTestFinished();
		} else {

			if (currentBatch == 2) {
				unmapClient();
				callAndStopOnFailure(RemoveOperationalLimitsFromConsentRequest.class);
			}

			callAndContinueOnFailure(RemoveConsentIdFromClientScopes.class);
			performAuthorizationFlow();
			currentBatch++;
		}

	}

	@Override
	protected void validateResponse() {
		runInLoggingBlock(() -> {
			callAndContinueOnFailure(EnsureResponseCodeWas200.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class);
			callAndStopOnFailure(ResourcesResponseValidatorV2.class);
		});
	}

}
