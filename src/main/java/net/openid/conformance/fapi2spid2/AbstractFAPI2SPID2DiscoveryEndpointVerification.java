package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointDiscoveryUrl;
import net.openid.conformance.condition.client.CheckDiscEndpointIssuer;
import net.openid.conformance.condition.client.CheckDiscEndpointRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClient;
import net.openid.conformance.condition.client.CheckDiscoveryEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.CheckTLSClientCertificateBoundAccessTokensTrue;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsCodeChallengeMethodS256;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsMTLS;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import net.openid.conformance.condition.client.FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported;
import net.openid.conformance.condition.client.FAPI2CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported;
import net.openid.conformance.condition.client.FAPI2CheckDiscEndpointUserinfoSigningAlgValuesSupported;
import net.openid.conformance.condition.client.FAPI2CheckDpopSigningAlgValuesSupported;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@VariantParameters({
	ClientAuthType.class
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
public abstract class AbstractFAPI2SPID2DiscoveryEndpointVerification extends AbstractTestModule {
	private Class<? extends ConditionSequence> variantAuthChecks;
	private Class<? extends ConditionSequence> supportMTLSEndpointAliases;

	protected Boolean isDpop;

	public static class MtlsChecks extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(EnsureServerConfigurationSupportsMTLS.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.1-5");

		}
	}

	public static class PrivateKeyJWTChecks extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(EnsureServerConfigurationSupportsPrivateKeyJwt.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.1-6");

		}
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		callAndStopOnFailure(GetDynamicServerConfiguration.class);
		callAndContinueOnFailure(EnsureDiscoveryEndpointResponseStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDCD-4");
		callAndContinueOnFailure(CheckDiscoveryEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4");

		if (supportMTLSEndpointAliases != null) {
			call(sequence(supportMTLSEndpointAliases));
		}

		setStatus(Status.CONFIGURED);
		fireSetupDone();

	}

	protected void performEndpointVerification() {

		callAndContinueOnFailure(CheckDiscEndpointDiscoveryUrl.class,Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointIssuer.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3", "OIDCD-7.2");

		if (isDpop) {
			callAndContinueOnFailure(FAPI2CheckDpopSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4-1");
		} else {
			callAndContinueOnFailure(CheckTLSClientCertificateBoundAccessTokensTrue.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.1-5", "RFC8705-3.3");
		}

		callAndContinueOnFailure(FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4-1");

		callAndContinueOnFailure(CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClient.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.1-6");
		callAndContinueOnFailure(FAPI2CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.4-1");

		call(condition(FAPI2CheckDiscEndpointUserinfoSigningAlgValuesSupported.class)
			.skipIfElementMissing("server", "userinfo_signing_alg_values_supported")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("FAPI2-SP-ID2-5.4")
			.dontStopOnFailure()
		);

		callAndContinueOnFailure(CheckDiscEndpointTokenEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

		call(condition(CheckDiscEndpointRegistrationEndpoint.class)
			.skipIfElementMissing("server", "registration_endpoint")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("OIDCD-3")
			.dontStopOnFailure()
		);

		callAndContinueOnFailure(CheckJwksUri.class, Condition.ConditionResult.FAILURE, "OIDCD-3");

		callAndContinueOnFailure(EnsureServerConfigurationSupportsCodeChallengeMethodS256.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.2-5");

		call(sequence(variantAuthChecks));
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		performEndpointVerification();

		fireTestFinished();

	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		variantAuthChecks = MtlsChecks.class;
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		variantAuthChecks = PrivateKeyJWTChecks.class;

		if (getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS) {
			supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		}
	}
}
