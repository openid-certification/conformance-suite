package net.openid.conformance.security;

import com.google.common.base.Suppliers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.util.LoteBuilder;
import net.openid.conformance.util.LoteList;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Serves the suite's lists of trusted entities ({@link LoteBuilder}): {@code /lote/{slug}.jwt}
 * lists the suite CA for the role, {@code /lote/empty/{slug}.jwt} lists nobody. They let an
 * implementation under test, or the suite's own CI pairings, take the suite's trust anchors from
 * a list exactly as they would take them from a scheme operator's. Each list is memoised for an
 * hour so repeated fetches see the same sequence number and a NextUpdate well ahead.
 */
@Controller
@Tag(name = SwaggerConfig.TAG_SERVER)
public class LoteEndpoint {

	/** The media type the EUDI reference lists are served as. */
	public static final String CONTENT_TYPE = "application/jwt";

	private final Map<LoteList, Supplier<String>> lists = new EnumMap<>(LoteList.class);
	private final Map<LoteList, Supplier<String>> emptyLists = new EnumMap<>(LoteList.class);

	public LoteEndpoint() {
		for (LoteList list : LoteList.values()) {
			lists.put(list, Suppliers.memoizeWithExpiration(() -> LoteBuilder.build(list, false), 1, TimeUnit.HOURS));
			emptyLists.put(list, Suppliers.memoizeWithExpiration(() -> LoteBuilder.build(list, true), 1, TimeUnit.HOURS));
		}
	}

	// no produces: a fetcher sending a narrow Accept header must still get the list, not a 406
	@GetMapping(LoteBuilder.PATH_PREFIX + "{slug}.jwt")
	@ResponseBody
	@Operation(operationId = "lote",
		summary = "Returns one of the suite's lists of trusted entities",
		description = "Returns the ETSI TS 119 602 list of trusted entities (a compact JWS) that lists the suite's CA for the"
			+ " named role: pid-providers, wallet-providers, wrpac-providers or wrprc-providers. The sealing certificate"
			+ " is served at /trust-anchors/lote-signer.pem.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The signed list",
			content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(type = "string", description = "A compact JWS"))),
		@ApiResponse(responseCode = "404", description = "No list with that name", content = @Content)
	})
	// empty @SecurityRequirements clears the document-wide bearerAuth requirement: this endpoint is permitAll
	@SecurityRequirements
	public ResponseEntity<String> getList(
			@Parameter(description = "The list's name") @PathVariable("slug") String slug) {
		return respond(slug, lists);
	}

	// no produces, as above
	@GetMapping(LoteBuilder.EMPTY_PATH_PREFIX + "{slug}.jwt")
	@ResponseBody
	@Operation(operationId = "loteEmpty",
		summary = "Returns an empty variant of one of the suite's lists of trusted entities",
		description = "Returns a signed list of the named type that lists no trusted entity at all, for tests of what an"
			+ " implementation does when nothing is trusted.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The signed empty list",
			content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(type = "string", description = "A compact JWS"))),
		@ApiResponse(responseCode = "404", description = "No list with that name", content = @Content)
	})
	// empty @SecurityRequirements clears the document-wide bearerAuth requirement: this endpoint is permitAll
	@SecurityRequirements
	public ResponseEntity<String> getEmptyList(
			@Parameter(description = "The list's name") @PathVariable("slug") String slug) {
		return respond(slug, emptyLists);
	}

	private static ResponseEntity<String> respond(String slug, Map<LoteList, Supplier<String>> suppliers) {
		LoteList list = LoteList.bySlug(slug);
		if (list == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(CONTENT_TYPE))
			.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
			.body(suppliers.get(list).get());
	}
}
