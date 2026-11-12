package net.openid.conformance.security;

import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decodes ID tokens per login registration. Spring's own factory verifies with a
 * single algorithm per registration, RS256 unless told otherwise, which suits
 * every registration except the one named {@value #IDP_REGISTRATION_ID}: that
 * OpenID Provider may sign with any of {@link IdpJwsAlgorithms#DEFAULT}, so its
 * ID tokens are accepted with each of them and otherwise validated as Spring
 * validates any ID token.
 */
final class IdTokenDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

	/** The login registration (spring.security.oauth2.client.registration.idp.*) for the oidc.idp.* provider. */
	static final String IDP_REGISTRATION_ID = "idp";

	private final OidcIdTokenDecoderFactory springDefault = new OidcIdTokenDecoderFactory();

	private final Map<String, JwtDecoder> idpDecoders = new ConcurrentHashMap<>();

	@Override
	public JwtDecoder createDecoder(ClientRegistration clientRegistration) {
		if (!IDP_REGISTRATION_ID.equals(clientRegistration.getRegistrationId())) {
			return springDefault.createDecoder(clientRegistration);
		}
		return idpDecoders.computeIfAbsent(clientRegistration.getRegistrationId(), id -> idpDecoder(clientRegistration));
	}

	private static JwtDecoder idpDecoder(ClientRegistration clientRegistration) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri())
			.jwsAlgorithms(algorithms -> algorithms.addAll(IdpJwsAlgorithms.ID_TOKEN))
			.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
			new JwtTimestampValidator(), new OidcIdTokenValidator(clientRegistration)));
		decoder.setClaimSetConverter(new ClaimTypeConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverters()));
		return decoder;
	}
}
