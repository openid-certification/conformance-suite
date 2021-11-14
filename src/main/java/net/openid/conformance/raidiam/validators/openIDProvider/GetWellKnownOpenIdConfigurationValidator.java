package net.openid.conformance.raidiam.validators.openIDProvider;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;
/**
 * Api url: ****
 * Api endpoint: GET /.well-known/openid-configuration
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET Well Known OpenId Configuration")
public class GetWellKnownOpenIdConfigurationValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new StringArrayField
				.Builder("acr_values_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("authorization_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("claims_parameter_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("claims_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("code_challenge_methods_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("end_session_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("check_session_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("grant_types_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("id_token_signing_alg_values_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("issuer")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("jwks_uri")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("registration_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("request_object_signing_alg_values_supported")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("request_parameter_supported")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("request_uri_parameter_supported")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("require_request_uri_registration")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("pushed_authorization_request_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("response_modes_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("response_types_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("scopes_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("subject_types_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("token_endpoint_auth_methods_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("token_endpoint_auth_signing_alg_values_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("token_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("userinfo_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("userinfo_signing_alg_values_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("authorization_signing_alg_values_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("introspection_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("introspection_endpoint_auth_methods_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("introspection_endpoint_auth_signing_alg_values_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("revocation_endpoint")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("revocation_endpoint_auth_methods_supported")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("revocation_endpoint_auth_signing_alg_values_supported")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("frontchannel_logout_supported")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("frontchannel_logout_session_supported")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("tls_client_certificate_bound_access_tokens")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("claim_types_supported")
				.setOptional()
				.build());

		return environment;
	}
}
