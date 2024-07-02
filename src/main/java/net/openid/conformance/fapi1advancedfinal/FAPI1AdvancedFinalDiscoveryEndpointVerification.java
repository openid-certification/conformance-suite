package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointAcrClaimSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthEncryptAlgValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthEncryptEncValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthSignAlgValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.CheckDiscEndpointPARSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointResponseModesSupportedContainsJwt;
import net.openid.conformance.condition.client.CheckDiscEndpointResponseTypeCodeSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointScopesSupportedContainsOpenId;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupportedContainsPublic;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.CheckDiscRequirePushedAuthorizationRequestsIsABoolean;
import net.openid.conformance.condition.client.CheckDiscRequirePushedAuthorizationRequestsNotSet;
import net.openid.conformance.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsCodeChallengeMethodS256;
import net.openid.conformance.condition.client.FAPIAuCdrCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported;
import net.openid.conformance.condition.client.FAPIBrazilOpenInsuranceCheckDiscEndpointAcrValuesSupported;
import net.openid.conformance.condition.client.FAPIBrazilOpinCheckDiscEndpointAcrValuesSupportedShould;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectEncryptionAlgValuesSupportedContainsRsaOaep;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectEncryptionEncValuesSupportedContainsA256gcm;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointGrantTypesSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointScopesSupported;
import net.openid.conformance.condition.client.FAPIRWCheckDiscEndpointResponseTypesSupported;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi1-advanced-final-discovery-end-point-verification",
	displayName = "FAPI1-Advanced-Final: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configuration (including scopes, response_types, grant_types etc) contains values required by the specification",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
	}
)
@VariantParameters({
	FAPI1FinalOPProfile.class,
	FAPIResponseMode.class,
	FAPIAuthRequestMethod.class
})
public class FAPI1AdvancedFinalDiscoveryEndpointVerification extends AbstractFAPI1AdvancedFinalDiscoveryEndpointVerification {

	private ConditionSequence profileSpecificChecks;

	protected Boolean jarm;

	protected Boolean par;

