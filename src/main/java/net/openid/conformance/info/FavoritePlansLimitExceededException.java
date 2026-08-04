package net.openid.conformance.info;

/**
 * Thrown by {@link FavoritePlansService#addFavoritePlanForCurrentUser(String)} when the user
 * already holds {@link FavoritePlansService#MAX_FAVORITES_PER_USER} favorites. The API layer
 * translates it into a 400 rather than letting it surface as a 500 — the request is a legitimate
 * one that the server is declining, not a server fault.
 */
public class FavoritePlansLimitExceededException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FavoritePlansLimitExceededException(String message) {
		super(message);
	}

}
