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
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AddNonExistentClaimToDcqlQuery_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddNonExistentClaimToDcqlQuery cond;

	@BeforeEach
	public void setUp() {
		cond = new AddNonExistentClaimToDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void sdJwtQuery_appendsTopLevelClaim() {
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": { "vct_values": [ "urn:eudi:pid:1" ] },
			      "claims": [ { "path": [ "given_name" ] }, { "path": [ "family_name" ] } ]
			    }
			  ]
			}
			""");

		cond.execute(env);

		JsonArray claims = env.getObject("dcql_query").getAsJsonArray("credentials")
			.get(0).getAsJsonObject().getAsJsonArray("claims");
		assertEquals(3, claims.size(), "one claim should have been appended");
		JsonArray path = claims.get(2).getAsJsonObject().getAsJsonArray("path");
		assertEquals(1, path.size());
		assertEquals(AddNonExistentClaimToDcqlQuery.NON_EXISTENT_CLAIM, OIDFJSON.getString(path.get(0)));
		assertEquals("given_name", OIDFJSON.getString(claims.get(0).getAsJsonObject().getAsJsonArray("path").get(0)),
			"existing claims must be left untouched");
	}

	@Test
	public void mdocQuery_usesExistingNamespace() {
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": { "doctype_value": "org.iso.18013.5.1.mDL" },
			      "claims": [ { "path": [ "org.iso.18013.5.1", "family_name" ] } ]
			    }
			  ]
			}
			""");

		cond.execute(env);

		JsonArray claims = env.getObject("dcql_query").getAsJsonArray("credentials")
			.get(0).getAsJsonObject().getAsJsonArray("claims");
		assertEquals(2, claims.size());
		JsonArray path = claims.get(1).getAsJsonObject().getAsJsonArray("path");
		assertEquals(2, path.size(), "an mdoc claim path is [namespace, element identifier]");
		assertEquals("org.iso.18013.5.1", OIDFJSON.getString(path.get(0)),
			"the namespace must be one the credential really has, so only the element is missing");
		assertEquals(AddNonExistentClaimToDcqlQuery.NON_EXISTENT_CLAIM, OIDFJSON.getString(path.get(1)));
	}

	@Test
	public void claimSetsPresent_throws() {
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": { "vct_values": [ "urn:eudi:pid:1" ] },
			      "claims": [ { "id": "a", "path": [ "given_name" ] }, { "id": "b", "path": [ "family_name" ] } ],
			      "claim_sets": [ [ "a", "b" ], [ "a" ] ]
			    }
			  ]
			}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void noClaims_throws() {
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": { "vct_values": [ "urn:eudi:pid:1" ] }
			    }
			  ]
			}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void credentialSetsPresent_throws() {
		// with credential_sets the credential could be optional, and the wallet could then legitimately respond
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": { "vct_values": [ "urn:eudi:pid:1" ] },
			      "claims": [ { "path": [ "given_name" ] } ]
			    }
			  ],
			  "credential_sets": [ { "options": [ [ "my_credential" ] ], "required": false } ]
			}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void severalCredentials_throws() {
		// only the first credential would be made unsatisfiable; the wallet could still return the second
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [
			    {
			      "id": "first",
			      "format": "dc+sd-jwt",
			      "meta": { "vct_values": [ "urn:eudi:pid:1" ] },
			      "claims": [ { "path": [ "given_name" ] } ]
			    },
			    {
			      "id": "second",
			      "format": "dc+sd-jwt",
			      "meta": { "vct_values": [ "urn:example:other:1" ] },
			      "claims": [ { "path": [ "something" ] } ]
			    }
			  ]
			}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void queryIsNotOtherwiseModified() {
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
		assertEquals(1, dcqlQuery.getAsJsonArray("credentials").size(), "no credential query should be added");
		assertEquals(false, dcqlQuery.has("credential_sets"), "credential_sets must not be introduced");
	}
}
