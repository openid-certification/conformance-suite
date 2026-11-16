package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ExtractPDPSignedMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractPDPSignedMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractPDPSignedMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private static String base64Url(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	/** A compact JWS; the signature is not verified here, so any base64url value will do. */
	private static String jws(String headerJson, String claimsJson) {
		return base64Url(headerJson) + "." + base64Url(claimsJson) + "." + base64Url("signature");
	}

	private void putSignedMetadata(JsonElement signedMetadata) {
		JsonObject pdp = new JsonObject();
		pdp.addProperty("policy_decision_point", "https://pdp.example.com");
		if (signedMetadata != null) {
			pdp.add("signed_metadata", signedMetadata);
		}
		env.putObject("pdp", pdp);
	}

	@Test
	public void signedJwt_isExtracted() {
		putSignedMetadata(new JsonPrimitive(jws("{\"alg\":\"HS256\",\"typ\":\"JWT\"}",
			"{\"iss\":\"https://pdp.example.com\",\"access_evaluation_endpoint\":\"https://pdp.example.com/access/v1/evaluation\"}")));
		cond.execute(env);
		assertEquals("HS256", OIDFJSON.getString(env.getElementFromObject("pdp_signed_metadata", "header.alg")));
		assertEquals("https://pdp.example.com", OIDFJSON.getString(env.getElementFromObject("pdp_signed_metadata", "claims.iss")));
	}

	@Test
	public void nonNumericTimeClaim_failsToParse() {
		// The time claim checks rely on this: a non-numeric exp, iat or nbf never reaches them.
		for (String claim : new String[] {"exp", "iat", "nbf"}) {
			putSignedMetadata(new JsonPrimitive(jws("{\"alg\":\"HS256\"}",
				"{\"iss\":\"https://pdp.example.com\",\"" + claim + "\":\"yesterday\"}")));
			ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
			assertTrue(e.getMessage().contains("Couldn't parse jwt from signed_metadata"), e.getMessage());
		}
	}

	@Test
	public void nonStringIss_failsToParse() {
		putSignedMetadata(new JsonPrimitive(jws("{\"alg\":\"HS256\"}", "{\"iss\":5}")));
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Couldn't parse jwt from signed_metadata"), e.getMessage());
	}

	@Test
	public void nullClaim_fails() {
		// The parser keeps a JSON null, so the condition must reject it for the claim checks.
		for (String claim : new String[] {"iss", "exp", "iat", "nbf"}) {
			String iss = "iss".equals(claim) ? "" : "\"iss\":\"https://pdp.example.com\",";
			putSignedMetadata(new JsonPrimitive(jws("{\"alg\":\"HS256\"}", "{" + iss + "\"" + claim + "\":null}")));
			ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
			assertTrue(e.getMessage().contains("`" + claim + "` claim of the discovery metadata `signed_metadata` JWT is null"),
				e.getMessage());
		}
	}

	@Test
	public void missingSignedMetadata_fails() {
		putSignedMetadata(null);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain `signed_metadata`"), e.getMessage());
	}

	@Test
	public void nullSignedMetadata_fails() {
		putSignedMetadata(JsonNull.INSTANCE);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain `signed_metadata`"), e.getMessage());
	}

	@Test
	public void numberSignedMetadata_fails() {
		putSignedMetadata(new JsonPrimitive(42));
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("must be a JWT string"), e.getMessage());
	}

	@Test
	public void objectSignedMetadata_fails() {
		putSignedMetadata(JsonParser.parseString("{\"iss\":\"https://pdp.example.com\"}"));
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("must be a JWT string"), e.getMessage());
	}

	@Test
	public void malformedJwt_fails() {
		putSignedMetadata(new JsonPrimitive("not-a-jwt"));
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Couldn't parse jwt from signed_metadata"), e.getMessage());
	}
}
