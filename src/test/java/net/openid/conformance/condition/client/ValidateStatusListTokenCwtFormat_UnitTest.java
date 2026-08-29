package net.openid.conformance.condition.client;

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
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.Tagged;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateStatusListTokenCwtFormat_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateStatusListTokenCwtFormat cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateStatusListTokenCwtFormat();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_URI,
			StatusListCwtTestFixtures.DEFAULT_URI);
	}

	@Test
	public void testEvaluate_acceptsWellFormedToken() throws Exception {
		putToken(StatusListCwtTestFixtures.validStatusListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsDisallowedSignatureAlgorithm() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithDisallowedAlgorithm());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("-257"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsX5chainInUnprotectedHeader() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithX5chainInUnprotectedHeader());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("x5chain"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsUntaggedCoseSign1() throws Exception {
		byte[] tagged = StatusListCwtTestFixtures.validStatusListToken();
		byte[] untagged = Cbor.INSTANCE.encode(
			((Tagged) Cbor.INSTANCE.decode(tagged)).getTaggedItem());
		putToken(untagged);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("COSE_Sign1 tag"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsSubjectThatDoesNotMatchTheStatusListUri() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListToken("https://elsewhere.example.com/statuslists/9",
			StatusListCwtTestFixtures.CertProfile.CONFORMANT));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("sub claim"), error.getMessage());
	}

	private void putToken(byte[] token) {
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN,
			StatusListCwtTestFixtures.encode(token));
	}

	@org.junit.jupiter.api.Test
	public void testEvaluate_acceptsTextAggregationUri() throws Exception {
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN,
			StatusListCwtTestFixtures.encode(StatusListCwtTestFixtures.statusListTokenWithAggregationUri(
				new org.multipaz.cbor.Tstr("https://issuer.example.com/statuslists"))));

		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> cond.execute(env));
	}

	@org.junit.jupiter.api.Test
	public void testEvaluate_rejectsNonTextAggregationUri() throws Exception {
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN,
			StatusListCwtTestFixtures.encode(StatusListCwtTestFixtures.statusListTokenWithAggregationUri(
				org.multipaz.cbor.DataItemExtensionsKt.toDataItem(42))));

		net.openid.conformance.condition.ConditionError e = org.junit.jupiter.api.Assertions.assertThrows(
			net.openid.conformance.condition.ConditionError.class, () -> cond.execute(env));
		org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("aggregation_uri"), e.getMessage());
	}
}
