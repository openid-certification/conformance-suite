package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointContainsOpenidResources;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsurePaymentDateIsToday;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideCNPJ;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDCMSubjectDnTestClient1;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithAllDadosScopes;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithOpenIdPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetProtectedResourceUrlToPaymentsEndpoint;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = ResourcesDcmSubjectDnTestModule.testName,
	displayName = "Brazil DCM: resources: check that subjectdn can be updated using the dynamic client management endpoint",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client), register a new client on the target authorization server, perform a successful client credentials grant. Verify that the client credentials grant fails when using a certificate with a different subjectdn. Then use the DCM endpoint to change subjectdn for the client to the subjectdn for the other certificate, and verify the client credentials grant succeeds with that certificate but fails with the original certificate.\n\nNote that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.resourceUrl"
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
