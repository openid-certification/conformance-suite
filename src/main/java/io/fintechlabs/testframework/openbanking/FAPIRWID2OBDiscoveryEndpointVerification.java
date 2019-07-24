package io.fintechlabs.testframework.openbanking;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointClaimsSupported;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointScopesSupported;
import io.fintechlabs.testframework.fapi.FAPIRWID2;
import io.fintechlabs.testframework.fapi.FAPIRWID2DiscoveryEndpointVerification;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
		testName = "fapi-rw-id2-ob-discovery-end-point-verification",
		displayName = "FAPI-RW-ID2-OB: Discovery Endpoint Verification",
		summary = "This test ensures that the server's configurations (including scopes, response_types, grant_types etc) is containing the required value in the specification",
		profile = "FAPI-RW-ID2-OB",
		configurationFields = {
			"server.discoveryUrl",
		},
		notApplicableForVariants = {
			FAPIRWID2.variant_mtls,
			FAPIRWID2.variant_privatekeyjwt
		}
)

public class FAPIRWID2OBDiscoveryEndpointVerification extends FAPIRWID2DiscoveryEndpointVerification {

	@Variant(name = FAPIRWID2.variant_openbankinguk_mtls)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Variant(name = FAPIRWID2.variant_openbankinguk_privatekeyjwt)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	@Override
	protected void performProfileSpecificChecks() {
		callAndContinueOnFailure(CheckJwksUriIsHostedOnOpenBankingDirectory.class, Condition.ConditionResult.WARNING, "OBSP-3.4");

		callAndContinueOnFailure(FAPIOBCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE, "OBSP-3.4");
		callAndContinueOnFailure(FAPIOBCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(FAPIOBCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
	}
}
