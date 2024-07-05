package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashSet;
import java.util.Set;

public class ValidateEntityStatementMetadataClaim extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "entity_statement_body" } )
	public Environment evaluate(Environment env) {

		JsonElement metadataClaim = env.getElementFromObject("entity_statement_body", "metadata");
		if (metadataClaim == null) {
			logSuccess("Entity statement does not contain the metadata claim");
			return env;
		}

		JsonObject metadata = metadataClaim.getAsJsonObject();

		Set<String> validTopLevelKeys = ImmutableSet.of(
			"federation_entity",
			"openid_relying_party",
			"openid_provider",
			"oauth_authorization_server",
			"oauth_client",
			"oauth_resource"
		);
		Set<String> keys = metadata.keySet();
		Set<String> difference = new HashSet<>(keys);
		difference.removeAll(validTopLevelKeys);
		if (!difference.isEmpty()) {
			throw error("The metadata claim contains invalid entity types", args("expected", validTopLevelKeys, "actual", keys));
		}

		logSuccess("Entity statement contains a valid metadata claim", args("metadata", metadata));
		return env;
	}

	/*
	{
	  "metadata": {
		"federation_entity": {
		  "federation_fetch_endpoint": "string",
		  "federation_list_endpoint": "string",
		  "federation_resolve_endpoint": "string",
		  "federation_trust_mark_status_endpoint": "string",
		  "federation_trust_mark_list_endpoint": "string",
		  "federation_trust_mark_endpoint": "string",
		  "federation_historical_keys_endpoint": "string",
		  "organization_name": "string",
		  "homepage_uri": "string"
		},
		"openid_relying_party": {
		  "organization_name": "string",
		  "contacts": [
			"string"
		  ],
		  "logo_uri": "string",
		  "policy_uri": "string",
		  "homepage_uri": "string",
		  "redirect_uris": [
			"string"
		  ],
		  "response_types": [
			"string"
		  ],
		  "grant_types": [
			[
			  "authorization_code",
			  "implicit"
			]
		  ],
		  "application_type": "native",
		  "client_name": "string",
		  "client_uri": "string",
		  "tos_uri": "string",
		  "jwks_uri": "string",
		  "jwks": {
			"keys": [
			  {
				"kty": "RSA",
				"use": "sig",
				"key_ops": "encrypt",
				"alg": "RS256",
				"kid": "1",
				"x5u": "https://example.com/cert.pem",
				"x5c": [
				  "MIIDQzCCA...+3whvMF1XEt0K2bA8wpPmSTPgQ==",
				  "MIIDQzCCA...+3whvMF1XEt0K2bA8wpPmSTPgQ=="
				],
				"x5t": "0fVuYF8jJ3onI+9Zk2/Iy+Oh5ZpE",
				"x5t#S256": "1MvI4/VhnEzTz7Jo/0Q/d/jI3rE7IMoMT34wvAjyLvs",
				"revoked": {
				  "revoked_at": "string",
				  "reason": "string"
				}
			  }
			]
		  },
		  "sector_identifier_uri": "string",
		  "subject_type": "string",
		  "id_token_signed_response_alg": "string",
		  "id_token_encrypted_response_alg": "string",
		  "id_token_encrypted_response_enc": "string",
		  "userinfo_signed_response_alg": "string",
		  "userinfo_encrypted_response_alg": "string",
		  "userinfo_encrypted_response_enc": "string",
		  "request_object_signing_alg": "string",
		  "request_object_encryption_alg": "string",
		  "request_object_encryption_enc": "string",
		  "token_endpoint_auth_method": "string",
		  "token_endpoint_auth_signing_alg": "string",
		  "default_max_age": 0,
		  "require_auth_time": true,
		  "default_acr_values": [
			"string"
		  ],
		  "initiate_login_uri": "string",
		  "request_uris": [
			"string"
		  ],
		  "scope": "string",
		  "software_id": "string",
		  "software_version": "string",
		  "client_id": "string",
		  "client_secret": "string",
		  "client_id_issued_at": 0,
		  "client_secret_expires_at": 0,
		  "registration_access_token": "string",
		  "registration_client_uri": "string",
		  "claims_redirect_uris": [
			"string"
		  ],
		  "nfv_token_signed_response_alg": "string",
		  "nfv_token_encrypted_response_alg": "string",
		  "nfv_token_encrypted_response_enc": "string",
		  "tls_client_certificate_bound_access_tokens": true,
		  "tls_client_auth_subject_dn": "string",
		  "tls_client_auth_san_dns": "string",
		  "tls_client_auth_san_uri": "string",
		  "tls_client_auth_san_ip": "string",
		  "tls_client_auth_san_email": "string",
		  "require_signed_request_object": true,
		  "require_pushed_authorization_requests": true,
		  "introspection_signed_response_alg": "string",
		  "introspection_encrypted_response_alg": "string",
		  "introspection_encrypted_response_enc": "string",
		  "frontchannel_logout_uri": "string",
		  "frontchannel_logout_session_required": true,
		  "backchannel_logout_uri": "string",
		  "backchannel_logout_session_required": true,
		  "post_logout_redirect_uris": [
			"string"
		  ],
		  "authorization_details_types": [
			"string"
		  ],
		  "dpop_bound_access_tokens": true,
		  "client_registration_types": [
			"automatic"
		  ]
		},
		"openid_provider": {
		  "organization_name": "string",
		  "contacts": [
			"string"
		  ],
		  "logo_uri": "string",
		  "policy_uri": "string",
		  "homepage_uri": "string",
		  "issuer": "string",
		  "authorization_endpoint": "string",
		  "token_endpoint": "string",
		  "userinfo_endpoint": "string",
		  "jwks_uri": "string",
		  "registration_endpoint": "string",
		  "scopes_supported": [
			"string"
		  ],
		  "response_types_supported": [
			"string"
		  ],
		  "response_modes_supported": [
			"string"
		  ],
		  "grant_types_supported": [
			"string"
		  ],
		  "acr_values_supported": [
			"string"
		  ],
		  "subject_types_supported": [
			"string"
		  ],
		  "id_token_signing_alg_values_supported": [
			"string"
		  ],
		  "id_token_encryption_alg_values_supported": [
			"string"
		  ],
		  "id_token_encryption_enc_values_supported": [
			"string"
		  ],
		  "userinfo_signing_alg_values_supported": [
			"string"
		  ],
		  "userinfo_encryption_alg_values_supported": [
			"string"
		  ],
		  "userinfo_encryption_enc_values_supported": [
			"string"
		  ],
		  "request_object_signing_alg_values_supported": [
			"string"
		  ],
		  "request_object_encryption_alg_values_supported": [
			"string"
		  ],
		  "request_object_encryption_enc_values_supported": [
			"string"
		  ],
		  "token_endpoint_auth_methods_supported": [
			"string"
		  ],
		  "token_endpoint_auth_signing_alg_values_supported": [
			"string"
		  ],
		  "display_values_supported": [
			"string"
		  ],
		  "claim_types_supported": [
			"string"
		  ],
		  "claims_supported": [
			"string"
		  ],
		  "service_documentation": "string",
		  "claims_locales_supported": [
			"string"
		  ],
		  "ui_locales_supported": [
			"string"
		  ],
		  "claims_parameter_supported": true,
		  "request_parameter_supported": true,
		  "request_uri_parameter_supported": true,
		  "require_request_uri_registration": true,
		  "op_policy_uri": "string",
		  "op_tos_uri": "string",
		  "revocation_endpoint": "string",
		  "revocation_endpoint_auth_methods_supported": [
			"string"
		  ],
		  "revocation_endpoint_auth_signing_alg_values_supported": [
			"string"
		  ],
		  "introspection_endpoint": "string",
		  "introspection_endpoint_auth_methods_supported": [
			"string"
		  ],
		  "introspection_endpoint_auth_signing_alg_values_supported": [
			"string"
		  ],
		  "code_challenge_methods_supported": [
			"string"
		  ],
		  "signed_metadata": "string",
		  "device_authorization_endpoint": "string",
		  "tls_client_certificate_bound_access_tokens": true,
		  "mtls_endpoint_aliases": {
			"token_endpoint": "https://mtls.example.com/token",
			"revocation_endpoint": "https://mtls.example.com/revo",
			"introspection_endpoint": "https://mtls.example.com/introspect"
		  },
		  "nfv_token_signing_alg_values_supported": [
			"string"
		  ],
		  "nfv_token_encryption_alg_values_supported": [
			"string"
		  ],
		  "nfv_token_encryption_enc_values_supported": [
			"string"
		  ],
		  "require_signed_request_object": true,
		  "pushed_authorization_request_endpoint": "string",
		  "require_pushed_authorization_requests": true,
		  "introspection_signing_alg_values_supported": [
			"string"
		  ],
		  "introspection_encryption_alg_values_supported": [
			"string"
		  ],
		  "introspection_encryption_enc_values_supported": [
			"string"
		  ],
		  "authorization_response_iss_parameter_supported": true,
		  "check_session_iframe": "string",
		  "client_registration_types_supported": [
			"string"
		  ],
		  "federation_registration_endpoint": "string",
		  "request_authentication_methods_supported": {
			"authorization_endpoint": [
			  "string"
			],
			"pushed_authorization_request_endpoint": [
			  "string"
			]
		  },
		  "request_authentication_signing_alg_values_supported": [
			"string"
		  ]
		},
		"oauth_authorization_server": {
		  "organization_name": "string",
		  "contacts": [
			"string"
		  ],
		  "logo_uri": "string",
		  "policy_uri": "string",
		  "homepage_uri": "string",
		  "issuer": "https://example.com",
		  "authorization_endpoint": "https://example.com/oauth2/authorize",
		  "token_endpoint": "https://example.com/oauth2/token",
		  "jwks_uri": "https://example.com/oauth2/jwks",
		  "registration_endpoint": "https://example.com/oauth2/register",
		  "scopes_supported": [
			"openid",
			"profile",
			"email"
		  ],
		  "response_types_supported": [
			"code",
			"token",
			"id_token"
		  ],
		  "response_modes_supported": [
			"query",
			"fragment",
			"form_post"
		  ],
		  "grant_types_supported": [
			"authorization_code",
			"implicit",
			"client_credentials",
			"refresh_token"
		  ],
		  "token_endpoint_auth_methods_supported": [
			"client_secret_basic",
			"private_key_jwt"
		  ],
		  "token_endpoint_auth_signing_alg_values_supported": [
			"RS256",
			"ES256"
		  ],
		  "service_documentation": "https://example.com/service_documentation",
		  "ui_locales_supported": [
			"en-US",
			"fr-FR"
		  ],
		  "op_policy_uri": "https://example.com/op_policy",
		  "op_tos_uri": "https://example.com/op_tos",
		  "revocation_endpoint": "https://example.com/oauth2/revoke",
		  "revocation_endpoint_auth_methods_supported": [
			"client_secret_basic",
			"private_key_jwt"
		  ],
		  "revocation_endpoint_auth_signing_alg_values_supported": [
			"RS256",
			"ES256"
		  ],
		  "introspection_endpoint": "https://example.com/oauth2/introspect",
		  "introspection_endpoint_auth_methods_supported": [
			"client_secret_basic",
			"private_key_jwt"
		  ],
		  "introspection_endpoint_auth_signing_alg_values_supported": [
			"RS256",
			"ES256"
		  ],
		  "code_challenge_methods_supported": [
			"plain",
			"S256"
		  ],
		  "signed_metadata": "string",
		  "device_authorization_endpoint": "string",
		  "tls_client_certificate_bound_access_tokens": true,
		  "mtls_endpoint_aliases": {
			"token_endpoint": "https://mtls.example.com/token",
			"revocation_endpoint": "https://mtls.example.com/revo",
			"introspection_endpoint": "https://mtls.example.com/introspect"
		  },
		  "nfv_token_signing_alg_values_supported": [
			"string"
		  ],
		  "nfv_token_encryption_alg_values_supported": [
			"string"
		  ],
		  "nfv_token_encryption_enc_values_supported": [
			"string"
		  ],
		  "userinfo_endpoint": "string",
		  "acr_values_supported": [
			"string"
		  ],
		  "subject_types_supported": [
			"string"
		  ],
		  "id_token_signing_alg_values_supported": [
			"string"
		  ],
		  "id_token_encryption_alg_values_supported": [
			"string"
		  ],
		  "id_token_encryption_enc_values_supported": [
			"string"
		  ],
		  "userinfo_signing_alg_values_supported": [
			"string"
		  ],
		  "userinfo_encryption_alg_values_supported": [
			"string"
		  ],
		  "userinfo_encryption_enc_values_supported": [
			"string"
		  ],
		  "request_object_signing_alg_values_supported": [
			"string"
		  ],
		  "request_object_encryption_alg_values_supported": [
			"string"
		  ],
		  "request_object_encryption_enc_values_supported": [
			"string"
		  ],
		  "display_values_supported": [
			"string"
		  ],
		  "claim_types_supported": [
			"string"
		  ],
		  "claims_supported": [
			"string"
		  ],
		  "claims_locales_supported": [
			"string"
		  ],
		  "claims_parameter_supported": true,
		  "request_parameter_supported": true,
		  "request_uri_parameter_supported": true,
		  "require_request_uri_registration": true,
		  "require_signed_request_object": true,
		  "pushed_authorization_request_endpoint": "string",
		  "require_pushed_authorization_requests": true,
		  "introspection_signing_alg_values_supported": [
			"string"
		  ],
		  "introspection_encryption_alg_values_supported": [
			"string"
		  ],
		  "introspection_encryption_enc_values_supported": [
			"string"
		  ],
		  "authorization_response_iss_parameter_supported": true,
		  "check_session_iframe": "string"
		},
		"oauth_client": {
		  "organization_name": "string",
		  "contacts": [
			"string"
		  ],
		  "logo_uri": "string",
		  "policy_uri": "string",
		  "homepage_uri": "string",
		  "redirect_uris": [
			"string"
		  ],
		  "token_endpoint_auth_method": "none",
		  "grant_types": [
			"authorization_code"
		  ],
		  "response_types": [
			"code"
		  ],
		  "client_name": "string",
		  "client_uri": "string",
		  "scope": "string",
		  "tos_uri": "string",
		  "jwks_uri": "string",
		  "jwks": {
			"keys": [
			  {
				"kty": "RSA",
				"use": "sig",
				"key_ops": "encrypt",
				"alg": "RS256",
				"kid": "1",
				"x5u": "https://example.com/cert.pem",
				"x5c": [
				  "MIIDQzCCA...+3whvMF1XEt0K2bA8wpPmSTPgQ==",
				  "MIIDQzCCA...+3whvMF1XEt0K2bA8wpPmSTPgQ=="
				],
				"x5t": "0fVuYF8jJ3onI+9Zk2/Iy+Oh5ZpE",
				"x5t#S256": "1MvI4/VhnEzTz7Jo/0Q/d/jI3rE7IMoMT34wvAjyLvs",
				"revoked": {
				  "revoked_at": "string",
				  "reason": "string"
				}
			  }
			]
		  },
		  "software_id": "string",
		  "software_version": "string",
		  "client_id": "string",
		  "client_secret": "string",
		  "client_id_issued_at": 0,
		  "client_secret_expires_at": 0,
		  "registration_access_token": "string",
		  "registration_client_uri": "string",
		  "application_type": "string",
		  "sector_identifier_uri": "string",
		  "subject_type": "string",
		  "id_token_signed_response_alg": "string",
		  "id_token_encrypted_response_alg": "string",
		  "id_token_encrypted_response_enc": "string",
		  "userinfo_signed_response_alg": "string",
		  "userinfo_encrypted_response_alg": "string",
		  "userinfo_encrypted_response_enc": "string",
		  "request_object_signing_alg": "string",
		  "request_object_encryption_alg": "string",
		  "request_object_encryption_enc": "string",
		  "token_endpoint_auth_signing_alg": "string",
		  "default_max_age": 0,
		  "require_auth_time": true,
		  "default_acr_values": [
			"string"
		  ],
		  "initiate_login_uri": "string",
		  "request_uris": [
			"string"
		  ],
		  "claims_redirect_uris": [
			"string"
		  ],
		  "nfv_token_signed_response_alg": "string",
		  "nfv_token_encrypted_response_alg": "string",
		  "nfv_token_encrypted_response_enc": "string",
		  "tls_client_certificate_bound_access_tokens": true,
		  "tls_client_auth_subject_dn": "string",
		  "tls_client_auth_san_dns": "string",
		  "tls_client_auth_san_uri": "string",
		  "tls_client_auth_san_ip": "string",
		  "tls_client_auth_san_email": "string",
		  "require_signed_request_object": true,
		  "require_pushed_authorization_requests": true,
		  "introspection_signed_response_alg": "string",
		  "introspection_encrypted_response_alg": "string",
		  "introspection_encrypted_response_enc": "string",
		  "frontchannel_logout_uri": "string",
		  "frontchannel_logout_session_required": true,
		  "backchannel_logout_uri": "string",
		  "backchannel_logout_session_required": true,
		  "post_logout_redirect_uris": [
			"string"
		  ],
		  "authorization_details_types": [
			"string"
		  ],
		  "dpop_bound_access_tokens": true
		},
		"oauth_resource": {
		  "organization_name": "string",
		  "contacts": [
			"string"
		  ],
		  "logo_uri": "string",
		  "policy_uri": "string",
		  "homepage_uri": "string",
		  "resource": "string",
		  "authorization_servers": [
			"string"
		  ],
		  "jwks_uri": "string",
		  "scopes_supported": [
			"string"
		  ],
		  "bearer_methods_supported": [
			"string"
		  ],
		  "resource_signing_alg_values_supported": [
			"string"
		  ],
		  "resource_documentation": "string",
		  "resource_policy_uri": "string",
		  "resource_tos_uri": "string"
		}
	  }
	}
	*/
}
