package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AddRequiredNonMatchingCredentialToDcqlQuery_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddRequiredNonMatchingCredentialToDcqlQuery cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new AddRequiredNonMatchingCredentialToDcqlQuery();

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

		JsonObject fakeCredential = credentials.get(1).getAsJsonObject();
		assertEquals("dc+sd-jwt", OIDFJSON.getString(fakeCredential.get("format")),
			"the non-matching credential should use the same format as the original");

		JsonArray credentialSets = dcqlQuery.getAsJsonArray("credential_sets");
		assertEquals(2, credentialSets.size());
		for (var setEl : credentialSets) {
			JsonObject set = setEl.getAsJsonObject();
			assertTrue(OIDFJSON.getBoolean(set.get("required")),
				"both credential sets must be required, but found: " + set);
		}
	}

	@Test
	public void testEvaluate_mdocQuery() {
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": { "doctype_value": "org.iso.18013.5.1.mDL" },
			      "claims": [ { "path": [ "org.iso.18013.5.1", "given_name" ] } ]
			    }
			  ]
			}
			""");

		cond.execute(env);

		JsonObject dcqlQuery = env.getObject("dcql_query");
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		assertEquals(2, credentials.size());

		JsonObject fakeCredential = credentials.get(1).getAsJsonObject();
		assertEquals("mso_mdoc", OIDFJSON.getString(fakeCredential.get("format")));
		assertNotEquals("org.iso.18013.5.1.mDL",
			OIDFJSON.getString(fakeCredential.getAsJsonObject("meta").get("doctype_value")),
			"the added credential must not match the real doctype");

		JsonArray credentialSets = dcqlQuery.getAsJsonArray("credential_sets");
		assertEquals(2, credentialSets.size());
		for (var setEl : credentialSets) {
			assertTrue(OIDFJSON.getBoolean(setEl.getAsJsonObject().get("required")));
		}
	}

	@Test
	public void testEvaluate_credentialSetsAlreadyPresent() {
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [ { "id": "my_credential", "format": "dc+sd-jwt" } ],
			  "credential_sets": [ { "options": [ [ "my_credential" ] ] } ]
			}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
