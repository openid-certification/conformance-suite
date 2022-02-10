package net.openid.conformance.pagination;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import java.util.function.BiFunction;
import java.util.function.Function;

public class PaginationRequest {

	private int draw;

	private int start;

	private int length;

	private String search;

	private String order;

	public int getDraw() {
		return draw;
	}

	public void setDraw(int draw) {
		this.draw = draw;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		if (length > 1000) {
			throw new RuntimeException("Page length in excess of 1000 entries requested.");
		}
		this.length = length;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public <T> PaginationResponse<T> getResponse(
			Function<Pageable, Page<T>> queryAll,
			BiFunction<String, Pageable, Page<T>> querySearch) {

		Page<T> allResults = queryAll.apply(getPageable());
		Page<T> filteredResults = Strings.isNullOrEmpty(search) ? allResults : querySearch.apply('\"' + search + '\"', getPageable());

		return new PaginationResponse<T>(draw,
				allResults.getTotalElements(),
				filteredResults.getTotalElements(),
				Lists.newArrayList(filteredResults));
	}

	private Pageable getPageable() {
		int l = length;
		if (l == 0) {
			l = 10;
		}

		return PageRequest.of(start / l, l, getSort());
	}

	private Sort getSort() {

		Sort sort = Sort.unsorted();

		if (order != null) {
			String[] orderParts = order.split(",");
			for (int i = 0; i < orderParts.length; i += 2) {
				String column = orderParts[i];
				String dir = (i + 1 < orderParts.length) ? orderParts[i + 1] : "asc";
				Order order = dir.equals("desc") ? Sort.Order.desc(column) : Sort.Order.asc(column);
				sort = sort.and(Sort.by(order));
			}
		}

		return sort;
	}
}
