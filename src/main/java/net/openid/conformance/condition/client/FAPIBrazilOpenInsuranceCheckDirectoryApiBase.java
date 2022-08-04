package net.openid.conformance.condition.client;

public class FAPIBrazilOpenInsuranceCheckDirectoryApiBase extends AbstractFAPIBrazilCheckDirectoryApiBase {

	@Override
	protected String getErrorMessage() {
		return "Testing for Brazil certification must be done using the Brazil directory.";
	}

	@Override
	String getExpectedUrl() {
		return "https://matls-api.sandbox.directory.opinbrasil.com.br/";
	}

}
