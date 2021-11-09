package net.openid.conformance.openbanking_brasil.testmodules.support;

public class OverrideClientWithDadosClientThatHasClientSpecificJwks extends AbstractOverrideClient {
	// "OrganisationId":"39f08c6a-579e-4cbb-9f83-49708902d908",
	// "SoftwareStatementId":"d59833a8-0471-42e2-b286-19d87ea8cd0e",

	@Override
	String clientCert() {
		return "-----BEGIN CERTIFICATE-----\n" +
			"MIIHDzCCBfegAwIBAgIUY/dUEHH8PY/75rJd20EBPmwsF4owDQYJKoZIhvcNAQEL\n" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\n" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIxMTEwMzEyMDQwMFoXDTIyMTIwMzEy\n" +
			"MDQwMFowggExMQswCQYDVQQGEwJCUjELMAkGA1UECBMCU1AxEjAQBgNVBAcTCVNB\n" +
			"TyBQQVVMTzEsMCoGA1UEChMjT3BlbiBCYW5raW5nIEJyYXNpbCAtIFBvcnRhbCAt\n" +
			"IEh5c3QxLTArBgNVBAsTJDM5ZjA4YzZhLTU3OWUtNGNiYi05ZjgzLTQ5NzA4OTAy\n" +
			"ZDkwODEgMB4GA1UEAxMXbW9jay1kYWRvczIucmFpZGlhbS5jb20xFzAVBgNVBAUT\n" +
			"DjAyODcyMzY5MDAwMTEwMR4wHAYDVQQPExVOb24tQ29tbWVyY2lhbCBFbnRpdHkx\n" +
			"EzARBgsrBgEEAYI3PAIBAxMCQlIxNDAyBgoJkiaJk/IsZAEBEyRkNTk4MzNhOC0w\n" +
			"NDcxLTQyZTItYjI4Ni0xOWQ4N2VhOGNkMGUwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n" +
			"DwAwggEKAoIBAQCvN19iQA2x7Ss9mBtlBlr1cX36aT2fDL0aoZlGSV7nULbBf8RN\n" +
			"fK1fM/jqMYIN7mnLYEkHvGqAJ4/cdwEyqcfGDmSPTwdHFJ0BpZt2AohJVgTTAaSF\n" +
			"NATY1kazkUTkIV19LLo6ql57Ne/pyP7XCcTZzfwOkvA2pEwjc2Sr3kvdLepCcRYJ\n" +
			"SZwloZlQGbIiI2jsSbbPu+zZ3zCUg8k/Ll+o43KcK1qVA+XzWaXWXXJb+AS4Ax3c\n" +
			"FfoilKNOLeiVL7VYnmTM9VsKy7aYuJwwthQciaSb4xz3YYE0qo9UEwopSqW3ZQYA\n" +
			"+u2YgMKQ/wclaP8dXpYNsaaAFwMDD/H2PXqxAgMBAAGjggLbMIIC1zAMBgNVHRMB\n" +
			"Af8EAjAAMB0GA1UdDgQWBBQTnUUMqIuv9scXhRzTOgj7dG3GXTAfBgNVHSMEGDAW\n" +
			"gBSGf1itF/WCtk60BbP7sM4RQ99MvjBMBggrBgEFBQcBAQRAMD4wPAYIKwYBBQUH\n" +
			"MAGGMGh0dHA6Ly9vY3NwLnNhbmRib3gucGtpLm9wZW5iYW5raW5nYnJhc2lsLm9y\n" +
			"Zy5icjBLBgNVHR8ERDBCMECgPqA8hjpodHRwOi8vY3JsLnNhbmRib3gucGtpLm9w\n" +
			"ZW5iYW5raW5nYnJhc2lsLm9yZy5ici9pc3N1ZXIuY3JsMCIGA1UdEQQbMBmCF21v\n" +
			"Y2stZGFkb3MyLnJhaWRpYW0uY29tMA4GA1UdDwEB/wQEAwIFoDATBgNVHSUEDDAK\n" +
			"BggrBgEFBQcDAjCCAaEGA1UdIASCAZgwggGUMIIBkAYKKwYBBAGDui9kATCCAYAw\n" +
			"ggE2BggrBgEFBQcCAjCCASgMggEkVGhpcyBDZXJ0aWZpY2F0ZSBpcyBzb2xlbHkg\n" +
			"Zm9yIHVzZSB3aXRoIFJhaWRpYW0gU2VydmljZXMgTGltaXRlZCBhbmQgb3RoZXIg\n" +
			"cGFydGljaXBhdGluZyBvcmdhbmlzYXRpb25zIHVzaW5nIFJhaWRpYW0gU2Vydmlj\n" +
			"ZXMgTGltaXRlZHMgVHJ1c3QgRnJhbWV3b3JrIFNlcnZpY2VzLiBJdHMgcmVjZWlw\n" +
			"dCwgcG9zc2Vzc2lvbiBvciB1c2UgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0\n" +
			"aGUgUmFpZGlhbSBTZXJ2aWNlcyBMdGQgQ2VydGljaWNhdGUgUG9saWN5IGFuZCBy\n" +
			"ZWxhdGVkIGRvY3VtZW50cyB0aGVyZWluLjBEBggrBgEFBQcCARY4aHR0cDovL2Nw\n" +
			"cy5zYW5kYm94LnBraS5vcGVuYmFua2luZ2JyYXNpbC5vcmcuYnIvcG9saWNpZXMw\n" +
			"DQYJKoZIhvcNAQELBQADggEBAJaxIDiLdiXIEBsjieGNYBEEGLunHPi8BPCPRmyO\n" +
			"ZPxail6P1UdqAdhwjhrX0/xkg6tcynEMAgCDtssChQ0hwcake2pwiEad7IgbaVzf\n" +
			"6GDqpLj4gQjTS4yGH/kawAvJYSAECq/2jOHx2iX+njbOv/EgNSYC5Q4h8UBaeXlx\n" +
			"FF/WDSVieZALk1fUJIAxCkkdyKnvBRpdU6jsI2CPOhhBIpr3NB7oZdSGVj11g+Th\n" +
			"cgaNDsn902C729E7wc6CAV82Bu0h1/GCwT4qhkcT1hCUjdM9uTYaw9p/OgEjPYRi\n" +
			"IUukTbnUAQluhKyEKOBqxQkx1V1lzSmWCgMJ/IxONsOLx3M=\n" +
			"-----END CERTIFICATE-----";
	}

