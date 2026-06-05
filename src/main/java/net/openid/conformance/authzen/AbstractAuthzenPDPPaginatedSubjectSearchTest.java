package net.openid.conformance.authzen;

/**
 * Subject Search paginated test base. Inherits the @VariantConfigurationFields
 * declaration for {@code pdp.search_subject_endpoint}, the request-construction
 * sequence, the endpoint setter, and the response-processing chain from
 * {@link AbstractAuthzenPDPSubjectSearchTest}; replaces {@code performAuthzenApiFlow}
 * with the paginated loop shared by every search family
 * ({@link AbstractAuthzenPDPSearchTest#runPaginatedSearchLoop}).
 */
public abstract class AbstractAuthzenPDPPaginatedSubjectSearchTest extends AbstractAuthzenPDPSubjectSearchTest {

	@Override
	protected void performAuthzenApiFlow() {
		runPaginatedSearchLoop();
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		// Validation is performed inside the paginated loop.
	}
}
