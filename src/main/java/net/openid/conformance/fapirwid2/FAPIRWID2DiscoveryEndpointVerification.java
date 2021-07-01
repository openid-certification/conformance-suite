package net.openid.conformance.fapirwid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointPARSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.CheckDiscRequirePushedAuthorizationRequestsIsABoolean;
import net.openid.conformance.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointGrantTypesSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointScopesSupported;
import net.openid.conformance.condition.client.FAPIRWCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.FAPIRWCheckDiscEndpointGrantTypesSupported;
import net.openid.conformance.condition.client.FAPIRWCheckDiscEndpointJARMResponseModesSupported;
import net.openid.conformance.condition.client.FAPIRWCheckDiscEndpointJARMResponseTypesSupported;
import net.openid.conformance.condition.client.FAPIRWCheckDiscEndpointResponseTypesSupported;
import net.openid.conformance.condition.client.FAPIRWCheckDiscEndpointScopesSupported;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.FAPIRWOPProfile;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi-rw-id2-discovery-end-point-verification",
	displayName = "FAPI-RW-ID2: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configuration (including scopes, response_types, grant_types etc) contains values required by the specification",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
	}
)
@VariantParameters({
	FAPIRWOPProfile.class,
	FAPIResponseMode.class,
	FAPIAuthRequestMethod.class
})
public class FAPIRWID2DiscoveryEndpointVerification extends AbstractFAPIDiscoveryEndpointVerification {

	private Class<? extends ConditionSequence> profileSpecificChecks;

	protected boolean jarm = false;

	protected boolean par = false;

	@VariantSetup(parameter = FAPIRWOPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		profileSpecificChecks = PlainFAPIDiscoveryEndpointChecks.class;
	}

	@VariantSetup(parameter = FAPIRWOPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		profileSpecificChecks = OpenBankingUkDiscoveryEndpointChecks.class;
	}

	@VariantSetup(parameter = FAPIRWOPProfile.class, value = "consumerdataright_au")
	public void setupConsumerDataRightAu() {
		profileSpecificChecks = PlainFAPIDiscoveryEndpointChecks.class;
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		jarm = getVariant(FAPIResponseMode.class) == FAPIResponseMode.JARM;
		par = getVariant(FAPIAuthRequestMethod.class) == FAPIAuthRequestMethod.PUSHED;
		super.configure(config, baseUrl, externalUrlOverride);
	}

	@Override
	protected void performEndpointVerification() {

		if (jarm) {
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointJARMResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "JARM-4.1.1");
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointJARMResponseModesSupported.class, Condition.ConditionResult.FAILURE, "JARM-4.3.4");
		} else {
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-2");
		}

		if (par) {
			callAndContinueOnFailure(CheckDiscEndpointPARSupported.class, Condition.ConditionResult.FAILURE, "PAR-5");
		}

		super.performEndpointVerification();

		if (par) {
			callAndContinueOnFailure(CheckDiscRequirePushedAuthorizationRequestsIsABoolean.class, Condition.ConditionResult.FAILURE, "PAR-5");
		} else {
			callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-1", "OIDCD-3");
		}

		callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(sequence(profileSpecificChecks));
	}

	public static class PlainFAPIDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class OpenBankingUkDiscoveryEndpointChecks extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckJwksUriIsHostedOnOpenBankingDirectory.class, Condition.ConditionResult.WARNING, "OBSP-3.4");

			// OB uk servers must support the client requesting the openbanking_intent_id so must support the client parameter
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "FAPI-RW-5.2.3-3");

			callAndContinueOnFailure(FAPIOBCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE, "OBSP-3.4");
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
		}
	}
}
