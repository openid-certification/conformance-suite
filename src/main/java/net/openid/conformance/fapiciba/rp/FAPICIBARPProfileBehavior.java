package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.client.AddCibaTokenDeliveryModePollToTokenDeliveryModesSupported;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;

public class FAPICIBARPProfileBehavior {

	protected AbstractFAPICIBAClientTest module;

	public void setModule(AbstractFAPICIBAClientTest module) {
		this.module = module;
	}

	public Environment getEnv() {
		return module.getEnv();
	}

	public ConditionSequence applyProfileSpecificServerConfigurationSetup() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddCibaTokenDeliveryModePollToTokenDeliveryModesSupported.class);
				callAndStopOnFailure(ExtractServerSigningAlg.class);
			}
		};
	}

	public ConditionSequence getPingNotificationEndpointCallSteps() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(PingClientNotificationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");
			}
		};
	}

	public ConditionSequence applyProfileSpecificServerAuthAlgSetup() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
				callAndStopOnFailure(AddBackchannelAuthenticationRequestSigningAlgValuesSupportedToServer.class);
			}
		};
	}

	public void exposeProfileSpecificEndpoints() {
		module.exposeMtlsPath("accounts_endpoint", AbstractFAPICIBAClientTest.ACCOUNTS_PATH);
	}

	public ConditionSequence applyProfileSpecificAccountsEndpointChecks() {
		return null;
	}

	public Class<? extends ConditionSequence> getAccountsEndpointResponseSteps() {
		return null;
	}

	public ConditionSequence applyProfileSpecificBackchannelScopeChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(EnsureRequestedScopeIsEqualToConfiguredScopeDisregardingOrder.class);
			}
		};
	}

	public ConditionSequence getClientCredentialsGrantTypeSteps() {
		return null;
	}

	public boolean requiresMtlsForBackchannelEndpoint() {
		return false;
	}

	/**
	 * Access tokens are always mTLS certificate-bound in FAPI-CIBA regardless of profile or
	 * client authentication type - see the unconditional checkMtlsCertificate() call in
	 * AbstractFAPICIBAClientTest.tokenEndpoint(). Per RFC8705 section 3, any endpoint accepting
	 * such a token, including userinfo, must therefore be called over mTLS.
	 */
	public boolean userInfoEndpointRequiresMTLS() {
		return true;
	}

	public boolean claimsProfileSpecificMtlsPath(String path) {
		return false;
	}

	public boolean acceptsGenericAccountsEndpoint() {
		return true;
	}

	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		throw new UnsupportedOperationException("No profile-specific mTLS path handler is configured");
	}

	public ConditionSequence prepareNonResourceEndpointFapiInteractionId() {
		return null;
	}

	public ConditionSequence addFapiInteractionIdToTokenEndpointResponse() {
		return null;
	}

	public ConditionSequence addFapiInteractionIdToBackchannelEndpointResponse() {
		return null;
	}

	public ConditionSequence addFapiInteractionIdToUserInfoEndpointResponse() {
		return null;
	}

	public ConditionSequence applyProfileSpecificBackchannelRequestChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(BackchannelRequestRequestedExpiryIsAnInteger.class)
					.skipIfObjectsMissing("backchannel_request_object")
					.onSkip(Condition.ConditionResult.SUCCESS)
					.onFail(Condition.ConditionResult.FAILURE)
					.requirements("CIBA-7.1", "CIBA-7.1.1")
					.dontStopOnFailure());
			}
		};
	}

	public ConditionSequence applyProfileSpecificClientConfigurationValidation() {
		return null;
	}

	public ConditionSequence applyProfileSpecificBackchannelEndpointResponse() {
		return null;
	}

	public ConditionSequence applyProfileSpecificIdTokenClaims() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(GenerateIdTokenClaims.class);
			}
		};
	}

	public ConditionSequence applyProfileSpecificAcrClaim() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(AddACRClaimToIdTokenClaims.class)
					.skipIfStringsMissing("requested_id_token_acr_values")
					.onSkip(Condition.ConditionResult.INFO)
					.onFail(Condition.ConditionResult.FAILURE)
					.requirements("OIDCC-3.1.3.7-12")
					.dontStopOnFailure());
			}
		};
	}

	public ConditionSequence applyProfileSpecificTokenEndpointChecks() {
		return null;
	}

	public ConditionSequence applyProfileSpecificUserInfoChecks() {
		return null;
	}

	public Class<? extends Condition> getSignIdTokenCondition() {
		return SignIdToken.class;
	}

	public ConditionSequence applyProfileSpecificIdTokenEncryption() {
		return null;
	}
}
