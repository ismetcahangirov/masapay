package az.masapay.menu.importing;

import az.masapay.menu.importing.MenuDocumentReader.SourceLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guesses a menu from its lines, the way a person skims one: a line that ends with a price is a
 * dish ("Plov ........ 12,50 AZN"), a short line without a price is a category ("SALATLAR"),
 * and a longer line right under a dish is its description. A short comma list ("toyuq, düyü,
 * zəfəran") under a dish or in brackets after its name is its main ingredients. It is a
 * guess: the manager reviews and corrects the result before anything is saved.
 */
public final class MenuTextParser {

	/** A dish as guessed; the picture is the raw bytes found next to it, if any. */
	public record Dish(String name, String description, String ingredients, BigDecimal price, byte[] picture) {
	}

	public record Category(String name, List<Dish> dishes) {
	}

	/** Items standing before the first heading go here; the manager renames it. */
	public static final String DEFAULT_CATEGORY = "Menyu";

	private static final Locale AZ = Locale.forLanguageTag("az");
	private static final String CURRENCY = "(?:₼|azn|manat|man\\.?|m\\.?)";
	/** A price alone: "12", "12.50", "12,5 AZN", "₼ 9". */
	private static final Pattern PRICE_ONLY = Pattern.compile(
		"^(?:" + CURRENCY + "\\s*)?(\\d{1,5}(?:[.,]\\d{1,2})?)\\s*" + CURRENCY + "?$", Pattern.CASE_INSENSITIVE);
	/** Text, then a price at the end after a space, dot leaders, a dash or a bar. */
	private static final Pattern WITH_PRICE = Pattern.compile(
		"^(.*?\\p{L}.*?)[\\s.·…_\\-–—:|/]+(?:" + CURRENCY + "\\s*)?(\\d{1,5}(?:[.,]\\d{1,2})?)\\s*" + CURRENCY
			+ "?$", Pattern.CASE_INSENSITIVE);
	/** A bullet (as escapes: the project rules ban emoji-like symbols in source) or "1." / "1)". */
	private static final Pattern LEADING_MARK = Pattern.compile(
		"^(?:[\\u2022\\u00B7*\\u25AA\\u25CF\\u25E6\\u2023-]+|\\d{1,3}[.)])\\s+");
	private static final Pattern NAME_WITH_BRACKETS = Pattern.compile("^(.+?)\\s*\\((.+)\\)$");
	private static final Pattern NAME_DASH_DESCRIPTION = Pattern.compile("^(.+?)\\s+[-–—]\\s+(.+)$");
	private static final Pattern TRAILING_JUNK = Pattern.compile("[\\s.·…_\\-–—:|/]+$");

	private MenuTextParser() {
	}

	public static List<Category> parse(List<SourceLine> source) {
		String current = DEFAULT_CATEGORY;
		MutableDish last = null;
		// Keyed by the lower-case name, so "SALATLAR" twice in a file is one category.
		Map<String, String> names = new LinkedHashMap<>();
		Map<String, List<MutableDish>> byCategory = new LinkedHashMap<>();
		byte[] pendingPicture = null;

		for (SourceLine line : mergePriceOnlyLines(source)) {
			String text = stripMarks(line.text());
			if (text.isEmpty()) {
				if (line.picture() != null) {
					// A picture on its own line goes with the dish right under it.
					pendingPicture = line.picture();
				}
				continue;
			}
			MutableDish dish = line.cells().size() > 1 ? fromRow(line.cells()) : fromLine(text);
			if (dish != null) {
				dish.picture = line.picture() != null ? line.picture() : pendingPicture;
				pendingPicture = null;
				String key = current.toLowerCase(AZ);
				names.putIfAbsent(key, current);
				byCategory.computeIfAbsent(key, k -> new ArrayList<>()).add(dish);
				last = dish;
				continue;
			}
			pendingPicture = line.picture() != null ? line.picture() : pendingPicture;
			// No letters, or mostly digits (a phone number, opening hours): not part of the menu.
			if (!hasLetter(text) || digits(text) > letters(text)) {
				continue;
			}
			if (last != null && looksLikeDescription(text)) {
				last.addDetail(text);
			} else if (text.length() <= 60) {
				current = headingName(text);
				last = null;
			}
			// Longer text before any dish (a welcome note, the address) is not part of the menu.
		}
		return byCategory.entrySet().stream()
			.map(entry -> new Category(names.get(entry.getKey()),
				entry.getValue().stream().map(MutableDish::toDish).toList()))
			.toList();
	}

	/** "Plov" then "12 AZN" on the next line, as some PDF tables come out, becomes "Plov 12 AZN". */
	private static List<SourceLine> mergePriceOnlyLines(List<SourceLine> source) {
		List<SourceLine> merged = new ArrayList<>();
		for (SourceLine line : source) {
			boolean priceOnly = line.cells().size() == 1 && PRICE_ONLY.matcher(line.text()).matches();
			if (priceOnly && !merged.isEmpty()) {
				SourceLine previous = merged.get(merged.size() - 1);
				if (previous.cells().size() == 1 && hasLetter(previous.text())
						&& !WITH_PRICE.matcher(previous.text()).matches()) {
					merged.set(merged.size() - 1,
						new SourceLine(List.of(previous.text() + " " + line.text()), previous.picture()));
					continue;
				}
			}
			merged.add(line);
		}
		return merged;
	}

	/** A table row: the price is the last cell holding only a price, the name the first text cell. */
	private static MutableDish fromRow(List<String> cells) {
		int priceCell = -1;
		for (int i = cells.size() - 1; i >= 0; i--) {
			if (PRICE_ONLY.matcher(cells.get(i)).matches()) {
				priceCell = i;
				break;
			}
		}
		if (priceCell < 0) {
			return fromLine(stripMarks(String.join(" ", cells).trim()));
		}
		List<String> texts = new ArrayList<>();
		for (int i = 0; i < cells.size(); i++) {
			String cell = stripMarks(cells.get(i));
			if (i != priceCell && hasLetter(cell)) {
				texts.add(cell);
			}
		}
		if (texts.isEmpty()) {
			return null;
		}
		Matcher price = PRICE_ONLY.matcher(cells.get(priceCell));
		price.matches();
		String description = texts.size() > 1 ? String.join(" ", texts.subList(1, texts.size())) : null;
		return dish(texts.get(0), description, price.group(1));
	}

	private static MutableDish fromLine(String text) {
		Matcher matcher = WITH_PRICE.matcher(text);
		if (!matcher.matches()) {
			return null;
		}
		String name = TRAILING_JUNK.matcher(matcher.group(1)).replaceAll("").trim();
		// "Tel: 012 555 55 55" ends with a number too, but is mostly digits.
		if (digits(name) > letters(name)) {
			return null;
		}
		String description = null;
		Matcher brackets = NAME_WITH_BRACKETS.matcher(name);
		Matcher dash = NAME_DASH_DESCRIPTION.matcher(name);
		if (brackets.matches()) {
			name = brackets.group(1).trim();
			description = brackets.group(2).trim();
		} else if (dash.matches()) {
			name = dash.group(1).trim();
			description = dash.group(2).trim();
		}
		return dish(name, description, matcher.group(2));
	}

	private static MutableDish dish(String name, String description, String price) {
		BigDecimal amount = new BigDecimal(price.replace(',', '.'));
		if (amount.signum() <= 0 || amount.compareTo(new BigDecimal("10000")) >= 0) {
			return null;
		}
		MutableDish dish = new MutableDish();
		dish.name = name;
		if (description != null && !description.isBlank()) {
			dish.addDetail(description.trim());
		}
		dish.price = amount.setScale(2);
		return dish;
	}

	/** "pomidor, xiyar, quzu əti": at least two items, each one or two words; a sentence has longer parts. */
	static boolean looksLikeIngredients(String text) {
		if (!text.contains(",") || text.length() > 300) {
			return false;
		}
		for (String part : text.split(",| və | and | и ")) {
			String item = part.trim();
			if (item.isEmpty() || item.split("\\s+").length > 2) {
				return false;
			}
		}
		return true;
	}

	/** Lower-case start, commas, or more than four words: a description, not a heading. */
	private static boolean looksLikeDescription(String text) {
		if (text.endsWith(":") || text.equals(text.toUpperCase(AZ))) {
			return false;
		}
		return Character.isLowerCase(text.codePointAt(0)) || text.contains(",") || text.split(" ").length > 4
			|| text.length() > 40;
	}

	/** "SALATLAR:" becomes "Salatlar"; a mixed-case heading stays as written. */
	private static String headingName(String text) {
		String name = TRAILING_JUNK.matcher(text).replaceAll("").trim();
		if (name.equals(name.toUpperCase(AZ))) {
			String lower = name.toLowerCase(AZ);
			name = lower.substring(0, 1).toUpperCase(AZ) + lower.substring(1);
		}
		return name;
	}

	private static String stripMarks(String text) {
		return LEADING_MARK.matcher(text.trim()).replaceFirst("").trim();
	}

	private static boolean hasLetter(String text) {
		return letters(text) > 0;
	}

	private static long letters(String text) {
		return text.codePoints().filter(Character::isLetter).count();
	}

	private static long digits(String text) {
		return text.codePoints().filter(Character::isDigit).count();
	}

	private static final class MutableDish {
		String name;
		String description;
		String ingredients;
		BigDecimal price;
		byte[] picture;

		Dish toDish() {
			return new Dish(name, description, ingredients, price, picture);
		}

		/** A comma list of short items is the ingredients (once); anything else adds to the description. */
		void addDetail(String text) {
			if (ingredients == null && looksLikeIngredients(text)) {
				ingredients = text;
			} else {
				description = description == null ? text : description + " " + text;
			}
		}
	}
}
