package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureEntityStatementJwksHasMinimumKeyLength_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureEntityStatementJwksHasMinimumKeyLength condition;

	// Public-only keys (no "d" parameter) — entity statements contain only public keys
	private final String rsa1024 = "{" +
		"\"kty\":\"RSA\"," +
		"\"e\":\"AQAB\"," +
		"\"use\":\"sig\"," +
		"\"kid\":\"1\"," +
		"\"n\":\"mw_sqPCmmiuEdRx4ir-Ov8K0UiwN60vElWUFHAHasjH7-wnxQDUvDjzsXpb_eRRxl9tdSFrYsP_F10e1nWJS_E9MxAiRxJEC3BKD90TRyagfaJPb6RHuAhB1jDS3ASHZlgt3mF5rhWHdmX5jswC2i2vWdPGTlTE-eCPeScfvIck\"" +
		"}";

	private final String rsa2048 = "{" +
		"\"kty\":\"RSA\"," +
		"\"e\":\"AQAB\"," +
		"\"use\":\"sig\"," +
		"\"kid\":\"1\"," +
		"\"n\":\"z7bQvDZMscdc-7C0Q87tXNKrfgYxjqsv7OleF1OmOi6A5U8ldJL-zK4-cnGa_YPlj-3HXryrTXbM_GodVLUApLHMq-58Q6a_76b3w6ybOifz8B1C4tFkj7O-KH2-qpnK73kXACB-q0Hi0-ozhRACOhMS0zrVlRgz3Gxwk-nCixtNzfvTsjXI9SY0e43sY_YnzpF_PqIcVGbMJhQi2Y2_5WDCq9nr7PnSj-Ep3G7JfjC5rnBuBJJWaMaupNbDGctO2YEErL8z1IAuyYSr1s05LaeBWHv0SH2bAUEjntnOyPAlGTqXmsyKPCCalRbk4HhXU3jkjellWwX5jOgtx2FU8Q\"" +
		"}";

	private final String ecP256 = "{" +
		"\"kty\":\"EC\"," +
		"\"use\":\"sig\"," +
		"\"crv\":\"P-256\"," +
		"\"kid\":\"2\"," +
		"\"x\":\"RsJ58leViXVAIvcR0jx7LfnALhm_0qcns3h4v6b8Pdk\"," +
		"\"y\":\"7Y0pNoArqzvFS_Li45WK3MfUf_YJaxWVVCbfEHPtdo0\"" +
		"}";

	private final String okpEd25519 = "{" +
		"\"kty\":\"OKP\"," +
		"\"use\":\"sig\"," +
		"\"crv\":\"Ed25519\"," +
		"\"kid\":\"3\"," +
		"\"x\":\"11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo\"" +
		"}";

	@BeforeEach
	public void setUp() {
		condition = new EnsureEntityStatementJwksHasMinimumKeyLength();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private JsonObject buildJwks(String... keyStrings) {
		JsonObject jwks = new JsonObject();
		JsonArray keys = new JsonArray();
		for (String keyString : keyStrings) {
			keys.add(JsonParser.parseString(keyString));
		}
		jwks.add("keys", keys);
		return jwks;
	}

	@Test
	public void acceptsEcP256AndRsa2048() {
		env.putObject("ec_jwks", buildJwks(ecP256, rsa2048));
		condition.execute(env);
	}

	@Test
	public void rejectsRsa1024() {
		env.putObject("ec_jwks", buildJwks(ecP256, rsa1024));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void acceptsOkpKeyWithoutLengthCheck() {
		// OKP keys are not subject to minimum length enforcement
		env.putObject("ec_jwks", buildJwks(okpEd25519));
		condition.execute(env);
	}
}
