package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(MockitoExtension.class)
public class ExtractKSASignedConsentRequest_UnitTest {

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractKSASignedConsentRequest cond;
	private Environment env;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractKSASignedConsentRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		RSAKey key = new RSAKeyGenerator(2048).keyID("k1").generate();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issuer("client-1234")
			.claim("message", Map.of("Data", Map.of("Permissions", java.util.List.of("ReadAccountsBasic"))))
			.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.PS256).keyID("k1").build(), claims);
		jwt.sign(new RSASSASigner(key));

		JsonObject incoming = new JsonObject();
		incoming.addProperty("body", jwt.serialize());
		env = new Environment();
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
}
