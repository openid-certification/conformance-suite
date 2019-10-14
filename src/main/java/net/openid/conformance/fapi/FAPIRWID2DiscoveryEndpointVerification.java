package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
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
import net.openid.conformance.testmodule.Variant;

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

	protected boolean jarm = false;

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

	@Variant(name = FAPIRWID2.variant_mtls_jarm)
	public void setupMTLSJarm() {
		jarm = true;
		// FIXME: need JARM variant
		setupMTLS();
	}

	@Variant(name = FAPIRWID2.variant_privatekeyjwt_jarm)
	public void setupPrivateKeyJwtJarm() {
		jarm = true;
		// FIXME: need JARM variant
		setupPrivateKeyJwt();
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

		if (jarm) {
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointJARMResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "JARM-4.1.1");
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointJARMResponseModesSupported.class, Condition.ConditionResult.FAILURE, "JARM-4.3.4");
		} else {
			callAndContinueOnFailure(FAPIRWCheckDiscEndpointResponseTypesSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-2");
		}

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
