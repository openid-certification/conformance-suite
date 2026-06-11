package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborBuilder;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.MapBuilder;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;
import org.multipaz.documenttype.knowntypes.DrivingLicense;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocIssuerSignedItemDigests_UnitTest {

	private ValidateMdocIssuerSignedItemDigests cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocIssuerSignedItemDigests();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private static DataItem buildIssuerSignedItem(long digestId, DataItem random, String elementIdentifier, DataItem elementValue) {
		MapBuilder<CborBuilder> item = CborMap.Companion.builder();
		item.put("digestID", digestId);
		item.put("random", random);
		item.put("elementIdentifier", new Tstr(elementIdentifier));
		item.put("elementValue", elementValue);
		return new Tagged(Tagged.ENCODED_CBOR, new Bstr(Cbor.INSTANCE.encode(item.end().build())));
	}

	@Test
	public void testEvaluate_passesWhenAllDigestsMatch() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenElementValueTampered() throws Exception {
		byte[] tampered = MdocCredentialTestUtil.rebuildWithItems(
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE),
			(originalItems, newItems) -> {
				for (DataItem item : originalItems.getItems()) {
					DataItem inner = item.getAsTaggedEncodedCbor();
					if (inner.get("elementIdentifier").getAsTstr().equals("given_name")) {
						newItems.add(buildIssuerSignedItem(inner.get("digestID").getAsNumber(), inner.get("random"),
							"given_name", new Tstr("Tampered")));
					} else {
						newItems.add(item);
					}
				}
			});
		MdocCredentialTestUtil.putCredential(env, tampered);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("digest"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenItemHasNoDigestInMso() throws Exception {
		byte[] withBogusItem = MdocCredentialTestUtil.rebuildWithItems(
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE),
			(originalItems, newItems) -> {
				for (DataItem item : originalItems.getItems()) {
					newItems.add(item);
				}
				newItems.add(buildIssuerSignedItem(9999L, new Bstr(new byte[32]), "bogus_element", new Tstr("bogus")));
			});
		MdocCredentialTestUtil.putCredential(env, withBogusItem);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Mobile Security Object"), e.getMessage());
	}
}
