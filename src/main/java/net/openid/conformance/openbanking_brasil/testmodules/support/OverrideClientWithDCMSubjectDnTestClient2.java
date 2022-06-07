package net.openid.conformance.openbanking_brasil.testmodules.support;

public class OverrideClientWithDCMSubjectDnTestClient2 extends AbstractOverrideClient {

	@Override
	String clientCert() {
		return "-----BEGIN CERTIFICATE-----\n" +
			"MIIG/zCCBeegAwIBAgIULuafdShY18kkOWfG80rAuZmh8+MwDQYJKoZIhvcNAQEL\n" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\n" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIyMDUyNDIyNTIwMFoXDTIzMDYyMzIy\n" +
			"NTIwMFowggEZMQswCQYDVQQGEwJCUjELMAkGA1UECBMCU1AxDzANBgNVBAcTBkxv\n" +
			"bmRvbjEcMBoGA1UEChMTT3BlbiBCYW5raW5nIEJyYXNpbDEtMCsGA1UECxMkNzRl\n" +
			"OTI5ZDktMzNiNi00ZDg1LThiYTctYzE0NmM4NjdhODE3MSEwHwYDVQQDExhodHRw\n" +
			"czovL2RvbWFpbnR3by5jb20uYnIxFzAVBgNVBAUTDjQzMTQyNjY2MDAwMTk3MRgw\n" +
			"FgYDVQQPEw9CdXNpbmVzcyBFbnRpdHkxEzARBgsrBgEEAYI3PAIBAxMCVUsxNDAy\n" +
			"BgoJkiaJk/IsZAEBEyQ5N2U5MDE2OS05NTQzLTRlYzUtYjQ1MC01MjRhMmQ0NzU2\n" +
			"YjQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDGlGsY1mcZFoXl4Q4v\n" +
			"PkD3V41gxGmlbqqxgkh/6Qw+w7ZSbGLRDUtOD0JLYDtaAt1z0XeBHjDm1YeT3G9f\n" +
			"2b8VOnpdQzqNG68Q+lg5bDsu4ys+PRKj+R74OrowYkmUWZ0JrrxaE/gyNgHV4Hwf\n" +
			"uclfmIlFlBfPW9YGexikrI68keN+7iE8pbAWU9PT+Igv+w+mpcw/yp2ai2JIZVSh\n" +
			"y8ir+5oG9FLeRJLsO7AJrPgIMchbHNQ7BfyL8GEEV32mvIPUNZ5VwJGr3XCAr5Qj\n" +
			"UCzZW6Oz2UUN0Pvpw8D2efyeRuVCpJhrLOTqVfKFTJVG4kRX42677vfiyagBKRs2\n" +
			"bBoHAgMBAAGjggLjMIIC3zAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBRL2xnthKGh\n" +
			"yZzxtPYGQwqmKB9XrzAfBgNVHSMEGDAWgBSGf1itF/WCtk60BbP7sM4RQ99MvjBM\n" +
			"BggrBgEFBQcBAQRAMD4wPAYIKwYBBQUHMAGGMGh0dHA6Ly9vY3NwLnNhbmRib3gu\n" +
			"cGtpLm9wZW5iYW5raW5nYnJhc2lsLm9yZy5icjBLBgNVHR8ERDBCMECgPqA8hjpo\n" +
			"dHRwOi8vY3JsLnNhbmRib3gucGtpLm9wZW5iYW5raW5nYnJhc2lsLm9yZy5ici9p\n" +
			"c3N1ZXIuY3JsMCMGA1UdEQQcMBqCGGh0dHBzOi8vZG9tYWludHdvLmNvbS5icjAO\n" +
			"BgNVHQ8BAf8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwggGoBgNVHSAEggGf\n" +
			"MIIBmzCCAZcGCisGAQQBg7ovZAEwggGHMIIBNgYIKwYBBQUHAgIwggEoDIIBJFRo\n" +
			"aXMgQ2VydGlmaWNhdGUgaXMgc29sZWx5IGZvciB1c2Ugd2l0aCBSYWlkaWFtIFNl\n" +
			"cnZpY2VzIExpbWl0ZWQgYW5kIG90aGVyIHBhcnRpY2lwYXRpbmcgb3JnYW5pc2F0\n" +
			"aW9ucyB1c2luZyBSYWlkaWFtIFNlcnZpY2VzIExpbWl0ZWRzIFRydXN0IEZyYW1l\n" +
			"d29yayBTZXJ2aWNlcy4gSXRzIHJlY2VpcHQsIHBvc3Nlc3Npb24gb3IgdXNlIGNv\n" +
			"bnN0aXR1dGVzIGFjY2VwdGFuY2Ugb2YgdGhlIFJhaWRpYW0gU2VydmljZXMgTHRk\n" +
			"IENlcnRpY2ljYXRlIFBvbGljeSBhbmQgcmVsYXRlZCBkb2N1bWVudHMgdGhlcmVp\n" +
			"bi4wSwYIKwYBBQUHAgEWP2h0dHA6Ly9yZXBvc2l0b3J5LnNhbmRib3gucGtpLm9w\n" +
			"ZW5iYW5raW5nYnJhc2lsLm9yZy5ici9wb2xpY2llczANBgkqhkiG9w0BAQsFAAOC\n" +
			"AQEAuAZxr7ytJgcejnjkbKetBWAbV37gzjRDHm1eTSV4qspsEsZ8qTyF083320sx\n" +
			"EToyfJosYH8coOkoZ5jmBGFfx0bIhyA9EShiU6zaFsRKsgUxbQGFnhXG7vkKoSSZ\n" +
			"gtXezEgAF9Ry/2ifSfIzCV0N4Vt2TFKk6yaSa8CLMCHYyHMhGKdJxqXPZYrcNDXh\n" +
			"5I683WCw2l+H7h2o27tKXehjhDt/snkZaTBuUgzJhOFut/L3uPuDZMmy7LSvT3gp\n" +
			"F22CpL1keUAuZP56ScQt2CZ0VuqYMdyf9FYzTthKAHOI2sgb9BVj1rca1F6hT5hH\n" +
			"f3CAGxWT59uZEklHzbGNszaXlQ==\n" +
			"-----END CERTIFICATE-----";
	}
	@Override
	String clientKey() {
		return "-----BEGIN PRIVATE KEY-----\n" +
			"MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDGlGsY1mcZFoXl\n" +
			"4Q4vPkD3V41gxGmlbqqxgkh/6Qw+w7ZSbGLRDUtOD0JLYDtaAt1z0XeBHjDm1YeT\n" +
			"3G9f2b8VOnpdQzqNG68Q+lg5bDsu4ys+PRKj+R74OrowYkmUWZ0JrrxaE/gyNgHV\n" +
			"4HwfuclfmIlFlBfPW9YGexikrI68keN+7iE8pbAWU9PT+Igv+w+mpcw/yp2ai2JI\n" +
			"ZVShy8ir+5oG9FLeRJLsO7AJrPgIMchbHNQ7BfyL8GEEV32mvIPUNZ5VwJGr3XCA\n" +
			"r5QjUCzZW6Oz2UUN0Pvpw8D2efyeRuVCpJhrLOTqVfKFTJVG4kRX42677vfiyagB\n" +
			"KRs2bBoHAgMBAAECggEAYDF0IZ92aLI+4WjgKKV07fvk2du7O2581CkCuqdnDZ5b\n" +
			"mtizE4ZnlKQSnpW6mnMp2Bk/VPYNhTwphUgyUVGW3Q8rSDMxUTp4VvaWNxbFIoTb\n" +
			"pTYLag/a3y7/k0GBWBIC3rZcuhvTTmtX/0VTi0DBmgO3d1NyJzna1t/ZQ6ftKs17\n" +
			"gFaPY6kNB66864M86DP6B3pFbTKNla3afm8q/k0yl33Mhxd1p8eElsb4cogLN1ge\n" +
			"9TkDjnfpzItbE6FmAHx1NZETo61FCBRqN3p0DFWuzLj5dkx+nR/lxrWsf+i1SkMZ\n" +
			"tj50bgMiZUtXCfKrH3JJVMPb2qHmBIqDh3VlLd81/QKBgQDOMEAy9Sn/zuqRQaoH\n" +
			"46nIocGs6BYvze9NRkf2qkLD05lTrV7TFnPwvn6mYwy+xXYsEDQB9B7hrPP7m+77\n" +
			"3FmjMn/05MgLcD7v4/c0PF7zBUxvuzAYuSisH6g5iHd/HvP/YIzWQsvZUpd1Pf+7\n" +
			"eCYIRsaToHBhgsWRhYLAKgyy4wKBgQD2jZr8pKyDi/XGwKw1SMpc32IxpNHiTODZ\n" +
			"dU1Kkcu1RPPsqhHWkfV9lA9/REApI0oABh/xnxTflvpufOYUqbpUJH3KJG7mg0d9\n" +
			"oBj83gWS8nU/+/vGt34JnunsIHQkFdVANkUzdq3JNBGPj6CkW/Z6UdDWM1p8/IYH\n" +
			"VMlcH2mRjQKBgQC3L+M54tuHrNx2ZXKdQ8WqvRwHdMORgVNkwlZZbneW6D5HO7cN\n" +
			"r9sePwDi7wl5zJHrIBI00iFFu5WhK68uEV2KQumwODsK/pTMKSuOOzzYiRKaCiL3\n" +
			"NDpQgmfBhd81D9kUQUGfcwUNdfWecpDsjBiuLcNrkZWG7Np/KbE67aEOpQKBgBSS\n" +
			"JAhIRQyEtPDsSnH3rMqq6Kpzsf9LoscB/nzmRTTOl/t7BRn9+5mKrPBVZXUJOLdf\n" +
			"y3KBb2BoXGSzjw7SSnWSxdwDxiz9bw2QOgV/EL/98sJv58XgsWdkFuhYnNgV6kD9\n" +
			"RjDRChbrfTuZJDISajIyeVYl2rrq5tPdwx8oj8pRAoGAbMLlzbnCJl8d5s5EX2WU\n" +
			"gbxcKuzxjvzSvczeP1+l/MZpDZr2DLemnoyJyU7g8vfFNWaEzTeLCblDS3JqMy4H\n" +
			"ifjvebGDnswZFSV9It11ZQ7qxQxVUAKmYOChIZMZ7fOKY7bkpY7K85Ft9hglKknC\n" +
			"htaJm7oW3Bt9RcBt9r68ty0=\n" +
			"-----END PRIVATE KEY-----\n";
	}

	@Override
	String clientJwks() {
		// there's no separate key for this client, it uses the org jwks for auth
		return orgJwks;
	}

	@Override
	String role() {
		return "DCM client 2";
	}

	@Override
	String directoryClientId() {
		// can be extracted from cert
		return "97e90169-9543-4ec5-b450-524a2d4756b4";
	}
}
