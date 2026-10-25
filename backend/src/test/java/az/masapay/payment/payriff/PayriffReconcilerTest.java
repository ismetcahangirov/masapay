package az.masapay.payment.payriff;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.repository.TransactionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayriffReconcilerTest {

	private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final PayriffClient client = mock(PayriffClient.class);
	private final PayriffWebhookService webhookService = mock(PayriffWebhookService.class);
	private final PayriffReconciler reconciler = new PayriffReconciler(transactionRepository, client, webhookService,
			Clock.fixed(NOW, ZoneOffset.UTC), true, Duration.ofMinutes(10), Duration.ofDays(2));

	private Transaction pending(String orderId, Duration age) {
		Transaction transaction = new Transaction();
		transaction.setId(UUID.randomUUID());
		transaction.setStatus(TransactionStatus.PENDING);
		transaction.setPayriffOrderId(orderId);
		transaction.setCreatedAt(NOW.minus(age));
		return transaction;
	}

	private void stale(Transaction... transactions) {
		when(transactionRepository.findStalePayriffPayments(any(), any())).thenReturn(List.of(transactions));
	}

	@Test
	void anOrderPaidAtPayriffWithoutACallbackIsSettled() {
		Transaction lostCallback = pending("ord-1", Duration.ofMinutes(15));
		stale(lostCallback);
		when(client.getOrder("ord-1")).thenReturn(new PayriffOrderInfo("ord-1", "APPROVED"));

		reconciler.reconcile();

		verify(webhookService).apply(lostCallback.getId(), "ord-1", "APPROVED");
	}

	@Test
	void anOrderTheGuestLeftUnpaidFailsOnceExpiredAtPayriff() {
		Transaction left = pending("ord-2", Duration.ofMinutes(40));
		stale(left);
		when(client.getOrder("ord-2")).thenReturn(new PayriffOrderInfo("ord-2", "EXPIRED"));

		reconciler.reconcile();

		verify(webhookService).apply(left.getId(), "ord-2", "EXPIRED");
	}

	@Test
	void anOrderStillOpenIsLeftAloneUntilItIsTooOld() {
		Transaction recent = pending("ord-3", Duration.ofHours(1));
		Transaction ancient = pending("ord-4", Duration.ofDays(3));
		stale(recent, ancient);
		when(client.getOrder(anyString())).thenAnswer(inv -> new PayriffOrderInfo(inv.getArgument(0), "CREATED"));

		reconciler.reconcile();

		verify(webhookService, never()).apply(recent.getId(), "ord-3", PayriffReconciler.EXPIRED);
		verify(webhookService).apply(ancient.getId(), "ord-4", PayriffReconciler.EXPIRED);
	}

	@Test
	void unknownToPayriffFailsButPayriffDownChangesNothing() {
		Transaction unknown = pending("ord-5", Duration.ofMinutes(20));
		Transaction unreachable = pending("ord-6", Duration.ofDays(5));
		stale(unknown, unreachable);
		when(client.getOrder("ord-5")).thenThrow(new PayriffException("getOrder", 404, "01000", "Order not found"));
		when(client.getOrder("ord-6")).thenThrow(new PayriffException("getOrder", 503, null, "Service Unavailable"));

		reconciler.reconcile();

		verify(webhookService).apply(unknown.getId(), "ord-5", PayriffReconciler.EXPIRED);
		// Never given up because of an outage, however old.
		verify(webhookService, never()).apply(unreachable.getId(), "ord-6", PayriffReconciler.EXPIRED);
	}

	@Test
	void switchedOffItDoesNothing() {
		new PayriffReconciler(transactionRepository, client, webhookService, Clock.fixed(NOW, ZoneOffset.UTC), false,
				Duration.ofMinutes(10), Duration.ofDays(2)).reconcile();

		verifyNoInteractions(transactionRepository, client, webhookService);
	}
}