	@Override
	String clientKey() {
		return "-----BEGIN PRIVATE KEY-----\n" +
			"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCvN19iQA2x7Ss9\n" +
			"mBtlBlr1cX36aT2fDL0aoZlGSV7nULbBf8RNfK1fM/jqMYIN7mnLYEkHvGqAJ4/c\n" +
			"dwEyqcfGDmSPTwdHFJ0BpZt2AohJVgTTAaSFNATY1kazkUTkIV19LLo6ql57Ne/p\n" +
			"yP7XCcTZzfwOkvA2pEwjc2Sr3kvdLepCcRYJSZwloZlQGbIiI2jsSbbPu+zZ3zCU\n" +
			"g8k/Ll+o43KcK1qVA+XzWaXWXXJb+AS4Ax3cFfoilKNOLeiVL7VYnmTM9VsKy7aY\n" +
			"uJwwthQciaSb4xz3YYE0qo9UEwopSqW3ZQYA+u2YgMKQ/wclaP8dXpYNsaaAFwMD\n" +
			"D/H2PXqxAgMBAAECgf9EGcF47mxkO0oTJot/OHkh7HqKWV0Z9FmLgEOSjfHU9goS\n" +
			"KxahB1RbLFgSritePsvfwAyidRjvtnCzv403o8WBtRZgnNa3Scu8WCGUPGe8GTLG\n" +
			"0FyhaRZ5MM9J+6xK/qzXegrnwk/2k4Ar3XMvIRzhiq3s/C9hDXqKTM9T/ZJ+5DOj\n" +
			"JWlFR7ta7hgJnVE8zgF7d+eEr07pWG10XexEI5Y9v5NZFyi/yCzNwlfyEsfuaSYX\n" +
			"fVeUqlxy0H36APYHoX6WFQAgMtiIxwdiF0dVkhNkX/zY6rBYZUN1gBBIPZqSD90b\n" +
			"8wyL2e1e1G1fOigniIOEeZ6RivUmkHI95M5ZhwECgYEAugZk7UsdedbS9p1eIJlf\n" +
			"x+pQimkEyR3FIsdaIiH0tureqOYbiQqnTUarvqgq+mH9VcVjIm9IyS2tmzn1pTxl\n" +
			"Z5c2UjZe2RCZ3zObOz4R55qzs3EVTmLro0eoNmAFCx3WodQEzeUhyJtYitDRoj+R\n" +
			"C7GmQmUyexrAkTHivUrHl5ECgYEA8SAjMKz0z6C45a0M/LBUXqlGpqWFaj2i1G01\n" +
			"i8G6rjI4t3QGy4VeE/OaOkWRFOjDKScDNSIrQBnB4LzAyQyngfDOw5TKCb1qSDAQ\n" +
			"GRmfQnj1IDF7YdhslPkx4ylNUyYKsmcSoFIiigvu2VJu7ZBzLS4AA8mG0iJjD0LL\n" +
			"UFhFYSECgYEAlzmtgI5o5uxx0dP6eivVBnMAiJR5NHnEEdLX/hl2k+D5o3p09gI4\n" +
			"IYULB1I0phlU1g5B2fN3yzwb/q2ueWZQ1VbC7TCQCIwLWrzcO9vouEw2O/D5LiES\n" +
			"iEkvgQyr7O6T1jjCsxLLVQacgzTgxrJAWq5Ph3g3Aoar9x0Dw3hwK1ECgYEAlJ5k\n" +
			"UjooYlkD3bx9xrnAJuBbBZf71XHMDPaLDGsJ7xhRUPGYxpSbWdzJ8KS0OZkFwTWk\n" +
			"inlXAxldIHH2uXStQi/0oSPyK3KuMXQ+V9otCUGYhJE7JSHKO0Ak+YiZemh2K2PQ\n" +
			"XTSCyWd1fkmKya5A71Xs+GicSqSbPerDefAOQ0ECgYAOHc2BNk0JFTfUed3PXKiL\n" +
			"mp7fcC3nJ8EXzW4YsraCAuUrFMHPJuOUQ/a38BGs8Y9FaDXWksfUd/6mMLOEE8mI\n" +
			"xNnsCSRJbTAy8TymHt0IednExyFGEpddqp1NGxjcLMClW4dIGyEhUH32yQFMWZZr\n" +
			"5AnuprVkceyv69zO7zTviA==\n" +
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
			"      \"alg\": \"PS256\",\n" +
			"      \"kid\": \"\",\n" +
			"      \"n\": \"yVQ03Cl2kb_fm6sKrSB_7glTZcRQDfg2nd8M6apDdCQD_zuxLdvhRPF11boYL7zzeus1dWOAOt3LVXIMeuoj2ewaF_jSSw7pIqoOUnA-y5vWoMTMojKJ4fxWv8p36KnJpVWX6_PsLad7BuXxFR1oZbaTATBRIC2ApasWHLlDJsHl8QF7h1fPN5MG1eXjb60Rvi0MQJwIhgRfqEFZNXNTl9wfbLOEo5VSWvayVs5mMKHNy3G-oIb7HCWA035E7cjOxncTLVC54mcq1CIuQ94tEL_S_pugKJERjGKMHe_zB_HGufceez6aC40_kGYYhWDhjal4VGgwe-oLSVt4dg8xWQ\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"d\": \"Djotk9MT_4bAE9H3M-SLWB1kT8ctR19xwlaR27-nMhPCDR-BMWhLt8Gsfe3j-_OkyJMUAeU84rU06AK99HbUyFLtISH5km5Nvw3K2ZevCFgox2GunzFmPeOgvAipPgiSu0AuycxA2kvu35l-RFlK88iQuyzqx94PUSXIHK2G5Sm1r8MAD2FPVPC8_rfBf7uNdi8W3GlI2neF089sF6YwKEZuEQ_O9vaKGmJdp4Hr0FqbEOCnwJRI39ImcI-cLBJKzede6k7OzRSr6zB-ouoXxTgkGUkQ4oS_O_jPLthYyIqJe0CWzygHzrptX15bksykmAqSfU53MQ5utkOup4qKdQ\",\n" +
			"      \"p\": \"6Sd3QxdLlCeTbjK_jeiVToxF39aSADZVVGJiq1iV1ao-E6vJ_fnQKRDJtdwyCT3WmEe-9WhqhtXkeuHzoVETeULAXzto5wQq7JmKXCvflRNSH4dJS9nj8vHi_5ABp81HbvEcLaEfwfdcmSlPjmbIXadnT1aS13WiHTpjbvzJmnM\",\n" +
			"      \"q\": \"3Q5sa65xLFJNZavNagwz-WVfzK4BTtFnd_-nvW5QIznTnQYd90YCGIF7NzyXNGJXehXlTiVCicNdzscsbHbNexds_4R1SQwEdB98HwH6WthX015V5abIgLn4wNtgCIxsx5y1liRbf_dSp7WCIktPZwwCxLgh7t2GELe2nDhBlgM\",\n" +
			"      \"dp\": \"rju27SUobrPQgeZcraCkiwlaYjbH7m6r_55yW9ecSQqqzypujWQ1AasxAb1miu9yQWREOMJp8Q9AafAWj3O2GMUgW0pbT2AW766h7dU_hPjmQvdnd86BgRjv6Mll408NVcPKqfKhd-LxiOeNMz5t90bVdj2dCHQ4zJzIl7VCty8\",\n" +
			"      \"dq\": \"gJUIOeahS_-fp5k0hhANF2-BNwSsHBWwUtuxzX1iPetADSFmwtPFGk4OxwWGimD9szUTSc5gktPCOCogzAKa6ZOpwkNhGZlU6dr6jurOnpMjGv7PhiLqk_4ZFM1GYhKUn9OmyGvjkV7ihE0PIjWePTl-TsEwc8oFVqoWHW6Xr9M\",\n" +
			"      \"qi\": \"D7x1VV0d85Zl7-9niX9I8xKjOOqFw5TnNSZR4S_f37hhGeA5-Ozw4U4mt-Lp7kwBiUepNCbMWkUViMuOdXIFklergtdWMK2ZKQl2ZOEpZTScxkBk1j0tMstWs4HaSVQySijo9qa_mMkWD1tAAIKtwwf8C84HFo9rjskkL7qx7ng\"\n" +
			"    }\n" +
			"  ]\n" +
			"}";
	}

	@Override
	String role() {
		return "DADOS (different organization)";
	}

	@Override
	String directoryClientId() {
		return "H9OSA4g2hFEm0IXqstRjs";
	}
}
