package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AddOptionalNonMatchingCredentialToDcqlQuery_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddOptionalNonMatchingCredentialToDcqlQuery cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new AddOptionalNonMatchingCredentialToDcqlQuery();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_sdJwtQuery() {
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": { "vct_values": [ "urn:eudi:pid:1" ] },
			      "claims": [ { "path": [ "given_name" ] } ]
			    }
			  ]
			}
			""");

		cond.execute(env);

		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		assertEquals(2, credentials.size(), "a non-matching credential entry should have been added");

		JsonArray credentialSets = dcqlQuery.getAsJsonArray("credential_sets");
		assertEquals(2, credentialSets.size());

		JsonObject originalSet = credentialSets.get(0).getAsJsonObject();
		assertTrue(OIDFJSON.getBoolean(originalSet.get("required")),
			"the real credential's set must be required");

		JsonObject fakeSet = credentialSets.get(1).getAsJsonObject();
		assertFalse(OIDFJSON.getBoolean(fakeSet.get("required")),
			"the non-matching credential's set must be optional");
	}

}
