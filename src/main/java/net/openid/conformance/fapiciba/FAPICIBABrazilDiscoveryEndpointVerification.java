package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalDiscoveryEndpointVerification;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi-ciba-id1-brazil-discovery-end-point-verification",
	displayName = "FAPI-CIBA-BR: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configuration (including scopes, response_types, grant_types etc) contains values required by the specification",
	profile = "FAPI-CIBA-BR",
	configurationFields = {
			"server.discoveryUrl",
	}
)
@VariantParameters({
	CIBAMode.class,
	FAPICIBAProfile.class
})
@VariantNotApplicable(parameter = CIBAMode.class, values = { "push" })
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = { "plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBABrazilDiscoveryEndpointVerification extends AbstractFAPI1AdvancedFinalDiscoveryEndpointVerification {

	private FAPICIBAServerProfileBehavior profileBehavior;

	@VariantSetup(parameter = FAPICIBAProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		profileBehavior = new OpenBankingBrazilCibaServerProfileBehavior();
	}

	@Override
	protected void performEndpointVerification() {
		super.performEndpointVerification();

		callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-1", "OIDCD-3");

		callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckDiscEndpointBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(sequence(profileBehavior.getProfileSpecificDiscoveryChecks()));

	}
}
