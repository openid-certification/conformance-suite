package net.openid.conformance.openbanking_brasil.testmodules.dcr;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointContainsOpenidResources;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractDcmSubjectDnTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDCMSubjectDnTestClient1;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithAllDadosScopes;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = ResourcesDcmSubjectDnTestModule.testName,
	displayName = "Brazil DCM: resources: check that subjectdn can be updated using the dynamic client management endpoint",
	summary = "\u2022 Perform Dynamic Client Registration with first credential set (1) setting subject_dn (1)\n" +
		"\u2022 Use client_credentials grant to obtain Brazil consent\n" +
		"\u2022 Switch to certificate (2) with different subjectdn, verify that client credentials grant fails\n" +
		"\u2022 Make PUT request to client configuration endpoint with subjectdn for second certificate (2) -> This will update tls_subject_dn to (2)\n" +
		"\u2022 Use client_credentials grant to obtain Brazil consent\n" +
		"\u2022 Switch back to original certificate (1), verify that client credentials grant fails\n" +
		"\u2022 Use original certificate (1) to make a GET request to client configuration endpoint expecting a success\n" +
		"\u2022 Use original certificate (1) to make a PUT request on the client configuration endpoint expecting a success" +
		"\u2022 Unregister dynamically registered client with original certificate (1)\n\n" +
		"Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.resourceUrl",
		"consent.productType"
	}
)
// hide various config values from the FAPI base module we don't need
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment"
})

public class ResourcesDcmSubjectDnTestModule extends AbstractDcmSubjectDnTestModule {
	public static final String testName = "resources-dcm-subject-dn-test";

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithDCMSubjectDnTestClient1.class);
		callAndStopOnFailure(OverrideScopeWithAllDadosScopes.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		super.configureClient();
	}

	@Override
	protected void validateDcrResponseScope() {
		// many banks don't support all phase2 scopes, so we may get fewer scopes than we requested
		callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointContainsOpenidResources.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1.1", "RFC7591-2", "RFC7591-3.2.1");
	}

}
