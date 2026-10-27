package az.masapay.menu.dto;

import java.util.List;

/**
 * What the file looks like as a menu, for the manager to review; nothing is saved yet.
 * {@code skippedImages} counts pictures in the file that were not JPEG, PNG or WebP photos.
 */
public record MenuImportDraft(List<MenuImportCategory> categories, int skippedImages) {
}
