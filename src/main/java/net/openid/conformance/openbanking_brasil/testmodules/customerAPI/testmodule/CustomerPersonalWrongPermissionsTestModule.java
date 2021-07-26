package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalFinancialRelationships;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalQualifications;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.ProvideIncorrectPermissionsForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = " Customer-Personal-api-wrong-permissions-test",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test",
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
public class CustomerPersonalWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForCustomerApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the  Customer Personal Qualification", () -> {
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Personal Identifications", () -> {
			callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Customer Personal Financial-relations", () -> {
			callAndStopOnFailure(PrepareToGetPersonalFinancialRelationships.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
