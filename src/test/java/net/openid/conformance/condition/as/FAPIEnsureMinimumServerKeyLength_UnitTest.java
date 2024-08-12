package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FAPIEnsureMinimumServerKeyLength_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIEnsureMinimumServerKeyLength cond;

	private String rsa1024 = "{" +
		"\"kty\":\"RSA\"," +
		"\"d\":\"Hah7FIkK5JwENr4eVDN1YzojcUAZ3XJdhFGRG51DLigfURHCwgwqFugSMblh3c_KNkhv3CH8JkaVmaqOky62PlwyasvkFMHyMt_LNzb-9u1nB7gf7nPFoNw_QjSCFHoKdAZzcUAJR1khp6E2GtybFf5lCGPakF4IRjitEJV2w3E\"," +
		"\"e\":\"AQAB\"," +
		"\"use\":\"sig\"," +
		"\"kid\":\"1\"," +
		"\"n\":\"mw_sqPCmmiuEdRx4ir-Ov8K0UiwN60vElWUFHAHasjH7-wnxQDUvDjzsXpb_eRRxl9tdSFrYsP_F10e1nWJS_E9MxAiRxJEC3BKD90TRyagfaJPb6RHuAhB1jDS3ASHZlgt3mF5rhWHdmX5jswC2i2vWdPGTlTE-eCPeScfvIck\"" +
		"}";

	private String rsa2048 = "{" +
		"\"kty\":\"RSA\"," +
		"\"d\":\"Utec5JPbfsP-h7twCeNLs8up7fdWaIpJ8PkaN796-pV1fv9T9uxxhvyvl0FMCXRDaGXzMAubXqfVFY7U5-XQvd7TvHa_RNQKlPBIvNfoiQdKOSfSd1f__Xg4jTmpg60WzO_Ehp5vJqp-ZvpvVdCOss7MoUZDzNT8ShtNwxBmMMgYHsalbAUtYjbFvC6PfdrLB2scHEevEPK7kXT291g_T74hjd3IKOOSEzssldzkhLRS__9hcwhFCusZFHsuh-1Db5ArYZO3jAFfYZ8Uh_oSAMTy4d99MIJ9nQu4MrRoOJtdsnouXI7m8QzcTIfCix__RT13POtyoLdD7YTFqjLNKQ\","
		+
		"\"e\":\"AQAB\"," +
		"\"use\":\"sig\"," +
		"\"kid\":\"1\"," +
		"\"n\":\"z7bQvDZMscdc-7C0Q87tXNKrfgYxjqsv7OleF1OmOi6A5U8ldJL-zK4-cnGa_YPlj-3HXryrTXbM_GodVLUApLHMq-58Q6a_76b3w6ybOifz8B1C4tFkj7O-KH2-qpnK73kXACB-q0Hi0-ozhRACOhMS0zrVlRgz3Gxwk-nCixtNzfvTsjXI9SY0e43sY_YnzpF_PqIcVGbMJhQi2Y2_5WDCq9nr7PnSj-Ep3G7JfjC5rnBuBJJWaMaupNbDGctO2YEErL8z1IAuyYSr1s05LaeBWHv0SH2bAUEjntnOyPAlGTqXmsyKPCCalRbk4HhXU3jkjellWwX5jOgtx2FU8Q\""
		+
		"}";

	private String ecP256 = "{" +
		"\"kty\":\"EC\"," +
		"\"d\":\"fRqbe7uuYBrzCLqC1Z2rxPIskrf3PrpbKAS5RdoRh_s\"," +
		"\"use\":\"sig\"," +
		"\"crv\":\"P-256\"," +
		"\"kid\":\"1\"," +
		"\"x\":\"RsJ58leViXVAIvcR0jx7LfnALhm_0qcns3h4v6b8Pdk\"," +
		"\"y\":\"7Y0pNoArqzvFS_Li45WK3MfUf_YJaxWVVCbfEHPtdo0\"" +
		"}";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPIEnsureMinimumServerKeyLength();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
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
	public void testEvaluate_good() {

		env.putObject("server_jwks", buildJwks(ecP256, rsa2048));
		cond.execute(env);

		verify(env, atLeastOnce()).getObject("server_jwks");
	}

	@Test
	public void testEvaluate_jwksMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_shortRSA() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("server_jwks", buildJwks(ecP256, rsa1024));
			cond.execute(env);

		});

	}

}
