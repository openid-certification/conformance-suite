package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointClaimsParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointClaimsSupported;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointScopesSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointClaimsSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointResponseTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointScopesSupported;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-discovery-end-point-verification",
	displayName = "FAPI-RW-ID2: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations (including scopes, response_types, grant_types etc) is containing the required value in the specification",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
	}
)
public class FAPIRWID2DiscoveryEndpointVerification extends AbstractFAPIDiscoveryEndpointVerification {

	private Class<? extends ConditionSequence> profileSpecificChecks;

	@Variant(name = FAPIRWID2.variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
		profileSpecificChecks = PlainFAPIDiscoveryEndpointChecks.class;
	}

	@Variant(name = FAPIRWID2.variant_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
		profileSpecificChecks = PlainFAPIDiscoveryEndpointChecks.class;
	}

	@Variant(name = FAPIRWID2.variant_openbankinguk_mtls)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
		profileSpecificChecks = OpenBankingUkDiscoveryEndpointChecks.class;
	}

	@Variant(name = FAPIRWID2.variant_openbankinguk_privatekeyjwt)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
		profileSpecificChecks = OpenBankingUkDiscoveryEndpointChecks.class;
	}

	@Override
	protected void performEndpointVerification() {

		callAndContinueOnFailure(FAPIRWCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-2");

		super.performEndpointVerification();

		callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.WARNING, "OB-7.1-1");
		callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE);

		// FAPI-RW ID2 servers are required to return acrs, which means they must support requesting the acr claim,
		// hence must support the claims parameter
		callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "FAPI-RW-5.2.3-3");

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

			callAndContinueOnFailure(FAPIOBCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE, "OBSP-3.4");
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
		}
	}
}
