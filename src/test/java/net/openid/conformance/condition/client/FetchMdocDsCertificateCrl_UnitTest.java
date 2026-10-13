package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocIacaCrl;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.documenttype.knowntypes.DrivingLicense;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class FetchMdocDsCertificateCrl_UnitTest {

	private static final String HTTP_URI = "http://issuer.example/ca.crl";
	private static final String HTTPS_URI = "https://issuer.example/ca.crl";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private TestableFetchMdocDsCertificateCrl cond;
	private byte[] crl;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new TestableFetchMdocDsCertificateCrl();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		crl = MdocIacaCrl.generate();
		// a document signer certificate naming two URIs for the one CRL
		MdocCredentialTestUtil.putCredential(env,
			MdocDsCertificateTestFixtures.credentialWithDsCertCrlDistributionPoint(HTTP_URI, HTTPS_URI));
	}

	@Test
	public void storesTheCrlNamedByTheSuitesOwnDocumentSigner() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));
		cond.respond(TestKeysAndCerts.getCrlDistributionPointUri(), ok(crl));

		cond.execute(env);

		assertEquals(TestKeysAndCerts.getCrlDistributionPointUri(),
			env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
		assertArrayEquals(crl, Base64.getDecoder().decode(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_CRL)));
	}

	@Test
	public void storesTheCrlFromTheFirstUriThatAnswers() {
		cond.respond(HTTP_URI, ok(crl));

		cond.execute(env);

		assertArrayEquals(crl, Base64.getDecoder().decode(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_CRL)));
		assertEquals(HTTP_URI, env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
		assertTrue(env.containsObject(AbstractMdocDsCertificateCrlCondition.ENV_RESPONSE));
		assertEquals(200, env.getInteger(AbstractMdocDsCertificateCrlCondition.ENV_RESPONSE, "status").intValue());
	}

	@Test
	public void fallsBackToTheNextUriWhenTheFirstIsNot2xx() {
		cond.respond(HTTP_URI, ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]));
		cond.respond(HTTPS_URI, ok(crl));

		cond.execute(env);

		assertEquals(HTTPS_URI, env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
		assertArrayEquals(crl, Base64.getDecoder().decode(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_CRL)));
	}

	@Test
	public void fallsBackToTheNextUriWhenTheFirstCannotBeReached() {
		// no response registered for HTTP_URI: the GET throws
		cond.respond(HTTPS_URI, ok(crl));

		cond.execute(env);

		assertEquals(HTTPS_URI, env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
	}

	@Test
	public void treatsAnEmptyBodyAsAFailedUri() {
		cond.respond(HTTP_URI, ok(new byte[0]));
		cond.respond(HTTPS_URI, ok(crl));

		cond.execute(env);

		assertEquals(HTTPS_URI, env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
	}

	@Test
	public void failsWhenNoUriYieldsACrl() {
		cond.respond(HTTP_URI, ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(e.getMessage().contains("CRL"), e.getMessage());
		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_CRL));
		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
	}

	@Test
	public void failsAndClearsEarlierStateWhenTheCertificateHasNoDistributionPoint() throws Exception {
		env.putString(AbstractMdocDsCertificateCrlCondition.ENV_CRL, "stale");
		env.putString(AbstractMdocDsCertificateCrlCondition.ENV_URI, "http://stale.example/x.crl");
		env.putString(AbstractMdocDsCertificateCrlCondition.ENV_AUTHENTICATED, "true");
		MdocCredentialTestUtil.putCredential(env, MdocDsCertificateTestFixtures.credentialWithCaProfileDsCert());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(e.getMessage().contains("no CRL distribution point"), e.getMessage());
		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_CRL));
		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_AUTHENTICATED));
		assertFalse(env.containsObject(AbstractMdocDsCertificateCrlCondition.ENV_RESPONSE));
	}

	@Test
	public void checksEachDocumentSignerCertificateOnce() {
		cond.respond(HTTP_URI, ok(crl));
		cond.execute(env);
		cond.forget(HTTP_URI);

		// same certificate, second credential: no fetch (it would throw), nothing stored for the checks
		cond.execute(env);

		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_CRL));
		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
		assertEquals(HTTP_URI, env.getString(AbstractMdocDsCertificateCrlCondition.ENV_CHECKED,
			fingerprintOfDsCert() + ".uri"));
	}

	@Test
	public void reportsAnUnreachableCrlOncePerCertificate() {
		assertThrows(ConditionError.class, () -> cond.execute(env));

		cond.execute(env);

		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_CRL));
	}

	@Test
	public void fetchesAgainForADifferentDocumentSignerCertificate() throws Exception {
		cond.respond(HTTP_URI, ok(crl));
		cond.execute(env);
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));
		cond.respond(TestKeysAndCerts.getCrlDistributionPointUri(), ok(crl));

		cond.execute(env);

		assertEquals(TestKeysAndCerts.getCrlDistributionPointUri(),
			env.getString(AbstractMdocDsCertificateCrlCondition.ENV_URI));
	}

	private String fingerprintOfDsCert() {
		JsonObject checked = env.getObject(AbstractMdocDsCertificateCrlCondition.ENV_CHECKED);
		assertEquals(1, checked.size());
		return checked.keySet().iterator().next();
	}

	private static ResponseEntity<byte[]> ok(byte[] body) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, MdocIacaCrl.CONTENT_TYPE);
		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}

	private static class TestableFetchMdocDsCertificateCrl extends FetchMdocDsCertificateCrl {
		private final Map<String, ResponseEntity<byte[]>> responses = new HashMap<>();

		void respond(String uri, ResponseEntity<byte[]> response) {
			responses.put(uri, response);
		}

		void forget(String uri) {
			responses.remove(uri);
		}

		@Override
		protected ResponseEntity<byte[]> getCrl(Environment env, String uri) {
			ResponseEntity<byte[]> response = responses.get(uri);
			if (response == null) {
				throw new IllegalStateException("connection refused: " + uri);
			}
			return response;
		}
	}
}
