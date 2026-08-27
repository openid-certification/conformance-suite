package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrValidateRequestObjectIdTokenACRClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrValidateRequestObjectIdTokenACRClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrValidateRequestObjectIdTokenACRClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addRequestObjectWithAcrClaim(String acrClaimJson) {
		env.putObject("authorization_request_object",
			JsonParser.parseString("{\"claims\":{\"claims\":{\"id_token\":{\"acr\":" + acrClaimJson + "}}}}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_cdr2Value() {
		addRequestObjectWithAcrClaim("{\"essential\":true,\"value\":\"urn:cds.au:cdr:2\"}");
		cond.execute(env);
		assertEquals("[\"urn:cds.au:cdr:2\"]", env.getString("requested_id_token_acr_values"));
	}

	@Test
	public void testEvaluate_cdrValuesArray() {
		addRequestObjectWithAcrClaim("{\"essential\":true,\"values\":[\"urn:cds.au:cdr:2\",\"urn:cds.au:cdr:3\"]}");
		cond.execute(env);
		assertEquals("[\"urn:cds.au:cdr:2\",\"urn:cds.au:cdr:3\"]", env.getString("requested_id_token_acr_values"));
	}

	@Test
	public void testEvaluate_psd2ValueNotAccepted() {
		assertThrows(ConditionError.class, () -> {
			addRequestObjectWithAcrClaim("{\"essential\":true,\"value\":\"urn:openbanking:psd2:sca\"}");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_acrNotRequested() {
		env.putObject("authorization_request_object",
			JsonParser.parseString("{\"claims\":{\"claims\":{\"id_token\":{}}}}").getAsJsonObject());
		cond.execute(env);
		assertNull(env.getString("requested_id_token_acr_values"));
	}

}
