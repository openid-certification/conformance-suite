package net.openid.conformance.info;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class FavoritePlansApi_UnitTest {

	private FavoritePlansService service;
	private FavoritePlansApi api;

	@BeforeEach
	public void setUp() {
		service = Mockito.mock(FavoritePlansService.class);
		api = new FavoritePlansApi();
		ReflectionTestUtils.setField(api, "favoritePlansService", service);
	}

	private static List<String> planNames(JsonObject body) {
		JsonArray plans = body.getAsJsonArray("plans");
		return plans.asList().stream().map(OIDFJSON::getString).toList();
	}

	@Test
	public void getWithNoFavoritesReturnsEmptyPlansArray() {
		Mockito.when(service.getFavoritePlansForCurrentUser()).thenReturn(List.of());

		ResponseEntity<Object> response = api.getFavoritePlans();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(JsonObject.class);
		JsonObject body = (JsonObject) response.getBody();
		assertThat(body.has("plans")).isTrue();
		assertThat(planNames(body)).isEmpty();
	}

	@Test
	public void getReturnsServiceListInOrder() {
		Mockito.when(service.getFavoritePlansForCurrentUser())
			.thenReturn(List.of("plan-a", "plan-b", "plan-c"));

		ResponseEntity<Object> response = api.getFavoritePlans();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(planNames((JsonObject) response.getBody()))
			.containsExactly("plan-a", "plan-b", "plan-c");
	}

	@Test
	public void postAddsPlanAndReturnsUpdatedList() {
		Mockito.when(service.addFavoritePlanForCurrentUser("plan-b"))
			.thenReturn(List.of("plan-a", "plan-b"));

		JsonObject request = new JsonObject();
		request.addProperty("plan", "plan-b");

		ResponseEntity<Object> response = api.addFavoritePlan(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(planNames((JsonObject) response.getBody()))
			.containsExactly("plan-a", "plan-b");
		Mockito.verify(service).addFavoritePlanForCurrentUser("plan-b");
	}

	@Test
	public void postWithMissingPlanReturnsBadRequest() {
		ResponseEntity<Object> response = api.addFavoritePlan(new JsonObject());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		Mockito.verifyNoInteractions(service);
	}

	@Test
	public void postWithBlankPlanReturnsBadRequest() {
		JsonObject request = new JsonObject();
		request.addProperty("plan", "   ");

		ResponseEntity<Object> response = api.addFavoritePlan(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		Mockito.verifyNoInteractions(service);
	}

	@Test
	public void deleteRemovesPlanAndReturnsUpdatedList() {
		Mockito.when(service.removeFavoritePlanForCurrentUser("plan-b"))
			.thenReturn(List.of("plan-a"));

		ResponseEntity<Object> response = api.removeFavoritePlan("plan-b");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(planNames((JsonObject) response.getBody())).containsExactly("plan-a");
		Mockito.verify(service).removeFavoritePlanForCurrentUser("plan-b");
	}

	@Test
	public void deleteNonFavoriteIsNoOpSuccess() {
		Mockito.when(service.removeFavoritePlanForCurrentUser("not-a-favorite"))
			.thenReturn(List.of("plan-a", "plan-b"));

		ResponseEntity<Object> response = api.removeFavoritePlan("not-a-favorite");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(planNames((JsonObject) response.getBody()))
			.containsExactly("plan-a", "plan-b");
	}
}
