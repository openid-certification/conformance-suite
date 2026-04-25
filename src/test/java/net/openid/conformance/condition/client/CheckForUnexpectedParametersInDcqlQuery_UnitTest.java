package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedParametersInDcqlQuery_UnitTest extends AbstractVciUnitTest {

	private CheckForUnexpectedParametersInDcqlQuery cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedParametersInDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_noWarningWhenNoUnknownProperties() {
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
			  ]
			}
			""";
		putDcql(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownPropertyInCredential() {
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
			      "unexpected": "boom"
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data,"$.credentials[0].unexpected");
	}

	@Test
	public void testEvaluate_unknownPropertyAtTopLevel() {
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
			  "unexpected_top": true
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data,"$.unexpected_top");
	}

	@Test
	public void testEvaluate_unknownPropertyInMeta() {
		String json = """
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ],
			        "unexpected_meta": true
			      }
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data,"$.credentials[0].meta.unexpected_meta");
	}

	@Test
	public void testEvaluate_unknownPropertyInClaim() {
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
			          "unexpected_claim": "boom"
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data,"$.credentials[0].claims[0].unexpected_claim");
	}

	@Test
	public void testEvaluate_unknownPropertyInTrustedAuthorities() {
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
			      "trusted_authorities": [
			        {
			          "type": "aki",
			          "values": [
			            "thumbprint"
			          ],
			          "unexpected_auth": "boom"
			        }
			      ]
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data,"$.credentials[0].trusted_authorities[0].unexpected_auth");
	}

	@Test
	public void testEvaluate_unknownPropertyInCredentialSet() {
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
			          "credential_1"
			        ]
			      ],
			      "unexpected_set": "boom"
			    }
			  ]
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data,"$.credential_sets[0].unexpected_set");
	}

	@Test
	public void testEvaluate_unknownPropertyInCredentialSetsArray() {
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
			          "credential_1"
			        ]
			      ]
			    }
			  ],
			  "credential_sets_unexpected": []
			}
			""";
		putDcql(json);

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertUnknownPropertyAtPath(data,"$.credential_sets_unexpected");
	}

	@Test
	public void testEvaluate_structuralErrorsDoNotWarn() {
		// Empty credentials array — this is a structural error (minItems), not an unknown
		// property. Uses a top-level error to avoid the anyOf cascade at credential level
		// which can produce additionalProperties from the non-matching branch.
		String json = """
			{
			  "credentials": []
			}
			""";
		putDcql(json);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	private void putDcql(String json) {
		JsonObject dcql = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("dcql_query", dcql);
	}

}
