// author: ddrysdale

package io.fintechlabs.testframework.openbanking;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointClaimsSupported;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIOBCheckDiscEndpointScopesSupported;
import io.fintechlabs.testframework.fapi.FAPIRWID2DiscoveryEndpointVerification;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
		testName = "fapi-rw-id2-ob-discovery-end-point-verification",
		displayName = "FAPI-RW-ID2-OB: Discovery Endpoint Verification",
		summary = "This test ensures that the server's configurations (including scopes, response_types, grant_types etc) is containing the required value in the specification",
		profile = "FAPI-RW-ID2-OB",
		configurationFields = {
			"server.discoveryUrl",
		}
)

public class FAPIRWID2OBDiscoveryEndpointVerification extends FAPIRWID2DiscoveryEndpointVerification {

	@Override
	protected void performProfileSpecificChecks() {
		callAndContinueOnFailure(CheckJwksUriIsHostedOnOpenBankingDirectory.class, Condition.ConditionResult.WARNING);

		callAndContinueOnFailure(FAPIOBCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(FAPIOBCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(FAPIOBCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
	}
}
