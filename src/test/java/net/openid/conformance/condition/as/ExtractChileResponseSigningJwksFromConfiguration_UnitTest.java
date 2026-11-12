package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExtractChileResponseSigningJwksFromConfiguration_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private ExtractChileResponseSigningJwksFromConfiguration cond;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		env.putObject("config", new JsonObject());
		cond = new ExtractChileResponseSigningJwksFromConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private void configure(List<JWK> keys) {
		configure(JsonParser.parseString(new JWKSet(keys).toString(false)).getAsJsonObject());
	}

	private void configure(JsonElement value) {
		JsonObject server = new JsonObject();
		server.add("response_signing_jwks", value);
		env.getObject("config").add("server", server);
	}

	private static JWK rsaKey(String kid) throws Exception {
		RSAKeyGenerator generator = new RSAKeyGenerator(2048);
		if (kid != null) {
			generator.keyID(kid);
		}
		return generator.generate();
	}

	@Test
	public void loadsOneRsaPrivateKeyWithKid() throws Exception {
		configure(List.of(rsaKey("bank-sig")));

		cond.execute(env);

		JsonObject privateJwks = env.getObject(ExtractChileResponseSigningJwksFromConfiguration.RESPONSE_SIGNING_JWKS);
		JsonObject publicJwks = env.getObject(ExtractChileResponseSigningJwksFromConfiguration.RESPONSE_SIGNING_PUBLIC_JWKS);
		assertThat(privateJwks.getAsJsonArray("keys").get(0).getAsJsonObject().has("d")).isTrue();
		JsonObject publicKey = publicJwks.getAsJsonArray("keys").get(0).getAsJsonObject();
		assertThat(publicKey.has("d")).isFalse();
		assertThat(OIDFJSON.getString(publicKey.get("kid"))).isEqualTo("bank-sig");
	}

	@Test
	public void missingFieldFails() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void valueThatIsNotAnObjectFails() {
		configure(new JsonPrimitive("not a jwks"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void unparseableJwksFails() {
		configure(JsonParser.parseString("{\"keys\":[{\"kty\":\"RSA\"}]}"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void publicOnlyKeyFails() throws Exception {
		configure(List.of(rsaKey("bank-sig").toPublicJWK()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void keyWithoutKidFails() throws Exception {
		configure(List.of(rsaKey(null)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void nonRsaKeyFails() throws Exception {
		configure(List.of(new ECKeyGenerator(Curve.P_256).keyID("ec").generate()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void twoKeysFail() throws Exception {
		configure(List.of(rsaKey("one"), rsaKey("two")));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
