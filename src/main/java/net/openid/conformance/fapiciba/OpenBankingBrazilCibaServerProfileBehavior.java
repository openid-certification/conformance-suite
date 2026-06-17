package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckCIBAModeIsPing;
import net.openid.conformance.condition.client.CheckDiscEndpointAcrClaimSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.EnsureAccessTokenValuesAreDifferent;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported;
import net.openid.conformance.condition.client.FAPIBrazilValidateExpiresIn;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsCiba;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectEncryptionAlgValuesSupportedContainsRsaOaep;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointRequestObjectEncryptionEncValuesSupportedContainsA256gcm;
import net.openid.conformance.condition.client.SetHintTypeToLoginHint;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.sequence.client.RefreshTokenRequestSteps;
import net.openid.conformance.variant.ClientAuthType;

import java.util.function.Supplier;

public class OpenBankingBrazilCibaServerProfileBehavior extends FAPICIBAServerProfileBehavior {

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return OpenBankingBrazilDiscoveryEndpointChecks::new;
	}

	public static class OpenBankingBrazilDiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "BrazilOB-5.2.2-3");
			callAndContinueOnFailure(CheckDiscEndpointAcrClaimSupported.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-3", "BrazilOB-5.2.2-6");
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsCiba.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-6");
			callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould.class, Condition.ConditionResult.WARNING, "BrazilOB-5.2.2-7");
			callAndContinueOnFailure(CheckDiscEndpointUserinfoEndpoint.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
			callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectEncryptionAlgValuesSupportedContainsRsaOaep.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1.1-1");
			callAndContinueOnFailure(FAPICheckDiscEndpointRequestObjectEncryptionEncValuesSupportedContainsA256gcm.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1.1-1");
		}
	}

	@Override
	public boolean shouldKeepBackchannelAuthenticationEndpointAlias(ClientAuthType authType) {
		return true;
	}

	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return () -> {
			boolean isSecondClient = module.isSecondClient();
			boolean isDpop = false;
			boolean stopAfterConsentEndpoint = false;
			return new OpenBankingBrazilPreAuthorizationSteps(
				isSecondClient, isDpop, module.addTokenEndpointClientAuthentication, false, false, stopAfterConsentEndpoint, false
			);
		};
	}

	@Override
	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return AbstractFAPICIBAID1.OpenBankingBrazilProfileAuthorizationEndpointSetupSteps.class;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return AbstractFAPICIBAID1.OpenBankingBrazilProfileIdTokenValidationSteps.class;
	}

	@Override
	public ConditionSequence onConfigure() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CheckCIBAModeIsPing.class, "BrazilCIBA-5.2.2");
				callAndStopOnFailure(SetHintTypeToLoginHint.class, "BrazilCIBA-5.2.2");
			}
		};
	}

	@Override
	public ConditionSequence validateExpiresIn() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilValidateExpiresIn.class)
					.skipIfObjectsMissing("expires_in")
					.onSkip(Condition.ConditionResult.INFO)
					.onFail(Condition.ConditionResult.FAILURE)
					.requirements("BrazilOB-5.2.2-13")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence setupResourceEndpointRequestBody() {
		return null;
	}

	@Override
	public ConditionSequence createUpdateResourceRequestSteps(boolean isSecondClient, Class<? extends ConditionSequence> addTokenEndpointClientAuthentication) {
		return new RefreshTokenRequestSteps(isSecondClient, addTokenEndpointClientAuthentication)
				.skip(EnsureAccessTokenValuesAreDifferent.class, "");
	}

	@Override
	public ConditionSequence validateResourceEndpointResponse() {
		return null;
	}
}
