package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import org.springframework.beans.factory.annotation.Value;

public class OverrideClientWithDadosClient extends AbstractOverrideClient {

	@Override
	String clientCert() {
		return "-----BEGIN CERTIFICATE-----\n" +
			"MIIG+zCCBeOgAwIBAgIUQM4mvQoLWC9dXaYyXeOKc7VbERkwDQYJKoZIhvcNAQEL\n" +
			"BQAwcTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
			"FTATBgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNB\n" +
			"TkRCT1ggSXNzdWluZyBDQSAtIEcxMB4XDTIxMTAyOTAxMjEwMFoXDTIyMTEyODAx\n" +
			"MjEwMFowggEeMQswCQYDVQQGEwJCUjELMAkGA1UECBMCUkoxETAPBgNVBAcTCEJP\n" +
			"VEFGT0dPMRwwGgYDVQQKExNPcGVuIEJhbmtpbmcgQnJhc2lsMS0wKwYDVQQLEyQ3\n" +
			"NGU5MjlkOS0zM2I2LTRkODUtOGJhNy1jMTQ2Yzg2N2E4MTcxHzAdBgNVBAMTFm1v\n" +
			"Y2stZGFkb3MucmFpZGlhbS5jb20xFjAUBgNVBAUTDTEzMzUzMjM2MDAxODkxHjAc\n" +
			"BgNVBA8TFU5vbi1Db21tZXJjaWFsIEVudGl0eTETMBEGCysGAQQBgjc8AgEDEwJC\n" +
			"UjE0MDIGCgmSJomT8ixkAQETJGJmNGZlNTg1LWE1NmYtNGM2OC04MmZlLWViMTI2\n" +
			"NTI2MTcyNzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIshLMj4zfgz\n" +
			"tSxtMRX54erZ+sna9Z5MH6FH3xwMKEQCId4+8mUMRbLYU87FJiUyXFdISiow6U5v\n" +
			"m0Hw8HmzbOCZbhhvzCsppR9BIck/LMTSWwtWJ25rb0mhqNE7H0fyjJ9YF7WL6kxE\n" +
			"OWXt0ZUSG2BJrdeNl+QiAxNhgAiQb6E3dBjPyJ2NdqfVi95iLEvxuce25anoHDR9\n" +
			"ePQ1FIj/KJ/VKu3Tmb3+sGldhkf5wy3jYhcXfJ+VGDA9XkNRVEMmwLMShYDGxrDe\n" +
			"Wl58GrrdD3bD0Xn0TQOYpJpKdofH1z6U2xaXMQiIttyx0Tj+4at3jWj+nvlFH/Pa\n" +
			"W11yq3XjmSUCAwEAAaOCAtowggLWMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFKYs\n" +
			"Wkr+hifrTbPi6MGZLc3vFAT+MB8GA1UdIwQYMBaAFIZ/WK0X9YK2TrQFs/uwzhFD\n" +
			"30y+MEwGCCsGAQUFBwEBBEAwPjA8BggrBgEFBQcwAYYwaHR0cDovL29jc3Auc2Fu\n" +
			"ZGJveC5wa2kub3BlbmJhbmtpbmdicmFzaWwub3JnLmJyMEsGA1UdHwREMEIwQKA+\n" +
			"oDyGOmh0dHA6Ly9jcmwuc2FuZGJveC5wa2kub3BlbmJhbmtpbmdicmFzaWwub3Jn\n" +
			"LmJyL2lzc3Vlci5jcmwwIQYDVR0RBBowGIIWbW9jay1kYWRvcy5yYWlkaWFtLmNv\n" +
			"bTAOBgNVHQ8BAf8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwggGhBgNVHSAE\n" +
			"ggGYMIIBlDCCAZAGCisGAQQBg7ovZAEwggGAMIIBNgYIKwYBBQUHAgIwggEoDIIB\n" +
			"JFRoaXMgQ2VydGlmaWNhdGUgaXMgc29sZWx5IGZvciB1c2Ugd2l0aCBSYWlkaWFt\n" +
			"IFNlcnZpY2VzIExpbWl0ZWQgYW5kIG90aGVyIHBhcnRpY2lwYXRpbmcgb3JnYW5p\n" +
			"c2F0aW9ucyB1c2luZyBSYWlkaWFtIFNlcnZpY2VzIExpbWl0ZWRzIFRydXN0IEZy\n" +
			"YW1ld29yayBTZXJ2aWNlcy4gSXRzIHJlY2VpcHQsIHBvc3Nlc3Npb24gb3IgdXNl\n" +
			"IGNvbnN0aXR1dGVzIGFjY2VwdGFuY2Ugb2YgdGhlIFJhaWRpYW0gU2VydmljZXMg\n" +
			"THRkIENlcnRpY2ljYXRlIFBvbGljeSBhbmQgcmVsYXRlZCBkb2N1bWVudHMgdGhl\n" +
			"cmVpbi4wRAYIKwYBBQUHAgEWOGh0dHA6Ly9jcHMuc2FuZGJveC5wa2kub3BlbmJh\n" +
			"bmtpbmdicmFzaWwub3JnLmJyL3BvbGljaWVzMA0GCSqGSIb3DQEBCwUAA4IBAQCm\n" +
			"KlXr6U6L1UsjD+JT9CysnkzXTO8I9ScqhL8DlS/lX07s3v0caOy6exoVFm8HSB8M\n" +
			"5A9QSKQpPr26O/aTiT9CK+m4c36jzSHFfrDqOxvGxW89Pcw8Hll2Ymm+c5Pba+sg\n" +
			"fL+6GVi7RdvrH+LTZo8aNzEYOhUF/HH2dacdnwzGTMXiiNj/EVGKCnOHe3Kv0OTu\n" +
			"GqVD98sqi1CnnsZL5yLGzBsUgLyDYEX7O5sfCOox/cq/XUaBjLskfmxH08GvZ7aH\n" +
			"++7l+8Kwo/jM8Qo6HuICnH1NTnuZ6trmoXI9beaqFfs+R8N/Dq3b/JVIaNt/XQz/\n" +
			"2XMI5+j/a1gUhzSAMLkO\n" +
			"-----END CERTIFICATE-----";
	}
	@Override
	String clientKey() {
		return "-----BEGIN PRIVATE KEY-----\n" +
			"MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCLISzI+M34M7Us\n" +
			"bTEV+eHq2frJ2vWeTB+hR98cDChEAiHePvJlDEWy2FPOxSYlMlxXSEoqMOlOb5tB\n" +
			"8PB5s2zgmW4Yb8wrKaUfQSHJPyzE0lsLVidua29JoajROx9H8oyfWBe1i+pMRDll\n" +
			"7dGVEhtgSa3XjZfkIgMTYYAIkG+hN3QYz8idjXan1YveYixL8bnHtuWp6Bw0fXj0\n" +
			"NRSI/yif1Srt05m9/rBpXYZH+cMt42IXF3yflRgwPV5DUVRDJsCzEoWAxsaw3lpe\n" +
			"fBq63Q92w9F59E0DmKSaSnaHx9c+lNsWlzEIiLbcsdE4/uGrd41o/p75RR/z2ltd\n" +
			"cqt145klAgMBAAECgf9G7CJoUGyuma1BeGgNS9ReY7/0JioPB78X+AtDwO7FvFCH\n" +
			"eo8V86uT/7K5NMa9L27jD3a/Cy/jTAFZG/l6EHzwAUaeLl9loVULGaifbhTkQFco\n" +
			"8EIph5qzbjUgc4L+qiYFFGMBqNFHD4Ay9Yhg9Vn/O4/i0cscADqRJqKduwMsqtea\n" +
			"C61PV2KE2WAIK8xRsmewzBEHie8qv4L01qlShwPj0RRacvNlzACBkIjubtUlEgAA\n" +
			"Ln1D1sE3MtDM28wDrBh7pkkbKZpcCode3/YbZX0IhKMHK/O9DDodQkRnMClMKzVn\n" +
			"EQqWBIwEVhvvNr2h5ODPa6rIIZ+B5Oefna/oDz8CgYEAu+ArzmmAtxk84sK/wUQe\n" +
			"LFlSGFX+r3XLhdRHbsppjBKieHsZYq7akSDJjmIUxb+mJ7HwGIBABR6hKT+7cqvY\n" +
			"O/ewWpdtBRXG78/yYZaDlvT308PHhxhaD8gRF4obMZQJ/ckndFBWJ5S2Jj25IhTm\n" +
			"ztD2hgHjypoWQkErBdkDGxsCgYEAvZQUOj3SfxhvrEipng24oUTNP3Icf/VDvr0o\n" +
			"RFNNWQkdLbNoNWXzRudYDncfUpyHfPQALUzJwY1JwUbsATov5YfmDfcvK35dRnBS\n" +
			"JRH+EKzw7e16THh8cTFkwrgfR4iL1VDR7nF1+h//WA88k8B6KhX5becZ6yW/Xbwd\n" +
			"15d9IL8CgYEAgsDqW/arQUMMT9kwMWaiBUWI0C0RWrYFljd+G4o92OGjN2yc50RA\n" +
			"E18ZR8HVKSdPkTLSoYqog6ekjBux7oXQsTOaBgJ7Ol3EnWRBEU/6dxY4YKZR+CCn\n" +
			"lXItCw/wCxo3tevITLgYreVeu9CbRy/6zpc561vnWDrTIPtP4Pr+oV8CgYEAkVof\n" +
			"yCfuZJQPy0BLDGvoy309ARA02cbrEitl3D0iaMmcHZYYqBhq1ko6MZ0T3vs0xpEu\n" +
			"MY1Wg5gYnKAMA0ThxFLWCI8lE2vniEJGOTX6Xj662zHhOxCGGSON5d3V3brLu9oj\n" +
			"m3ZSxlwSIypLf91rzSjb/W/bZ3ehqfS5zLrRrXECgYB3kAdftbUuThamcUOxfTlL\n" +
			"ltIasKckYUuT9ZOIX8MDw7e9v3Aew1v5jfMJtI4UwcjNdcYVD3zMiGbL2BH6B6+x\n" +
			"LbRnFyWJhNbAo8ZdHpf4gda105ojwkkNouE4PjMtD0Xnu2/sJcVIOKU5ux4sojd9\n" +
			"hE7WxukcmzDoebd7tRAFoA==\n" +
			"-----END PRIVATE KEY-----\n";
	}

	@Override
	String role() {
		return "DADOS";
	}

	@Override
	String directoryClientId() {
		return "rK3VIPBPDKToTv-jR1tvz";
	}
}
