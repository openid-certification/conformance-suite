package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.TestExecutionUnit;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIWalletAuthorizationCodeFlowVariant;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOffer;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOfferRedirectUrl;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOfferUri;
import net.openid.conformance.vci10wallet.condition.VCIGenerateIssuerState;
import net.openid.conformance.vci10wallet.condition.VCIInjectCredentialConfigurationIdHint;
import net.openid.conformance.vci10wallet.condition.VCIPreparePreAuthorizationCode;
import net.openid.conformance.vci10wallet.condition.VCIVerifyIssuerStateInAuthorizationRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the issuer-initiated authorization code flow support that
 * {@link VCIClientProfileBehavior} layers on top of the FAPI2SP client tests when they run
 * inside the VCI wallet plan (GitLab #1901: no credential offer was presented, so the
 * wallet had nothing to scan and the tests hung).
 */
public class VCIClientProfileBehavior_UnitTest {

	/** Minimal concrete FAPI2SP client module so the behavior has something to read variants from. */
	private static class StubModule extends AbstractFAPI2SPFinalClientTest {
		@Override
		protected void addCustomValuesToIdToken() {
		}
	}

	private VCIClientProfileBehavior behaviorFor(VCIWalletAuthorizationCodeFlowVariant flow,
												 VCICredentialOfferParameterVariant offerVariant) {
		Map<Class<? extends Enum<?>>, Enum<?>> variant = new HashMap<>();
		variant.put(VCIGrantType.class, VCIGrantType.AUTHORIZATION_CODE);
		if (flow != null) {
			variant.put(VCIWalletAuthorizationCodeFlowVariant.class, flow);
		}
		if (offerVariant != null) {
			variant.put(VCICredentialOfferParameterVariant.class, offerVariant);
		}
		StubModule module = new StubModule();
		module.setVariant(variant);
		VCIClientProfileBehavior behavior = new VCIClientProfileBehavior();
		behavior.setModule(module);
		return behavior;
	}

	@Test
	public void issuerInitiatedIsDetectedFromPlanLevelVariant() {
		assertThat(behaviorFor(VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED, null).isIssuerInitiated()).isTrue();
		assertThat(behaviorFor(VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED, null).isIssuerInitiated()).isFalse();
	}

	@Test
	public void missingFlowVariantDefaultsToWalletInitiated() {
		// FAPI2SP client modules don't declare the VCI wallet variants; outside the wallet
		// plan there is no plan-level context, so the variant's default must apply.
		assertThat(behaviorFor(null, null).isIssuerInitiated()).isFalse();
		assertThat(behaviorFor(null, null).issuerInitiatedSetupSteps()).isNull();
		assertThat(behaviorFor(null, null).additionalAuthorizationRequestChecks()).isNull();
	}

