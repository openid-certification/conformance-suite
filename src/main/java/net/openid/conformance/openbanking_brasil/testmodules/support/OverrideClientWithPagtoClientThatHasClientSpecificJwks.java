package net.openid.conformance.openbanking_brasil.testmodules.support;

public class OverrideClientWithPagtoClientThatHasClientSpecificJwks extends AbstractOverrideClient {
	// "OrganisationId":"39f08c6a-579e-4cbb-9f83-49708902d908",
	// "SoftwareStatementId":"5c116654-c1b2-4c87-a1b2-102043b79b0a",

	@Override
	String clientCert() {
		return "-----BEGIN CERTIFICATE-----\n" +
			"MIIHDzCCBfegAwIBAgIUEWTij/ViUwNpcRCg6CYf37n89XUwDQYJKoZIhvcNAQEL\n" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\n" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIxMTEwMzEyMDMwMFoXDTIyMTIwMzEy\n" +
			"MDMwMFowggExMQswCQYDVQQGEwJCUjELMAkGA1UECBMCU1AxEjAQBgNVBAcTCVNB\n" +
			"TyBQQVVMTzEsMCoGA1UEChMjT3BlbiBCYW5raW5nIEJyYXNpbCAtIFBvcnRhbCAt\n" +
			"IEh5c3QxLTArBgNVBAsTJDM5ZjA4YzZhLTU3OWUtNGNiYi05ZjgzLTQ5NzA4OTAy\n" +
			"ZDkwODEgMB4GA1UEAxMXbW9jay1wYWd0bzIucmFpZGlhbS5jb20xFzAVBgNVBAUT\n" +
			"DjAyODcyMzY5MDAwMTEwMR4wHAYDVQQPExVOb24tQ29tbWVyY2lhbCBFbnRpdHkx\n" +
			"EzARBgsrBgEEAYI3PAIBAxMCQlIxNDAyBgoJkiaJk/IsZAEBEyQ1YzExNjY1NC1j\n" +
			"MWIyLTRjODctYTFiMi0xMDIwNDNiNzliMGEwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n" +
			"DwAwggEKAoIBAQDf2mt3Ok7glDvUIKU7l0IoLQq1gnrgSe0rLxfZSsMecv+uMrP7\n" +
			"nkr6GLz/jRz/XyELUu/DvbbpvWxRirZe94t3+4nxWzcFHRQvb5Zl3ug4heeoGzRq\n" +
			"zV094xavfZ+GCYz28tWDKbcOUn3hCxpbDqbpz0wk6zxTJJ8tysST0EPB8PIsFcxW\n" +
			"0ItRbFW39brF0Sb9ru6UczoJKhI9eLBpV3SFxsb0BdUW+BDOLbARtaqQy9SD5Xf/\n" +
			"vX4wVuJzXKsMBWW5xBRa3v3/Ku5koj1HRKkP9itu58/CmDQKinOUWAZV+pQQ1Tfd\n" +
			"cuM4DCbc5DD9rzllWcQ5YL/nbIvKmIaPHiCFAgMBAAGjggLbMIIC1zAMBgNVHRMB\n" +
			"Af8EAjAAMB0GA1UdDgQWBBRYIJpvu7n7z9AL7pSrKAr30zhtLjAfBgNVHSMEGDAW\n" +
			"gBSGf1itF/WCtk60BbP7sM4RQ99MvjBMBggrBgEFBQcBAQRAMD4wPAYIKwYBBQUH\n" +
			"MAGGMGh0dHA6Ly9vY3NwLnNhbmRib3gucGtpLm9wZW5iYW5raW5nYnJhc2lsLm9y\n" +
			"Zy5icjBLBgNVHR8ERDBCMECgPqA8hjpodHRwOi8vY3JsLnNhbmRib3gucGtpLm9w\n" +
			"ZW5iYW5raW5nYnJhc2lsLm9yZy5ici9pc3N1ZXIuY3JsMCIGA1UdEQQbMBmCF21v\n" +
			"Y2stcGFndG8yLnJhaWRpYW0uY29tMA4GA1UdDwEB/wQEAwIFoDATBgNVHSUEDDAK\n" +
			"BggrBgEFBQcDAjCCAaEGA1UdIASCAZgwggGUMIIBkAYKKwYBBAGDui9kATCCAYAw\n" +
			"ggE2BggrBgEFBQcCAjCCASgMggEkVGhpcyBDZXJ0aWZpY2F0ZSBpcyBzb2xlbHkg\n" +
			"Zm9yIHVzZSB3aXRoIFJhaWRpYW0gU2VydmljZXMgTGltaXRlZCBhbmQgb3RoZXIg\n" +
			"cGFydGljaXBhdGluZyBvcmdhbmlzYXRpb25zIHVzaW5nIFJhaWRpYW0gU2Vydmlj\n" +
			"ZXMgTGltaXRlZHMgVHJ1c3QgRnJhbWV3b3JrIFNlcnZpY2VzLiBJdHMgcmVjZWlw\n" +
			"dCwgcG9zc2Vzc2lvbiBvciB1c2UgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0\n" +
			"aGUgUmFpZGlhbSBTZXJ2aWNlcyBMdGQgQ2VydGljaWNhdGUgUG9saWN5IGFuZCBy\n" +
			"ZWxhdGVkIGRvY3VtZW50cyB0aGVyZWluLjBEBggrBgEFBQcCARY4aHR0cDovL2Nw\n" +
			"cy5zYW5kYm94LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIvcG9saWNpZXMw\n" +
			"DQYJKoZIhvcNAQELBQADggEBAAA7YnrdGTSy6p4Wh72rstEZNKPwuIrbHOsJr4Ou\n" +
			"Ym9u6+GFgsb5Jd/KdU1PrDbJ7lznp5h/haSHU9EcfJU9q7kTAcGZbmzfnpI/b5+q\n" +
			"v9UwCr27DgyJWaw5Dvy86EBOSIokExm/lPOGMqlWUqXFllkCwGBcReCdjQLVHR5X\n" +
			"uF2GTYeV1YJrXz03uKccefuNsuefzaTzwy1ZhjMQCAfuMSDWgpJSumICvr1690LK\n" +
			"/us/I8LYNsQ4eAUVRHcg36Wpt44WwbiKkgu8zpXpQMsHHw8edfMNilYWj0Q2bdCv\n" +
			"QBYZSlVJTsTC3Npssdvvheoqpn8NbcJlAs2zVoWOsvx67K8=\n" +
			"-----END CERTIFICATE-----";
	}

