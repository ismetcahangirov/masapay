package az.masapay.menu.dto;

import java.util.List;

/**
 * What the file looks like as a menu, for the manager to review; nothing is saved yet.
 * {@code skippedImages} counts pictures in the file that were not JPEG, PNG or WebP photos.
 * {@code ocr} is true when the text was recognised from pictures (a scan or a photo), which
 * mistakes letters and digits more often, so the manager should check the prices closely.
 */
public record MenuImportDraft(List<MenuImportCategory> categories, int skippedImages, boolean ocr) {
}
