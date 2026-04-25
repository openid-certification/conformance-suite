package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateDCQLQuery_UnitTest extends AbstractVciUnitTest {

	private ValidateDCQLQuery cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateDCQLQuery();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_validDcqlWithVcSdJwt() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "path": [
			            "given_name"
			          ]
			        },
			        {
			          "path": [
			            "family_name"
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validDcqlWithMdoc() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "mso_mdoc",
			      "meta": {
			        "doctype_value": "org.iso.18013.5.1.mDL"
			      },
			      "claims": [
			        {
			          "path": [
			            "org.iso.18013.5.1",
			            "given_name"
			          ]
			        },
			        {
			          "path": [
			            "org.iso.18013.5.1",
			            "family_name"
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validDcqlWithCredentialSets() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "id": "claim_1",
			          "path": [
			            "given_name"
			          ]
			        },
			        {
			          "id": "claim_2",
			          "path": [
			            "family_name"
			          ]
			        }
			      ],
			      "claim_sets": [
			        [
			          "claim_1",
			          "claim_2"
			        ]
			      ]
			    }
			  ],
			  "credential_sets": [
			    {
			      "options": [
			        [
			          "credential_1"
			        ]
			      ],
			      "required": true
			    }
			  ]
			}
			""";
		putDcql(json);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_claimSetsWithoutClaims() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claim_sets": [
			        [
			          "claim_1"
			        ]
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].claims", "required property 'claims' not found");
	}

	@Test
	public void testEvaluate_claimSetsClaimsMissingId() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "path": [
			            "given_name"
			          ]
			        }
			      ],
			      "claim_sets": [
			        [
			          "claim_1"
			        ]
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].claims[0].id", "required property 'id' not found");
	}

	@Test
	public void testEvaluate_emptyClaimsArray() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": []
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorWithMessageFragment(data, "$.credentials[0].claims", "at least 1 items");
	}

	@Test
	public void testEvaluate_emptyClaimSetsArray() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "id": "claim_1",
			          "path": [
			            "given_name"
			          ]
			        }
			      ],
			      "claim_sets": []
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorWithMessageFragment(data, "$.credentials[0].claim_sets", "at least 1 items");
	}

	@Test
	public void testEvaluate_duplicateClaimIds() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "id": "claim_1",
			          "path": [
			            "given_name"
			          ]
			        },
			        {
			          "id": "claim_1",
			          "path": [
			            "family_name"
			          ]
			        }
			      ],
			      "claim_sets": [
			        [
			          "claim_1"
			        ]
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_duplicateClaimPaths() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "path": [
			            "given_name"
			          ]
			        },
			        {
			          "path": [
			            "given_name"
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_claimSetsReferencesUnknownClaimId() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "id": "claim_1",
			          "path": [
			            "given_name"
			          ]
			        }
			      ],
			      "claim_sets": [
			        [
			          "claim_missing"
			        ]
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_unknownPropertiesDoNotCauseError() {
		// Unknown properties are filtered out as additionalProperties errors
		// and handled separately by CheckForUnexpectedParametersInDcqlQuery as warnings.
		// The if/then/else schema structure (keyed on format) ensures that
		// unknown properties inside credential items only produce errors from
		// the matching format branch, avoiding cascade errors.
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "unexpected_credential": "boom"
			    }
			  ],
			  "credential_sets": [
			    {
			      "options": [
			        [
			          "credential_1"
			        ]
			      ],
			      "unexpected_set": "boom"
			    }
			  ],
			  "unexpected_top": true
			}
			""";
		putDcql(json);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_emptyCredentialsArray() {
		String json = """
			{
			  "credentials": []
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorWithMessageFragment(data, "$.credentials", "at least 1 items");
	}

	@Test
	public void testEvaluate_emptyCredentialSetsArray() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ],
			  "credential_sets": []
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorWithMessageFragment(data, "$.credential_sets", "at least 1 items");
	}

	@Test
	public void testEvaluate_duplicateCredentialIds() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    },
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		putDcql(json);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_credentialSetsReferencesUnknownCredentialId() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ],
			  "credential_sets": [
			    {
			      "options": [
			        [
			          "credential_missing"
			        ]
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingDcql() {
		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingRequiredCredentials() {
		String json = """
			{
			  "credential_sets": []
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials", "required property 'credentials' not found");
	}

	@Test
	public void testEvaluate_missingRequiredFormat() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].format", "required property 'format' not found");
	}

	@Test
	public void testEvaluate_missingRequiredMeta() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt"
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].meta", "required property 'meta' not found");
	}

	@Test
	public void testEvaluate_missingRequiredVctValues() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {}
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].meta.vct_values", "required property 'vct_values' not found");
	}

	@Test
	public void testEvaluate_missingRequiredDoctypeValue() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "mso_mdoc",
			      "meta": {}
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].meta.doctype_value", "required property 'doctype_value' not found");
	}

	@Test
	public void testEvaluate_invalidRequireCryptographicHolderBindingType() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "require_cryptographic_holder_binding": "true",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorWithMessageFragment(data, "$.credentials[0].require_cryptographic_holder_binding", "boolean");
	}

	@Test
	public void testEvaluate_invalidCredentialIdPattern() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential 1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorWithMessageFragment(data, "$.credentials[0].id", "does not match");
	}

	@Test
	public void testEvaluate_invalidClaimIdPattern() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "id": "claim 1",
			          "path": [
			            "given_name"
			          ]
			        }
			      ],
			      "claim_sets": [
			        [
			          "claim 1"
			        ]
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorWithMessageFragment(data, "$.credentials[0].claims[0].id", "does not match");
	}

	@Test
	public void testEvaluate_emptyCredentialId() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorForPath(data, "$.credentials[0].id");
	}

	@Test
	public void testEvaluate_emptyClaimId() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "id": "",
			          "path": [
			            "given_name"
			          ]
			        }
			      ],
			      "claim_sets": [
			        [
			          ""
			        ]
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorForPath(data, "$.credentials[0].claims[0].id");
	}

	@Test
	public void testEvaluate_emptyDoctypeValue() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "mso_mdoc",
			      "meta": {
			        "doctype_value": ""
			      },
			      "claims": [
			        {
			          "path": [
			            "org.iso.18013.5.1",
			            "given_name"
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorForPath(data, "$.credentials[0].meta.doctype_value");
	}

	@Test
	public void testEvaluate_emptyVctValuesEntry() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          ""
			        ]
			      }
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorForPath(data, "$.credentials[0].meta.vct_values[0]");
	}

	@Test
	public void testEvaluate_emptyMdocClaimPathSegment() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "mso_mdoc",
			      "meta": {
			        "doctype_value": "org.iso.18013.5.1.mDL"
			      },
			      "claims": [
			        {
			          "path": [
			            "",
			            "given_name"
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorForPath(data, "$.credentials[0].claims[0].path[0]");
	}

	@Test
	public void testEvaluate_emptyClaimValueString() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "path": [
			            "given_name"
			          ],
			          "values": [
			            ""
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorForPath(data, "$.credentials[0].claims[0].values[0]");
	}

	@Test
	public void testEvaluate_invalidFormatValue() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "invalid-format",
			      "meta": {}
			    }
			  ]
			}
			""";
		putDcql(json);

		// This should throw an error - just verify the validation fails
		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_invalidMdocClaimPath() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "mso_mdoc",
			      "meta": {
			        "doctype_value": "org.iso.18013.5.1.mDL"
			      },
			      "claims": [
			        {
			          "path": [
			            "only_one_element"
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		// mso_mdoc claims must have exactly 2 elements in the path array
		assertContainsExpectedError(data, "$.credentials[0].claims[0].path", "must have at least 2 items but found 1");
	}

	@Test
	public void testEvaluate_missingRequiredPathInClaim() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "values": [
			            "test"
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credentials[0].claims[0].path", "required property 'path' not found");
	}

	@Test
	public void testEvaluate_emptyValuesArray() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      },
			      "claims": [
			        {
			          "path": [
			            "given_name"
			          ],
			          "values": []
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsErrorWithMessageFragment(data, "$.credentials[0].claims[0].values", "at least 1 items");
	}

	@Test
	public void testEvaluate_missingRequiredOptionsInCredentialSet() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ],
			  "credential_sets": [
			    {
			      "required": true
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertContainsExpectedError(data, "$.credential_sets[0].options", "required property 'options' not found");
	}

	private void putDcql(String json) {
		JsonObject dcql = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("dcql_query", dcql);
	}

	private void assertContainsErrorWithMessageFragment(Map<String, Object> data, String property, String expectedErrorFragment) {
		JsonObject entry = findErrorByPath(data, property);
		org.junit.jupiter.api.Assertions.assertNotNull(entry);
		String propertyError = OIDFJSON.getString(entry.get("error"));
		org.junit.jupiter.api.Assertions.assertNotNull(propertyError);
		org.junit.jupiter.api.Assertions.assertTrue(propertyError.contains(expectedErrorFragment));
	}

	private void assertContainsErrorForPath(Map<String, Object> data, String property) {
		org.junit.jupiter.api.Assertions.assertNotNull(findErrorByPath(data, property));
	}

	private JsonObject findErrorByPath(Map<String, Object> data, String property) {
		Object invalidEntries = data.get("invalid_entries");
		org.junit.jupiter.api.Assertions.assertTrue(invalidEntries instanceof java.util.List<?>);

		for (Object entry : (java.util.List<?>) invalidEntries) {
			if (!(entry instanceof JsonObject)) {
				continue;
			}
			String propertyPath = OIDFJSON.getString(((JsonObject) entry).get("path"));
			if (property.equals(propertyPath)) {
				return (JsonObject) entry;
			}
		}

		return null;
	}
}