	@Override
	String clientKey() {
		return "-----BEGIN PRIVATE KEY-----\n" +
			"MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDf2mt3Ok7glDvU\n" +
			"IKU7l0IoLQq1gnrgSe0rLxfZSsMecv+uMrP7nkr6GLz/jRz/XyELUu/DvbbpvWxR\n" +
			"irZe94t3+4nxWzcFHRQvb5Zl3ug4heeoGzRqzV094xavfZ+GCYz28tWDKbcOUn3h\n" +
			"CxpbDqbpz0wk6zxTJJ8tysST0EPB8PIsFcxW0ItRbFW39brF0Sb9ru6UczoJKhI9\n" +
			"eLBpV3SFxsb0BdUW+BDOLbARtaqQy9SD5Xf/vX4wVuJzXKsMBWW5xBRa3v3/Ku5k\n" +
			"oj1HRKkP9itu58/CmDQKinOUWAZV+pQQ1TfdcuM4DCbc5DD9rzllWcQ5YL/nbIvK\n" +
			"mIaPHiCFAgMBAAECggEAXlwaR8BW5njlvVXjgLqSYxAVfwyY0jmnVTg9M9W9aPTt\n" +
			"zRbHYo3HqiPKa2im7AjEC5tywQ6T73RvF/JCTMbJThLEg25BQ5EN5K8DdT6goc7k\n" +
			"JysMjeayYrd6ZRAtsdkNyDz1fpwTc2CXekjtW6vR9SXyQW+9VojN5oYeFfbI0RN/\n" +
			"ixRTi0QdPDEmF0wgYHw//E7/iC9qgrngNu++gvHphzy0BJPTqLy1PCG0KigCOJR2\n" +
			"2X7rIPeaUdZwWCE4G2GgXqvbckq7IVeEyn+rC5WoFbJc9P+StoO9++A6aR6aT1BB\n" +
			"EkZuc/qqLBOCCdmproh9nCedd5jC8o/mmrVwWcZQMwKBgQDqDtl5jDp8Tn+Ahnrp\n" +
			"s0cSaG5tA4sjgNw+biSwFrPoXSvOh0qkb5++sKL9Ddl7C2BZnxe/dzpnQHdB3No5\n" +
			"9+eTXM+mVs34RijtaYSaKarrzi2pGuPFuXd2BhaCL6NnT43X3shdCGtvYPfyIlUh\n" +
			"8zfr+YEvzsquUCzdsmeifER1WwKBgQD01qpf8dkfWcBhx8kFsWKTiHM1/SvNqDEv\n" +
			"KxoueW9dy6EgNrnJ4ADiVgrDguNGn1scZCxcjCNLyA+b7kP7NWVjDCtbjOcu9/JR\n" +
			"i9vjiTy44me5pvstnV6MFQd8fU3cv1V5/kM+guV4QOikRRh7AH5MKz2Mt+NCwLlF\n" +
			"2LtFgLdHnwKBgQDE04f+tvXX4wtFwdFLBgAWcsup0PkI+iw8M9OWYQEZvoBARXV3\n" +
			"oobrjQ8DTso8tuxncWo/ELyEZ6niMR8y8E5Flh7o+sZqqlVwkPN50OBzPAcZ1gsF\n" +
			"E1pqXeQ/xl/bWQMnLanA0nvGRf08GbMNwcKdJkyFL5kxzSptpMGR2n6XUQKBgFTg\n" +
			"Idn23hznITEEcCSAFLsuPzpFFK1LlBlU6NOZl0i9sZKYRhm8hLHxv/N86UrvLUeP\n" +
			"Vm2Syx2XMgoNNBXesdH/QvyOwQngPGmWgVjEl39ERU7vmv16I2+OTrUSPy66SWhs\n" +
			"A2WlPOVOIxfnOBBT4HSJPOIOUUMvykpzUqBKeb7jAoGBAOc3qSc5s78fYuHYMmh8\n" +
			"rl7FrfIGPLgAtCHyrlL4cy0w6L+OUbHyXc8o8faDwZ7ihibhRwJWYhQw8Jqfpb1V\n" +
			"sIE4aPNA7tRcQ4sISzeWeHYWHEVSmNIWpoDRuDLuT0l+2IiegqBKRdv3e5D8Fh5A\n" +
			"ugwIV0uL3vZLisDniRpkeuTQ\n" +
			"-----END PRIVATE KEY-----\n";
	}

