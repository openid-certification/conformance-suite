package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.client.AddCibaTokenDeliveryModePollToTokenDeliveryModesSupported;
import net.openid.conformance.testmodule.Environment;

public class FAPICIBARPProfileBehavior {

	protected AbstractFAPICIBAClientTest module;

	public void setModule(AbstractFAPICIBAClientTest module) {
		this.module = module;
	}

	public Environment getEnv() {
		return module.getEnv();
	}

	public void applyProfileSpecificServerConfigurationSetup() {
		module.callCondition(AddCibaTokenDeliveryModePollToTokenDeliveryModesSupported.class);
		module.callCondition(ExtractServerSigningAlg.class);
	}

	public void applyProfileSpecificServerAuthAlgSetup() {
		module.callCondition(FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		module.callCondition(AddBackchannelAuthenticationRequestSigningAlgValuesSupportedToServer.class);
	}

	public void exposeProfileSpecificEndpoints() {
		module.exposeMtlsPath("accounts_endpoint", AbstractFAPICIBAClientTest.ACCOUNTS_PATH);
	}

	public void applyProfileSpecificAccountsEndpointChecks() {
		// No-op by default
	}

	public void applyProfileSpecificBackchannelScopeChecks() {
		module.callCondition(EnsureRequestedScopeIsEqualToConfiguredScopeDisregardingOrder.class);
	}

	public boolean handleProfileSpecificClientCredentialsGrant() {
		return false; // Returns true if it handles the grant
	}

	public boolean requiresMtlsOrBrazilAuth() {
		return false; // Actually clientAuthType is MTLS or Brazil. Can be handled differently.
	}

	public void applyProfileSpecificBackchannelRequestChecks() {
		module.callConditionSkipIfMissing(null, new String[]{"backchannel_request_object"}, Condition.ConditionResult.SUCCESS,
			BackchannelRequestRequestedExpiryIsAnInteger.class, Condition.ConditionResult.FAILURE, "CIBA-7.1", "CIBA-7.1.1");
	}

	public void applyProfileSpecificBackchannelEndpointResponse() {
		// No-op by default
	}

	public void applyProfileSpecificIdTokenClaims() {
		module.callCondition(GenerateIdTokenClaims.class);
	}

	public void applyProfileSpecificAcrClaim() {
		module.callConditionSkipIfMissing(null, new String[]{"requested_id_token_acr_values"}, Condition.ConditionResult.INFO,
			AddACRClaimToIdTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
	}

	public void applyProfileSpecificTokenEndpointChecks() {
		// No-op by default
	}

	public void applyProfileSpecificUserInfoChecks() {
		// No-op by default
	}

	public Class<? extends Condition> getSignIdTokenCondition() {
		return SignIdToken.class;
	}
}
