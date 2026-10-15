package net.openid.conformance.condition.client;

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
import org.multipaz.crypto.X509CertJvmKt;
import org.multipaz.documenttype.knowntypes.DrivingLicense;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureMdocDsCertificateNotRevokedByCrl_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureMdocDsCertificateNotRevokedByCrl cond;
	private BigInteger dsSerial;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureMdocDsCertificateNotRevokedByCrl();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));
		dsSerial = X509CertJvmKt.getJavaX509Certificate(TestKeysAndCerts.getDocumentSignerCert()).getSerialNumber();
	}

	@Test
	public void passesForAnEmptyCrl() {
		MdocCrlTestFixtures.putCrl(env, MdocIacaCrl.generate());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenOnlyOtherSerialsAreListed() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().revoke(dsSerial.add(BigInteger.ONE)).build());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenTheDsCertificateSerialIsListed() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().revoke(BigInteger.TEN).revoke(dsSerial).build());
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("revoked"), e.getMessage());
	}
}
