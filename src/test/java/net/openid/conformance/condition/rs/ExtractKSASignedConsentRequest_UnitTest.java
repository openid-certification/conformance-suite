package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExtractKSASignedConsentRequest_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractKSASignedConsentRequest cond;
	private Environment env;
	private RSAKey key;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractKSASignedConsentRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		key = new RSAKeyGenerator(2048).keyID("k1").generate();
		env = new Environment();
		setBody(signedJwt(new JWTClaimsSet.Builder()
			.issuer("client-1234")
			.claim("message", Map.of("Data", Map.of("Permissions", java.util.List.of("ReadAccountsBasic"))))
			.build()));
	}

	private String signedJwt(JWTClaimsSet claims) throws Exception {
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.PS256).keyID("k1").build(), claims);
		jwt.sign(new RSASSASigner(key));
		return jwt.serialize();
	}

	private void setBody(String body) {
		JsonObject incoming = new JsonObject();
		if (body != null) {
			incoming.addProperty("body", body);
		}
		env.putObject("incoming_request", incoming);
	}

	@Test
	public void testExtractsClaimsAndMessage() {
		env = cond.evaluate(env);

		JsonObject parsed = env.getObject("parsed_client_request_jwt");
		assertThat(parsed, notNullValue());
		assertThat(parsed.get("value"), notNullValue());

		JsonObject message = env.getObject("new_consent_request");
		assertThat(message.getAsJsonObject("Data").has("Permissions"), is(true));
	}

	@Test
	public void testMissingBodyThrows() {
		setBody(null);

		ConditionError err = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertThat(err.getMessage(), containsString("body is empty"));
	}

	@Test
	public void testEmptyBodyThrows() {
		setBody("");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertThat(err.getMessage(), containsString("body is empty"));
	}

	@Test
	public void testUnparseableJwtThrows() {
		setBody("this-is-not-a-jwt");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertThat(err.getMessage(), containsString("Could not parse"));
	}

	@Test
	public void testMissingMessageClaimThrows() throws Exception {
		setBody(signedJwt(new JWTClaimsSet.Builder().issuer("client-1234").build()));

		ConditionError err = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertThat(err.getMessage(), containsString("'message' object"));
	}

	@Test
	public void testNonObjectMessageClaimThrows() throws Exception {
		setBody(signedJwt(new JWTClaimsSet.Builder()
			.issuer("client-1234")
			.claim("message", "not-an-object")
			.build()));

		ConditionError err = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertThat(err.getMessage(), containsString("'message' object"));
	}
}
