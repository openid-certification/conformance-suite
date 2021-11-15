package net.openid.conformance.raidiam.validators.openIDProvider;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: ****
 * Api endpoint: POST /reg
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Reg")
public class PostRegValidator extends AbstractJsonAssertingCondition {

	private static final Set<String> TYPES = Sets.newHashSet("web");

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new StringField
				.Builder("application_type")
				.setEnums(TYPES)
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("grant_types")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("id_token_signed_response_alg")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("require_auth_time")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("subject_type")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("response_types")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("post_logout_redirect_uris")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("tls_client_certificate_bound_access_token")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("token_endpoint_auth_method")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("introspection_endpoint_auth_method")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("revocation_endpoint_auth_method")
				.setOptional()
				.build());

		assertField(body,
			new IntField
				.Builder("client_id_issued_at")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("client_id")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("jwks_uri")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("registration_client_uri")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("registration_access_token")
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("redirect_uris")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("organisation_id")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("organisation_name")
				.setMinLength(1)
				.setMaxLength(255)
				.build());

		assertField(body,
			new StringField
				.Builder("organisation_number")
				.setMinLength(1)
				.setMaxLength(255)
				.build());

		assertField(body,
			new StringArrayField
				.Builder("software_roles")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("software_description")
				.setMaxLength(255)
				.build());

		assertField(body,
			new StringField
				.Builder("request_object_signing_alg")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("require_signed_request_object")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("require_pushed_authorization_requests")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("authorization_signed_response_alg")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("backchannel_user_code_parameter")
				.setOptional()
				.build());

		assertField(body,
			new IntField
				.Builder("client_secret_expires_at")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("client_secret")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("scope")
				.setOptional()
				.build());

		return environment;
	}
}