	@Override
	String orgJwks() {
		return "{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"alg\": \"PS256\",\n" +
			"      \"kid\": \"zBOcUM3PQ6tc74R_4dNdZbuUOrqJJRfbu9OlzBV8TSY\",\n" +
			"      \"n\": \"ikHmLu-GUggwBhLf-69QIaeHXLfpX1ErMop9YDG0QvsMxnpyF0hVw3_AHCuXAGvy_q_8vr6b8IJmUM_lDgrVAr7nBjGWE8rTddKD9N19Ojk8fJLWbiKzS7vABcXf0sJJIcl1DqLfVWeM0vdmMqodTMrRiBL5ZDkdNSqRAh1FkLo1vdu0rYrD1prKdk5DClvGr0XKO5zUEO-2KSzwTH_ycZ-wV9uEvyVYD_Z6YYxmttEsHNxPsxvvCYx_hHwgaV_WKROM8X5-b4Gj9tH3COm_MQ1iccaRf5B39blKDmTIn7_7ZCv4Q6ERsjT0gjlNZAa-BekWk3FvoEnSrDoAW3JU2Q\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"d\": \"ApDwD-QgEwg2CfrSNPafTwyavCtpYGdI3Q6J1AiqzvPKxtP8J6H5qOAHZbkfu6Ev-K8KtX34tilZcvu02u1JCwdrkXzHS2qVllzYAv9pxQmjGDB_3I9LfXxIjLg27A9xR42uHPLZddlERCCAQVr7zGfW-pwC4dZsjBwpkQeQObwwFDT7IcVhottaO2JncwFI4EdrfafEnUO0-lzHWoENlThUS0NYYrBt3YS67lHh3fOsfDkAboy0gWfc0dLtyF0UlSJGJDzTBLBIoImJA7_QWdOVoUjbkkPvmsbJyyM4wN8yUJVNyWlAewUBNg2fiJ-aYY3vO2jh2lwXjcNibeM77Q\",\n" +
			"      \"p\": \"wQo3XydDKZutZ_aBQWWVis93yd7kzSXntrQwq0jSBr2UgUItkW5x15SBd0Cuoe0hIMZ2P5MFRSxpWnSCTGdKaL3llMRKLDK5hCnxFkbQQYoyOIEvuEO5X1D39YICRX73QAl9u3dhq2QkdeWFOQsLfMWhau3AoPE322fYqPvBCs0\",\n" +
			"      \"q\": \"t1mkgh9z1b0T_ALDKulrvv3I5MtdAF6gMWPidgaXwImsVhZDllKC8hX_gHF99IaUS4w-HUSXT0HrhRhyblmOZbWj8PLT_A3AdsFtWGBqLc63OGsldivujd_c3hQJcWCq4NOLRt83KX8n7eCpktTOE5EiGMP0V8vlfZW72UWhyj0\",\n" +
			"      \"dp\": \"MHrrWxdWM73oN-LdpVnoy5q9H9K9rZPmdKkeS_YW4SB9ilTfctXE-3pNZXC2Ku6N0lhlXCQFP9EeiFwYWS3bryB55vnBEwaONtX9uTWBmeQmJrCzFljT1k9UZrEG9wMi_08i55Dc05lr2rwQ0Dmo4eYUWvFo3kKWX6Dd9dp8KcU\",\n" +
			"      \"dq\": \"c3FN02BaXsmeO57Bo9M0tBy20Nf6xrDNzEtH22hrRB9rEwkRpSRurl6LcSQEWmIiHS9ALM1zN8QZtsOdyT06G3AyuRMrxhgihqNjZbHPKOhvFGbiP3WJzmqVdn7HM0vaS5TmrMj-wnH9ghliq8CxwEAxZ8Z5oo4PAPO2QydEp0E\",\n" +
			"      \"qi\": \"K1Nu3RrzuMmUtJlF2ESQcfwQdSXBlkPc8Epr0OCWUa48d3V0iObMPVxtUFLNXOYoiKSSbOHqgUo1w5eCypGQpWRlUZmwaFDpFHFhxxfH8vdnMQl8WQvEBwAqMT2hTaMxmo5BkStUjupix5lxSTdgz0NjGBvUfnMC3FXdo2u1bnM\"\n" +
			"    }\n" +
			"  ]\n" +
			"}";
	}

