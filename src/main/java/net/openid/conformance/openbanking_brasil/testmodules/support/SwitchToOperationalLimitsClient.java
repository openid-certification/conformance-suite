package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.PEMFormatter;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonUtils;

public class SwitchToOperationalLimitsClient extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client", "config", "mutual_tls_authentication"})
	@PostEnvironment(required = {"original_client", "original_mutual_tls_authentication"})


	public Environment evaluate(Environment env) {
		JsonObject originalClient = env.getObject("client").deepCopy();
		JsonObject originalMtls = env.getObject("mutual_tls_authentication").deepCopy();
		env.putObject("original_client", originalClient);
		env.putObject("original_mutual_tls_authentication", originalMtls);

		//Client

		String jwksJson =
			"{\n" +
				"          \"keys\": [\n" +
				"              {\n" +
				"                  \"p\": \"_uuGZO8dk21jYVKeZ37BepkwzpWr3-KFZqvSkMaX7iPqIYBD1Ee0utzuGlV395HylJ9UPq3fhm7qemzNnkBUCxBM950vDQEN4ySDkQUftDw4r9WMrbxpz8XpBwkXuo6qTE9iRgXa9IP7ij3Pr6Qz64JmTvpnYjHSi1g4q73j9Nc\",\n" +
				"                  \"kty\": \"RSA\",\n" +
				"                  \"q\": \"zDafMjBVbeGfeo2y_kv0rXCX80vEEZCL_F3DClRvZoQgZVfHTk5axSUGsjIg7DpKG_qI6hKLMxXcUhKbw3Lzxfix10DjL4U9RIQLC1OKS4hVGvyRhkmqQjrVrUoj1Qqel6IKo2mVG_b6V1ar4XxvmNmeB3NV9Zjh2Zd0kDFT4uU\",\n" +
				"                  \"d\": \"B70PyvjAuBNj1RbZcIYZYlWPD27H-wRNCoXofgU1EVaqcIGiGAx3QKIABgesHRylggaRnRlatHNG_Q_-pam-R8wSH4r9kXCLdG233WoBBE5V9bqjCORuO8R_IKl5CNqVF_ADPAJPL0cJTLBy2q0jhsfnYLTto0KxnTWIgUEkxHjts6dQrrC1TZL1K_670sRJsF0lld1bCjVePcmHzwKPyGBg9RU9kDm782TsGjyeh_uCpOqZtSsviiw2OChkTEYu83Th045xwe7YjTUQKkL-ezLHwJs2iuLJWE2YqoshzLV0ja3U7laMt_Gm_a4HiUJtKdTlOvw87d3GsmWEoKBU0Q\",\n" +
				"                  \"e\": \"AQAB\",\n" +
				"                  \"kid\": \"XCB0CnPSpt3Zy8J84Omx63a_oIemAR1gXDfVNn833Zc\",\n" +
				"                  \"qi\": \"qjzKeV4CX4dmoRB2xRlRKMzZYMkNV-cY9GzODR56WlZaUUOS9BTiCwsSDvy7KdtBstPvLKAyeWyezBLBLkaDZQHA713QwErw5_hDaEHulznNIAHOgH7ATq5GhLMWRsNXBiSlK6bP85D86bO4zIjkWUtQn9qHzXeMStCsN58Vrls\",\n" +
				"                  \"dp\": \"QNZs920uK5pOBan66j3-Pg2KsXC2J5q47XVUeEhaylJOMbkrB2o5mLGqyf3lJo3zkkO4LL0StmGtt_fGkLKx7aJ2wSA0oG0dOCHIaPFRsAkFLgv13RaoHxWqgISaiXPR8tjaGQ9SMpEDQJUaJwVvwz4GgZ4E_5_GenS07VG-Ej8\",\n" +
				"                  \"dq\": \"E4P4zLwPvfN84SJfg6vQky6aZtbLwv30UsbHF9qf1sUzr1unIVWdmQ-Dx03BFbIwWOIlaXEiOlyRdIpwtknq__VXUYHS9DygcUbJI2j1Y2iA8ZZHdfTeP3wN5YSbTgW4yDblG17AsC53GBehT2gr5giN8JpWEaVtl_TAX_NG1_0\",\n" +
				"                  \"n\": \"y1oTTQpwLMPW_ZtBbI1v7dNjqzKlpoVyOjH9h3ZcO4HtcE10-_OcVyw0jto9qxY8OEIRNeLUDBxF1Zr3V7M2-KVAoKc46QTQREcPt-3F7xl7zFPWe4gE8oaPAK1w8culG9P7fFxMiCG6LByxsZPU4VpYVqo5FztrW62l9RVaShYipdgGL89i2gVmItK2oDo01VCn6YpQP_3uI6IWVjDhlWBFTOAXCPPNuV9LUHKUNLw9rlDNk_XH0sMkz75ratyS__IBnqngl5mgYEEmpPJWoFi4E4AKXAb0jXeRPAONHnF2buYY4BcTZ7N2WUIBJPIwArh9Au7PSGqL9ab5o8TSUw\",\n" +
				"                  \"alg\": \"PS256\"\n" +
				"              }\n" +
				"          ]\n" +
				"      }";


		JsonObject operationalLimitsClient = new JsonObject();
		Gson gson = JsonUtils.createBigDecimalAwareGson();
		JsonObject jwks = gson.fromJson(jwksJson, JsonObject.class);
		operationalLimitsClient.add("jwks", jwks);
		operationalLimitsClient.add("org_jwks", jwks);
		operationalLimitsClient.addProperty("client_id", env.getString("config", "client.client_id_operational_limits"));
		operationalLimitsClient.add("scope", originalClient.get("scope"));

		env.putObject("client", operationalLimitsClient);

		//MTLS

		JsonObject operationalLimitsMtls = new JsonObject();
		String certString = "-----BEGIN CERTIFICATE-----\nMIIHODCCBiCgAwIBAgIUMHGmMzUBDyvZ719lk0iTmFLA6RgwDQYJKoZIhvcNAQEL\nBQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\nFTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\nTkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIyMDcwNTA0NTkwMFoXDTIzMDgwNDA0\nNTkwMFowggE4MQswCQYDVQQGEwJCUjELMAkGA1UECBMCU1AxDzANBgNVBAcTBkxv\nbmRvbjEcMBoGA1UEChMTT3BlbiBCYW5raW5nIEJyYXNpbDEtMCsGA1UECxMkNzRl\nOTI5ZDktMzNiNi00ZDg1LThiYTctYzE0NmM4NjdhODE3MTswOQYDVQQDEzJ3ZWIu\nY29uZm9ybWFuY2UuZGlyZWN0b3J5Lm9wZW5iYW5raW5nYnJhc2lsLm9yZy5icjEX\nMBUGA1UEBRMONDMxNDI2NjYwMDAxOTcxHTAbBgNVBA8TFFByaXZhdGUgT3JnYW5p\nemF0aW9uMRMwEQYLKwYBBAGCNzwCAQMTAlVLMTQwMgYKCZImiZPyLGQBARMkYmU2\nMGZmZmMtNjVmOC00MmQ1LWEzZWItMDEwNmI0YTUyYmQ0MIIBIjANBgkqhkiG9w0B\nAQEFAAOCAQ8AMIIBCgKCAQEA50JYks3I5jOLv3Z6dpyJVV+VhO0TsOelkGrDCERX\nL6TVsRNc1kyJkQ+OwkIm3NadSyUMKIqUFmq3VnCmqPYlNikru6DC1RetQKMzSQX0\n/bfoOIP3a0i6ziPP1EsaPB1AZ48ziRNSXKC1fSkIdB22rJyDuPo1LOcmYTORIgH0\n+yEBTZ9HmxTcmVBxTPp622a88JTTJm5IU9M/4c69fD/zoFBQRGWBlFB1p4/HeBX0\nxjqTe/GE2lfF7wzZnH13X8XL6bLtDxsrEfSIE318b2fAfnE2PKSvE3ad8LqSPSeo\niXwbmPPsnExqOfwG1ugf14fWJGGqzD/e0FiJAy5stGU5qwIDAQABo4IC/TCCAvkw\nDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQULbkhvlDeMujNOb/5tISzsGibOrswHwYD\nVR0jBBgwFoAUhn9YrRf1grZOtAWz+7DOEUPfTL4wTAYIKwYBBQUHAQEEQDA+MDwG\nCCsGAQUFBzABhjBodHRwOi8vb2NzcC5zYW5kYm94LnBraS5vcGVuYmFua2luZ2Jy\nYXNpbC5vcmcuYnIwSwYDVR0fBEQwQjBAoD6gPIY6aHR0cDovL2NybC5zYW5kYm94\nLnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIvaXNzdWVyLmNybDA9BgNVHREE\nNjA0gjJ3ZWIuY29uZm9ybWFuY2UuZGlyZWN0b3J5Lm9wZW5iYW5raW5nYnJhc2ls\nLm9yZy5icjAOBgNVHQ8BAf8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwggGo\nBgNVHSAEggGfMIIBmzCCAZcGCisGAQQBg7ovZAEwggGHMIIBNgYIKwYBBQUHAgIw\nggEoDIIBJFRoaXMgQ2VydGlmaWNhdGUgaXMgc29sZWx5IGZvciB1c2Ugd2l0aCBS\nYWlkaWFtIFNlcnZpY2VzIExpbWl0ZWQgYW5kIG90aGVyIHBhcnRpY2lwYXRpbmcg\nb3JnYW5pc2F0aW9ucyB1c2luZyBSYWlkaWFtIFNlcnZpY2VzIExpbWl0ZWRzIFRy\ndXN0IEZyYW1ld29yayBTZXJ2aWNlcy4gSXRzIHJlY2VpcHQsIHBvc3Nlc3Npb24g\nb3IgdXNlIGNvbnN0aXR1dGVzIGFjY2VwdGFuY2Ugb2YgdGhlIFJhaWRpYW0gU2Vy\ndmljZXMgTHRkIENlcnRpY2ljYXRlIFBvbGljeSBhbmQgcmVsYXRlZCBkb2N1bWVu\ndHMgdGhlcmVpbi4wSwYIKwYBBQUHAgEWP2h0dHA6Ly9yZXBvc2l0b3J5LnNhbmRi\nb3gucGtpLm9wZW5iYW5raW5nYnJhc2lsLm9yZy5ici9wb2xpY2llczANBgkqhkiG\n9w0BAQsFAAOCAQEAqMjHHRqpTzzR4qXUaGxjAt6QAUQ1je6mSroxAKdCocsLLupH\nL2/e+WRfuyhOBtTB7KHCPVQhfsyyYz5dOLb2dAOMgA9y1I4ivrHrUUBPSmoTTdT9\nps9Y1n34O6nl+Z7OFZTmVPH+Jk4hJ/5vMDKk/3i2LHaUpU9gaR8vXOR7L3W1GOoU\nLreruJ5twd1j++N7WmSJio7LRAEXnugBSv7y58wWNubsRuw2JYxLjtVxrTuI1PED\nb93DDGVOh6RPfXiRlho5xBH0xVq1IAvl4j8mo6ak8CfUXvIncwzzHPtLelnByRP7\ni2WLbQomBgLbSIHsY1gn7d2p8bbdvZHcZ8nijQ==\n-----END CERTIFICATE-----";
		String keyString = "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDnQliSzcjmM4u/\ndnp2nIlVX5WE7ROw56WQasMIRFcvpNWxE1zWTImRD47CQibc1p1LJQwoipQWardW\ncKao9iU2KSu7oMLVF61AozNJBfT9t+g4g/drSLrOI8/USxo8HUBnjzOJE1JcoLV9\nKQh0HbasnIO4+jUs5yZhM5EiAfT7IQFNn0ebFNyZUHFM+nrbZrzwlNMmbkhT0z/h\nzr18P/OgUFBEZYGUUHWnj8d4FfTGOpN78YTaV8XvDNmcfXdfxcvpsu0PGysR9IgT\nfXxvZ8B+cTY8pK8Tdp3wupI9J6iJfBuY8+ycTGo5/AbW6B/Xh9YkYarMP97QWIkD\nLmy0ZTmrAgMBAAECggEAXPoqvFMqO4tr2z2aPQfwmzeD2N6sdQqdYTPbV/6KAyjF\nlZ0QTUSWZpEXt3h7QXyz3tt1SPN2WJDtRz6hcvsLuPvSjmwzCROs33j+DJTti/id\nz+MW3bZvDKQPUe4kDRlBpKFJegofqggfD8Qolu6/XCTNPNiz+mqw3pGp/z9ELJaz\nsPiFNOkbSvHs6nycRb6fGHzeX7mpnoh2JICWXjUUUHd1RQ2pQTFgFQbpIqh9UPeY\n1nfrjc8cUL9U4qe1U77EHB8zlHJog0nfa8fRUZWCopov0gwj9Kzu84MDawx2hSZL\nv36MqsAF5TP+vnHdnf9EddVhsThqEVcNfPC8YVDcSQKBgQD8zKlVufrvaab3ARaX\nxmSUJQx9Dwng4XyEuTVVKQznO1NnLwgTNkKiC6X9OrkEWpaHdKrpIDUFVmH1REww\nkQ/4SPUES3Kvh4Yzp4fKFRn4zngXFKulMcEaot6Uc8mZF2XkRtz4GDajo1Gxh1JV\nAKXNTNAhSFw0d/Y6Uuh48s1BvQKBgQDqL98BFwfBj9/3BemNvE4Fei7bDiH75DnU\ndW8WRw9i91QfG5LxQl4k2R9Z7hEbqSllu7MOH3Hst4c1qlVSeL6XeBkOM9OSgIHo\n6SUM3FTK6/C+HPIhdC9BNp6gnyxR57KVCmxpyDIF2HOiV7sPsjeE1VXeIihc02TQ\n0jy/jZU7hwKBgBFUwqMy7eYt+xliBLeBDXIunA2S012Md+ntfJ+LAex0X8JqMgaH\nKMhNPiL/PKH0x+8fa3wyDhCAnJShCwwlVc/yyIFz3rfz9Zpi+Oc8zRwGo6sJar8X\niyKVWecZjQ+m0AB3bzy/BDfxwW8HVPio47UQeBBgEpoyQzo6Zt+r3bCNAoGBAOPB\nRNexhziHbWC3nkna/eIzzwf5ubFZZ6ipwSzgVSWBto0+au06B4c8plH2xyqonPs0\nzdIJYOnAH75gdvRrO6jiAzQd0UDku/NBc2gLjGeBiTPLwOwqK573uNQrmZXaiYKL\njZ5fNc8bkHItcl60i2wjcoxDmg+VA7JXFP+SebVXAoGBAPcUWtRDsIP7p7FFA5He\nqoCXfP1eGhaPttxsA9quHA71GxnUPfTN9/rwQ6qdZMQF5iRTCmX6rsqxrzuVG+ns\nz5b+SsFoOw/YaTtkNkQoZBNeIiT7EeTTAwGG7x0Qji1tG75oF9bXDyNX1WBAYuou\nwHYFNAlYpQltlBqjISDQWcta\n-----END PRIVATE KEY-----";
		String caString = "-----BEGIN CERTIFICATE-----\nMIIEajCCA1KgAwIBAgIUdIYzEFdw7QJcrySyq6IiEwZfTfAwDQYJKoZIhvcNAQEL\nBQAwazELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\nFTATBgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJv\nb3QgU0FOREJPWCAtIEcxMB4XDTIwMTIxMTEwMDAwMFoXDTIzMTIxMTEwMDAwMFow\ncTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwxFTAT\nBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNBTkRC\nT1ggSXNzdWluZyBDQSAtIEcxMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\nAQEA6fX+272mHX5QAcDaWkVHFWjnDIcORNUJU3OuNyeuOYhlvXJWydrXe3O+cV+P\nS39faMj/nfem3GfJBE7Xn0bWA/8ksxSfrg1BUBJDge4YBBw+VflI3A0g1fk9wJ3H\nGInsvV4serRJ/ISJTfs0uRNugX+RrbkT/T0tup4vGd3Kl2sbwUdDjokuJNJHANeO\nDRkQ+ra+9Wht71FBlc07yPf7qtpaWHm6aS3s47OJD35ixkG4xiZuHsScxcVtlo1V\nW98P2cQfH9H2lll4wWlPTVHpPThB2EYrPhwcxDh8kHkkOHNkyHO/fYM47u7H4VeQ\nV75LXWKa7iWmZg+WhFb8TXSr/wIDAQABo4H/MIH8MA4GA1UdDwEB/wQEAwIBBjAP\nBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBSGf1itF/WCtk60BbP7sM4RQ99MvjAf\nBgNVHSMEGDAWgBSHE+yWPmLsIRwMSlY68iUM45TpyzBMBggrBgEFBQcBAQRAMD4w\nPAYIKwYBBQUHMAGGMGh0dHA6Ly9vY3NwLnNhbmRib3gucGtpLm9wZW5iYW5raW5n\nYnJhc2lsLm9yZy5icjBLBgNVHR8ERDBCMECgPqA8hjpodHRwOi8vY3JsLnNhbmRi\nb3gucGtpLm9wZW5iYW5raW5nYnJhc2lsLm9yZy5ici9pc3N1ZXIuY3JsMA0GCSqG\nSIb3DQEBCwUAA4IBAQBy4928pVPeiHItbneeOAsDoc4Obv5Q4tn0QpqTlSeCSBbH\nIURfEr/WaS8sv0JTbIPQEfiO/UtaN8Qxh7j5iVqTwTwgVaE/vDkHxGOen5YxAuyV\n1Fpm4W4oQyybiA6puHEBcteuiYZHppGSMus3bmFYTPE+9B0+W914VZeHDujJ2Y3Y\nMc32Q+PC+Zmv8RfaXp7+QCNYSXR5Ts3q3IesWGmlvAM5tLQi75JmzdWXJ1uKU4u3\nNrw5jY4UaOlvB5Re2BSmcjxdLT/5pApzkS+tO6lICnPAtk/Y6dOJ0YxQBMImtliY\np02yfwRaqP8WJ4CnwUHil3ZRt8U9I+psU8b4WV/3\n-----END CERTIFICATE-----\n-----BEGIN CERTIFICATE-----\nMIIDpjCCAo6gAwIBAgIUS3mWeRx1uG/SMl/ql55VwRtNz7wwDQYJKoZIhvcNAQEL\nBQAwazELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\nFTATBgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJv\nb3QgU0FOREJPWCAtIEcxMB4XDTIwMTIxMTEwMDAwMFoXDTI1MTIxMDEwMDAwMFow\nazELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwxFTAT\nBgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJvb3Qg\nU0FOREJPWCAtIEcxMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp50j\njNh0wu8ioziC1HuWqOfgXwxeiePiRGw5tKDqKIbC7XV1ghEcDiymTHHWWJSQ1LEs\nmYpZVwaos5Mrz2xJwytg8K5eqFqa7QvfOOul29bnzEFk+1gX/0nOYws3Lba9E7S+\nuPaUmfElF4r2lcCNL2f3F87RozqZf+DQBdGUzAt9n+ipY1JpqfI3KF/5qgRkPoIf\nJD+aj2Y1D6eYjs5uMRLU8FMYt0CCfv/Ak6mq4Y9/7CaMKp5qjlrrDux00IDpxoXG\nKx5cK0KgACb2UBZ98oDQxcGrbRIyp8VGmv68BkEQcm7NljP863uBVxtnVTpRwQ1x\nwYEbmSSyoonXy575wQIDAQABo0IwQDAOBgNVHQ8BAf8EBAMCAQYwDwYDVR0TAQH/\nBAUwAwEB/zAdBgNVHQ4EFgQUhxPslj5i7CEcDEpWOvIlDOOU6cswDQYJKoZIhvcN\nAQELBQADggEBAFoYqwoH7zvr4v0SQ/hWx/bWFRIcV/Rf6rEWGyT/moVAEjPbGH6t\nyHhbxh3RdGcPY7Pzn797lXDGRu0pHv+GAHUA1v1PewCp0IHYukmN5D8+Qumem6by\nHyONyUASMlY0lUOzx9mHVBMuj6u6kvn9xjL6xsPS+Cglv/3SUXUR0mMCYf963xnF\nBIRLTRlbykgJomUptVl/F5U/+8cD+lB/fcZPoQVI0kK0VV51jAODSIhS6vqzQzH4\ncpUmcPh4dy+7RzdTTktxOTXTqAy9/Yx+fk18O9qSQw1MKa9dDZ4YLnAQS2fJJqIE\n1DXIta0LpqM4pMoRMXvp9SLU0atVZLEu6Sc=\n-----END CERTIFICATE-----\n";


		try {
			certString = PEMFormatter.stripPEM(certString);
			keyString = PEMFormatter.stripPEM(keyString);
			caString = PEMFormatter.stripPEM(caString);

		} catch (IllegalArgumentException e) {
			throw error("Couldn't decode Operational Limits certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		}


		operationalLimitsMtls.addProperty("cert", certString);
		operationalLimitsMtls.addProperty("key", keyString);
		operationalLimitsMtls.addProperty("ca", caString);


		env.putObject("mutual_tls_authentication", operationalLimitsMtls);

		logSuccess("Switched to hardcoded Operational Limits Client",
			args("Current Client", operationalLimitsClient, "Current MTLS", operationalLimitsMtls,
				"Original client", originalClient, "Original MTLS", originalMtls));


		return env;
	}
}
