package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointDiscoveryUrl;
import net.openid.conformance.condition.client.CheckDiscEndpointIdTokenSigningAlgValuesSupportedContainsPS256OrES256;
import net.openid.conformance.condition.client.CheckDiscEndpointIssuer;
import net.openid.conformance.condition.client.CheckDiscEndpointRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClient;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported;
import net.openid.conformance.condition.client.CheckDiscoveryEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.CheckTLSClientCertificateBoundAccessTokensTrue;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsMTLS;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointUserinfoSigningAlgValuesSupported;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@VariantParameters({
	ClientAuthType.class
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
public abstract class AbstractFAPI1AdvancedFinalDiscoveryEndpointVerification extends AbstractTestModule {
	private Class<? extends ConditionSequence> variantAuthChecks;
	private Class<? extends ConditionSequence> supportMTLSEndpointAliases;

	public static class MtlsChecks extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(EnsureServerConfigurationSupportsMTLS.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-6");

		}
	}

	public static class PrivateKeyJWTChecks extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(EnsureServerConfigurationSupportsPrivateKeyJwt.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-6");

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

		callAndContinueOnFailure(CheckTLSClientCertificateBoundAccessTokensTrue.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-6", "RFC8705-3.3");

		callAndContinueOnFailure(CheckDiscEndpointIdTokenSigningAlgValuesSupportedContainsPS256OrES256.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-8.6");

		callAndContinueOnFailure(CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClient.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-14");
		callAndContinueOnFailure(CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-8.6");

		call(condition(FAPICheckDiscEndpointUserinfoSigningAlgValuesSupported.class)
			.skipIfElementMissing("server", "userinfo_signing_alg_values_supported")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("FAPI1-ADV-8.6")
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
		// FAPI requires the use of MTLS sender constrained access tokens, so we must use the MTLS version of the
		// token endpoint even when using private_key_jwt client authentication
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
	}
}
