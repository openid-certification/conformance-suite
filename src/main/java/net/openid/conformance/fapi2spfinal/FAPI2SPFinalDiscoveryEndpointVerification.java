package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthEncryptAlgValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthEncryptEncValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthSignAlgValuesIsJsonArray;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointPARSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointResponseModesSupportedContainsJwt;
import net.openid.conformance.condition.client.CheckDiscEndpointResponseTypeCodeSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointScopesSupportedContainsOpenId;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupported;
import net.openid.conformance.condition.client.CheckDiscRequirePushedAuthorizationRequestsIsABoolean;
import net.openid.conformance.condition.client.FAPI2CheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import net.openid.conformance.ekyc.condition.client.EnsureAuthorizationResponseIssParameterSupportedIsTrue;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi2-security-profile-final-discovery-end-point-verification",
	displayName = "FAPI2-Security-Profile-Final: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configuration (including scopes, response_types, grant_types etc) contains values required by the specification",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
	}
)
@VariantParameters({
	AuthorizationRequestType.class,
	FAPI2FinalOPProfile.class,
	FAPI2SenderConstrainMethod.class,
	FAPI2AuthRequestMethod.class,
	FAPIResponseMode.class,
	FAPIOpenIDConnect.class
})
public class FAPI2SPFinalDiscoveryEndpointVerification extends AbstractFAPI2SPFinalDiscoveryEndpointVerification {

	private Class<? extends ConditionSequence> oidcChecks;

	protected Boolean signedRequest;

	protected boolean brazil = false;

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "fapi_client_credentials_grant")
	public void setupFapiClientCredentialsGrant() {
		profileBehavior = new ClientCredentialsGrantProfileBehavior();
		clientCredentialsGrant = true;
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		profileBehavior = new OpenBankingUkProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "consumerdataright_au")
	public void setupConsumerDataRightAu() {
		profileBehavior = new ConsumerDataRightAuProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		profileBehavior = new OpenBankingBrazilProfileBehavior();
		brazil = true;
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "connectid_au")
	public void setupConnectId() {
		profileBehavior = new ConnectIdAuProfileBehavior();
	}

	@VariantSetup(parameter = FAPIOpenIDConnect.class, value = "openid_connect")
	public void setupOidc() {
		oidcChecks = OidcDiscoveryEndpointChecks.class;
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "cbuae")
	public void setupCBUAE() {
		profileBehavior = new CbuaeProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "vci")
	public void setupVci() {
		profileBehavior = new VCIProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "vci_haip")
	public void setupVciHaip() {
		profileBehavior = new VCIHaipProfileBehavior();
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		signedRequest = getVariant(FAPI2AuthRequestMethod.class) == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION;
		isDpop = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.DPOP;
		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);
	}

	@Override
	protected void performEndpointVerification() {

		if (! clientCredentialsGrant) {
			callAndContinueOnFailure(CheckDiscEndpointResponseTypeCodeSupported.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.2.2-1");
			if (jarm) {
				callAndContinueOnFailure(CheckDiscEndpointResponseModesSupportedContainsJwt.class, Condition.ConditionResult.FAILURE, "JARM-2.3.4");
				callAndContinueOnFailure(CheckDiscEndpointAuthSignAlgValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
				callAndContinueOnFailure(CheckDiscEndpointAuthEncryptAlgValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
				callAndContinueOnFailure(CheckDiscEndpointAuthEncryptEncValuesIsJsonArray.class, Condition.ConditionResult.FAILURE, "JARM-4");
			} else {
				// https://bitbucket.org/openid/fapi/issues/478/fapi2-baseline-jarm-iss-draft
				callAndContinueOnFailure(EnsureAuthorizationResponseIssParameterSupportedIsTrue.class, Condition.ConditionResult.FAILURE, "OAuth2-iss-3", "FAPI2-SP-FINAL-5.3.2.2-7");
			}

			callAndContinueOnFailure(CheckDiscEndpointPARSupported.class, Condition.ConditionResult.FAILURE, "PAR-5", "FAPI2-SP-FINAL-5.3.2.2-2");
		}

		super.performEndpointVerification();

		if (! clientCredentialsGrant) {
			// although PAR is required by FAPI2, the server may support non-FAPI2-use-cases, so we can't require this to be 'true'
			callAndContinueOnFailure(CheckDiscRequirePushedAuthorizationRequestsIsABoolean.class, Condition.ConditionResult.FAILURE, "PAR-5");

			if (signedRequest) {
				callAndContinueOnFailure(FAPI2CheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);
			}

			callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE);
		}

		call(sequence(profileBehavior.getProfileSpecificDiscoveryChecks()));
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
}
