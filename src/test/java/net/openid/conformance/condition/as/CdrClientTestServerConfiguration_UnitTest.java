package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrClientTestServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private <T extends net.openid.conformance.condition.AbstractCondition> T init(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		return cond;
	}

	@Test
	public void testAddClaimsSupported() {
		env.putObject("server", new JsonObject());
		init(new CdrAddClaimsSupportedToServerConfiguration()).execute(env);
		assertEquals("[\"sub\",\"acr\",\"sharing_duration\"]",
			env.getElementFromObject("server", "claims_supported").toString());
	}

	@Test
	public void testAddAcrValuesSupported() {
		env.putObject("server", new JsonObject());
		init(new CdrAddAcrValuesSupportedToServerConfiguration()).execute(env);
		assertEquals("[\"urn:cds.au:cdr:2\",\"urn:cds.au:cdr:3\"]",
			env.getElementFromObject("server", "acr_values_supported").toString());
	}

	@Test
	public void testAddAcrClaimToIdToken_cdr2() {
		env.putObject("id_token_claims", new JsonObject());
		env.putString("requested_id_token_acr_values", "[\"urn:cds.au:cdr:2\"]");
		init(new CdrAddACRClaimToIdTokenClaims()).execute(env);
		assertEquals("urn:cds.au:cdr:2",
			OIDFJSON.getString(env.getElementFromObject("id_token_claims", "acr")));
	}

	@Test
	public void testAddAcrClaimToIdToken_prefersCdr3() {
		env.putObject("id_token_claims", new JsonObject());
		env.putString("requested_id_token_acr_values", "[\"urn:cds.au:cdr:2\",\"urn:cds.au:cdr:3\"]");
		init(new CdrAddACRClaimToIdTokenClaims()).execute(env);
		assertEquals("urn:cds.au:cdr:3",
			OIDFJSON.getString(env.getElementFromObject("id_token_claims", "acr")));
	}

	@Test
	public void testAddAcrClaimToIdToken_unknownValue() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("id_token_claims", new JsonObject());
			env.putString("requested_id_token_acr_values", "[\"urn:openbanking:psd2:sca\"]");
			init(new CdrAddACRClaimToIdTokenClaims()).execute(env);
		});
	}

}
