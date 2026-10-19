package az.masapay.pos;

import az.masapay.domain.Order;
import az.masapay.payment.OrderPaidEvent;
import az.masapay.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** Closes the bill in the restaurant's POS once masapay has recorded the full payment (#41). */
@Component
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PosOrderCloser {

	private static final Logger log = LoggerFactory.getLogger(PosOrderCloser.class);

	private final OrderRepository orderRepository;
	private final PosAdapterRegistry adapters;

	public PosOrderCloser(OrderRepository orderRepository, PosAdapterRegistry adapters) {
		this.orderRepository = orderRepository;
		this.adapters = adapters;
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void onOrderPaid(OrderPaidEvent event) {
		Order order = orderRepository.findById(event.orderId()).orElseThrow();
		if (order.getPosOrderId() == null) {
			return;
		}
		try {
			adapters.forRestaurant(order.getRestaurant())
					.closeOrder(order.getRestaurant(), order.getPosOrderId(), order.getTotalAmount());
		} catch (RuntimeException e) {
			// The payment already stands; staff close the bill by hand if the POS refuses.
			log.error("Could not close POS order {} for order {}", order.getPosOrderId(), order.getId(), e);
		}
	}
}
