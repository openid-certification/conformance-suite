package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvent;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateTamperedStreamSET.TamperMode;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFInMemoryEventStore;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFGenerateTamperedStreamSET_UnitTest {

	private static final String STREAM_ID = "stream-1";
	private static final String ISSUER = "https://transmitter.example.com";
	private static final String AUDIENCE = "https://receiver.example.com";
	private static final String SERVER_KID = "server-signing-key";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private RSAKey serverKey;

	private OIDSSFInMemoryEventStore eventStore;

	@BeforeEach
	public void setUp() throws Exception {
		serverKey = new RSAKeyGenerator(2048).keyID(SERVER_KID).keyUse(KeyUse.SIGNATURE).generate();
		JsonObject jwks = JsonParser.parseString(new JWKSet(serverKey).toString(false)).getAsJsonObject();
		env.putObject("server_jwks", jwks);

		JsonObject streamConfig = new JsonObject();
		JsonObject streams = new JsonObject();
		streams.add(STREAM_ID, streamConfig);
		JsonObject ssf = new JsonObject();
		ssf.addProperty("issuer", ISSUER);
		ssf.add("streams", streams);
		env.putObject("ssf", ssf);

		JsonObject config = JsonParser.parseString("{\"ssf\":{\"stream\":{\"audience\":\"" + AUDIENCE + "\"}}}").getAsJsonObject();
		env.putObject("config", config);

		eventStore = new OIDSSFInMemoryEventStore();
	}

	private OIDSSFGenerateTamperedStreamSET createCondition(TamperMode tamperMode, List<OIDSSFSecurityEvent> generated) {
		JsonObject subject = new JsonObject();
		subject.addProperty("format", "email");
		subject.addProperty("email", "user@example.com");
		SsfEvent ssfEvent = new SsfEvent(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, Map.of("event_timestamp", 1L), Set.of());
		OIDSSFGenerateTamperedStreamSET condition = new OIDSSFGenerateTamperedStreamSET(eventStore, STREAM_ID, subject, ssfEvent,
			tamperMode, false, generated::add);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private SignedJWT generate(OIDSSFGenerateTamperedStreamSET condition, List<OIDSSFSecurityEvent> generated) throws Exception {
		int before = generated.size();
		condition.execute(env);
		assertEquals(before + 1, generated.size());
		return SignedJWT.parse(generated.get(before).securityEventToken());
	}

	@Test
	public void unknownKidSignsWithAKeyThatIsNotInTheServerJwks() throws Exception {
		List<OIDSSFSecurityEvent> generated = new ArrayList<>();
		SignedJWT set = generate(createCondition(TamperMode.UNKNOWN_KID, generated), generated);

		String kid = set.getHeader().getKeyID();
		assertTrue(kid.startsWith("unknown-"), "kid should mark the key as unknown, was: " + kid);
		assertNotEquals(SERVER_KID, kid);
		JWKSet serverJwks = JWKSet.parse(env.getObject("server_jwks").toString());
		assertNull(serverJwks.getKeyByKeyId(kid), "the kid must not resolve against server_jwks");

		assertFalse(set.verify(new RSASSAVerifier(serverKey.toRSAPublicKey())), "the signature must not verify against the transmitter's published key");

		// the claims are otherwise valid: only the signing key is wrong
		assertEquals(ISSUER, set.getJWTClaimsSet().getIssuer());
		assertEquals(List.of(AUDIENCE), set.getJWTClaimsSet().getAudience());
		assertEquals("secevent+jwt", set.getHeader().getType().getType());
	}

	@Test
	public void unknownKidReusesTheSameKeyWithinOneConditionInstance() throws Exception {
		List<OIDSSFSecurityEvent> generated = new ArrayList<>();
		OIDSSFGenerateTamperedStreamSET condition = createCondition(TamperMode.UNKNOWN_KID, generated);
		SignedJWT first = generate(condition, generated);
		SignedJWT second = generate(condition, generated);
		assertEquals(first.getHeader().getKeyID(), second.getHeader().getKeyID());

		List<OIDSSFSecurityEvent> otherGenerated = new ArrayList<>();
		SignedJWT other = generate(createCondition(TamperMode.UNKNOWN_KID, otherGenerated), otherGenerated);
		assertNotEquals(first.getHeader().getKeyID(), other.getHeader().getKeyID());
	}

	@Test
	public void otherTamperModesStillSignWithTheServerKey() throws Exception {
		List<OIDSSFSecurityEvent> generated = new ArrayList<>();
		SignedJWT set = generate(createCondition(TamperMode.WRONG_ISSUER, generated), generated);

		assertEquals(SERVER_KID, set.getHeader().getKeyID());
		assertTrue(set.verify(new RSASSAVerifier(serverKey.toRSAPublicKey())));
		assertNotEquals(ISSUER, set.getJWTClaimsSet().getIssuer());
	}

	@Test
	public void invalidSignatureKeepsTheServerKidButDoesNotVerify() throws Exception {
		List<OIDSSFSecurityEvent> generated = new ArrayList<>();
		SignedJWT set = generate(createCondition(TamperMode.INVALID_SIGNATURE, generated), generated);

		assertEquals(SERVER_KID, set.getHeader().getKeyID());
		assertFalse(set.verify(new RSASSAVerifier(serverKey.toRSAPublicKey())));
	}

	@Test
	public void tamperedSetIsOnlyEnqueuedWhenRequested() throws Exception {
		List<OIDSSFSecurityEvent> generated = new ArrayList<>();
		generate(createCondition(TamperMode.UNKNOWN_KID, generated), generated);
		assertTrue(eventStore.getQueuedEvents(STREAM_ID).isEmpty());
	}
}
