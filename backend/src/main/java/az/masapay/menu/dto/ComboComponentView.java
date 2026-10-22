package az.masapay.menu.dto;

import az.masapay.domain.MenuComboComponent;
import java.util.UUID;

public record ComboComponentView(UUID itemId, String name, int quantity) {

	public static ComboComponentView of(MenuComboComponent row) {
		return new ComboComponentView(row.getComponent().getId(), row.getComponent().getName(), row.getQuantity());
	}
}
