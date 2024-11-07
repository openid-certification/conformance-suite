package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckClaimsSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointAcrClaimSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthEncryptAlgValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthEncryptEncValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthSignAlgValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.CheckDiscEndpointIdTokenSigningAlgValuesSupportedContainsPS256;
import net.openid.conformance.condition.client.CheckDiscEndpointPARSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256;
import net.openid.conformance.condition.client.CheckDiscEndpointResponseModesSupportedContainsJwt;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationRequestTypesSupportedContainsTestType;
import net.openid.conformance.condition.client.CheckDiscEndpointResponseTypeCodeSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointScopesSupportedContainsOpenId;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupportedContainsOnlyPairwise;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.CheckDiscRequirePushedAuthorizationRequestsIsABoolean;
import net.openid.conformance.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
import net.openid.conformance.condition.client.FAPI2CheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import net.openid.conformance.condition.client.FAPIAuCdrCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointCpfOrCnpjClaimSupported;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointGrantTypesSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointScopesSupported;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.ekyc.condition.client.EnsureAuthorizationResponseIssParameterSupportedIsTrue;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-discovery-end-point-verification",
	displayName = "FAPI2-Security-Profile-ID2: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configuration (including scopes, response_types, grant_types etc) contains values required by the specification",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
	}
)
@VariantParameters({
	FAPI2ID2OPProfile.class,
	FAPI2SenderConstrainMethod.class,
	FAPI2AuthRequestMethod.class,
	FAPIResponseMode.class,
	FAPIOpenIDConnect.class
})
public class FAPI2SPID2DiscoveryEndpointVerification extends AbstractFAPI2SPID2DiscoveryEndpointVerification {

	private Class<? extends ConditionSequence> profileSpecificChecks;
	private Class<? extends ConditionSequence> oidcChecks;

	protected Boolean jarm;
	protected Boolean signedRequest;
	protected Boolean isOpenId;

	protected boolean brazil = false;

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		profileSpecificChecks = PlainFAPIDiscoveryEndpointChecks.class;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		profileSpecificChecks = OpenBankingUkDiscoveryEndpointChecks.class;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "consumerdataright_au")
	public void setupConsumerDataRightAu() {
		profileSpecificChecks = AuCdrDiscoveryEndpointChecks.class;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		profileSpecificChecks = OpenBankingBrazilDiscoveryEndpointChecks.class;
		brazil = true;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "connectid_au")
	public void setupConnectId() {
		profileSpecificChecks = ConnectIdAuDiscoveryEndpointChecks.class;
	}

	@VariantSetup(parameter = FAPIOpenIDConnect.class, value = "openid_connect")
	public void setupOidc() {
		oidcChecks = OidcDiscoveryEndpointChecks.class;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "cbuae")
	public void setupCBUAE() {
		profileSpecificChecks = OpenBankingUAEDiscoveryEndpointChecks.class;
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		jarm = getVariant(FAPIResponseMode.class) == FAPIResponseMode.JARM;
		signedRequest = getVariant(FAPI2AuthRequestMethod.class) == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION;
		isDpop = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.DPOP;
		isOpenId = getVariant(FAPIOpenIDConnect.class) == FAPIOpenIDConnect.OPENID_CONNECT;
		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);
	}

	@Override
	protected void performEndpointVerification() {

		callAndContinueOnFailure(CheckDiscEndpointResponseTypeCodeSupported.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.2-1");
		if (jarm) {
			callAndContinueOnFailure(CheckDiscEndpointResponseModesSupportedContainsJwt.class, Condition.ConditionResult.FAILURE, "JARM-2.3.4");
			callAndContinueOnFailure(CheckDiscEndpointAuthSignAlgValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
			callAndContinueOnFailure(CheckDiscEndpointAuthEncryptAlgValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
			callAndContinueOnFailure(CheckDiscEndpointAuthEncryptEncValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
		} else {
			// https://bitbucket.org/openid/fapi/issues/478/fapi2-baseline-jarm-iss-draft
			callAndContinueOnFailure(EnsureAuthorizationResponseIssParameterSupportedIsTrue.class, Condition.ConditionResult.FAILURE, "OAuth2-iss-3", "FAPI2-SP-ID2-5.3.1.2-7");
		}

		callAndContinueOnFailure(CheckDiscEndpointPARSupported.class, Condition.ConditionResult.FAILURE, "PAR-5", "FAPI2-SP-ID2-5.3.1.2-2");

		super.performEndpointVerification();

		// although PAR is required by FAPI2, the server may support non-FAPI2-use-cases, so we can't require this to be 'true'
		callAndContinueOnFailure(CheckDiscRequirePushedAuthorizationRequestsIsABoolean.class, Condition.ConditionResult.FAILURE, "PAR-5");

		if (signedRequest) {
			callAndContinueOnFailure(FAPI2CheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);
		}

		callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(sequence(profileSpecificChecks));
		if (oidcChecks != null) {
			call(sequence(oidcChecks));
		}
	}

	public static class OidcDiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointScopesSupportedContainsOpenId.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointSubjectTypesSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		}
	}

	public static class PlainFAPIDiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class ConnectIdAuDiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(CheckDiscEndpointIdTokenSigningAlgValuesSupportedContainsPS256.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-SP-5.3");
			callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-SP-5.2.2-8");
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-SP-5");
			callAndContinueOnFailure(AustraliaConnectIdCheckClaimsSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-SP-5");
			callAndContinueOnFailure(CheckDiscEndpointSubjectTypesSupportedContainsOnlyPairwise.class, Condition.ConditionResult.FAILURE, "CID-SP-5");
			callAndContinueOnFailure(CheckDiscEndpointUserinfoEndpoint.class, Condition.ConditionResult.FAILURE, "CID-SP-5");
		}
	}

	public static class AuCdrDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			// claims parameter support is required in Australia
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-IDA-5.1");

			callAndContinueOnFailure(FAPIAuCdrCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class OpenBankingUkDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			// OBUK servers are required to return acrs, which means they must support requesting the acr claim (as well
			// as the intent id claim), and hence must support the claims parameter
			// FIXME No obvious FAPI2 equivalent.
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

			callAndContinueOnFailure(CheckJwksUriIsHostedOnOpenBankingDirectory.class, Condition.ConditionResult.WARNING, "OBSP-3.4");

			callAndContinueOnFailure(FAPIOBCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE, "OBSP-3.4");
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class OpenBankingBrazilDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE,
				"OIDCD-3", "BrazilOB-5.2.2-4");

			callAndContinueOnFailure(CheckDiscEndpointAcrClaimSupported.class, Condition.ConditionResult.FAILURE,
				"BrazilOB-5.2.2-4", "BrazilOB-5.2.2-5", "BrazilOB-5.2.2-6");
			callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointCpfOrCnpjClaimSupported.class, Condition.ConditionResult.FAILURE,
				"BrazilOB-5.2.2-4", "BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10");
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-5");
			callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould.class, Condition.ConditionResult.WARNING, "BrazilOB-5.2.2-6");
			callAndContinueOnFailure(CheckDiscEndpointUserinfoEndpoint.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-7");
		}
	}

	public static class OpenBankingUAEDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(RARSupport.ExtractRARFromConfig.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointAuthorizationRequestTypesSupportedContainsTestType.class, Condition.ConditionResult.WARNING);

		}
	}
}
