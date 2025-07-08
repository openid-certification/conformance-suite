package net.openid.conformance.openid;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.FAPIBrazilEncryptRequestObject;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddAuthorizationResponseSigningAlgES256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddAuthorizationResponseSigningAlgPS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddAuthorizationResponseSigningAlgRS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientIdToRequestObject;
import net.openid.conformance.condition.client.AddClientNameToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientRegistrationOptionsToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientX509CertificateClaimToPublicJWKs;
import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddContactsToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddDpopBoundAccessTokensTrueToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddImplicitGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddNbfToRequestObject;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddPlainErrorResponseAsAuthorizationEndpointResponseForJARM;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRequestObjectSigningAlgES256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRequestObjectSigningAlgPS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRequestObjectSigningAlgRS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddResponseTypesArrayToDynamicRegistrationRequestFromEnvironment;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddTLSBoundAccessTokensTrueToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthMethodSelfSignedTlsToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment;
import net.openid.conformance.condition.client.AddTokenEndpointAuthSigningAlgES256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthSigningAlgPS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthSigningAlgRS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectByValueRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectPostToPAREndpoint;
import net.openid.conformance.condition.client.BuildUnsignedPAREndpointRequest;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CallPAREndpointAllowingDpopNonceError;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallProtectedResourceAllowingDpopNonceError;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckCallbackContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.CheckCallbackHttpMethodIsPost;
import net.openid.conformance.condition.client.CheckForPARResponseExpiresIn;
import net.openid.conformance.condition.client.CheckForRequestUriValue;
import net.openid.conformance.condition.client.CheckPAREndpointResponse201WithNoError;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateEmptyDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureMinimumRequestUriEntropy;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponseFromJARMResponse;
import net.openid.conformance.condition.client.ExtractJARMFromURLQuery;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClientConfiguration;
import net.openid.conformance.condition.client.ExtractRequestUriFromPARResponse;
import net.openid.conformance.condition.client.FAPI2ValidateJarmSigningAlg;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.condition.client.GenerateES256ClientJWKsWithKeyID;
import net.openid.conformance.condition.client.GenerateMTLSCertificateFromJWKs;
import net.openid.conformance.condition.client.GeneratePS256ClientJWKsWithKeyID;
import net.openid.conformance.condition.client.GenerateRS256ClientJWKsWithKeyID;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.RejectNonJarmResponsesInUrlQuery;
import net.openid.conformance.condition.client.SaveMutualTLsAuthenticationToConfig;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToFormPost;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToFormPostJWT;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToJWT;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeFromEnvironment;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.SignRequestObjectIncludeMediaType;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateJARMEncryptionAlg;
import net.openid.conformance.condition.client.ValidateJARMEncryptionEnc;
import net.openid.conformance.condition.client.ValidateJARMExpRecommendations;
import net.openid.conformance.condition.client.ValidateJARMFromURLQueryEncryption;
import net.openid.conformance.condition.client.ValidateJARMResponse;
import net.openid.conformance.condition.client.ValidateJARMSignatureUsingKid;
import net.openid.conformance.condition.client.ValidateJARMSigningAlg;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToPAREndpointRequest;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.variant.AccessTokenSenderConstrainMethod;
import net.openid.conformance.variant.AuthRequestMethod;
import net.openid.conformance.variant.AuthRequestNonRepudiationMethod;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.SecurityProfile;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

import java.util.function.Supplier;


@VariantParameters({
	SecurityProfile.class,
	FAPIResponseMode.class,
	AuthRequestMethod.class,
	AuthRequestNonRepudiationMethod.class,
	AccessTokenSenderConstrainMethod.class
})
@VariantConfigurationFields(parameter = AccessTokenSenderConstrainMethod.class, value = "dpop", configurationFields = {
	"client.dpop_signing_alg",
	"client2.dpop_signing_alg",
})

