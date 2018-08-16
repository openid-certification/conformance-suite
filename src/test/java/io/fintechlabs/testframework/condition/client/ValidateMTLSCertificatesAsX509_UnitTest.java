/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidateMTLSCertificatesAsX509_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	//private ExtractMTLSCertificatesFromConfiguration cond;
	private ValidateMTLSCertificatesAsX509 cond;

	private String cert = "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";

	private String key = "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";

	// OB issuer chain
	private String caString = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tDQpNSUlGYVR" +
		"DQ0ExR2dBd0lCQWdJRVdXZFUwekFOQmdrcWhraUc5dzBCQVFzRkFEQmpNUXN3Q1FZ" +
		"RFZRUUdFd0pIDQpRakVkTUJzR0ExVUVDaE1VVDNCbGJpQkNZVzVyYVc1bklFeHBiV" +
		"2wwWldReEVUQVBCZ05WQkFzVENGUmxjM1FnDQpVRXRKTVNJd0lBWURWUVFERXhsUG" +
		"NHVnVJRUpoYm10cGJtY2dWR1Z6ZENCU2IyOTBJRU5CTUI0WERURTNNRGN4DQpOakl" +
		"3TlRZek9Wb1hEVEkzTURjeE5qSXhNall6T1Zvd1pqRUxNQWtHQTFVRUJoTUNSMEl4" +
		"SFRBYkJnTlZCQW9UDQpGRTl3Wlc0Z1FtRnVhMmx1WnlCTWFXMXBkR1ZrTVJFd0R3W" +
		"URWUVFMRXdoVVpYTjBJRkJMU1RFbE1DTUdBMVVFDQpBeE1jVDNCbGJpQkNZVzVyYV" +
		"c1bklGUmxjM1FnU1hOemRXbHVaeUJEUVRDQ0FTSXdEUVlKS29aSWh2Y05BUUVCDQp" +
		"CUUFEZ2dFUEFEQ0NBUW9DZ2dFQkFLUnFua05RU2VBRHhJVjU4U05DLzNCOUEzR3Rn" +
		"Z09vTkllOHRiZ2dWSjlTDQpwR3Q0WVI3dG1uK2lqL3U2dEtWYXVjNFphMmQ4UkR3O" +
		"WFvaDY2cjBlbUVNbGNTeEZNN1BsQXNRZVFYTGUyL1E5DQpGQmxSWm9qQWN1NHB5bn" +
		"o4cERqRWJjSzk0ZGNVNjFjaGN0VmYwNGlkc0J0UWxSSnFoc1dkT3pIb0J1ZVZoOUp" +
		"RDQp1RG9tNVcwKzQrdVNDM1lxdXVpa1F4RlJPWW1PQXozcVpSUVhHNm5Yc3cyYXJk" +
		"TVVSak9sZkhCTXUvbDJ6SkdGDQowRy9oVEZXdkZHTUI3ZzdKVXRHZlpmTG5nUTFaL" +
		"01vbFc2bzNjdDhOeXhBSjY3NXdzT0xlUDBMaUZGTHM2VEc0DQpvWFV4U21sdWtEcE" +
		"w3bWdjTy9iWFdOb0d5TUppWGVKTXZNUDBwVmhscFpVQ0F3RUFBYU9DQVNBd2dnRWN" +
		"NQTRHDQpBMVVkRHdFQi93UUVBd0lCQmpBU0JnTlZIUk1CQWY4RUNEQUdBUUgvQWdF" +
		"QU1JRzFCZ05WSFI4RWdhMHdnYW93DQpMS0Fxb0NpR0ptaDBkSEE2THk5dllpNTBjb" +
		"lZ6ZEdsekxtTnZiUzl2WW5SbGMzUnliMjkwWTJFdVkzSnNNSHFnDQplS0IycEhRd2" +
		"NqRUxNQWtHQTFVRUJoTUNSMEl4SFRBYkJnTlZCQW9URkU5d1pXNGdRbUZ1YTJsdVp" +
		"5Qk1hVzFwDQpkR1ZrTVJFd0R3WURWUVFMRXdoVVpYTjBJRkJMU1RFaU1DQUdBMVVF" +
		"QXhNWlQzQmxiaUJDWVc1cmFXNW5JRlJsDQpjM1FnVW05dmRDQkRRVEVOTUFzR0ExV" +
		"UVBeE1FUTFKTU1UQWZCZ05WSFNNRUdEQVdnQlRTWWtQemJVdjVDNjlZDQpMQzBycU" +
		"kwMWo5cllXVEFkQmdOVkhRNEVGZ1FVRHdIQUwraG9iUGNqdjQ1bGJva054cWFGZDd" +
		"jd0RRWUpLb1pJDQpodmNOQVFFTEJRQURnZ0lCQUR6czBuU2UweXFnb3h6cDQvVkM2" +
		"L0hrSFVqMDJNalVCNWN3cU9tanIyMjJYQlpEDQpHYTlBRXpnZ3N6YWxoT0tHUHIra" +
		"zZqWWtyRkl2L2hjNHFCU0ZhUXl0aStuaWhKV3pISnVsUHM5Ukp5bm5BcmczDQpHdF" +
		"hKUGtCdEFIOWJJOTRFYWpTVENXcVdSTlUxTjVJWS9mTVJSK1N6WVBCVzBMN3FYRzd" +
		"5bCtCZ29NUDdiMElTDQpUamVCbmgvMnVZdFdNWWNWR1VnZ2lkMWgvMEZodThvRGNh" +
		"dU1OeWkrR3dYNWJhNEVGdGhXWE5MR0Y3NTRTaDlwDQpCM1c2UGJRNjA2YXRQclExV" +
		"HQvTVBDNFBVczl1R0E3cy9YdWRxd3NSSUo3SEU5dExNeFh3aXZSbk5xd1JjNkYrDQ" +
		"phMGJzalpoU0NZbHhIQkdFanBTbGpQNnM0ZWg1TDMzUDZWUW5EdEl3ZXVQWk5LVVB" +
		"NbndQajNsUm1tOTEvOU1mDQo3NTdQTUpVNHkvSHBMODBQTFVHeS9kNDNJOTZYNlU4" +
		"VmVlTEFpU3NSREZsVzlUOWVUY0lFRzZleGc2c01FZVpyDQorK0wrSGFiYzZBdnZsb" +
		"GtLSy80WlNJcERMU29ibTdZSWo4QnJ2ZzVmQm5wYkJOeDlQTjQrTG1yVkJRN1I4dn" +
		"JODQpKRmtwYnZYZk4rMmJHdisrMmxvQWlzaGhPUnVnMTB5TTFnU1B0QmhNVXN4N29" +
		"uNC9NcUxTeTBGZE5wdkxqSnA1DQp0N0pVSEN6SUQ5dVBkelZORFpsVGhOMFlza1R4" +
		"OVhYM1R3cVFvQ2VDWHg0dnNjTjB5Y2lOYmRKa05taWxNSkhuDQo3K2UyVjlnbFRKb" +
		"zN4dEZyUUIrc2VRaGxOTUNvcDhWZEM4dGcxSURrazdHUFhMaHlqS2VqTmdSRU40Nm" +
		"8NCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0NCi0tLS0tQkVHSU4gQ0VSVElGSUN" +
		"BVEUtLS0tLQ0KTUlJRjFqQ0NBNzZnQXdJQkFnSUVXV2RVSWpBTkJna3Foa2lHOXcw" +
		"QkFRc0ZBREJqTVFzd0NRWURWUVFHRXdKSA0KUWpFZE1Cc0dBMVVFQ2hNVVQzQmxia" +
		"UJDWVc1cmFXNW5JRXhwYldsMFpXUXhFVEFQQmdOVkJBc1RDRlJsYzNRZw0KVUV0Sk" +
		"1TSXdJQVlEVlFRREV4bFBjR1Z1SUVKaGJtdHBibWNnVkdWemRDQlNiMjkwSUVOQk1" +
		"CNFhEVEUzTURjeA0KTXpFd016WXlNMW9YRFRNM01EY3hNekV4TURZeU0xb3dZekVM" +
		"TUFrR0ExVUVCaE1DUjBJeEhUQWJCZ05WQkFvVA0KRkU5d1pXNGdRbUZ1YTJsdVp5Q" +
		"k1hVzFwZEdWa01SRXdEd1lEVlFRTEV3aFVaWE4wSUZCTFNURWlNQ0FHQTFVRQ0KQX" +
		"hNWlQzQmxiaUJDWVc1cmFXNW5JRlJsYzNRZ1VtOXZkQ0JEUVRDQ0FpSXdEUVlKS29" +
		"aSWh2Y05BUUVCQlFBRA0KZ2dJUEFEQ0NBZ29DZ2dJQkFMWU9GQVU3ZGpDSFNqUUgz" +
		"cnRwdWNrS0FHNnhIZDhmaEpmRzBENnVJWmJLU3RRaQ0KY21OSld0MjcxMmNLbGlFa" +
		"HdUeFJOZjFOcTZpdUJZcHlKanB5dThUS0hLWDNrMG9kMzl0czJxWktLSWt3RmdMMw" +
		"0KMVVVRWRqU2I0MUpWaWpqYU1Fd0Z4b3dKZG9qOXRHR3VNQ2FhT1k4VFZTbEdqbEx" +
		"MK1Mya1p4c1RLekJmTnVPTQ0KVkM0SHoyNWFNeXhpamVsU3N5UlpINHFtMHVhZnU5" +
		"dkNXTS9WdmRHT21zbm9BSC9zL0x0R0NwQlVOVlB4WUxoLw0KQXh6M2FiRCthUTlyN" +
		"29WcHFPV0E5SGdSOHZuSUlYVDhDRUZUR2xRMGdXZmR2UFh2T3hHcnMwSFkyQXVxU3" +
		"BpMg0KbWgxcm9oazE0b2dRRk15ZldRbU0zdkQ5SlVQWmczWldwbDRhUlFkTFJicUF" +
		"IV3h4aS9oTDl2amVMTkhzZldMKw0KcStseDdHU05QZ3JOTTM1bEhWaklIWnArbG9W" +
		"R2xOSGR1SmpvSCtoc3YwdjdsaSttVzE3NFIvaElpcXpDa1FhLw0KTTQxOWcvZ3h4Y" +
		"0pMaXFhbGovaEFFVjBXZlIyUUVLelU1YnF5Uk9xN2UycFRJalJwT1p2WThuZlhWeU" +
		"lXZ1hWNg0KdFhKSDV5c2theFUrSGw5Wm9BNXhzaWpaZ0Q2aVVQeDlxRGFxQlVHd0Y" +
		"zK2svOWFMVXhKdFJ6MTJuN091VmVjWg0KWmRKVEtDMkhyU09pYmJCY2ZsUU9NNHlZ" +
		"b0MvY3BoRHRRSlNWcU1SdW12aVJoQms0SURMcVNlVDJ4WnBXdkZ3eQ0KQmZYVTlsZ" +
		"3dTeXdqR3N6U09pOWd4SFVNUnhTa0JpYkxFTWFxODBycS9TdzY5SXBFMEVsOVFFeU" +
		"hTUE0vQWdNQg0KQUFHamdaRXdnWTR3RGdZRFZSMFBBUUgvQkFRREFnRUdNQThHQTF" +
		"VZEV3RUIvd1FGTUFNQkFmOHdLd1lEVlIwUQ0KQkNRd0lvQVBNakF4TnpBM01UTXhN" +
		"RE0yTWpOYWdROHlNRE0zTURjeE16RXhNRFl5TTFvd0h3WURWUjBqQkJndw0KRm9BV" +
		"TBtSkQ4MjFMK1F1dldDd3RLNmlOTlkvYTJGa3dIUVlEVlIwT0JCWUVGTkppUS9OdF" +
		"Mva0xyMWdzTFN1bw0KalRXUDJ0aFpNQTBHQ1NxR1NJYjNEUUVCQ3dVQUE0SUNBUUJ" +
		"tOXNxUThXKzh1Z0FpVmNiMTN0eGJ5MzJEcTlHLw0KeHpNWUZYK1ltRTFhbS9IN3lQ" +
		"aWxRc0hjNEVMRVhHSkxVZi9GbWlKOUs0ajlhNVRwclJjR1RjRzF3aWlET3VGMQ0KY" +
		"3MrOUxDWkFvbkxORXhWQjBXbVBTbDJScElDdVRnbm95K1RLb2EvWm12M21XanFtWG" +
		"twUUlHcFA5ajhERGs1Zg0KRVpQYnNBcDk5dHpJVkRkSzRoK1kvU2R6N3A0TGZaWEt" +
		"PTmtsNkRua1ZDeUlVWWNIVURtOEhFZ09UWU5wZHhrRw0KclFoTWtHdkJvdVZHSmNa" +
		"ek5WWTJQandZa3ZtS0xJeEhDcllKMzJiVmJqV1dWdUhWMmg4b2RRd0lnVStiNmU2a" +
		"g0KejErUTNZVUl1RmRHUERKN1FPaW01dVdheDhUZENVMnhEb3ZvYW5HKzhCYUFjeV" +
		"puREt5bkl0Y3crRks1TjBHbg0KeGUvL1BrTlZ6eWt6SGhIOHltVFphTkVscVZwQyt" +
		"Ud05IS3E1eUZ0ZnF6WjFnZ0J1cDhoM0xYNjVmMWd5NjFxLw0KN0xCY2NWUTJySWY3" +
		"TnhMeFIydUdTNjFqeWgrK0czSTF4bzRXSUhDMlpFTTROSjNtQzBtb2RiUDNiZlpLT" +
		"3pMTg0KVDErd2h1eTZ5Ylowa0hpenhyTzA2UXFMZHp1ZzIwcWQ5TEIrTmN0bnlIS2" +
		"03K0pBMVo3Tkc5ajdGKzltTklpUA0KcE1NSkJFbzJKNWd5SGdCREpBbDVMS0k2YjZ" +
		"FamRsNXNxRHB5NFFkMzdkdTdWeWRZYkdncDdjY0h4R0MrMFVYVQ0KODZuSmt4VDBp" +
		"V1VtdE9Ld1c1UkNGQitzMy81TFZRRHZTTjhyTGovNTEvNkRoZ0J6OGtXaFlvUXlUa" +
		"E9helM3RQ0KeGJGQlZkcUFrZ0E3dmc9PQ0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ0K";

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ValidateMTLSCertificatesAsX509("UNIT-TEST", eventLog, ConditionResult.INFO);

		//cond = new ExtractMTLSCertificatesFromConfiguration("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	/**
	 * Test method for {@link ExtractAuthorizationCodeFromAuthorizationResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"cert\":\"" + cert + "\","
			+ "\"key\":\"" + key + "\","
			+ "\"ca\":\"" + caString + "\""
			+ "}").getAsJsonObject();

		env.putObject("mutual_tls_authentication", config);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("mutual_tls_authentication", "cert");
		verify(env, atLeastOnce()).getString("mutual_tls_authentication", "key");
		verify(env, atLeastOnce()).getString("mutual_tls_authentication", "ca");

		//assertThat(env.getString("mutual_tls_authentication", "cert")).isEqualTo(cert);
		//assertThat(env.getString("mutual_tls_authentication", "key")).isEqualTo(key);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_noKey() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"cert\":\"" + cert + "\""
			+ "}").getAsJsonObject();

		env.putObject("mutual_tls_authentication", config);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_badKey() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"cert\":\"" + cert + "\","
			+ "\"key\":\"bad key value\""
			+ "}").getAsJsonObject();

		env.putObject("mutual_tls_authentication", config);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_noCert() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"key\":\"" + key + "\""
			+ "}").getAsJsonObject();

		env.putObject("mutual_tls_authentication", config);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_badCert() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"cert\":\"bad cert value\","
			+ "\"key\":\"" + key + "\""
			+ "}").getAsJsonObject();

		env.putObject("mutual_tls_authentication", config);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.evaluate(env);
	}

}
