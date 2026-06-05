package net.openid.conformance.authzen;

/**
 * Action Search paginated test base. Inherits the @VariantConfigurationFields
 * declaration for {@code pdp.search_action_endpoint}, the request-construction
 * sequence, the endpoint setter, and the response-processing chain from
 * {@link AbstractAuthzenPDPActionSearchTest}; replaces {@code performAuthzenApiFlow}
 * with the paginated loop shared by every search family
 * ({@link AbstractAuthzenPDPSearchTest#runPaginatedSearchLoop}).
 */
public abstract class AbstractAuthzenPDPPaginatedActionSearchTest extends AbstractAuthzenPDPActionSearchTest {

	@Override
	protected void performAuthzenApiFlow() {
		runPaginatedSearchLoop();
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		// Validation is performed inside the paginated loop.
	}
}