	@Override
	String clientJwks() {
		return "{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"kid\": \"j0pZ6CWi2LVZUnMFxrEVtJzEQPf8R3y53810w1nPCHk\",\n" +
			"      \"alg\": \"PS256\",\n" +
			"      \"n\": \"uw3GuWSlkk0mxoj9oytKfaP1KI3yiPJJBkvUAeDkDmk3hzhdDPIMaUB6mEfJYTrMax9gdTgJ22CcglvPwFGY2d5VOi5v88qJMPKpxnacM7oGh94fX-gcizCQo9yMv2Uocnfs40weDgXfZfl0AlW61FYTY19eKZW7XpPoqYI9rnAJkXOJkTPw4T1VBS9mhXj0njDHs-AzOzLatiGrUiJt0M9ioLSmqk8UP6T5-IjPFlQKGYvGWL8WoatNChcWD1mVT-TXvfnqjDZRs6zz74abGptfCm-ksa5W-0aqc7Y6YK0Js_iSD82svNmrB88k-q3MTkR4hpvwCMh6AGDz3nv99w\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"d\": \"GKAzrWymr6AgnrqiSb0FTY0sVW56o7TiEEYjXyvwWkVX3iF5fp7PK3wlp66rwHUxPFkhJc1-3rbVZAQaUcNsUCKJLeO3MW1UqnEIEOzEm7q96V1A3Ct-toRqRmhez0POE2Ped_4pZsc3JgG1WClZM2Mxoj-H8gmYZVcrpkVTQYec3HiyjldEbLXglZUURzSfa-sY2X0tham978ulpAkot6jLJw-VExS1UjmcxUSKntYhuhLTXdmtH6gQinYp-seiBeZUhdYMyeYnJzG1swuOzUB71Qw6cOZwjLADfr8FxllJ9PCjaUsnTu2c6urCoTbrn0UkmF-sJMKZx2-a4iMrAQ\",\n" +
			"      \"p\": \"2qQxBEkiXZLAEvMFNN8ysHKrmvMqBJeQiaiY2aYpq057N5wQvKNSohzwMUVJncXxxfjyb2jfU6mT5I2r5oS8yez_EefF2Wz0omwWyGJmpMGJ8WYQgV6HbczZrOUQoR0gUVQbk5QzCCz7fY9Pvw4T8T04tnPWG6DJFBJEvx0IYKE\",\n" +
			"      \"q\": \"2wPh0pKqmEq95vAsCaUPmBAl9814I9-XSnrLefljjW7wRZuGPUOUlgJ0uYq8pO4XepjPD3g11mTFJ5wlRKObWLFWNnZZfjGQslNPhupBBSjI_9CFIb-aOmL8WsKsleZpOY30Pt7drlnW8UhSXwqU9wLH_nArK4HKCLQIb5Inn5c\",\n" +
			"      \"dp\": \"FUO-2LHcO8mYEL--E-RZY7vjYNChl4y-LAVPyGtWxih952ywXAhucwHpgoFApa2o1B5gReGnRtXJYoM84tCqI-F-9Vjbb0gfiuSEWrznSsLgDbBljo-JEG7KBPzKX0Eb8Y0CmZniVLs7Qnz7vpM58U6JA4XEny9GH0OfmA7Uz8E\",\n" +
			"      \"dq\": \"o_b8EVGMNgd-tG4KCg5w5j7wrdw9nV2_PhtASkjSpwfvCa2tiiAWFVgxWtbq8-7r1PShz8sHQ0Kd91GG9SQnIPdiu9NOnJMu6NJGL5MgqmQmVp4djW6MYDnLA4fK_U5KaLRFruvaurS3nluuj0i0zVhfsbT4HNJGFs3xotWgpHE\",\n" +
			"      \"qi\": \"iaVxa6cEtEkhVw4ygW9qLFy4Mosc398vwP3EGUGOG-u4Kix6KTiCpLhH0FrI0yttZA2xRx7uoxLnbvTu88uuoQLh5gQZ6Bv2aa2iVdPV5Me7UhcSMbky9Id-y7IrEhoYx_oOdX4yyHB0w7xFGLqqaKQ5ggZqrLdX1RmY16VsGiI\"\n" +
			"    }\n" +
			"  ]\n" +
			"}";
	}

	@Override
	String role() {
		return "PAGTO (different organization)";
	}

	@Override
	String directoryClientId() {
		return "oyjbJCw6bR7OvqQAwlJWi";
	}
}
