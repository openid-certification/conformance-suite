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
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.GenerateOpenBankingBrazilAccountsEndpointResponse;
import net.openid.conformance.testmodule.TestFailureException;

public class OpenBankingBrazilCibaRPProfileBehavior extends FAPICIBARPProfileBehavior {

	@Override
	public ConditionSequence applyProfileSpecificUserInfoChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToUserInfoClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificServerConfigurationSetup() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CheckCIBAModeIsPing.class, Condition.ConditionResult.FAILURE, "BrazilCIBA-5.2.2");
				callAndStopOnFailure(SetServerSigningAlgToPS256.class, "BrazilOB-6.1-1");
				callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-3");
				callAndStopOnFailure(FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificServerAuthAlgSetup() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
				callAndStopOnFailure(BrazilAddBackchannelAuthenticationRequestSigningAlgValuesSupportedToServer.class);
			}
		};
	}

	@Override
	public void exposeProfileSpecificEndpoints() {
		module.exposeMtlsPath("consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH);
		module.exposeMtlsPath("resource_endpoint", FAPIBrazilRsPathConstants.BRAZIL_RESOURCE_PATH);
	}

	@Override
	public ConditionSequence applyProfileSpecificAccountsEndpointChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts.class);
				Boolean wasInitialConsentRequestToPaymentsEndpoint = getEnv().getBoolean("payments_consent_endpoint_called");
				if (wasInitialConsentRequestToPaymentsEndpoint != null && wasInitialConsentRequestToPaymentsEndpoint) {
					throw new TestFailureException(module.getId(), FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + " was called. The test must end at the payment initiation endpoint");
				}
			}
		};
	}

	@Override
	public Class<? extends ConditionSequence> getAccountsEndpointResponseSteps() {
		return GenerateOpenBankingBrazilAccountsEndpointResponse.class;
	}

	@Override
	public ConditionSequence getClientCredentialsGrantTypeSteps() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificBackchannelRequestChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(BackchannelRequestRequestedExpiryIsIgnoredForBrazil.class, "BrazilCIBA-6.2.6");
				callAndStopOnFailure(EnsureLoginHintEqualsConsentId.class);
				callAndStopOnFailure(FAPIBrazilChangeConsentStatusToAuthorized.class);
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificBackchannelScopeChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilValidateConsentScope.class);
				Boolean wasInitialConsentRequestToPaymentsEndpoint = getEnv().getBoolean("payments_consent_endpoint_called");
				if (wasInitialConsentRequestToPaymentsEndpoint != null && wasInitialConsentRequestToPaymentsEndpoint) {
					callAndStopOnFailure(EnsureScopeContainsPayments.class);
				} else {
					callAndStopOnFailure(EnsureScopeContainsConsents.class);
					callAndStopOnFailure(EnsureScopeContainsResources.class);
				}
			}
		};
	}

	@Override
	public boolean requiresMtlsForBackchannelEndpoint() {
		return true;
	}

	@Override
	public ConditionSequence applyProfileSpecificTokenEndpointChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class);
				callAndStopOnFailure(FAPIBrazilValidateIdTokenSigningAlg.class, "BrazilOB-6.1-1");
				callAndStopOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
				callAndStopOnFailure(FAPIBrazilValidateExpiresIn.class, "BrazilOB-5.2.2-13");
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificIdTokenClaims() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(GenerateIdTokenClaimsWith181DayExp.class);
				callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificAcrClaim() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilOBAddACRClaimToIdTokenClaims.class)
					.skipIfStringsMissing("requested_id_token_acr_values")
					.onSkip(Condition.ConditionResult.INFO)
					.onFail(Condition.ConditionResult.FAILURE)
					.requirements("OIDCC-3.1.3.7-12")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public Class<? extends Condition> getSignIdTokenCondition() {
		return SignIdTokenWithX5tS256.class;
	}
}