public abstract class AbstractOIDCCServerSecurityProfileTest extends AbstractOIDCCServerTest {
	protected Boolean profileRequiresMtlsEverywhere = false;
	protected Boolean useDpopAuthCodeBinding = false;
	protected Boolean isJarm;
	protected Boolean allowPlainErrorResponseForJarm = false;
	protected Boolean isPar;
	protected Boolean isOpenId;
	protected Boolean usePkce = true;
	protected Boolean isSignedRequest;

	protected Class <? extends ConditionSequence> addParEndpointClientAuthentication;
	protected Supplier <? extends ConditionSequence> createDpopForParEndpointSteps;
	protected Supplier <? extends ConditionSequence> createDpopForTokenEndpointSteps;
	protected Supplier <? extends ConditionSequence> createDpopForResourceEndpointSteps;
	protected Class <? extends ConditionSequence> profileAuthorizationEndpointSetupSteps = null;

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	@Override
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
		boolean mtlsRequired = isSenderConstrainMTLS() || profileRequiresMtlsEverywhere;
		profileCompleteClientConfiguration = () -> {
			ConditionSequence conditionSequence = new ConfigureClientForPrivateKeyJwt(serverSupportsDiscovery);
			// Add MTLS configuration for sender constrain or profileRequiresMtlsEverywhere
			if(mtlsRequired) {
				conditionSequence.then(new ConfigureClientForMtls(serverSupportsDiscovery(), isSecondClient(), false));
			}
			return conditionSequence;
		};
		addParEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	@Override
	public void setupMtls() {
		super.setupMtls();
		addParEndpointClientAuthentication = AddMTLSClientAuthenticationToPAREndpointRequest.class;
	}

	@VariantSetup(parameter = AccessTokenSenderConstrainMethod.class, value = "dpop")
	public void setupCreateDpopForEndpointSteps() {
		createDpopForParEndpointSteps = () -> CreateDpopProofSteps.createParEndpointDpopSteps();
		createDpopForTokenEndpointSteps = () -> CreateDpopProofSteps.createTokenEndpointDpopSteps();
		createDpopForResourceEndpointSteps = () -> CreateDpopProofSteps.createResourceEndpointDpopSteps();
	}

	@VariantSetup(parameter = AccessTokenSenderConstrainMethod.class, value = "mtls")
	public void setupMtlsForEndpointSteps() {
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
	}

	protected Boolean isSenderConstrainDpop() {
		return getVariant(AccessTokenSenderConstrainMethod.class) == AccessTokenSenderConstrainMethod.DPOP;
	}

