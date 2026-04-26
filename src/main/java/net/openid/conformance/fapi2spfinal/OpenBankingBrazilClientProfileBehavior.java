package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.EnsureScopeContainsAccounts;
import net.openid.conformance.condition.as.EnsureScopeContainsPayments;
import net.openid.conformance.condition.as.FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToUserInfoClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPIBrazilChangeConsentStatusToAuthorized;
import net.openid.conformance.condition.as.FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant;
import net.openid.conformance.condition.as.FAPIBrazilOBAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilSetGrantTypesSupportedInServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.client.FAPIBrazilValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts;
import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.GenerateOpenBankingBrazilAccountsEndpointResponse;
import net.openid.conformance.testmodule.TestFailureException;

/**
 * Profile behavior for OpenBanking Brazil client tests.
 * Requires mTLS everywhere; uses Brazil-specific server config (PS256, claims_supported,
 * Brazil grant types, Brazil settings); exposes Brazil mtls paths (accounts, consents,
 * payments-consents, payment-initiation); validates consent scope and CPF/CPNJ claims.
 *
 * <p>The actual request handlers for Brazil consent / payments paths currently live on
 * {@link AbstractFAPI2SPFinalClientTest} as {@code protected} helpers; this behavior only
 * dispatches to them. They will be moved into this class in a follow-up MR.
 */
public class OpenBankingBrazilClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public ConditionSequence addProfileSpecificServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(SetServerSigningAlgToPS256.class, "BrazilOB-6.1");
				// FIXME - BrazilOB-5.2.3-5 seems incorrect, but no obvious replacement.
				callAndStopOnFailure(FAPIBrazilSetGrantTypesSupportedInServerConfiguration.class);
				callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-4");
				callAndStopOnFailure(FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
			}
		};
	}

	@Override
	public Class<? extends Condition> getTokenEndpointAuthSigningAlgsCondition() {
		return FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class;
	}

	@Override
	public void exposeProfileEndpoints() {
		module.exposeMtlsPath("accounts_endpoint", FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH);
		module.exposeMtlsPath("consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH);
		module.exposeMtlsPath("payments_consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH);
		module.exposeMtlsPath("payment_initiation_path", FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH);
	}

	@Override
	public Class<? extends ConditionSequence> getAccountsEndpointProfileSteps() {
		return GenerateOpenBankingBrazilAccountsEndpointResponse.class;
	}

	@Override
	public boolean claimsHttpMtlsPath(String path) {
		return FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH.equals(path)
			|| path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/")
			|| FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH.equals(path)
			|| path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/")
			|| FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		if (FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH.equals(path)) {
			return module.brazilHandleNewConsentRequest(requestId, false);
		}
		if (path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/")) {
			return module.brazilHandleGetConsentRequest(requestId, path, false);
		}
		if (FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH.equals(path)) {
			return module.brazilHandleNewConsentRequest(requestId, true);
		}
		if (path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/")) {
			return module.brazilHandleGetConsentRequest(requestId, path, true);
		}
		// BRAZIL_PAYMENT_INITIATION_PATH
		return module.brazilHandleNewPaymentInitiationRequest(requestId);
	}

	@Override
	public boolean supportsClientCredentialsGrant() {
		return true;
	}

	@Override
	public ConditionSequence preClientCredentialsGrantSetup() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
			}
		};
	}

	@Override
	public ConditionSequence customizeAuthorizationEndpoint() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilChangeConsentStatusToAuthorized.class);
			}
		};
	}

	@Override
	public ConditionSequence validateIdTokenAcrClaims() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilValidateRequestObjectIdTokenACRClaims.class)
					.onFail(ConditionResult.FAILURE)
					.requirements("OIDCC-5.5.1.1", "BrazilOB-5.2.2-5", "BrazilOB-5.2.2-6")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence validateAuthorizationRequestScope() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilValidateConsentScope.class);
				Boolean wasInitialConsentRequestToPaymentsEndpoint =
					module.getEnv().getBoolean("payments_consent_endpoint_called");
				if (Boolean.TRUE.equals(wasInitialConsentRequestToPaymentsEndpoint)) {
					callAndStopOnFailure(EnsureScopeContainsPayments.class);
				} else {
					callAndStopOnFailure(EnsureScopeContainsAccounts.class);
				}
			}
		};
	}

	@Override
	public ConditionSequence customizeIdTokenClaimsAfterGenerate() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class,
					"BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10");
			}
		};
	}

	@Override
	public Class<? extends Condition> getAddAcrClaimToIdTokenClaimsCondition() {
		return FAPIBrazilOBAddACRClaimToIdTokenClaims.class;
	}

	@Override
	public ConditionSequence customizeUserInfoResponse() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToUserInfoClaims.class,
					"BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10");
			}
		};
	}

	@Override
	public ConditionSequence validateAccountsEndpointRequest() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts.class);
				Boolean wasInitialConsentRequestToPaymentsEndpoint =
					module.getEnv().getBoolean("payments_consent_endpoint_called");
				if (Boolean.TRUE.equals(wasInitialConsentRequestToPaymentsEndpoint)) {
					throw new TestFailureException(module.getId(),
						FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH
							+ " was called. The test must end at the payment initiation endpoint");
				}
			}
		};
	}
}
