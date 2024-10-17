package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class EntityUtils_UnitTest {

	@Test
	public void property_order_does_not_matter_when_comparing_json_objects() {
		String jsonStringA = """
				{ "prop1": "value1", "prop2": "value2", "prop3": "value3"}
				""";
		String jsonStringB = """
				{ "prop2": "value2", "prop3": "value3", "prop1": "value1"}
				""";

		JsonElement a = JsonParser.parseString(jsonStringA);
		JsonElement b = JsonParser.parseString(jsonStringB);

		List<String> claimNames = Arrays.asList("prop1", "prop2", "prop3");

		List<String> differences = EntityUtils.diffEntityStatements(claimNames, a, b);

		assertTrue(differences.isEmpty());
	}

	@Test
	public void only_the_listed_properties_are_checked_for_differences() {
		String jsonStringA = """
				{ "prop1": "value1", "prop2": "XXX", "prop3": "YYY" }
				""";
		String jsonStringB = """
				{ "prop1": "value2", "prop2": "ZZZ", "prop3": "WWW" }
				""";

		JsonElement a = JsonParser.parseString(jsonStringA);
		JsonElement b = JsonParser.parseString(jsonStringB);

		List<String> claimNames = Arrays.asList("prop1");

		List<String> differences = EntityUtils.diffEntityStatements(claimNames, a, b);

		assertEquals(1, differences.size());
	}

	@Test
	public void array_order_does_not_matter() {
		String jsonStringA = """
				{ "authority_hints": [ "https://one.com", "https://two.com" ] }
				""";
		String jsonStringB = """
				{ "authority_hints": [ "https://two.com", "https://one.com" ] }
				""";

		JsonElement a = JsonParser.parseString(jsonStringA);
		JsonElement b = JsonParser.parseString(jsonStringB);

		List<String> claimNames = Arrays.asList("authority_hints");

		List<String> differences = EntityUtils.diffEntityStatements(claimNames, a, b);

		assertEquals(1, differences.size());
	}

	@Test
	public void there_has_to_be_at_least_one_property_to_check() {
		JsonObject jsonA = new JsonObject();
		JsonObject jsonB = new JsonObject();

		assertThrows(IllegalArgumentException.class, () -> {
			EntityUtils.diffEntityStatements(List.of(), jsonA, jsonB);
		});

		assertThrows(IllegalArgumentException.class, () -> {
			EntityUtils.diffEntityStatements(null, jsonA, jsonB);
		});
	}

	@Test
	public void comparing_two_identical_entity_statetements_yields_zero_differences() {
		String jsonStringA = SUBORDINATE_STATEMENT_BY_INTERMEDIATE_FOR_LEAF;
		String jsonStringB = SUBORDINATE_STATEMENT_BY_INTERMEDIATE_FOR_LEAF;

		JsonElement a = JsonParser.parseString(jsonStringA);
		JsonElement b = JsonParser.parseString(jsonStringB);

		List<String> claimNames = EntityUtils.STANDARD_ENTITY_STATEMENT_CLAIMS;

		List<String> differences = EntityUtils.diffEntityStatements(claimNames, a, b);

		assertEquals(0, differences.size());
	}

	@Test
	public void comparing_two_non_identical_entity_statetements_yields_the_differences() {
		String jsonStringA = SUBORDINATE_STATEMENT_BY_INTERMEDIATE_FOR_LEAF;
		String jsonStringB = SUBORDINATE_STATEMENT_BY_INTERMEDIATE_FOR_LEAF
				.replace("\"exp\": 1728575553,", "\"exp\": 000,")
				.replace("\"iat\": 1728489153,", "\"exp\": 111,")
				.replace("\"jti\": c2a0e0b6da466cf53b3ee5e4534e03f9,", "\"jti\": \"ABC\",");

		JsonElement a = JsonParser.parseString(jsonStringA);
		JsonElement b = JsonParser.parseString(jsonStringB);

		List<String> claimNames = EntityUtils.STANDARD_ENTITY_STATEMENT_CLAIMS;

		List<String> differences = EntityUtils.diffEntityStatements(claimNames, a, b);

		// Note that jti is not a standard claim in entity statements,
		// so there should be two differences: "iat" and "exp".
		assertEquals(2, differences.size());
		assertTrue(differences.contains("iat"));
		assertTrue(differences.contains("exp"));
	}

	@Test
	public void comparing_two_entity_statements_issued_at_different_times_yields_three_differences() {
		String jsonStringA = ENTITY_STATEMENT_BY_LEAF_1;
		String jsonStringB = ENTITY_STATEMENT_BY_LEAF_2;

		JsonElement a = JsonParser.parseString(jsonStringA);
		JsonElement b = JsonParser.parseString(jsonStringB);

		List<String> claimNames = EntityUtils.STANDARD_ENTITY_STATEMENT_CLAIMS;

		List<String> differences = EntityUtils.diffEntityStatements(claimNames, a, b);

		assertEquals(3, differences.size());
		assertTrue(differences.contains("iat"));
		assertTrue(differences.contains("exp"));
		assertTrue(differences.contains("trust_marks"));
	}

	@Test
	public void excluding_properties_with_time_related_claims_leaves_zero_differences() {
		String jsonStringA = ENTITY_STATEMENT_BY_LEAF_1;
		String jsonStringB = ENTITY_STATEMENT_BY_LEAF_2;

		JsonElement a = JsonParser.parseString(jsonStringA);
		JsonElement b = JsonParser.parseString(jsonStringB);

		List<String> claimNames = new ArrayList<>(EntityUtils.STANDARD_ENTITY_STATEMENT_CLAIMS);
		claimNames.removeAll(List.of("iat", "exp", "trust_marks"));

		List<String> differences = EntityUtils.diffEntityStatements(claimNames, a, b);

		assertEquals(0, differences.size());
	}


	public static String SUBORDINATE_STATEMENT_BY_INTERMEDIATE_FOR_LEAF =
			"""
					{
					  "sub": "https://oidc.test.bankid.com",
					  "metadata": {
					    "openid_provider": {
					      "request_parameter_supported": true,
					      "request_object_signing_alg_values_supported": [
					        "none",
					        "RS256",
					        "ES256",
					        "PS256"
					      ],
					      "request_object_encryption_alg_values_supported": [
					        "RSA1_5",
					        "RSA-OAEP"
					      ],
					      "request_object_encryption_enc_values_supported": [
					        "A128CBC-HS256",
					        "A128GCM",
					        "A192CBC-HS384",
					        "A192GCM",
					        "A256CBC-HS512",
					        "A256GCM"
					      ],
					      "token_endpoint": "https://oidc.test.bankid.com/token",
					      "request_uri_parameter_supported": false,
					      "token_endpoint_auth_methods_supported": [
					        "client_secret_post",
					        "client_secret_basic",
					        "private_key_jwt"
					      ],
					      "token_endpoint_auth_signing_alg_values_supported": [
					        "RS256",
					        "ES256",
					        "PS256"
					      ],
					      "subject_types_supported": [
					        "public",
					        "pairwise"
					      ],
					      "claims_parameter_supported": true,
					      "jwks": {
					        "keys": [
					          {
					            "e": "AQAB",
					            "kid": "FU06ji53d1xJVy4BhUXw3pWSe3SpDZwZdsetIL8qh1Q",
					            "kty": "RSA",
					            "n": "gClrCXP5Ff9ON-Hfkqp9fZiK_OUWfz4ERWlKZUq54bswJml4oTywhLHdzIf2BK8oHMYUPzAM4uTmf2p37lkgsHHkZUCDbb5UT3TKHmbbtvRbJ7StWea9kmDXRcG0RW_FjfRbFNCrwuc1Z7gzILOXVGcE5nc1-WXSz-6XYAd84U562uTkbZIlolMSVXr_ZHkYAKNNGRyESfsU34kj3SXN9eJmooFUUGXRikskHEDE1otWC8Hds8DmnDcZXmH_MaioJGbJpK3OA6dpkeT5K55ygegO7ADkjWrbmuzPOhIHBovQGhMmkTJGCSJmwX82jZWgT8jEr8JgWc6dsSnPTg91NOqIFegiYyS-4UrlXOcf9h9OHftiPgysHqemAaFS_S_NYEWEcnKoPsPnE2dUxf7OjQuWESknjgqy8N6Jm4Y8srj5fE_4fXKV7ept8tsSFS2Fc3g4Wqpd4XYuiKUYeq8JDISf15jqWw4p129X1nRTskMKrF0FjhMCgWCJDOnD57one6sGsXz9toQ4AKJRjC2O0zylD0cFfmuWN1T5e4dGEiu5Q8nJEyXbQaiOneD1kC5x2pFIEUMuvZ2YRkbVLTzEieyOELbdwegMA6vkiU4IMLSUB3ondIz5IuqaLnp4T0OnWU-d9sdelw_liHA9n__188zrnktvSge5bf8B-SuhLsc",
					            "use": "sig"
					          },
					          {
					            "e": "AQAB",
					            "kid": "Cez4Zz2YANA6d9JfVFHzADqsWw_RWqdQ7_L1CPy7dFE",
					            "kty": "RSA",
					            "n": "nu4CC39I0lwnm6qV1ZSb3lHqpOOiS5XOZGPnszxWqLCPU6K1eFjL62vO2pIN9EC5cKVbJMjSA9-XCJYlLufHm7C80INlyuBOzKYqS7WP6dKc3KX2jsTzvhJpPiBNxyUEm760YpiKB3cHAf7NNa_V0EGnWToTc_jbRTG8GZSgv8lCNNgpBFlJ0LcDTAlB8oau-yKNY4s5Ik3RktVy5IkhO2cXIFSpzYVB3N8I9RD_yeWMrzPv2j_GVHL4wSoaXIZbEz-LVw2VpbKBEzcO-SGSaXpbE58doW15kdj6EipuPxOQKH81Lmi-CTz3D91tQUPjTaACqe_M7_ny3I-gIomhlw",
					            "use": "sig"
					          },
					          {
					            "e": "AQAB",
					            "kid": "fF5EqLffTfFXPAoKlwS1m8FxE55jvrjTaAd7H_R41xs",
					            "kty": "RSA",
					            "n": "yHQ9rcmfPjvHw1MGKb0X56nF5_s3b7FAtH_3XgZP71j_Qf3ez6Go_RuCob74oN-jlUdSNmilYda2w40uYB49J3ZbpIoH9GcPz6KC3qyZjg17Rk2m7_SnERNvZxjn_nd5uC-qfcpRXS2_I7Zu3VPg4TvjU8Zxe5Z6U9a0zmQoco0DLYMbt1mi707EkCjMooTILDKZR9uk_QWGBQKxwmMs8pNag0s9kLzWX1CD7bToWR1637wDv-NjSz3_kHkws0nLEWBYm0cmtXuU51R6OrcOif5Lh9j38P8MQUUC72dv8LwMgT_42DxAyZsbVXn5n-xwjeyVvtZVLfInU8rgdHXmcQ",
					            "use": "enc"
					          }
					        ]
					      },
					      "id_token_signing_alg_values_supported": [
					        "RS256",
					        "RS384",
					        "RS512",
					        "PS256",
					        "PS384",
					        "PS512"
					      ],
					      "authorization_endpoint": "https://oidc.test.bankid.com/authorize",
					      "require_request_uri_registration": false,
					      "introspection_endpoint": "https://oidc.test.bankid.com/introspect",
					      "service_documentation": "https://oidc.test.bankid.com/about",
					      "response_types_supported": [
					        "code"
					      ],
					      "response_modes_supported": [
					        "query"
					      ],
					      "grant_types_supported": [
					        "authorization_code"
					      ],
					      "scopes_supported": [
					        "openid",
					        "profile",
					        "https://id.oidc.se/scope/naturalPersonNumber",
					        "https://id.oidc.se/scope/naturalPersonInfo",
					        "https://id.oidc.se/scope/sign",
					        "https://id.oidc.bankid.com/scope/authnInfo"
					      ],
					      "acr_values_supported": [
					        "http://id.elegnamnden.se/loa/1.0/loa3"
					      ],
					      "userinfo_endpoint": "https://oidc.test.bankid.com/userinfo",
					      "userinfo_signing_alg_values_supported": [
					        "RS256",
					        "RS384",
					        "RS512",
					        "PS256",
					        "PS384",
					        "PS512"
					      ],
					      "op_tos_uri": "https://oidc.test.bankid.com/about",
					      "issuer": "https://oidc.test.bankid.com",
					      "op_policy_uri": "https://oidc.test.bankid.com/about",
					      "claim_types_supported": [
					        "normal"
					      ],
					      "claims_supported": [
					        "sub",
					        "name",
					        "given_name",
					        "family_name",
					        "txn",
					        "auth_time",
					        "https://id.oidc.se/claim/personalIdentityNumber",
					        "https://id.oidc.se/claim/userCertificate",
					        "https://id.oidc.se/claim/userSignature",
					        "https://id.oidc.se/claim/credentialValidFrom",
					        "https://id.oidc.se/claim/credentialValidTo",
					        "https://id.oidc.se/claim/deviceIp",
					        "https://id.oidc.se/claim/authnEvidence",
					        "https://id.oidc.bankid.com/claim/age"
					      ],
					      "display_values_supported": [
					        "page",
					        "popup",
					        "touch",
					        "wap"
					      ],
					      "code_challenge_methods_supported": [
					        "S256"
					      ],
					      "ui_locales_supported": [
					        "sv",
					        "en"
					      ],
					      "https://id.oidc.se/disco/userMessageSupported": true,
					      "https://id.oidc.se/disco/userMessageSupportedMimeTypes": [
					        "text/plain",
					        "text/markdown"
					      ],
					      "https://id.oidc.se/disco/authnProviderSupported": false
					    }
					  },
					  "jwks": {
					    "keys": [
					      {
					        "kty": "EC",
					        "crv": "P-256",
					        "kid": "sandbox-ie-1",
					        "x": "K7YaBE2feuWuV_eVvxNoBrGOA5OZdms6WrfnCIK5nac",
					        "y": "LweaRC8dOK0SC7CjzJynOTHXBfosUCmA6g3rPnzjmn8"
					      }
					    ]
					  },
					  "crit": [
					    "subject_entity_configuration_location"
					  ],
					  "source_endpoint": "https://sandbox.swedenconnect.se/oidfed/intermediate/fetch",
					  "subject_entity_configuration_location": "data:application/entity-statement+jwt,eyJraWQiOiJzYW5kYm94LWllLTEiLCJ0eXAiOiJlbnRpdHktc3RhdGVtZW50K2p3dCIsImFsZyI6IkVTMjU2In0.eyJzdWIiOiJodHRwczovL29pZGMudGVzdC5iYW5raWQuY29tIiwibWV0YWRhdGEiOnsib3BlbmlkX3Byb3ZpZGVyIjp7InJlcXVlc3RfcGFyYW1ldGVyX3N1cHBvcnRlZCI6dHJ1ZSwicmVxdWVzdF9vYmplY3Rfc2lnbmluZ19hbGdfdmFsdWVzX3N1cHBvcnRlZCI6WyJub25lIiwiUlMyNTYiLCJFUzI1NiIsIlBTMjU2Il0sInJlcXVlc3Rfb2JqZWN0X2VuY3J5cHRpb25fYWxnX3ZhbHVlc19zdXBwb3J0ZWQiOlsiUlNBMV81IiwiUlNBLU9BRVAiXSwicmVxdWVzdF9vYmplY3RfZW5jcnlwdGlvbl9lbmNfdmFsdWVzX3N1cHBvcnRlZCI6WyJBMTI4Q0JDLUhTMjU2IiwiQTEyOEdDTSIsIkExOTJDQkMtSFMzODQiLCJBMTkyR0NNIiwiQTI1NkNCQy1IUzUxMiIsIkEyNTZHQ00iXSwidG9rZW5fZW5kcG9pbnQiOiJodHRwczovL29pZGMudGVzdC5iYW5raWQuY29tL3Rva2VuIiwicmVxdWVzdF91cmlfcGFyYW1ldGVyX3N1cHBvcnRlZCI6ZmFsc2UsInRva2VuX2VuZHBvaW50X2F1dGhfbWV0aG9kc19zdXBwb3J0ZWQiOlsiY2xpZW50X3NlY3JldF9wb3N0IiwiY2xpZW50X3NlY3JldF9iYXNpYyIsInByaXZhdGVfa2V5X2p3dCJdLCJ0b2tlbl9lbmRwb2ludF9hdXRoX3NpZ25pbmdfYWxnX3ZhbHVlc19zdXBwb3J0ZWQiOlsiUlMyNTYiLCJFUzI1NiIsIlBTMjU2Il0sInN1YmplY3RfdHlwZXNfc3VwcG9ydGVkIjpbInB1YmxpYyIsInBhaXJ3aXNlIl0sImNsYWltc19wYXJhbWV0ZXJfc3VwcG9ydGVkIjp0cnVlLCJqd2tzIjp7ImtleXMiOlt7ImUiOiJBUUFCIiwia2lkIjoiRlUwNmppNTNkMXhKVnk0QmhVWHczcFdTZTNTcERad1pkc2V0SUw4cWgxUSIsImt0eSI6IlJTQSIsIm4iOiJnQ2xyQ1hQNUZmOU9OLUhma3FwOWZaaUtfT1VXZno0RVJXbEtaVXE1NGJzd0ptbDRvVHl3aExIZHpJZjJCSzhvSE1ZVVB6QU00dVRtZjJwMzdsa2dzSEhrWlVDRGJiNVVUM1RLSG1iYnR2UmJKN1N0V2VhOWttRFhSY0cwUldfRmpmUmJGTkNyd3VjMVo3Z3pJTE9YVkdjRTVuYzEtV1hTei02WFlBZDg0VTU2MnVUa2JaSWxvbE1TVlhyX1pIa1lBS05OR1J5RVNmc1UzNGtqM1NYTjllSm1vb0ZVVUdYUmlrc2tIRURFMW90V0M4SGRzOERtbkRjWlhtSF9NYWlvSkdiSnBLM09BNmRwa2VUNUs1NXlnZWdPN0FEa2pXcmJtdXpQT2hJSEJvdlFHaE1ta1RKR0NTSm13WDgyalpXZ1Q4akVyOEpnV2M2ZHNTblBUZzkxTk9xSUZlZ2lZeVMtNFVybFhPY2Y5aDlPSGZ0aVBneXNIcWVtQWFGU19TX05ZRVdFY25Lb1BzUG5FMmRVeGY3T2pRdVdFU2tuamdxeThONkptNFk4c3JqNWZFXzRmWEtWN2VwdDh0c1NGUzJGYzNnNFdxcGQ0WFl1aUtVWWVxOEpESVNmMTVqcVd3NHAxMjlYMW5SVHNrTUtyRjBGamhNQ2dXQ0pET25ENTdvbmU2c0dzWHo5dG9RNEFLSlJqQzJPMHp5bEQwY0ZmbXVXTjFUNWU0ZEdFaXU1UThuSkV5WGJRYWlPbmVEMWtDNXgycEZJRVVNdXZaMllSa2JWTFR6RWlleU9FTGJkd2VnTUE2dmtpVTRJTUxTVUIzb25kSXo1SXVxYUxucDRUME9uV1UtZDlzZGVsd19saUhBOW5fXzE4OHpybmt0dlNnZTViZjhCLVN1aExzYyIsInVzZSI6InNpZyJ9LHsiZSI6IkFRQUIiLCJraWQiOiJDZXo0WnoyWUFOQTZkOUpmVkZIekFEcXNXd19SV3FkUTdfTDFDUHk3ZEZFIiwia3R5IjoiUlNBIiwibiI6Im51NENDMzlJMGx3bm02cVYxWlNiM2xIcXBPT2lTNVhPWkdQbnN6eFdxTENQVTZLMWVGakw2MnZPMnBJTjlFQzVjS1ZiSk1qU0E5LVhDSllsTHVmSG03QzgwSU5seXVCT3pLWXFTN1dQNmRLYzNLWDJqc1R6dmhKcFBpQk54eVVFbTc2MFlwaUtCM2NIQWY3Tk5hX1YwRUduV1RvVGNfamJSVEc4R1pTZ3Y4bENOTmdwQkZsSjBMY0RUQWxCOG9hdS15S05ZNHM1SWszUmt0Vnk1SWtoTzJjWElGU3B6WVZCM044STlSRF95ZVdNcnpQdjJqX0dWSEw0d1NvYVhJWmJFei1MVncyVnBiS0JFemNPLVNHU2FYcGJFNThkb1cxNWtkajZFaXB1UHhPUUtIODFMbWktQ1R6M0Q5MXRRVVBqVGFBQ3FlX003X255M0ktZ0lvbWhsdyIsInVzZSI6InNpZyJ9LHsiZSI6IkFRQUIiLCJraWQiOiJmRjVFcUxmZlRmRlhQQW9LbHdTMW04RnhFNTVqdnJqVGFBZDdIX1I0MXhzIiwia3R5IjoiUlNBIiwibiI6InlIUTlyY21mUGp2SHcxTUdLYjBYNTZuRjVfczNiN0ZBdEhfM1hnWlA3MWpfUWYzZXo2R29fUnVDb2I3NG9OLWpsVWRTTm1pbFlkYTJ3NDB1WUI0OUozWmJwSW9IOUdjUHo2S0MzcXlaamcxN1JrMm03X1NuRVJOdlp4am5fbmQ1dUMtcWZjcFJYUzJfSTdadTNWUGc0VHZqVThaeGU1WjZVOWEwem1Rb2NvMERMWU1idDFtaTcwN0VrQ2pNb29USUxES1pSOXVrX1FXR0JRS3h3bU1zOHBOYWcwczlrTHpXWDFDRDdiVG9XUjE2Mzd3RHYtTmpTejNfa0hrd3MwbkxFV0JZbTBjbXRYdVU1MVI2T3JjT2lmNUxoOWozOFA4TVFVVUM3MmR2OEx3TWdUXzQyRHhBeVpzYlZYbjVuLXh3amV5VnZ0WlZMZkluVThyZ2RIWG1jUSIsInVzZSI6ImVuYyJ9XX0sImlkX3Rva2VuX3NpZ25pbmdfYWxnX3ZhbHVlc19zdXBwb3J0ZWQiOlsiUlMyNTYiLCJSUzM4NCIsIlJTNTEyIiwiUFMyNTYiLCJQUzM4NCIsIlBTNTEyIl0sImF1dGhvcml6YXRpb25fZW5kcG9pbnQiOiJodHRwczovL29pZGMudGVzdC5iYW5raWQuY29tL2F1dGhvcml6ZSIsInJlcXVpcmVfcmVxdWVzdF91cmlfcmVnaXN0cmF0aW9uIjpmYWxzZSwiaW50cm9zcGVjdGlvbl9lbmRwb2ludCI6Imh0dHBzOi8vb2lkYy50ZXN0LmJhbmtpZC5jb20vaW50cm9zcGVjdCIsInNlcnZpY2VfZG9jdW1lbnRhdGlvbiI6Imh0dHBzOi8vb2lkYy50ZXN0LmJhbmtpZC5jb20vYWJvdXQiLCJyZXNwb25zZV90eXBlc19zdXBwb3J0ZWQiOlsiY29kZSJdLCJyZXNwb25zZV9tb2Rlc19zdXBwb3J0ZWQiOlsicXVlcnkiXSwiZ3JhbnRfdHlwZXNfc3VwcG9ydGVkIjpbImF1dGhvcml6YXRpb25fY29kZSJdLCJzY29wZXNfc3VwcG9ydGVkIjpbIm9wZW5pZCIsInByb2ZpbGUiLCJodHRwczovL2lkLm9pZGMuc2Uvc2NvcGUvbmF0dXJhbFBlcnNvbk51bWJlciIsImh0dHBzOi8vaWQub2lkYy5zZS9zY29wZS9uYXR1cmFsUGVyc29uSW5mbyIsImh0dHBzOi8vaWQub2lkYy5zZS9zY29wZS9zaWduIiwiaHR0cHM6Ly9pZC5vaWRjLmJhbmtpZC5jb20vc2NvcGUvYXV0aG5JbmZvIl0sImFjcl92YWx1ZXNfc3VwcG9ydGVkIjpbImh0dHA6Ly9pZC5lbGVnbmFtbmRlbi5zZS9sb2EvMS4wL2xvYTMiXSwidXNlcmluZm9fZW5kcG9pbnQiOiJodHRwczovL29pZGMudGVzdC5iYW5raWQuY29tL3VzZXJpbmZvIiwidXNlcmluZm9fc2lnbmluZ19hbGdfdmFsdWVzX3N1cHBvcnRlZCI6WyJSUzI1NiIsIlJTMzg0IiwiUlM1MTIiLCJQUzI1NiIsIlBTMzg0IiwiUFM1MTIiXSwib3BfdG9zX3VyaSI6Imh0dHBzOi8vb2lkYy50ZXN0LmJhbmtpZC5jb20vYWJvdXQiLCJpc3N1ZXIiOiJodHRwczovL29pZGMudGVzdC5iYW5raWQuY29tIiwib3BfcG9saWN5X3VyaSI6Imh0dHBzOi8vb2lkYy50ZXN0LmJhbmtpZC5jb20vYWJvdXQiLCJjbGFpbV90eXBlc19zdXBwb3J0ZWQiOlsibm9ybWFsIl0sImNsYWltc19zdXBwb3J0ZWQiOlsic3ViIiwibmFtZSIsImdpdmVuX25hbWUiLCJmYW1pbHlfbmFtZSIsInR4biIsImF1dGhfdGltZSIsImh0dHBzOi8vaWQub2lkYy5zZS9jbGFpbS9wZXJzb25hbElkZW50aXR5TnVtYmVyIiwiaHR0cHM6Ly9pZC5vaWRjLnNlL2NsYWltL3VzZXJDZXJ0aWZpY2F0ZSIsImh0dHBzOi8vaWQub2lkYy5zZS9jbGFpbS91c2VyU2lnbmF0dXJlIiwiaHR0cHM6Ly9pZC5vaWRjLnNlL2NsYWltL2NyZWRlbnRpYWxWYWxpZEZyb20iLCJodHRwczovL2lkLm9pZGMuc2UvY2xhaW0vY3JlZGVudGlhbFZhbGlkVG8iLCJodHRwczovL2lkLm9pZGMuc2UvY2xhaW0vZGV2aWNlSXAiLCJodHRwczovL2lkLm9pZGMuc2UvY2xhaW0vYXV0aG5FdmlkZW5jZSIsImh0dHBzOi8vaWQub2lkYy5iYW5raWQuY29tL2NsYWltL2FnZSJdLCJkaXNwbGF5X3ZhbHVlc19zdXBwb3J0ZWQiOlsicGFnZSIsInBvcHVwIiwidG91Y2giLCJ3YXAiXSwiY29kZV9jaGFsbGVuZ2VfbWV0aG9kc19zdXBwb3J0ZWQiOlsiUzI1NiJdLCJ1aV9sb2NhbGVzX3N1cHBvcnRlZCI6WyJzdiIsImVuIl0sImh0dHBzOi8vaWQub2lkYy5zZS9kaXNjby91c2VyTWVzc2FnZVN1cHBvcnRlZCI6dHJ1ZSwiaHR0cHM6Ly9pZC5vaWRjLnNlL2Rpc2NvL3VzZXJNZXNzYWdlU3VwcG9ydGVkTWltZVR5cGVzIjpbInRleHQvcGxhaW4iLCJ0ZXh0L21hcmtkb3duIl0sImh0dHBzOi8vaWQub2lkYy5zZS9kaXNjby9hdXRoblByb3ZpZGVyU3VwcG9ydGVkIjpmYWxzZX19LCJqd2tzIjp7ImtleXMiOlt7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJraWQiOiJzYW5kYm94LWllLTEiLCJ4IjoiSzdZYUJFMmZldVd1Vl9lVnZ4Tm9CckdPQTVPWmRtczZXcmZuQ0lLNW5hYyIsInkiOiJMd2VhUkM4ZE9LMFNDN0Nqekp5bk9USFhCZm9zVUNtQTZnM3JQbnpqbW44In1dfSwiaXNzIjoiaHR0cHM6Ly9vaWRjLnRlc3QuYmFua2lkLmNvbSIsImF1dGhvcml0eV9oaW50cyI6WyJodHRwczovL3NhbmRib3guc3dlZGVuY29ubmVjdC5zZS9vaWRmZWQvaW50ZXJtZWRpYXRlIl0sImV4cCI6MTcyODU3NTU1MywiaWF0IjoxNzI4NDg5MTUzLCJqdGkiOiJlZTI2NGVkZTgxMTkxMjRiOWViNDdjZDJlZGRiMzRjNiIsInRydXN0X21hcmtzIjpbeyJpZCI6Imh0dHBzOi8vc2FuZGJveC5zd2VkZW5jb25uZWN0LnNlL3RydXN0LW1hcmstaWQvbG9hL3N1YnN0YW50aWFsIiwidHJ1c3RfbWFyayI6ImV5SnJhV1FpT2lKellXNWtZbTk0TFhSdGFTMHhJaXdpZEhsd0lqb2lkSEoxYzNRdGJXRnlheXRxZDNRaUxDSmhiR2NpT2lKRlV6STFOaUo5LmV5SnBjM01pT2lKb2RIUndjem92TDNOaGJtUmliM2d1YzNkbFpHVnVZMjl1Ym1WamRDNXpaUzl2YVdSbVpXUXZkSEoxYzNRdGJXRnlheTFwYzNOMVpYSWlMQ0p6ZFdJaU9pSm9kSFJ3Y3pvdkwyOXBaR011ZEdWemRDNWlZVzVyYVdRdVkyOXRJaXdpYVdRaU9pSm9kSFJ3Y3pvdkwzTmhibVJpYjNndWMzZGxaR1Z1WTI5dWJtVmpkQzV6WlM5MGNuVnpkQzF0WVhKckxXbGtMMnh2WVM5emRXSnpkR0Z1ZEdsaGJDSXNJbVY0Y0NJNk1UY3lPRFUzTVRneE9Td2lhV0YwSWpveE56STRORGcxTkRFNUxDSnFkR2tpT2lJellXUmxaVGt3Tnpaa016RTFNelEyTnpjek5UVTFaVGRtWlRJM05EbGlPU0o5LjZWb0RuQTltdnp1TFZGcFJZYkpNNEN4TUxEWVVqbW05SHFJc1JVcFJHdXFuNWV5S0t6WnJJTHptNGlZU29BdW05T0Z6NTZlUmVEV2hKYmhJT0szU1ZnIn1dfQ.R8QTVSGgg_mv1Of-nTbTjBQk29IBI0qaAlLwLOR0WFrZXy0w7uVrgVrOBggVPJ5TI_aTPV5MJU0kMV_gfutGxw",
					  "iss": "https://sandbox.swedenconnect.se/oidfed/intermediate",
					  "authority_hints": [
					    "https://sandbox.swedenconnect.se/oidfed/trust-anchor"
					  ],
					  "exp": 1728575553,
					  "iat": 1728489153,
					  "jti": "c2a0e0b6da466cf53b3ee5e4534e03f9",
					  "trust_marks": [
					    {
					      "id": "https://sandbox.swedenconnect.se/trust-mark-id/loa/substantial",
					      "trust_mark": "eyJraWQiOiJzYW5kYm94LXRtaS0xIiwidHlwIjoidHJ1c3QtbWFyaytqd3QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NhbmRib3guc3dlZGVuY29ubmVjdC5zZS9vaWRmZWQvdHJ1c3QtbWFyay1pc3N1ZXIiLCJzdWIiOiJodHRwczovL29pZGMudGVzdC5iYW5raWQuY29tIiwiaWQiOiJodHRwczovL3NhbmRib3guc3dlZGVuY29ubmVjdC5zZS90cnVzdC1tYXJrLWlkL2xvYS9zdWJzdGFudGlhbCIsImV4cCI6MTcyODU3MTgxOSwiaWF0IjoxNzI4NDg1NDE5LCJqdGkiOiIzYWRlZTkwNzZkMzE1MzQ2NzczNTU1ZTdmZTI3NDliOSJ9.6VoDnA9mvzuLVFpRYbJM4CxMLDYUjmm9HqIsRUpRGuqn5eyKKzZrILzm4iYSoAum9OFz56eReDWhJbhIOK3SVg"
					    }
					  ]
					}
					""";

	public static String ENTITY_STATEMENT_BY_LEAF_1 =
			"""
					{
					  "sub": "https://oidc.test.bankid.com",
					  "metadata": {
					    "openid_provider": {
					      "request_parameter_supported": true,
					      "request_object_signing_alg_values_supported": [
					        "none",
					        "RS256",
					        "ES256",
					        "PS256"
					      ],
					      "request_object_encryption_alg_values_supported": [
					        "RSA1_5",
					        "RSA-OAEP"
					      ],
					      "request_object_encryption_enc_values_supported": [
					        "A128CBC-HS256",
					        "A128GCM",
					        "A192CBC-HS384",
					        "A192GCM",
					        "A256CBC-HS512",
					        "A256GCM"
					      ],
					      "token_endpoint": "https://oidc.test.bankid.com/token",
					      "request_uri_parameter_supported": false,
					      "token_endpoint_auth_methods_supported": [
					        "client_secret_post",
					        "client_secret_basic",
					        "private_key_jwt"
					      ],
					      "token_endpoint_auth_signing_alg_values_supported": [
					        "RS256",
					        "ES256",
					        "PS256"
					      ],
					      "subject_types_supported": [
					        "public",
					        "pairwise"
					      ],
					      "claims_parameter_supported": true,
					      "jwks": {
					        "keys": [
					          {
					            "e": "AQAB",
					            "kid": "FU06ji53d1xJVy4BhUXw3pWSe3SpDZwZdsetIL8qh1Q",
					            "kty": "RSA",
					            "n": "gClrCXP5Ff9ON-Hfkqp9fZiK_OUWfz4ERWlKZUq54bswJml4oTywhLHdzIf2BK8oHMYUPzAM4uTmf2p37lkgsHHkZUCDbb5UT3TKHmbbtvRbJ7StWea9kmDXRcG0RW_FjfRbFNCrwuc1Z7gzILOXVGcE5nc1-WXSz-6XYAd84U562uTkbZIlolMSVXr_ZHkYAKNNGRyESfsU34kj3SXN9eJmooFUUGXRikskHEDE1otWC8Hds8DmnDcZXmH_MaioJGbJpK3OA6dpkeT5K55ygegO7ADkjWrbmuzPOhIHBovQGhMmkTJGCSJmwX82jZWgT8jEr8JgWc6dsSnPTg91NOqIFegiYyS-4UrlXOcf9h9OHftiPgysHqemAaFS_S_NYEWEcnKoPsPnE2dUxf7OjQuWESknjgqy8N6Jm4Y8srj5fE_4fXKV7ept8tsSFS2Fc3g4Wqpd4XYuiKUYeq8JDISf15jqWw4p129X1nRTskMKrF0FjhMCgWCJDOnD57one6sGsXz9toQ4AKJRjC2O0zylD0cFfmuWN1T5e4dGEiu5Q8nJEyXbQaiOneD1kC5x2pFIEUMuvZ2YRkbVLTzEieyOELbdwegMA6vkiU4IMLSUB3ondIz5IuqaLnp4T0OnWU-d9sdelw_liHA9n__188zrnktvSge5bf8B-SuhLsc",
					            "use": "sig"
					          },
					          {
					            "e": "AQAB",
					            "kid": "Cez4Zz2YANA6d9JfVFHzADqsWw_RWqdQ7_L1CPy7dFE",
					            "kty": "RSA",
					            "n": "nu4CC39I0lwnm6qV1ZSb3lHqpOOiS5XOZGPnszxWqLCPU6K1eFjL62vO2pIN9EC5cKVbJMjSA9-XCJYlLufHm7C80INlyuBOzKYqS7WP6dKc3KX2jsTzvhJpPiBNxyUEm760YpiKB3cHAf7NNa_V0EGnWToTc_jbRTG8GZSgv8lCNNgpBFlJ0LcDTAlB8oau-yKNY4s5Ik3RktVy5IkhO2cXIFSpzYVB3N8I9RD_yeWMrzPv2j_GVHL4wSoaXIZbEz-LVw2VpbKBEzcO-SGSaXpbE58doW15kdj6EipuPxOQKH81Lmi-CTz3D91tQUPjTaACqe_M7_ny3I-gIomhlw",
					            "use": "sig"
					          },
					          {
					            "e": "AQAB",
					            "kid": "fF5EqLffTfFXPAoKlwS1m8FxE55jvrjTaAd7H_R41xs",
					            "kty": "RSA",
					            "n": "yHQ9rcmfPjvHw1MGKb0X56nF5_s3b7FAtH_3XgZP71j_Qf3ez6Go_RuCob74oN-jlUdSNmilYda2w40uYB49J3ZbpIoH9GcPz6KC3qyZjg17Rk2m7_SnERNvZxjn_nd5uC-qfcpRXS2_I7Zu3VPg4TvjU8Zxe5Z6U9a0zmQoco0DLYMbt1mi707EkCjMooTILDKZR9uk_QWGBQKxwmMs8pNag0s9kLzWX1CD7bToWR1637wDv-NjSz3_kHkws0nLEWBYm0cmtXuU51R6OrcOif5Lh9j38P8MQUUC72dv8LwMgT_42DxAyZsbVXn5n-xwjeyVvtZVLfInU8rgdHXmcQ",
					            "use": "enc"
					          }
					        ]
					      },
					      "id_token_signing_alg_values_supported": [
					        "RS256",
					        "RS384",
					        "RS512",
					        "PS256",
					        "PS384",
					        "PS512"
					      ],
					      "authorization_endpoint": "https://oidc.test.bankid.com/authorize",
					      "require_request_uri_registration": false,
					      "introspection_endpoint": "https://oidc.test.bankid.com/introspect",
					      "service_documentation": "https://oidc.test.bankid.com/about",
					      "response_types_supported": [
					        "code"
					      ],
					      "response_modes_supported": [
					        "query"
					      ],
					      "grant_types_supported": [
					        "authorization_code"
					      ],
					      "scopes_supported": [
					        "openid",
					        "profile",
					        "https://id.oidc.se/scope/naturalPersonNumber",
					        "https://id.oidc.se/scope/naturalPersonInfo",
					        "https://id.oidc.se/scope/sign",
					        "https://id.oidc.bankid.com/scope/authnInfo"
					      ],
					      "acr_values_supported": [
					        "http://id.elegnamnden.se/loa/1.0/loa3"
					      ],
					      "userinfo_endpoint": "https://oidc.test.bankid.com/userinfo",
					      "userinfo_signing_alg_values_supported": [
					        "RS256",
					        "RS384",
					        "RS512",
					        "PS256",
					        "PS384",
					        "PS512"
					      ],
					      "op_tos_uri": "https://oidc.test.bankid.com/about",
					      "issuer": "https://oidc.test.bankid.com",
					      "op_policy_uri": "https://oidc.test.bankid.com/about",
					      "claim_types_supported": [
					        "normal"
					      ],
					      "claims_supported": [
					        "sub",
					        "name",
					        "given_name",
					        "family_name",
					        "txn",
					        "auth_time",
					        "https://id.oidc.se/claim/personalIdentityNumber",
					        "https://id.oidc.se/claim/userCertificate",
					        "https://id.oidc.se/claim/userSignature",
					        "https://id.oidc.se/claim/credentialValidFrom",
					        "https://id.oidc.se/claim/credentialValidTo",
					        "https://id.oidc.se/claim/deviceIp",
					        "https://id.oidc.se/claim/authnEvidence",
					        "https://id.oidc.bankid.com/claim/age"
					      ],
					      "display_values_supported": [
					        "page",
					        "popup",
					        "touch",
					        "wap"
					      ],
					      "code_challenge_methods_supported": [
					        "S256"
					      ],
					      "ui_locales_supported": [
					        "sv",
					        "en"
					      ],
					      "https://id.oidc.se/disco/userMessageSupported": true,
					      "https://id.oidc.se/disco/userMessageSupportedMimeTypes": [
					        "text/plain",
					        "text/markdown"
					      ],
					      "https://id.oidc.se/disco/authnProviderSupported": false
					    }
					  },
					  "jwks": {
					    "keys": [
					      {
					        "kty": "EC",
					        "crv": "P-256",
					        "kid": "sandbox-ie-1",
					        "x": "K7YaBE2feuWuV_eVvxNoBrGOA5OZdms6WrfnCIK5nac",
					        "y": "LweaRC8dOK0SC7CjzJynOTHXBfosUCmA6g3rPnzjmn8"
					      }
					    ]
					  },
					  "iss": "https://oidc.test.bankid.com",
					  "authority_hints": [
					    "https://sandbox.swedenconnect.se/oidfed/intermediate"
					  ],
					  "exp": 1728145055,
					  "iat": 1728058655,
					  "jti": "d1828936c30e10a0d51caf58d404d950",
					  "trust_marks": [
					    {
					      "id": "https://sandbox.swedenconnect.se/trust-mark-id/loa/substantial",
					      "trust_mark": "eyJraWQiOiJzYW5kYm94LXRtaS0xIiwidHlwIjoidHJ1c3QtbWFyaytqd3QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NhbmRib3guc3dlZGVuY29ubmVjdC5zZS9vaWRmZWQvdHJ1c3QtbWFyay1pc3N1ZXIiLCJzdWIiOiJodHRwczovL29pZGMudGVzdC5iYW5raWQuY29tIiwiaWQiOiJodHRwczovL3NhbmRib3guc3dlZGVuY29ubmVjdC5zZS90cnVzdC1tYXJrLWlkL2xvYS9zdWJzdGFudGlhbCIsImV4cCI6MTcyODEzOTgxOSwiaWF0IjoxNzI4MDUzNDE5LCJqdGkiOiI4NzFmZTBkYjdhZTA3NDE0NWI1MmNjNmNkMTdmNjM1NiJ9.wRSP03OJoRnPEsC3-vIpKVDQhrKGKIhlRdDdXRA5mKAC18PozP7kPUzLKGwoqUvQDTacYqIl7ngq_BdpD6wYpw"
					    }
					  ]
					}
					""";

	public static String ENTITY_STATEMENT_BY_LEAF_2 =
			"""
					{
					  "sub": "https://oidc.test.bankid.com",
					  "metadata": {
					    "openid_provider": {
					      "request_parameter_supported": true,
					      "request_object_signing_alg_values_supported": [
					        "none",
					        "RS256",
					        "ES256",
					        "PS256"
					      ],
					      "request_object_encryption_alg_values_supported": [
					        "RSA1_5",
					        "RSA-OAEP"
					      ],
					      "request_object_encryption_enc_values_supported": [
					        "A128CBC-HS256",
					        "A128GCM",
					        "A192CBC-HS384",
					        "A192GCM",
					        "A256CBC-HS512",
					        "A256GCM"
					      ],
					      "token_endpoint": "https://oidc.test.bankid.com/token",
					      "request_uri_parameter_supported": false,
					      "token_endpoint_auth_methods_supported": [
					        "client_secret_post",
					        "client_secret_basic",
					        "private_key_jwt"
					      ],
					      "token_endpoint_auth_signing_alg_values_supported": [
					        "RS256",
					        "ES256",
					        "PS256"
					      ],
					      "subject_types_supported": [
					        "public",
					        "pairwise"
					      ],
					      "claims_parameter_supported": true,
					      "jwks": {
					        "keys": [
					          {
					            "e": "AQAB",
					            "kid": "FU06ji53d1xJVy4BhUXw3pWSe3SpDZwZdsetIL8qh1Q",
					            "kty": "RSA",
					            "n": "gClrCXP5Ff9ON-Hfkqp9fZiK_OUWfz4ERWlKZUq54bswJml4oTywhLHdzIf2BK8oHMYUPzAM4uTmf2p37lkgsHHkZUCDbb5UT3TKHmbbtvRbJ7StWea9kmDXRcG0RW_FjfRbFNCrwuc1Z7gzILOXVGcE5nc1-WXSz-6XYAd84U562uTkbZIlolMSVXr_ZHkYAKNNGRyESfsU34kj3SXN9eJmooFUUGXRikskHEDE1otWC8Hds8DmnDcZXmH_MaioJGbJpK3OA6dpkeT5K55ygegO7ADkjWrbmuzPOhIHBovQGhMmkTJGCSJmwX82jZWgT8jEr8JgWc6dsSnPTg91NOqIFegiYyS-4UrlXOcf9h9OHftiPgysHqemAaFS_S_NYEWEcnKoPsPnE2dUxf7OjQuWESknjgqy8N6Jm4Y8srj5fE_4fXKV7ept8tsSFS2Fc3g4Wqpd4XYuiKUYeq8JDISf15jqWw4p129X1nRTskMKrF0FjhMCgWCJDOnD57one6sGsXz9toQ4AKJRjC2O0zylD0cFfmuWN1T5e4dGEiu5Q8nJEyXbQaiOneD1kC5x2pFIEUMuvZ2YRkbVLTzEieyOELbdwegMA6vkiU4IMLSUB3ondIz5IuqaLnp4T0OnWU-d9sdelw_liHA9n__188zrnktvSge5bf8B-SuhLsc",
					            "use": "sig"
					          },
					          {
					            "e": "AQAB",
					            "kid": "Cez4Zz2YANA6d9JfVFHzADqsWw_RWqdQ7_L1CPy7dFE",
					            "kty": "RSA",
					            "n": "nu4CC39I0lwnm6qV1ZSb3lHqpOOiS5XOZGPnszxWqLCPU6K1eFjL62vO2pIN9EC5cKVbJMjSA9-XCJYlLufHm7C80INlyuBOzKYqS7WP6dKc3KX2jsTzvhJpPiBNxyUEm760YpiKB3cHAf7NNa_V0EGnWToTc_jbRTG8GZSgv8lCNNgpBFlJ0LcDTAlB8oau-yKNY4s5Ik3RktVy5IkhO2cXIFSpzYVB3N8I9RD_yeWMrzPv2j_GVHL4wSoaXIZbEz-LVw2VpbKBEzcO-SGSaXpbE58doW15kdj6EipuPxOQKH81Lmi-CTz3D91tQUPjTaACqe_M7_ny3I-gIomhlw",
					            "use": "sig"
					          },
					          {
					            "e": "AQAB",
					            "kid": "fF5EqLffTfFXPAoKlwS1m8FxE55jvrjTaAd7H_R41xs",
					            "kty": "RSA",
					            "n": "yHQ9rcmfPjvHw1MGKb0X56nF5_s3b7FAtH_3XgZP71j_Qf3ez6Go_RuCob74oN-jlUdSNmilYda2w40uYB49J3ZbpIoH9GcPz6KC3qyZjg17Rk2m7_SnERNvZxjn_nd5uC-qfcpRXS2_I7Zu3VPg4TvjU8Zxe5Z6U9a0zmQoco0DLYMbt1mi707EkCjMooTILDKZR9uk_QWGBQKxwmMs8pNag0s9kLzWX1CD7bToWR1637wDv-NjSz3_kHkws0nLEWBYm0cmtXuU51R6OrcOif5Lh9j38P8MQUUC72dv8LwMgT_42DxAyZsbVXn5n-xwjeyVvtZVLfInU8rgdHXmcQ",
					            "use": "enc"
					          }
					        ]
					      },
					      "id_token_signing_alg_values_supported": [
					        "RS256",
					        "RS384",
					        "RS512",
					        "PS256",
					        "PS384",
					        "PS512"
					      ],
					      "authorization_endpoint": "https://oidc.test.bankid.com/authorize",
					      "require_request_uri_registration": false,
					      "introspection_endpoint": "https://oidc.test.bankid.com/introspect",
					      "service_documentation": "https://oidc.test.bankid.com/about",
					      "response_types_supported": [
					        "code"
					      ],
					      "response_modes_supported": [
					        "query"
					      ],
					      "grant_types_supported": [
					        "authorization_code"
					      ],
					      "scopes_supported": [
					        "openid",
					        "profile",
					        "https://id.oidc.se/scope/naturalPersonNumber",
					        "https://id.oidc.se/scope/naturalPersonInfo",
					        "https://id.oidc.se/scope/sign",
					        "https://id.oidc.bankid.com/scope/authnInfo"
					      ],
					      "acr_values_supported": [
					        "http://id.elegnamnden.se/loa/1.0/loa3"
					      ],
					      "userinfo_endpoint": "https://oidc.test.bankid.com/userinfo",
					      "userinfo_signing_alg_values_supported": [
					        "RS256",
					        "RS384",
					        "RS512",
					        "PS256",
					        "PS384",
					        "PS512"
					      ],
					      "op_tos_uri": "https://oidc.test.bankid.com/about",
					      "issuer": "https://oidc.test.bankid.com",
					      "op_policy_uri": "https://oidc.test.bankid.com/about",
					      "claim_types_supported": [
					        "normal"
					      ],
					      "claims_supported": [
					        "sub",
					        "name",
					        "given_name",
					        "family_name",
					        "txn",
					        "auth_time",
					        "https://id.oidc.se/claim/personalIdentityNumber",
					        "https://id.oidc.se/claim/userCertificate",
					        "https://id.oidc.se/claim/userSignature",
					        "https://id.oidc.se/claim/credentialValidFrom",
					        "https://id.oidc.se/claim/credentialValidTo",
					        "https://id.oidc.se/claim/deviceIp",
					        "https://id.oidc.se/claim/authnEvidence",
					        "https://id.oidc.bankid.com/claim/age"
					      ],
					      "display_values_supported": [
					        "page",
					        "popup",
					        "touch",
					        "wap"
					      ],
					      "code_challenge_methods_supported": [
					        "S256"
					      ],
					      "ui_locales_supported": [
					        "sv",
					        "en"
					      ],
					      "https://id.oidc.se/disco/userMessageSupported": true,
					      "https://id.oidc.se/disco/userMessageSupportedMimeTypes": [
					        "text/plain",
					        "text/markdown"
					      ],
					      "https://id.oidc.se/disco/authnProviderSupported": false
					    }
					  },
					  "jwks": {
					    "keys": [
					      {
					        "kty": "EC",
					        "crv": "P-256",
					        "kid": "sandbox-ie-1",
					        "x": "K7YaBE2feuWuV_eVvxNoBrGOA5OZdms6WrfnCIK5nac",
					        "y": "LweaRC8dOK0SC7CjzJynOTHXBfosUCmA6g3rPnzjmn8"
					      }
					    ]
					  },
					  "iss": "https://oidc.test.bankid.com",
					  "authority_hints": [
					    "https://sandbox.swedenconnect.se/oidfed/intermediate"
					  ],
					  "exp": 1728575554,
					  "iat": 1728489154,
					  "jti": "146628c643b7fddfd67be2d490672a15",
					  "trust_marks": [
					    {
					      "id": "https://sandbox.swedenconnect.se/trust-mark-id/loa/substantial",
					      "trust_mark": "eyJraWQiOiJzYW5kYm94LXRtaS0xIiwidHlwIjoidHJ1c3QtbWFyaytqd3QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NhbmRib3guc3dlZGVuY29ubmVjdC5zZS9vaWRmZWQvdHJ1c3QtbWFyay1pc3N1ZXIiLCJzdWIiOiJodHRwczovL29pZGMudGVzdC5iYW5raWQuY29tIiwiaWQiOiJodHRwczovL3NhbmRib3guc3dlZGVuY29ubmVjdC5zZS90cnVzdC1tYXJrLWlkL2xvYS9zdWJzdGFudGlhbCIsImV4cCI6MTcyODU3MTgxOSwiaWF0IjoxNzI4NDg1NDE5LCJqdGkiOiIzYWRlZTkwNzZkMzE1MzQ2NzczNTU1ZTdmZTI3NDliOSJ9.6VoDnA9mvzuLVFpRYbJM4CxMLDYUjmm9HqIsRUpRGuqn5eyKKzZrILzm4iYSoAum9OFz56eReDWhJbhIOK3SVg"
					    }
					  ]
					}
					""";

}
