package az.masapay.transaction;

import az.masapay.domain.Order;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Builds the JPA criteria query behind the admin transactions table (#45). */
final class TransactionSpecifications {

	private TransactionSpecifications() {
	}

	/**
	 * Transactions of the filter's restaurant that match every given criterion. The text query
	 * matches the Payriff order id or table label (substring) or the transaction id (prefix),
	 * ignoring case.
	 */
	static Specification<Transaction> matching(TransactionFilter filter) {
		return (root, query, cb) -> {
			Join<?, RestaurantTable> table = joinTable(root, query);
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("restaurant").get("id"), filter.restaurantId()));
			if (filter.status() != null) {
				predicates.add(cb.equal(root.get("status"), filter.status()));
			}
			if (filter.splitType() != null) {
				predicates.add(cb.equal(root.get("splitType"), filter.splitType()));
			}
			if (filter.createdFrom() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
			}
			if (filter.createdTo() != null) {
				predicates.add(cb.lessThan(root.get("createdAt"), filter.createdTo()));
			}
			if (filter.query() != null) {
				String text = escapeLike(filter.query().toLowerCase(Locale.ROOT));
				predicates.add(cb.or(
					cb.like(cb.lower(root.get("payriffOrderId")), "%" + text + "%", '\\'),
					cb.like(cb.lower(table.get("label")), "%" + text + "%", '\\'),
					cb.like(cb.lower(root.get("id").as(String.class)), text + "%", '\\')));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	/**
	 * Joins order and table. The page query fetches them in the same select so a row's table
	 * label needs no extra query; the count query only joins, since a count cannot fetch.
	 */
	private static Join<?, RestaurantTable> joinTable(Root<Transaction> root, CriteriaQuery<?> query) {
		Class<?> resultType = query.getResultType();
		if (resultType == Long.class || resultType == long.class) {
			return root.join("order", JoinType.INNER).join("table", JoinType.INNER);
		}
		Join<Transaction, Order> order = (Join<Transaction, Order>) root.<Transaction, Order>fetch("order",
			JoinType.INNER);
		return (Join<?, RestaurantTable>) order.<Order, RestaurantTable>fetch("table", JoinType.INNER);
	}

	/** Escapes LIKE wildcards so a typed "%" or "_" matches itself. */
	static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
