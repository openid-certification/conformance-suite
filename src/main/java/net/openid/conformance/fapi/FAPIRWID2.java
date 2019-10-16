package net.openid.conformance.fapi;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
		testName = "fapi-rw-id2",
		displayName = "FAPI-RW-ID2",
		summary = "This test uses two different OAuth clients, authenticates the user twice (using different variations on request object, registered redirect uri etc)",
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
public class FAPIRWID2 extends AbstractFAPIRWID2ServerTestModule {

}
