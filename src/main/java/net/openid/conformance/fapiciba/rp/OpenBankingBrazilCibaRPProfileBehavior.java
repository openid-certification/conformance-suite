package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.CheckCIBAModeIsPing;
import net.openid.conformance.condition.as.EnsureScopeContainsConsents;
import net.openid.conformance.condition.as.EnsureScopeContainsPayments;
import net.openid.conformance.condition.as.EnsureScopeContainsResources;
import net.openid.conformance.condition.as.FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToUserInfoClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPIBrazilChangeConsentStatusToAuthorized;
import net.openid.conformance.condition.as.FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant;
import net.openid.conformance.condition.as.FAPIBrazilOBAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.GenerateIdTokenClaimsWith181DayExp;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.as.SignIdTokenWithX5tS256;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.FAPIBrazilValidateExpiresIn;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts;
import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.testmodule.TestFailureException;

public class OpenBankingBrazilCibaRPProfileBehavior extends FAPICIBARPProfileBehavior {

	@Override
	public void applyProfileSpecificUserInfoChecks() {
		module.callCondition(FAPIBrazilAddCPFAndCPNJToUserInfoClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
	}

	@Override
	public void applyProfileSpecificServerConfigurationSetup() {
		module.callCondition(CheckCIBAModeIsPing.class, Condition.ConditionResult.FAILURE, "BrazilCIBA-5.2.2");
		module.callCondition(SetServerSigningAlgToPS256.class, "BrazilOB-6.1-1");
		module.callCondition(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-3");
		module.callCondition(FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
	}

	@Override
	public void applyProfileSpecificServerAuthAlgSetup() {
		module.callCondition(FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		module.callCondition(BrazilAddBackchannelAuthenticationRequestSigningAlgValuesSupportedToServer.class);
	}

	@Override
	public void exposeProfileSpecificEndpoints() {
		module.exposeMtlsPath("consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH);
		module.exposeMtlsPath("resource_endpoint", FAPIBrazilRsPathConstants.BRAZIL_RESOURCE_PATH);
	}

	@Override
	public void applyProfileSpecificAccountsEndpointChecks() {
		module.callCondition(FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts.class);
		Boolean wasInitialConsentRequestToPaymentsEndpoint = getEnv().getBoolean("payments_consent_endpoint_called");
		if (wasInitialConsentRequestToPaymentsEndpoint != null && wasInitialConsentRequestToPaymentsEndpoint) {
			throw new TestFailureException(module.getId(), FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + " was called. The test must end at the payment initiation endpoint");
		}
	}

	@Override
	public boolean handleProfileSpecificClientCredentialsGrant() {
		module.callCondition(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
		return true; // We handled it, the module should return clientCredentialsGrantType()
	}

	@Override
	public void applyProfileSpecificBackchannelRequestChecks() {
		module.callCondition(BackchannelRequestRequestedExpiryIsIgnoredForBrazil.class, "BrazilCIBA-6.2.6");

		if (getEnv().getElementFromObject("backchannel_request_object", "claims.login_hint") != null) {
			module.callCondition(EnsureLoginHintEqualsConsentId.class, Condition.ConditionResult.FAILURE);
		} else {
			throw new TestFailureException(module.getId(), "Open Banking/Insurance Brazil requires login_hint.");
		}

		module.callCondition(FAPIBrazilChangeConsentStatusToAuthorized.class);
	}

	@Override
	public void applyProfileSpecificBackchannelScopeChecks() {
		module.callCondition(FAPIBrazilValidateConsentScope.class);
		Boolean wasInitialConsentRequestToPaymentsEndpoint = getEnv().getBoolean("payments_consent_endpoint_called");
		if (wasInitialConsentRequestToPaymentsEndpoint != null && wasInitialConsentRequestToPaymentsEndpoint) {
			module.callCondition(EnsureScopeContainsPayments.class);
		} else {
			module.callCondition(EnsureScopeContainsConsents.class);
			module.callCondition(EnsureScopeContainsResources.class);
		}
	}

	@Override
	public boolean requiresMtlsOrBrazilAuth() {
		return true; // Brazil requires it
	}

	@Override
	public void applyProfileSpecificTokenEndpointChecks() {
		module.callCondition(ExtractIdTokenFromTokenResponse.class);
		module.callCondition(FAPIBrazilValidateIdTokenSigningAlg.class, "BrazilOB-6.1-1");
		module.callCondition(ExtractExpiresInFromTokenEndpointResponse.class);
		module.callCondition(FAPIBrazilValidateExpiresIn.class, "BrazilOB-5.2.2-13");
	}

	@Override
	public void applyProfileSpecificIdTokenClaims() {
		module.callCondition(GenerateIdTokenClaimsWith181DayExp.class);
		module.callCondition(FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
	}

	@Override
	public void applyProfileSpecificAcrClaim() {
		module.callConditionSkipIfMissing(null, new String[]{"requested_id_token_acr_values"}, Condition.ConditionResult.INFO,
			FAPIBrazilOBAddACRClaimToIdTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
	}

	@Override
	public Class<? extends Condition> getSignIdTokenCondition() {
		return SignIdTokenWithX5tS256.class;
	}
}
