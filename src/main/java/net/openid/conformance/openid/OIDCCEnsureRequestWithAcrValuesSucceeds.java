package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.OIDCCAddAcrValuesToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.ValidateIdTokenACRClaimAgainstAcrValuesRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Req_acr_values
@PublishTestModule(
	testName = "oidcc-ensure-request-with-acr-values-succeeds",
	displayName = "OIDCC: ensure request with acr_values succeeds",
	summary = "This test will send an acr_values containing the authorization server's advertised acr_values_supported (or for static server configuration, the user supplied acr_values, or if none advertised/configured, the values '1' and '2'). The authorization must succeed, and the returned id_tokens 'SHOULD' (but is not required to) contain an acr claim with one of the requested values. The test will pass if one of the requested values is returned, otherwise it will end with a warning.",
	profile = "OIDCC"
)
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"server.acr_values"
})
public class OIDCCEnsureRequestWithAcrValuesSucceeds extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(OIDCCAddAcrValuesToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1"));
	}

	@Override
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();
		// just a warning; the minimum required behaviour in the spec is not to fail:
		// "OPs MUST support requests for specific Authentication Context Class Reference values via the acr_values
		// parameter, as defined in Section 3.1.2. (Note that the minimum level of support required for this parameter
		// is simply to have its use not result in an error.)"

		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstAcrValuesRequest.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.1", "OIDCC-15.1");
	}
}
