package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class CheckForUnreferencedClaimsInDcqlQuery_UnitTest extends AbstractVciUnitTest {

	private CheckForUnreferencedClaimsInDcqlQuery cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnreferencedClaimsInDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_allClaimsReferencedPasses() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "id": "a", "path": [ "given_name" ] },
			        { "id": "b", "path": [ "family_name" ] }
			      ],
			      "claim_sets": [
			        [ "a", "b" ]
			      ]
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unreferencedClaimFails() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "id": "a", "path": [ "given_name" ] },
			        { "id": "b", "path": [ "family_name" ] },
			        { "id": "c", "path": [ "birthdate" ] }
			      ],
			      "claim_sets": [
			        [ "a" ],
			        [ "a", "b" ]
			      ]
			    }
			  ]
			}
			""");
		JsonArray offenders = assertFailsAndReturnOffenders();
		assertEquals(1, offenders.size());
		JsonObject offender = offenders.get(0).getAsJsonObject();
		assertEquals("credential_1", OIDFJSON.getString(offender.get("credential_id")));
		assertEquals("c", OIDFJSON.getString(offender.get("claim_id")));
		assertEquals(JsonParser.parseString("[\"birthdate\"]"), offender.get("path"));
	}

	@Test
	public void testEvaluate_claimReferencedOnlyByLaterOptionPasses() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "id": "a", "path": [ "given_name" ] },
			        { "id": "b", "path": [ "family_name" ] }
			      ],
			      "claim_sets": [
			        [ "a" ],
			        [ "b" ]
			      ]
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noClaimSetsPasses() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "path": [ "given_name" ] },
			        { "path": [ "family_name" ] }
			      ]
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_claimWithoutIdIsSkipped() {
		// A claims entry without an id when claim_sets is present is a structural error that
		// ValidateDCQLQuery reports; this condition must not also trip over it.
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "id": "a", "path": [ "given_name" ] },
			        { "path": [ "family_name" ] }
			      ],
			      "claim_sets": [
			        [ "a" ]
			      ]
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_mdocCredentialAlsoChecked() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "mso_mdoc",
			      "claims": [
			        { "id": "a", "path": [ "org.iso.18013.5.1", "given_name" ] },
			        { "id": "b", "path": [ "org.iso.18013.5.1", "family_name" ] }
			      ],
			      "claim_sets": [
			        [ "a" ]
			      ]
			    }
			  ]
			}
			""");
		JsonArray offenders = assertFailsAndReturnOffenders();
		assertEquals(1, offenders.size());
		assertEquals("b", OIDFJSON.getString(offenders.get(0).getAsJsonObject().get("claim_id")));
	}

	@Test
	public void testEvaluate_multipleCredentialsOnlyOffendingOneReported() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "id": "a", "path": [ "given_name" ] }
			      ],
			      "claim_sets": [
			        [ "a" ]
			      ]
			    },
			    {
			      "id": "credential_2",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "id": "a", "path": [ "given_name" ] },
			        { "id": "b", "path": [ "family_name" ] }
			      ],
			      "claim_sets": [
			        [ "a" ]
			      ]
			    }
			  ]
			}
			""");
		JsonArray offenders = assertFailsAndReturnOffenders();
		assertEquals(1, offenders.size());
		JsonObject offender = offenders.get(0).getAsJsonObject();
		assertEquals("credential_2", OIDFJSON.getString(offender.get("credential_id")));
		assertEquals("b", OIDFJSON.getString(offender.get("claim_id")));
	}

	private JsonArray assertFailsAndReturnOffenders() {
		return (JsonArray) assertValidationError(cond, env, eventLog).get("claims_not_referenced_by_any_claim_set");
	}

	private void putDcql(String json) {
		env.putObject("dcql_query", JsonParser.parseString(json).getAsJsonObject());
	}
}