	protected Boolean isSenderConstrainMTLS() {
		return getVariant(AccessTokenSenderConstrainMethod.class) == AccessTokenSenderConstrainMethod.MTLS;
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		isPar = getVariant(AuthRequestMethod.class) == AuthRequestMethod.REQUEST_OBJECT_PUSHED;
		isOpenId = getVariant(SecurityProfile.class) == SecurityProfile.OPENID_CONNECT;
		isJarm = getVariant(FAPIResponseMode.class) == FAPIResponseMode.JARM;
		isSignedRequest = getVariant(AuthRequestNonRepudiationMethod.class) == AuthRequestNonRepudiationMethod.SIGNED_NON_REPUDIATION;
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {

		// call sequences extracted  from OIDCCCreateDynamicClientRegistrationRequest

		// create basic dynamic registration request
		callAndStopOnFailure(CreateEmptyDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddClientNameToDynamicRegistrationRequest.class);


		// Use client config JWK if available, particularly if using MTLS sender constrain
		// which requires x5c certificates
		callAndContinueOnFailure(ExtractJWKSDirectFromClientConfiguration.class,  ConditionResult.INFO);
		String signingAlg = env.getString("config", "client.signing_alg");
		if(Strings.isNullOrEmpty(signingAlg)) {
			signingAlg = "RS256";
		}
		JsonObject clientJwks = env.getObject("client_jwks");
		JsonObject clientPublicJwks = env.getObject("client_public_jwks");
		if(clientJwks != null && clientPublicJwks != null) {
			// verify client keys
			env.mapKey("client", "config.client");
			call(condition(ValidateClientJWKsPrivatePart.class)
				.skipIfElementMissing("client", "jwks")
				.onSkip(Condition.ConditionResult.INFO)
				.requirements("RFC7517-1.1")
				.onFail(Condition.ConditionResult.FAILURE));
			env.unmapKey("client");
			callAndContinueOnFailure(FAPIEnsureMinimumClientKeyLength.class, ConditionResult.WARNING);
		} else {
			switch (signingAlg) {
				case "PS256":
					callAndStopOnFailure(GeneratePS256ClientJWKsWithKeyID.class);
					break;
				case "ES256":
					callAndStopOnFailure(GenerateES256ClientJWKsWithKeyID.class);
					break;
				case "RS256":
				default:
					callAndStopOnFailure(GenerateRS256ClientJWKsWithKeyID.class);
					break;
			}
			boolean mtlsRequired = getVariant(ClientAuthType.class) == ClientAuthType.MTLS || isSenderConstrainMTLS();
			if(mtlsRequired) {
				if(Strings.isNullOrEmpty(env.getString("client_name"))) {
					env.putString("client_name", env.getString("dynamic_registration_request", "client_name"));
				}
				callAndStopOnFailure(GenerateMTLSCertificateFromJWKs.class);
				// Add x5c to client JWK only if using MTLS client auth
				if(getVariant(ClientAuthType.class) == ClientAuthType.MTLS) {
					callAndStopOnFailure(AddClientX509CertificateClaimToPublicJWKs.class);
				}
				// save MTLS information so it can be extracted later
				callAndStopOnFailure(SaveMutualTLsAuthenticationToConfig.class);
			}
		}

		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		if (responseType.includesCode()) {
			callAndStopOnFailure(AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest.class);
		}
		if (responseType.includesIdToken() || responseType.includesToken()) {
			callAndStopOnFailure(AddImplicitGrantTypeToDynamicRegistrationRequest.class);
		}

		callAndStopOnFailure(AddPublicJwksToDynamicRegistrationRequest.class, "RFC7591-2");
		callAndStopOnFailure(AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment.class);
		callAndStopOnFailure(AddResponseTypesArrayToDynamicRegistrationRequestFromEnvironment.class);
		callAndStopOnFailure(AddRedirectUriToDynamicRegistrationRequest.class);
		callAndContinueOnFailure(AddContactsToDynamicRegistrationRequest.class, Condition.ConditionResult.INFO);

		if(isSenderConstrainMTLS()) {
			callAndStopOnFailure(AddTLSBoundAccessTokensTrueToDynamicRegistrationRequest.class);
			// add other required MTLS registrations to config.client.registrations_options
			// token_endpoint_auth_method (tls_client_auth, self_signed_tls_client_auth)
			// tls_client_auth_subject_dn
			// tls_client_auth_san_dns
			// tls_client_auth_san_uri
			// tls_client_auth_san_ip
			// tls_client_auth_san_email
		}
		if(isSenderConstrainDpop()) {
			callAndStopOnFailure(AddDpopBoundAccessTokensTrueToDynamicRegistrationRequest.class);
		}
		if(getVariant(ClientAuthType.class) == ClientAuthType.PRIVATE_KEY_JWT) {
			switch (signingAlg) {
				case "PS256":
					callAndContinueOnFailure(AddTokenEndpointAuthSigningAlgPS256ToDynamicRegistrationRequest.class, ConditionResult.WARNING);
					break;
				case "ES256":
					callAndContinueOnFailure(AddTokenEndpointAuthSigningAlgES256ToDynamicRegistrationRequest.class, ConditionResult.WARNING);
					break;
				case "RS256":
				default:
					callAndContinueOnFailure(AddTokenEndpointAuthSigningAlgRS256ToDynamicRegistrationRequest.class, ConditionResult.WARNING);
					break;
			}
		} else if (getVariant(ClientAuthType.class) == ClientAuthType.MTLS) {
			// add self_signed_tls_client_auth as client authentication method for MTLS
			// if using PKI MTLS, override in config.client.registration_options
			callAndContinueOnFailure(AddTokenEndpointAuthMethodSelfSignedTlsToDynamicRegistrationRequest.class, ConditionResult.WARNING);
		}
		if(getVariant(AuthRequestNonRepudiationMethod.class) == AuthRequestNonRepudiationMethod.SIGNED_NON_REPUDIATION) {
			switch (signingAlg) {
				case "PS256":
					callAndContinueOnFailure(AddRequestObjectSigningAlgPS256ToDynamicRegistrationRequest.class, ConditionResult.WARNING);
					break;
				case "ES256":
					callAndContinueOnFailure(AddRequestObjectSigningAlgES256ToDynamicRegistrationRequest.class, ConditionResult.WARNING);
					break;
				case "RS256":
				default:
					callAndContinueOnFailure(AddRequestObjectSigningAlgRS256ToDynamicRegistrationRequest.class, ConditionResult.WARNING);
					break;
			}
		}
		if(getVariant(FAPIResponseMode.class) == FAPIResponseMode.JARM) {
			JsonElement sigAlgsElement = env.getElementFromObject("server", "authorization_signing_alg_values_supported");
			if (sigAlgsElement != null) {
				JsonArray sigAlgsArray = sigAlgsElement.getAsJsonArray();
				if (sigAlgsArray.contains(new JsonPrimitive("PS256"))) {
					callAndContinueOnFailure(AddAuthorizationResponseSigningAlgPS256ToDynamicRegistrationRequest.class, ConditionResult.FAILURE);
				} else if (sigAlgsArray.contains(new JsonPrimitive("ES256"))) {
					callAndContinueOnFailure(AddAuthorizationResponseSigningAlgES256ToDynamicRegistrationRequest.class, ConditionResult.FAILURE);
				} else  {
					callAndContinueOnFailure(AddAuthorizationResponseSigningAlgRS256ToDynamicRegistrationRequest.class, ConditionResult.FAILURE);
				}
			}
		}
		// add other registration options as needed to config.client.registration_options
		callAndContinueOnFailure(AddClientRegistrationOptionsToDynamicRegistrationRequest.class, ConditionResult.INFO);
		expose("client_name", env.getString("dynamic_registration_request", "client_name"));
	}

	@Override
	protected void performAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		createAuthorizationRequest();

		if (isSignedRequest) {
			createAuthorizationRequestObject();
		} else {
			// the request object is implicitly created by the PAR endpoint, but
			// AbstractBuildRequestObjectRedirectToAuthorizationEndpoint needs to know what is in the implicit
			// request object.
			env.mapKey("request_object_claims", "pushed_authorization_request_form_parameters");
		}

		if (isPar) {
			eventLog.startBlock(currentClientString() + "Make request to PAR endpoint");
			if (isSignedRequest) {
				callAndStopOnFailure(BuildRequestObjectPostToPAREndpoint.class);
			} else {
				callAndStopOnFailure(BuildUnsignedPAREndpointRequest.class);
			}

			addClientAuthenticationToPAREndpointRequest();
			performParAuthorizationRequestFlow();
		} else {
			eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
			createAuthorizationRedirect();
			performRedirect();
		}

		eventLog.endBlock();
	}