	@Test
	public void issuerInitiatedSetupGeneratesIssuerStateAndCredentialConfigurationIdHint() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED,
			VCICredentialOfferParameterVariant.BY_REFERENCE);

		assertThat(getConditionClasses(behavior.issuerInitiatedSetupSteps()))
			.containsExactly(VCIGenerateIssuerState.class, VCIInjectCredentialConfigurationIdHint.class);
	}

	@Test
	public void walletInitiatedHasNoIssuerInitiatedSetup() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED, null);

		assertThat(behavior.issuerInitiatedSetupSteps()).isNull();
	}

	@Test
	public void credentialOfferStepsByReferenceCreateOfferUriAndRedirectUrl() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED,
			VCICredentialOfferParameterVariant.BY_REFERENCE);

		assertThat(getConditionClasses(behavior.credentialOfferSteps()))
			.containsExactly(
				VCICreateCredentialOffer.class,
				VCICreateCredentialOfferUri.class,
				VCICreateCredentialOfferRedirectUrl.class);
	}

	@Test
	public void credentialOfferStepsByValueSkipOfferUri() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED,
			VCICredentialOfferParameterVariant.BY_VALUE);

		assertThat(getConditionClasses(behavior.credentialOfferSteps()))
			.containsExactly(
				VCICreateCredentialOffer.class,
				VCICreateCredentialOfferRedirectUrl.class);
	}

	@Test
	public void sharedCredentialOfferStepsPrepareCodeForPreAuthorizedCodeGrant() {
		// The static helper is also what AbstractVCIWalletTest.prepareCredentialOffer uses,
		// so it must cover the pre-authorized code grant the wallet modules support.
		assertThat(getConditionClasses(VCIClientProfileBehavior.credentialOfferSteps(
			VCIGrantType.PRE_AUTHORIZATION_CODE, VCICredentialOfferParameterVariant.BY_REFERENCE)))
			.containsExactly(
				VCIPreparePreAuthorizationCode.class,
				VCICreateCredentialOffer.class,
				VCICreateCredentialOfferUri.class,
				VCICreateCredentialOfferRedirectUrl.class);
	}

	@Test
	public void defaultCredentialConfigurationIdFollowsCredentialFormat() {
		assertThat(VCIClientProfileBehavior.defaultCredentialConfigurationId(VCI1FinalCredentialFormat.MDOC))
			.isEqualTo("eu.europa.ec.eudi.pid.mdoc.1");
		assertThat(VCIClientProfileBehavior.defaultCredentialConfigurationId(VCI1FinalCredentialFormat.SD_JWT_VC))
			.isEqualTo("eu.europa.ec.eudi.pid.1");
		assertThat(VCIClientProfileBehavior.defaultCredentialConfigurationId(null))
			.isEqualTo("eu.europa.ec.eudi.pid.1");
	}

	@Test
	public void credentialOfferStepsDefaultToByValueWhenOfferVariantMissing() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED, null);

		assertThat(getConditionClasses(behavior.credentialOfferSteps()))
			.containsExactly(
				VCICreateCredentialOffer.class,
				VCICreateCredentialOfferRedirectUrl.class);
	}

	@Test
	public void issuerInitiatedVerifiesIssuerStateAtAuthorizationEndpoint() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED,
			VCICredentialOfferParameterVariant.BY_VALUE);

		assertThat(getConditionClasses(behavior.additionalAuthorizationRequestChecks()))
			.containsExactly(VCIVerifyIssuerStateInAuthorizationRequest.class);
	}

	@Test
	public void walletInitiatedDoesNotVerifyIssuerState() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED, null);

		assertThat(behavior.additionalAuthorizationRequestChecks()).isNull();
	}

	@Test
	public void claimsCredentialOfferByReferencePath() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED,
			VCICredentialOfferParameterVariant.BY_REFERENCE);

		assertThat(behavior.claimsHttpPath("credential_offer/abc123")).isTrue();
		// existing VCI paths must keep working
		assertThat(behavior.claimsHttpPath("credential")).isTrue();
		assertThat(behavior.claimsHttpPath("nonce")).isTrue();
		assertThat(behavior.claimsHttpPath("notification")).isTrue();
		// and unrelated paths are still left to the module's default routing
		assertThat(behavior.claimsHttpPath("authorize")).isFalse();
		assertThat(behavior.claimsHttpPath("token")).isFalse();
	}

	@Test
	public void credentialOfferPathDispatchServesOfferForMatchingIdAndNotFoundOtherwise() {
		VCIClientProfileBehavior behavior = behaviorFor(VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED,
			VCICredentialOfferParameterVariant.BY_REFERENCE);
		AbstractFAPI2SPFinalClientTest module = behavior.module;
		module.getEnv().putString("vci", "credential_offer_id", "abc123");
		com.google.gson.JsonObject offer = new com.google.gson.JsonObject();
		offer.addProperty("credential_issuer", "https://issuer.example/");
		module.getEnv().putObject("vci", "credential_offer", offer);

		FAPI2ClientProfileBehavior.PathDispatch match = behavior.getProfileSpecificPathDispatch("req1", "credential_offer/abc123");
		assertThat(match).isNotNull();
		assertThat(match.blockName()).isEqualTo("Credential offer endpoint");
		Object matchResponse = match.responseBuilder().apply(module);
		assertThat(matchResponse).isInstanceOf(org.springframework.http.ResponseEntity.class);
		org.springframework.http.ResponseEntity<?> matchEntity = (org.springframework.http.ResponseEntity<?>) matchResponse;
		assertThat(matchEntity.getStatusCode().value()).isEqualTo(200);
		assertThat(matchEntity.getHeaders().getCacheControl()).isEqualTo("no-cache");
		assertThat(matchEntity.getBody()).isEqualTo(offer);

		FAPI2ClientProfileBehavior.PathDispatch miss = behavior.getProfileSpecificPathDispatch("req2", "credential_offer/other");
		assertThat(miss).isNotNull();
		org.springframework.http.ResponseEntity<?> missEntity = (org.springframework.http.ResponseEntity<?>) miss.responseBuilder().apply(module);
		assertThat(missEntity.getStatusCode().value()).isEqualTo(404);
	}

	private List<Class<? extends Condition>> getConditionClasses(ConditionSequence sequence) {
		sequence.evaluate();
		return sequence.getTestExecutionUnits().stream()
			.<Class<? extends Condition>>map(this::getConditionClass)
			.toList();
	}

	private Class<? extends Condition> getConditionClass(TestExecutionUnit unit) {
		return ((ConditionCallBuilder) unit).getConditionClass();
	}
}
