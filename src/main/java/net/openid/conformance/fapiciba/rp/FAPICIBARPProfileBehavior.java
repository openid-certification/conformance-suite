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

	public boolean userInfoEndpointRequiresMTLS() {
		return false;
	}

	public boolean claimsProfileSpecificMtlsPath(String path) {
		return false;
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
}