	public static class CreateAuthorizationSecurityProfileRequestSteps extends AbstractConditionSequence {
		protected boolean formPost;
		private boolean isOpenId;
		protected boolean isJarm;
		protected boolean usePkce;
		protected Class <? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;

		public CreateAuthorizationSecurityProfileRequestSteps(boolean formPost) {
			this(true, false, false, formPost, null);
		}

		public CreateAuthorizationSecurityProfileRequestSteps(boolean isOpenId, boolean isJarm, boolean usePkce, boolean formPost, Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps) {
			this.formPost = formPost;
			this.isOpenId = isOpenId;
			this.isJarm = isJarm;
			this.usePkce = usePkce;
			this.profileAuthorizationEndpointSetupSteps = profileAuthorizationEndpointSetupSteps;
		}

		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

			if (profileAuthorizationEndpointSetupSteps != null) {
				call(sequence(profileAuthorizationEndpointSetupSteps));
			}

			callAndStopOnFailure(CreateRandomStateValue.class);
			call(exec().exposeEnvironmentString("state"));
			callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

			if(isOpenId) {
				callAndStopOnFailure(CreateRandomNonceValue.class);
				call(exec().exposeEnvironmentString("nonce"));
				callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);
			}

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeFromEnvironment.class);
			if (usePkce) {
				call(new SetupPkceAndAddToAuthorizationRequest());
			}
			if (formPost) {
				if(isJarm) {
					callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToFormPostJWT.class);
				} else {
					callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToFormPost.class);
				}
			} else {
				if (isJarm) {
					callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToJWT.class);
				}
			}
		}
	}

	protected void addPkceCodeVerifier() {
		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class, "RFC7636-4.5");
	}

	protected void addClientAuthenticationToPAREndpointRequest() {
		call(sequence(addParEndpointClientAuthentication));
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return new CreateAuthorizationSecurityProfileRequestSteps(isOpenId, isJarm, usePkce, formPost, null);
	}

	public static class CreateAuthorizationRequestObjectSteps extends AbstractConditionSequence {

		protected boolean isSecondClient;
		protected boolean encrypt;

		public CreateAuthorizationRequestObjectSteps(boolean isSecondClient, boolean encrypt) {
			this.isSecondClient = isSecondClient;
			this.encrypt = encrypt;
		}

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			if (isSecondClient) {
				callAndStopOnFailure(AddIatToRequestObject.class);
			}

			// TODO change references
			callAndStopOnFailure(AddNbfToRequestObject.class, "FAPI2-MS-ID1-5.3.1-3"); // mandatory in FAPI2-Message-Signing-ID1
			callAndStopOnFailure(AddExpToRequestObject.class, "FAPI2-MS-ID1-5.3.1-4");

			callAndStopOnFailure(AddAudToRequestObject.class, "FAPI2-SP-ID2-5.3.1.1-6");

			// iss is a 'should' in OIDC & jwsreq,
			callAndStopOnFailure(AddIssToRequestObject.class, "OIDCC-6.1");

			// jwsreq-26 is very explicit that client_id should be both inside and outside the request object
			callAndStopOnFailure(AddClientIdToRequestObject.class, "JAR-5", "FAPI2-MS-ID1-5.3.2-1");

			if (isSecondClient) {
				callAndStopOnFailure(SignRequestObjectIncludeMediaType.class, "JAR-4");
			}
			else {
				callAndStopOnFailure(SignRequestObject.class);
			}

			if (encrypt) {
				callAndStopOnFailure(FAPIBrazilEncryptRequestObject.class, "BrazilOB-5.2.2-1", "BrazilOB-6.1.2");
			}
		}
	}

	protected void createAuthorizationRequestObject() {
		call(makeCreateAuthorizationRequestObjectSteps());
	}

	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return new CreateAuthorizationRequestObjectSteps(isSecondClient(), false);
	}


	protected void performParAuthorizationRequestFlow() {
		// we only need to (and only should) supply an MTLS authentication when using MTLS client auth;
		// there's no need to pass mtls auth when using private_key_jwt
		boolean mtlsRequired = getVariant(ClientAuthType.class) == ClientAuthType.MTLS || isSenderConstrainMTLS();

		JsonObject mtls = null;
		if (!mtlsRequired) {
			mtls = env.getObject("mutual_tls_authentication");
			env.removeObject("mutual_tls_authentication");
		}

		callParEndpointAndStopOnFailure("PAR-2.1");

		if (!mtlsRequired && mtls != null) {
			env.putObject("mutual_tls_authentication", mtls);
		}

		processParResponse();
	}

	protected void processParResponse() {
		callAndStopOnFailure(CheckPAREndpointResponse201WithNoError.class, "PAR-2.2", "PAR-2.3");

		callAndStopOnFailure(CheckForRequestUriValue.class, "PAR-2.2");

		callAndContinueOnFailure(CheckForPARResponseExpiresIn.class, ConditionResult.FAILURE, "PAR-2.2");

		callAndStopOnFailure(ExtractRequestUriFromPARResponse.class);

		callAndContinueOnFailure(EnsureMinimumRequestUriEntropy.class, ConditionResult.FAILURE, "PAR-2.2", "PAR-7.1", "JAR-10.2");

		performPARRedirectWithRequestUri();
	}

	protected void performPARRedirectWithRequestUri() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class, "PAR-4");
		performRedirect();
	}

	/**
	 * Call Par endpoint with retry for DPoP nonce error
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallParEndpoint)
	 */
	protected void callParEndpointAndStopOnFailure(String... requirements) {
		if (isSenderConstrainDpop() && useDpopAuthCodeBinding) {
			final int MAX_RETRY = 2;
			int i = 0;
			while(i < MAX_RETRY){
				createDpopForParEndpoint();
				callAndStopOnFailure(CallPAREndpointAllowingDpopNonceError.class, requirements);
				if(Strings.isNullOrEmpty(env.getString("par_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		} else {
			callAndStopOnFailure(CallPAREndpoint.class, requirements);
		}
	}

	protected void createDpopForTokenEndpoint() {
		if(null == env.getElementFromObject("client", "dpop_private_jwk")) {
			callAndStopOnFailure(GenerateDpopKey.class);
		}
		if (null != createDpopForTokenEndpointSteps) {
			call(sequence(createDpopForTokenEndpointSteps));
		}
	}

	protected void createDpopForParEndpoint() {

		if(null == env.getElementFromObject("client", "dpop_private_jwk")) {
			callAndStopOnFailure(GenerateDpopKey.class);
		}
		if (null != createDpopForParEndpointSteps) {
			call(sequence(createDpopForParEndpointSteps));
		}
	}

	@Override
	protected void createAuthorizationRedirect() {
		if(isSignedRequest) {
			callAndStopOnFailure(BuildRequestObjectByValueRedirectToAuthorizationEndpoint.class);
		} else {
			callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
		}
	}

	@Override
	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		if (formPost) {
			env.mapKey("authorization_endpoint_response", "callback_body_form_params");
			callAndContinueOnFailure(CheckCallbackHttpMethodIsPost.class, ConditionResult.FAILURE, "OAuth2-FP-2");
			callAndContinueOnFailure(CheckCallbackContentTypeIsFormUrlEncoded.class, ConditionResult.FAILURE, "OAuth2-FP-2");
			// TODO process JARM post response
			if(isJarm) {
				env.mapKey("callback_query_params", "callback_body_form_params");
				processCallbackForJARM();
				env.unmapKey("callback_query_params");
			} else {
				callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");
				callAndContinueOnFailure(RejectErrorInUrlQuery.class, ConditionResult.FAILURE, "OAuth2-RT-5");
			}
		} else if (isJarm) {
			if(isCodeFlow()) {
				processCallbackForJARM();
			} else {
				env.mapKey("callback_query_params", "callback_params");
				processCallbackForJARM();
				env.unmapKey("callback_query_params");
			}
		} else if (isCodeFlow()) {
			env.mapKey("authorization_endpoint_response", "callback_query_params");
		} else {
			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");
			callAndContinueOnFailure(RejectErrorInUrlQuery.class, ConditionResult.FAILURE, "OAuth2-RT-5");
		}

		onAuthorizationCallbackResponse();
		eventLog.endBlock();
	}

	protected void processCallbackForJARM() {
		String errorParameter = env.getString("callback_query_params", "error");
		String responseParameter = env.getString("callback_query_params", "response");
		if(allowPlainErrorResponseForJarm && responseParameter==null && errorParameter!=null) {
			//plain error response, no jarm
			callAndStopOnFailure(AddPlainErrorResponseAsAuthorizationEndpointResponseForJARM.class);
		} else {
			skipIfMissing(new String[]{"client_jwks"}, null, ConditionResult.INFO,
				ValidateJARMFromURLQueryEncryption.class, ConditionResult.WARNING, "JARM-2.2");
			callAndStopOnFailure(ExtractJARMFromURLQuery.class, "FAPI2-MS-ID1-5.4.2-2", "JARM-2.3.4", "JARM-2.3.1");

			callAndContinueOnFailure(RejectNonJarmResponsesInUrlQuery.class, ConditionResult.FAILURE, "JARM-2.1");

			callAndStopOnFailure(ExtractAuthorizationEndpointResponseFromJARMResponse.class);

			callAndContinueOnFailure(ValidateJARMResponse.class, ConditionResult.FAILURE, "JARM-2.4-2", "JARM-2.4-3", "JARM-2.4-4");

			callAndContinueOnFailure(FAPI2ValidateJarmSigningAlg.class, ConditionResult.FAILURE);

			skipIfElementMissing("jarm_response", "jws_header", ConditionResult.INFO,
				ValidateJARMSigningAlg.class, ConditionResult.FAILURE);

			skipIfElementMissing("jarm_response", "jwe_header", ConditionResult.INFO,
				ValidateJARMEncryptionAlg.class, ConditionResult.FAILURE);

			skipIfElementMissing("jarm_response", "jwe_header", ConditionResult.INFO,
				ValidateJARMEncryptionEnc.class, ConditionResult.FAILURE);

			callAndContinueOnFailure(ValidateJARMExpRecommendations.class, ConditionResult.WARNING, "JARM-2.1");

			callAndContinueOnFailure(ValidateJARMSignatureUsingKid.class, ConditionResult.FAILURE, "JARM-2.4-5");
		}
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		super.createAuthorizationCodeRequest();
		if(usePkce) {
			addPkceCodeVerifier();
		}
	}

	/**
	 * Call sender constrained token endpoint. For DPOP nonce errors, it will retry with new server nonce value.
	 * @param fullResponse whether the full response should be returned
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse)
	 */
	protected void callSenderConstrainedTokenEndpointAndStopOnFailure(boolean fullResponse, String... requirements) {
		final int MAX_RETRY = 2;

		if (isSenderConstrainDpop()) {
			int i = 0;
			while(i < MAX_RETRY){
				createDpopForTokenEndpoint();
				callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class, requirements);
				if(Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		} else {
			callAndStopOnFailure(fullResponse ? CallTokenEndpointAndReturnFullResponse.class : CallTokenEndpoint.class, requirements);
		}
	}

	/**
	 * Call sender constrained token endpoint returning full response
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse)
	 */
	protected void callSenderConstrainedTokenEndpointAndStopOnFailure(String... requirements) {
		callSenderConstrainedTokenEndpointAndStopOnFailure(true, requirements);
	}

	/**
	 * Default Call to sender constrained token endpoint with non-full response
	 */
	protected void callSenderConstrainedTokenEndpoint() {
		callSenderConstrainedTokenEndpointAndStopOnFailure(false);
	}


	@Override
	protected void callTokenEndpoint() {
		if(getVariant(AccessTokenSenderConstrainMethod.class) == AccessTokenSenderConstrainMethod.NONE) {
			callAndStopOnFailure(CallTokenEndpoint.class);
		} else {
			callSenderConstrainedTokenEndpoint();
		}
	}

	protected void requestProtectedResourceUsingDpop() {
		if (isSenderConstrainDpop() && (createDpopForResourceEndpointSteps != null) ) {
			final int MAX_RETRY = 2;
			int i = 0;
			while(i < MAX_RETRY) {
				callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
				call(sequence(createDpopForResourceEndpointSteps));
				callAndStopOnFailure(CallProtectedResourceAllowingDpopNonceError.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
				if(Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break; // no nonce error so
				}
				// continue call with nonce
				++i;
			}
		}
	}
	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Userinfo endpoint tests");

		boolean mtlsRequired = isSenderConstrainMTLS() || profileRequiresMtlsEverywhere;

		JsonObject mtls = null;
		if (!mtlsRequired) {
			mtls = env.getObject("mutual_tls_authentication");
			env.removeObject("mutual_tls_authentication");
		}

		if (isSenderConstrainDpop() ) {
			requestProtectedResourceUsingDpop();
		} else  {
			callAndStopOnFailure(CallProtectedResource.class, "FAPI2-SP-FINAL-5.3.4-2");
		}
		if (!mtlsRequired && mtls != null) {
			env.putObject("mutual_tls_authentication", mtls);
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		eventLog.endBlock();
	}
}
