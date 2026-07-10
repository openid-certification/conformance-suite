package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.CheckCIBAModeIsPing;
import net.openid.conformance.condition.as.EnsureScopeContainsConsents;
import net.openid.conformance.condition.as.EnsureScopeContainsResources;
import net.openid.conformance.condition.as.EncryptIdToken;
import net.openid.conformance.condition.as.FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToUserInfoClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPIBrazilChangeConsentStatusToAuthorized;
import net.openid.conformance.condition.as.FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant;
import net.openid.conformance.condition.as.FAPIBrazilOBAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilSetRequiredIdTokenEncryptionConfig;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.FAPIEnsureClientJwksContainsAnEncryptionKey;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
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
				callAndStopOnFailure(CheckCIBAModeIsPing.class, Condition.ConditionResult.FAILURE, "BrazilCIBA-6.3.4");
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
	public ConditionSequence getClientCredentialsGrantTypeSteps() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificClientConfigurationValidation() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilSetRequiredIdTokenEncryptionConfig.class, "BrazilOB-5.1.1-1");
				callAndStopOnFailure(FAPIEnsureClientJwksContainsAnEncryptionKey.class, "FAPI1-ADV-5.2.3.1-5", "FAPI1-ADV-8.6.1-1");
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificBackchannelRequestChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(EnsureBackchannelRequestDoesNotContainRequestedExpiryForBrazil.class, "BrazilCIBA-6.3.7");
				callAndStopOnFailure(EnsureBackchannelRequestObjectDoesNotContainUserCode.class, "BrazilCIBA-6.3.5");
				callAndStopOnFailure(EnsureBackchannelRequestObjectBindingMessageDoesNotContainUrl.class, "BrazilCIBA-6.3.6");
				callAndStopOnFailure(EnsureLoginHintEqualsConsentId.class, "BrazilCIBA-6.3.2");
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
				callAndStopOnFailure(EnsureScopeContainsConsents.class);
				callAndStopOnFailure(EnsureScopeContainsResources.class);
			}
		};
	}

	@Override
	public boolean requiresMtlsForBackchannelEndpoint() {
		return true;
	}

	@Override
	public boolean claimsProfileSpecificMtlsPath(String path) {
		return FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH.equals(path)
			|| FAPIBrazilRsPathConstants.BRAZIL_RESOURCE_PATH.equals(path)
			|| path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/");
	}

	@Override
	public boolean acceptsGenericAccountsEndpoint() {
		return false;
	}

	@Override
	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		if (FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH.equals(path)) {
			return module.brazilHandleNewConsentRequest(requestId, false);
		}
		if (FAPIBrazilRsPathConstants.BRAZIL_RESOURCE_PATH.equals(path)) {
			return module.resourcesEndpoint(requestId);
		}
		if (path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/")) {
			return module.brazilHandleGetConsentRequest(requestId, path, false);
		}
		throw new TestFailureException(module.getId(), "Got unexpected Open Banking Brazil mTLS call to " + path);
	}

	@Override
	public ConditionSequence applyProfileSpecificIdTokenClaims() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(GenerateIdTokenClaims.class);
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
	public ConditionSequence applyProfileSpecificIdTokenEncryption() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(EncryptIdToken.class, "OIDCC-10.2", "FAPI1-ADV-5.2.2.1-6", "BrazilOB-5.1.1-1");
			}
		};
	}

}