	protected boolean brazil = false;

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		profileSpecificChecks = new PlainFAPIDiscoveryEndpointChecks();
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		profileSpecificChecks = new OpenBankingUkDiscoveryEndpointChecks();
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "consumerdataright_au")
	public void setupConsumerDataRightAu() {
		profileSpecificChecks = new AuCdrDiscoveryEndpointChecks();
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		profileSpecificChecks = new OpenBankingBrazilDiscoveryEndpointChecks();
		brazil = true;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_ksa")
	public void setupKSAFapi() {
		profileSpecificChecks = new OpenBankingKSADiscoveryEndpointChecks();
	}


	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil")
	public void setupOpenInsuranceBrazil() {
		profileSpecificChecks = new OpenInsuranceBrazilDiscoveryEndpointChecks();
		brazil = true;
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		jarm = getVariant(FAPIResponseMode.class) == FAPIResponseMode.JARM;
		par = getVariant(FAPIAuthRequestMethod.class) == FAPIAuthRequestMethod.PUSHED;
		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);
	}

	@Override
	protected void performEndpointVerification() {

		if (jarm) {
			callAndContinueOnFailure(CheckDiscEndpointResponseTypeCodeSupported.class, Condition.ConditionResult.FAILURE, "JARM-2.1.1");
			callAndContinueOnFailure(CheckDiscEndpointResponseModesSupportedContainsJwt.class, Condition.ConditionResult.FAILURE, "JARM-2.3.4");
			callAndContinueOnFailure(CheckDiscEndpointAuthSignAlgValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
			callAndContinueOnFailure(CheckDiscEndpointAuthEncryptAlgValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
			callAndContinueOnFailure(CheckDiscEndpointAuthEncryptEncValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
		} else {
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-2");
		}

		if (par) {
			callAndContinueOnFailure(CheckDiscEndpointPARSupported.class, Condition.ConditionResult.FAILURE, "PAR-5");
		}

		super.performEndpointVerification();

		if (par) {
			callAndContinueOnFailure(CheckDiscRequirePushedAuthorizationRequestsIsABoolean.class, Condition.ConditionResult.FAILURE, "PAR-5");
			callAndContinueOnFailure(EnsureServerConfigurationSupportsCodeChallengeMethodS256.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-18");
		} else {
			callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-1", "OIDCD-3");
			callAndStopOnFailure(CheckDiscRequirePushedAuthorizationRequestsNotSet.class, Condition.ConditionResult.FAILURE, "PAR-5");
		}

		callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(sequence(OidcDiscoveryEndpointChecks.class));

		call(profileSpecificChecks);

		if (brazil && !par) {
			// encrypted request object support is only required for redirect based flows
			callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectEncryptionAlgValuesSupportedContainsRsaOaep.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1.1-1");
			callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectEncryptionEncValuesSupportedContainsA256gcm.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1.1-1");
		}
	}

	public static class OidcDiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointSubjectTypesSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		}
	}

	public static class PlainFAPIDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointScopesSupportedContainsOpenId.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class AuCdrDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			// claims parameter support is required in Australia
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "FAPI1-ADV-5.2.3-3");

			callAndContinueOnFailure(FAPIAuCdrCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointScopesSupportedContainsOpenId.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class OpenBankingUkDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			// OBUK servers are required to return acrs, which means they must support requesting the acr claim (as well
			// as the intent id claim), and hence must support the claims parameter
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "FAPI1-ADV-5.2.3-3");

			callAndContinueOnFailure(CheckJwksUriIsHostedOnOpenBankingDirectory.class, Condition.ConditionResult.WARNING, "OBSP-3.4");

			callAndContinueOnFailure(FAPIOBCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE, "OBSP-3.4");
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class OpenBankingBrazilDiscoveryEndpointChecks extends OpenDataBrazilDiscoveryEndpointChecks {

		@Override
		public void checkACR() {
			callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-5");
			callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould.class, Condition.ConditionResult.WARNING, "BrazilOB-5.2.2-6");
		}

	}

	public static class OpenInsuranceBrazilDiscoveryEndpointChecks extends OpenDataBrazilDiscoveryEndpointChecks {

		@Override
		public void checkACR() {
			callAndContinueOnFailure(FAPIBrazilOpenInsuranceCheckDiscEndpointAcrValuesSupported.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-5");
			callAndContinueOnFailure(FAPIBrazilOpinCheckDiscEndpointAcrValuesSupportedShould.class, Condition.ConditionResult.WARNING, "BrazilOB-5.2.2-6");
		}

	}

	public abstract static class OpenDataBrazilDiscoveryEndpointChecks extends AbstractConditionSequence {

		public abstract void checkACR();

		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE,
					"OIDCD-3", "BrazilOB-5.2.2-3", "BrazilOPIN-page8");

			callAndContinueOnFailure(CheckDiscEndpointAcrClaimSupported.class, Condition.ConditionResult.FAILURE,
					"BrazilOB-5.2.2-3", "BrazilOB-5.2.2-6", "BrazilOPIN-page8");
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken.class, Condition.ConditionResult.FAILURE);

			// this will apply to openinsurance too when they switch to the new security profile
			callAndContinueOnFailure(CheckDiscEndpointSubjectTypesSupportedContainsPublic.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-22");

			checkACR();

			callAndContinueOnFailure(CheckDiscEndpointUserinfoEndpoint.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-8", "BrazilOPIN-page8");
		}
	}

	/**
	 * The OpenBanking KSA is defined as FAPI 1 Advanced with PAR required. So the requirement for the discovery
	 * endpoint response is the fapi1 requirement plus the requirement for supporting PAR.
	 */
	public static class OpenBankingKSADiscoveryEndpointChecks extends PlainFAPIDiscoveryEndpointChecks {

		@Override
		public void evaluate() {
			super.evaluate();
			callAndContinueOnFailure(CheckDiscEndpointPARSupported.class, Condition.ConditionResult.FAILURE, "KSA");
		}
	}
}
