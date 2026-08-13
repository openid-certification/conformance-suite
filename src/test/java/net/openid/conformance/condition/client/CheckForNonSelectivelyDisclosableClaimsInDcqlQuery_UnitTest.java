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
public class CheckForNonSelectivelyDisclosableClaimsInDcqlQuery_UnitTest extends AbstractVciUnitTest {

	private CheckForNonSelectivelyDisclosableClaimsInDcqlQuery cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForNonSelectivelyDisclosableClaimsInDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_selectivelyDisclosableClaimPasses() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "path": [ "given_name" ] }
			      ]
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_statusClaimFails() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "path": [ "given_name" ] },
			        { "path": [ "status" ] }
			      ]
			    }
			  ]
			}
			""");
		JsonArray offenders = assertFailsAndReturnOffenders();
		assertEquals(1, offenders.size());
		JsonObject offender = offenders.get(0).getAsJsonObject();
		assertEquals("credential_1", OIDFJSON.getString(offender.get("credential_id")));
		assertEquals(JsonParser.parseString("[\"status\"]"), offender.get("path"));
	}

	@Test
	public void testEvaluate_vctIntegrityAndCnfSubPathFail() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "path": [ "vct#integrity" ] },
			        { "path": [ "cnf", "jwk" ] }
			      ]
			    }
			  ]
			}
			""");
		JsonArray offenders = assertFailsAndReturnOffenders();
		assertEquals(2, offenders.size());
		assertEquals(JsonParser.parseString("[\"vct#integrity\"]"),
			offenders.get(0).getAsJsonObject().get("path"));
		assertEquals(JsonParser.parseString("[\"cnf\",\"jwk\"]"),
			offenders.get(1).getAsJsonObject().get("path"));
	}

	@Test
	public void testEvaluate_subAndIatMayBeSelectivelyDisclosablePasses() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "path": [ "sub" ] },
			        { "path": [ "iat" ] }
			      ]
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_nestedClaimWithRegisteredNamePasses() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "claims": [
			        { "path": [ "address", "status" ] }
			      ]
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_mdocFormatIsSkipped() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "mso_mdoc",
			      "claims": [
			        { "path": [ "org.iso.18013.5.1", "status" ] }
			      ]
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noClaimsPasses() {
		putDcql("""
			{
			  "credentials": [
			    {
			      "id": "credential_1",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [ "https://example.com/identity_credential" ]
			      }
			    }
			  ]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	private JsonArray assertFailsAndReturnOffenders() {
		return (JsonArray) assertValidationError(cond, env, eventLog).get("non_selectively_disclosable_claims_requested");
	}

	private void putDcql(String json) {
		env.putObject("dcql_query", JsonParser.parseString(json).getAsJsonObject());
	}
}
