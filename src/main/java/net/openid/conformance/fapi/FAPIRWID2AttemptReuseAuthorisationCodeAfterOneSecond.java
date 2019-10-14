package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ServerAllowedReusingAuthorisationCode;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.Variant;
import org.apache.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-rw-id2-attempt-reuse-authorisation-code-after-one-second",
	displayName = "FAPI-RW-ID2: try to reuse authorisation code after one second",
	summary = "This test tries reusing an authorization code after one second and expects AS return an error",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
public class FAPIRWID2AttemptReuseAuthorisationCodeAfterOneSecond extends AbstractFAPIRWID2AttemptReuseAuthorisationCode {

	@Variant(name = variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
	}

	@Variant(name = variant_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
	}

	@Variant(name = variant_mtls_jarm)
	public void setupMTLSJarm() {
		super.setupMTLSJarm();
	}

	@Variant(name = variant_privatekeyjwt_jarm)
	public void setupPrivateKeyJwtJarm() {
		super.setupPrivateKeyJwtJarm();
	}

	@Variant(
		name = variant_openbankinguk_mtls,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Variant(
		name = variant_openbankinguk_privatekeyjwt,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	@Override
	protected void waitForAmountOfTime() {
		callAndStopOnFailure(WaitForOneSecond.class);
	}

	@Override
	protected void verifyError() {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");
		if (httpStatus == HttpStatus.SC_OK) {
			callAndContinueOnFailure(ServerAllowedReusingAuthorisationCode.class, Condition.ConditionResult.WARNING);
		} else {
			super.verifyError();
		}
	}
}
