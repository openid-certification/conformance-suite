package net.openid.conformance.condition.client;

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
public class ValidateSdJwtKbSdHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateSdJwtKbSdHash cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ValidateSdJwtKbSdHash();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// final example from https://www.ietf.org/archive/id/draft-ietf-oauth-selective-disclosure-jwt-14.html#appendix-A.3
		String sdJwtStr =
			"eyJhbGciOiAiRVMyNTYiLCAidHlwIjogImRjK3NkLWp3dCJ9.eyJfc2QiOiBbIjBIWm1" +
				"uU0lQejMzN2tTV2U3QzM0bC0tODhnekppLWVCSjJWel9ISndBVGciLCAiOVpicGxDN1R" +
				"kRVc3cWFsNkJCWmxNdHFKZG1lRU9pWGV2ZEpsb1hWSmRSUSIsICJJMDBmY0ZVb0RYQ3V" +
				"jcDV5eTJ1anFQc3NEVkdhV05pVWxpTnpfYXdEMGdjIiwgIklFQllTSkdOaFhJbHJRbzU" +
				"4eWtYbTJaeDN5bGw5WmxUdFRvUG8xN1FRaVkiLCAiTGFpNklVNmQ3R1FhZ1hSN0F2R1R" +
				"yblhnU2xkM3o4RUlnX2Z2M2ZPWjFXZyIsICJodkRYaHdtR2NKUXNCQ0EyT3RqdUxBY3d" +
				"BTXBEc2FVMG5rb3ZjS09xV05FIiwgImlrdXVyOFE0azhxM1ZjeUE3ZEMtbU5qWkJrUmV" +
				"EVFUtQ0c0bmlURTdPVFUiLCAicXZ6TkxqMnZoOW80U0VYT2ZNaVlEdXZUeWtkc1dDTmc" +
				"wd1RkbHIwQUVJTSIsICJ3elcxNWJoQ2t2a3N4VnZ1SjhSRjN4aThpNjRsbjFqb183NkJ" +
				"DMm9hMXVnIiwgInpPZUJYaHh2SVM0WnptUWNMbHhLdUVBT0dHQnlqT3FhMXoySW9WeF9" +
				"ZRFEiXSwgImlzcyI6ICJodHRwczovL2lzc3Vlci5leGFtcGxlLmNvbSIsICJpYXQiOiA" +
				"xNjgzMDAwMDAwLCAiZXhwIjogMTg4MzAwMDAwMCwgInZjdCI6ICJodHRwczovL2JtaS5" +
				"idW5kLmV4YW1wbGUvY3JlZGVudGlhbC9waWQvMS4wIiwgImFnZV9lcXVhbF9vcl9vdmV" +
				"yIjogeyJfc2QiOiBbIkZjOElfMDdMT2NnUHdyREpLUXlJR085N3dWc09wbE1Makh2UkM" +
				"0UjQtV2ciLCAiWEx0TGphZFVXYzl6Tl85aE1KUm9xeTQ2VXNDS2IxSXNoWnV1cVVGS1N" +
				"DQSIsICJhb0NDenNDN3A0cWhaSUFoX2lkUkNTQ2E2NDF1eWNuYzh6UGZOV3o4bngwIiw" +
				"gImYxLVAwQTJkS1dhdnYxdUZuTVgyQTctRVh4dmhveHY1YUhodUVJTi1XNjQiLCAiazV" +
				"oeTJyMDE4dnJzSmpvLVZqZDZnNnl0N0Fhb25Lb25uaXVKOXplbDNqbyIsICJxcDdaX0t" +
				"5MVlpcDBzWWdETzN6VnVnMk1GdVBOakh4a3NCRG5KWjRhSS1jIl19LCAiX3NkX2FsZyI" +
				"6ICJzaGEtMjU2IiwgImNuZiI6IHsiandrIjogeyJrdHkiOiAiRUMiLCAiY3J2IjogIlA" +
				"tMjU2IiwgIngiOiAiVENBRVIxOVp2dTNPSEY0ajRXNHZmU1ZvSElQMUlMaWxEbHM3dkN" +
				"lR2VtYyIsICJ5IjogIlp4amlXV2JaTVFHSFZXS1ZRNGhiU0lpcnNWZnVlY0NFNnQ0alQ" +
				"5RjJIWlEifX19.Zkigt10NnCN2ZKjHUZ9Jo-1cJ2ULBz4lNu4dv1ZTR_cFg2lT9-6zJX" +
				"I-LMtpnA5HuvrWXeyYJBxiqvoTw128ag~WyJuUHVvUW5rUkZxM0JJZUFtN0FuWEZBIiw" +
				"gIm5hdGlvbmFsaXRpZXMiLCBbIkRFIl1d~WyJNMEpiNTd0NDF1YnJrU3V5ckRUM3hBIi" +
				"wgIjE4IiwgdHJ1ZV0~eyJhbGciOiAiRVMyNTYiLCAidHlwIjogImtiK2p3dCJ9.eyJub" +
				"25jZSI6ICIxMjM0NTY3ODkwIiwgImF1ZCI6ICJodHRwczovL3ZlcmlmaWVyLmV4YW1wb" +
				"GUub3JnIiwgImlhdCI6IDE3MzE2OTYyOTksICJzZF9oYXNoIjogInRhcW9uTjJnWkhIe" +
				"lI4VWJIcUZmMG9GTjFrTm1PVzZBdlYzQUp4bjNXTncifQ.BraAy1HQ2rHF6WyG1gtnnf" +
				"tqJIVkNMTfrWXWsTqaZ7anoaHKPBcbBegET5c0IAHMjQOIkj7xFL4mWmf5gjQlww";

		env.putString("vp_token", sdJwtStr);

		env.putString("sdjwt", "binding.claims.sd_hash", "taqonN2gZHHzR8UbHqFf0oFN1kNmOW6AvV3AJxn3WNw");

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		cond.execute(env);

		verify(env, atLeastOnce()).getString("vp_token");
	}

	@Test
	public void testEvaluate_incorrectHash() {
		assertThrows(ConditionError.class, () -> {

			env.putString("sdjwt", "binding.claims.sd_hash", "taqonN3gZHHzR8UbHqFf0oFN1kNmOW6AvV3AJxn3WNw");

			cond.execute(env);

			verify(env, atLeastOnce()).getString("vp_token");

		});
	}

}
