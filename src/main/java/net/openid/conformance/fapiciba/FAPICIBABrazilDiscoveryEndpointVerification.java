package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointAcrClaimSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsCiba;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectEncryptionAlgValuesSupportedContainsRsaOaep;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectEncryptionEncValuesSupportedContainsA256gcm;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalDiscoveryEndpointVerification;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
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
	FAPI1FinalOPProfile.class
})
@VariantNotApplicable(parameter = CIBAMode.class, values = { "push" })
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = { "plain_fapi", "openbanking_uk", "consumerdataright_au", "openinsurance_brazil", "openbanking_ksa"})
public class FAPICIBABrazilDiscoveryEndpointVerification extends AbstractFAPI1AdvancedFinalDiscoveryEndpointVerification {

	private ConditionSequence profileSpecificChecks;

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		profileSpecificChecks = new OpenBankingBrazilDiscoveryEndpointChecks(false);
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil")
	public void setupOpenInsuranceBrazil() {
		profileSpecificChecks = new OpenBankingBrazilDiscoveryEndpointChecks(false);
	}

	@Override
	protected void performEndpointVerification() {
		super.performEndpointVerification();

		callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-1", "OIDCD-3");

		callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckDiscEndpointBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(profileSpecificChecks);

		callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectEncryptionAlgValuesSupportedContainsRsaOaep.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1.1-1");
		callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectEncryptionEncValuesSupportedContainsA256gcm.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1.1-1");

	}

	public static class OpenBankingBrazilDiscoveryEndpointChecks extends AbstractConditionSequence {
		boolean openInsurance;

		public OpenBankingBrazilDiscoveryEndpointChecks(boolean openInsurance) {
			this.openInsurance = openInsurance;
		}

		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "BrazilOB-5.2.2-3", "BrazilOPIN-page8");
			callAndContinueOnFailure(CheckDiscEndpointAcrClaimSupported.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-3", "BrazilOB-5.2.2-6", "BrazilOPIN-page8");
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsCiba.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-6");
			callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould.class, Condition.ConditionResult.WARNING, "BrazilOB-5.2.2-7");
			callAndContinueOnFailure(CheckDiscEndpointUserinfoEndpoint.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-8", "BrazilOPIN-page8");
		}
	}
}
