package az.masapay.pos.iiko;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

/** One payment line sent to iiko; masapay payments are "External" and already processed. */
public record IikoPayment(
		String paymentTypeKind,
		BigDecimal sum,
		UUID paymentTypeId,
		@JsonProperty("isProcessedExternally") boolean processedExternally) {}
