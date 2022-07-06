package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractOverrideClient extends AbstractCondition {
    String orgJwks = "{ \"keys\": [ { \"p\": \"_QaFFuyZriEWtbiKexy7pIYicgU0ePMepvyNuiiFqlsUFQ24q9gp_iWECDhi8qqss__i1LW_eHQATKWfqUc6HdDY-ickJXvVktrQiT-buvJ24iTtK_NooPMKQW976NIX39_sEPJ6ceo9ZDFxTTCkmJus3eXDJmRZT2YzSZJcvcc\", \"kty\": \"RSA\", \"q\": \"3x1_dyovnWXSr7y7ufe6AHUV0kHBYVrGW2vdNZxKrrgBW5r2mm93NnHsrG-mJlzW7kG_jR41bWf74sucGdwXTx96riRVy8bov9SzPCDl9_QCHzPpqZOxjngfzQYq1L1qJA2BAie0Sq6YZhgLJ1fWPLutEO5soIYAkLXSJ9IVb00\", \"d\": \"ZvivfZxJMbbdTQmxyE0lmE6ZuH8cMQOLyZxdaA5pNwm7ZEnPBEftZs8aR9ijhCDWMieui3h--rXwlXqbEm3g1sVgQ-WKTFV-NLKaJC1h-EU5HKdlOflstr7x57zhKp60ZIK69GyEXyJccUfzcD32u8raec9NplQ2MqS5MA1lnQFlocFoX1RNU4tSpEdJQq2UzqtX5WPhc88A6fTc1xu2fA5wyxzZu7fUjIETzLimcu-dDaEvvgm7c_A1ulm8EQuCN10k3FrIIe9RfuXHyxh9Rcd0aiIP9qwitxd5Cl0io7zby8MBIAaSei2co7y4tciBt4AfnzpBlGbtjgfr2gxD0Q\", \"e\": \"AQAB\", \"alg\": \"PS256\", \"use\": \"sig\", \"kid\": \"X5d4vFYfLxaG1gg8_l3bFYFhUaUVmE6PaEsRWX2EYqM\", \"qi\": \"eHhOb5NUDfMGXwNscjEcMrMFS425qsoJAXfGJ10hm8svZOkNYgVpb7Hs7oT8XytanRl0Gk8gKH0yhvya65B5ipyby17uBMkikNN-EeYoY2AkvEfM_nO0dvHbDdF11rkM5Q_SEDlGmojxnx4_Euj8WeuSgcwiCQkR23aZlQGx1Mg\", \"dp\": \"bQ9aXj8tHnj0qO8aAWapGokWX78OlvNzytYg4JSGyJ7pUQnRB4Ds2Lai6kgjniUiu5MX2kdceDbHykG5R-WDj0Ztv6UPV3jA3cOjDwVzwmiwBVmVQNRxzK31Ra8f4YJs9_o0bjmVvXQRchY9l9_Xkk_Hev2F2A540Fhk0tlbUBE\", \"dq\": \"XPYDZ_kxwZjtQb-XUBLBcvNV1jcDhba2stysXGv0SfvsxOg6G3qZ5xtsiyQxzAYen0LRttCBXkZXEtXXAodLRvJMwUXuYWtNCrBqxYDHkJogUDPnBXq-Hig6x8fsDJunH8JooCc-3WcFpHQcIZZdcwyXPVi59eAfWCwJlgHYYHk\", \"n\": \"3IXVqA9zCrmaz30WWjzZexdSuhagP_oVfgB-Y5S7XXeHhU4tho7c3wcNhsrGCKLYSf_47rV78-2OfhPY9WrSJVyqIXoaniwoTnD9splF90J7yog9LgP4kqkWl6wm8gdR5C-UC4Gnl0D2cZKnE6MZ7k_b5nM0ZfC-7H_bO13B4aYpu069th7hFWGK7ps65uiDxcQGYP3oSeqahE5qwfUef2QMkhyVtM_nWP9OVAzOGtJ8km4TdCrq9aF78No9u2YQuaUOJgeOnwKMBjmgEwcuaQe9DEQFXhzBtRmPlml2pTy2QDvxe1HgOTOsnh9igFvX70AGroY3PFY-lJAxC4Fd2w\" } ] }";
    String sandboxCa = "-----BEGIN CERTIFICATE-----\n" +
        "MIIEajCCA1KgAwIBAgIUdIYzEFdw7QJcrySyq6IiEwZfTfAwDQYJKoZIhvcNAQEL\n" +
        "BQAwazELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
        "FTATBgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJv\n" +
        "b3QgU0FOREJPWCAtIEcxMB4XDTIwMTIxMTEwMDAwMFoXDTIzMTIxMTEwMDAwMFow\n" +
        "cTELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwxFTAT\n" +
        "BgNVBAsTDE9wZW4gQmFua2luZzEtMCsGA1UEAxMkT3BlbiBCYW5raW5nIFNBTkRC\n" +
        "T1ggSXNzdWluZyBDQSAtIEcxMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\n" +
        "AQEA6fX+272mHX5QAcDaWkVHFWjnDIcORNUJU3OuNyeuOYhlvXJWydrXe3O+cV+P\n" +
        "S39faMj/nfem3GfJBE7Xn0bWA/8ksxSfrg1BUBJDge4YBBw+VflI3A0g1fk9wJ3H\n" +
        "GInsvV4serRJ/ISJTfs0uRNugX+RrbkT/T0tup4vGd3Kl2sbwUdDjokuJNJHANeO\n" +
        "DRkQ+ra+9Wht71FBlc07yPf7qtpaWHm6aS3s47OJD35ixkG4xiZuHsScxcVtlo1V\n" +
        "W98P2cQfH9H2lll4wWlPTVHpPThB2EYrPhwcxDh8kHkkOHNkyHO/fYM47u7H4VeQ\n" +
        "V75LXWKa7iWmZg+WhFb8TXSr/wIDAQABo4H/MIH8MA4GA1UdDwEB/wQEAwIBBjAP\n" +
        "BgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBSGf1itF/WCtk60BbP7sM4RQ99MvjAf\n" +
        "BgNVHSMEGDAWgBSHE+yWPmLsIRwMSlY68iUM45TpyzBMBggrBgEFBQcBAQRAMD4w\n" +
        "PAYIKwYBBQUHMAGGMGh0dHA6Ly9vY3NwLnNhbmRib3gucGtpLm9wZW5iYW5raW5n\n" +
        "YnJhc2lsLm9yZy5icjBLBgNVHR8ERDBCMECgPqA8hjpodHRwOi8vY3JsLnNhbmRi\n" +
        "b3gucGtpLm9wZW5iYW5raW5nYnJhc2lsLm9yZy5ici9pc3N1ZXIuY3JsMA0GCSqG\n" +
        "SIb3DQEBCwUAA4IBAQBy4928pVPeiHItbneeOAsDoc4Obv5Q4tn0QpqTlSeCSBbH\n" +
        "IURfEr/WaS8sv0JTbIPQEfiO/UtaN8Qxh7j5iVqTwTwgVaE/vDkHxGOen5YxAuyV\n" +
        "1Fpm4W4oQyybiA6puHEBcteuiYZHppGSMus3bmFYTPE+9B0+W914VZeHDujJ2Y3Y\n" +
        "Mc32Q+PC+Zmv8RfaXp7+QCNYSXR5Ts3q3IesWGmlvAM5tLQi75JmzdWXJ1uKU4u3\n" +
        "Nrw5jY4UaOlvB5Re2BSmcjxdLT/5pApzkS+tO6lICnPAtk/Y6dOJ0YxQBMImtliY\n" +
        "p02yfwRaqP8WJ4CnwUHil3ZRt8U9I+psU8b4WV/3\n" +
        "-----END CERTIFICATE-----\n" +
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDpjCCAo6gAwIBAgIUS3mWeRx1uG/SMl/ql55VwRtNz7wwDQYJKoZIhvcNAQEL\n" +
        "BQAwazELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwx\n" +
        "FTATBgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJv\n" +
        "b3QgU0FOREJPWCAtIEcxMB4XDTIwMTIxMTEwMDAwMFoXDTI1MTIxMDEwMDAwMFow\n" +
        "azELMAkGA1UEBhMCQlIxHDAaBgNVBAoTE09wZW4gQmFua2luZyBCcmFzaWwxFTAT\n" +
        "BgNVBAsTDE9wZW4gQmFua2luZzEnMCUGA1UEAxMeT3BlbiBCYW5raW5nIFJvb3Qg\n" +
        "U0FOREJPWCAtIEcxMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp50j\n" +
        "jNh0wu8ioziC1HuWqOfgXwxeiePiRGw5tKDqKIbC7XV1ghEcDiymTHHWWJSQ1LEs\n" +
        "mYpZVwaos5Mrz2xJwytg8K5eqFqa7QvfOOul29bnzEFk+1gX/0nOYws3Lba9E7S+\n" +
        "uPaUmfElF4r2lcCNL2f3F87RozqZf+DQBdGUzAt9n+ipY1JpqfI3KF/5qgRkPoIf\n" +
        "JD+aj2Y1D6eYjs5uMRLU8FMYt0CCfv/Ak6mq4Y9/7CaMKp5qjlrrDux00IDpxoXG\n" +
        "Kx5cK0KgACb2UBZ98oDQxcGrbRIyp8VGmv68BkEQcm7NljP863uBVxtnVTpRwQ1x\n" +
        "wYEbmSSyoonXy575wQIDAQABo0IwQDAOBgNVHQ8BAf8EBAMCAQYwDwYDVR0TAQH/\n" +
        "BAUwAwEB/zAdBgNVHQ4EFgQUhxPslj5i7CEcDEpWOvIlDOOU6cswDQYJKoZIhvcN\n" +
        "AQELBQADggEBAFoYqwoH7zvr4v0SQ/hWx/bWFRIcV/Rf6rEWGyT/moVAEjPbGH6t\n" +
        "yHhbxh3RdGcPY7Pzn797lXDGRu0pHv+GAHUA1v1PewCp0IHYukmN5D8+Qumem6by\n" +
        "HyONyUASMlY0lUOzx9mHVBMuj6u6kvn9xjL6xsPS+Cglv/3SUXUR0mMCYf963xnF\n" +
        "BIRLTRlbykgJomUptVl/F5U/+8cD+lB/fcZPoQVI0kK0VV51jAODSIhS6vqzQzH4\n" +
        "cpUmcPh4dy+7RzdTTktxOTXTqAy9/Yx+fk18O9qSQw1MKa9dDZ4YLnAQS2fJJqIE\n" +
        "1DXIta0LpqM4pMoRMXvp9SLU0atVZLEu6Sc=\n" +
        "-----END CERTIFICATE-----\n";

	abstract String clientCert();

	abstract String clientKey();

	abstract String role();

	abstract String directoryClientId();

	abstract String clientJwks();

	String orgJwks() {
		return orgJwks;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject client = (JsonObject) env.getElementFromObject("config", "client");
		if (client == null) {
			client = new JsonObject();
			env.putObject("config", "client", client);
		}
		client.add("jwks", new JsonParser().parse(clientJwks()).getAsJsonObject());
		client.add("org_jwks", new JsonParser().parse(orgJwks()).getAsJsonObject());

		JsonObject directory = (JsonObject) env.getElementFromObject("config", "directory");
		if (directory == null) {
			directory = new JsonObject();
			env.putObject("config", "directory", directory);
		}
		directory.addProperty("client_id", directoryClientId());

		JsonObject mtls = (JsonObject) env.getElementFromObject("config", "mtls");
		if (mtls == null) {
			mtls = new JsonObject();
			env.putObject("config", "mtls", mtls);
		}
		mtls.addProperty("cert", clientCert());
		mtls.addProperty("key", clientKey());
		mtls.addProperty("ca", sandboxCa);

		log("Hardcoded client with "+role()+" role loaded into config",
			env.getObject("config"));

		return env;
	}
}
