package az.masapay.menu.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import az.masapay.menu.importing.MenuDocumentReader.SourceLine;
import az.masapay.menu.importing.MenuTextParser.Category;
import az.masapay.menu.importing.MenuTextParser.Dish;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuTextParserTest {

	private static List<Category> parse(String... lines) {
		return MenuTextParser.parse(Arrays.stream(lines).map(SourceLine::of).toList());
	}

	@Test
	void headingsDishesPricesAndDescriptionsAreRecognised() {
		List<Category> menu = parse(
			"Restoran Bulvar",
			"SALATLAR",
			"Çoban salatı ........ 6,50 AZN",
			"pomidor, xiyar, soğan",
			"Sezar (toyuq, parmezan) 9 ₼",
			"İsti yeməklər:",
			"1. Plov - zəfəranlı düyü, toyuq 12",
			"• Dolma 10.5");

		assertThat(menu).extracting(Category::name).containsExactly("Salatlar", "İsti yeməklər");
		assertThat(menu.get(0).dishes()).extracting(Dish::name, Dish::ingredients, Dish::price).containsExactly(
			tuple("Çoban salatı", "pomidor, xiyar, soğan", new BigDecimal("6.50")),
			tuple("Sezar", "toyuq, parmezan", new BigDecimal("9.00")));
		assertThat(menu.get(1).dishes()).extracting(Dish::name, Dish::ingredients, Dish::price).containsExactly(
			tuple("Plov", "zəfəranlı düyü, toyuq", new BigDecimal("12.00")),
			tuple("Dolma", null, new BigDecimal("10.50")));
		assertThat(menu.get(0).dishes()).extracting(Dish::description).containsOnlyNulls();
	}

	@Test
	void aSentenceIsTheDescriptionAndAShortListTheIngredients() {
		List<Category> menu = parse(
			"Lülə kabab 14",
			"Kömür üstündə bişirilmiş, lavaş ilə verilir",
			"quzu əti, soğan, sumaq");

		assertThat(menu.get(0).dishes()).singleElement().satisfies(dish -> {
			assertThat(dish.description()).isEqualTo("Kömür üstündə bişirilmiş, lavaş ilə verilir");
			assertThat(dish.ingredients()).isEqualTo("quzu əti, soğan, sumaq");
		});
	}

	@Test
	void ingredientsNeedAtLeastTwoShortItems() {
		assertThat(MenuTextParser.looksLikeIngredients("pomidor, xiyar və soğan")).isTrue();
		assertThat(MenuTextParser.looksLikeIngredients("toyuq")).isFalse();
		assertThat(MenuTextParser.looksLikeIngredients("ən təzə yerli tərəvəzlərdən, ev üsulu ilə")).isFalse();
	}

	@Test
	void dishesBeforeAnyHeadingGoToADefaultCategory() {
		List<Category> menu = parse("Çay 2", "Qəhvə 4");

		assertThat(menu).singleElement().satisfies(category -> {
			assertThat(category.name()).isEqualTo(MenuTextParser.DEFAULT_CATEGORY);
			assertThat(category.dishes()).extracting(Dish::name).containsExactly("Çay", "Qəhvə");
		});
	}

	@Test
	void aPriceOnTheNextLineBelongsToTheDishAbove() {
		List<Category> menu = parse("İçkilər", "Ayran", "1,50", "Kompot", "2 AZN");

		assertThat(menu.get(0).dishes()).extracting(Dish::name, Dish::price).containsExactly(
			tuple("Ayran", new BigDecimal("1.50")),
			tuple("Kompot", new BigDecimal("2.00")));
	}

	@Test
	void phoneNumbersAndOpeningHoursAreNotDishes() {
		List<Category> menu = parse("Tel: 012 555 55 55", "Açıq: 09:00 - 23:00", "Desertlər", "Paxlava 3",
			"Tel: 012 555 55 55");

		assertThat(menu).singleElement().satisfies(category -> {
			assertThat(category.name()).isEqualTo("Desertlər");
			assertThat(category.dishes()).extracting(Dish::name, Dish::description)
				.containsExactly(tuple("Paxlava", null));
		});
	}

	@Test
	void aTableRowKeepsNameDescriptionAndPriceApart() {
		byte[] photo = {1, 2, 3};
		List<Category> menu = MenuTextParser.parse(List.of(
			new SourceLine(List.of("Ad", "Tərkibi", "Qiymət"), null),
			new SourceLine(List.of("Şorbalar"), null),
			new SourceLine(List.of("Mərci", "qırmızı mərci, limon", "4.00"), photo)));

		assertThat(menu).singleElement().satisfies(category -> {
			assertThat(category.name()).isEqualTo("Şorbalar");
			assertThat(category.dishes()).singleElement().satisfies(dish -> {
				assertThat(dish.name()).isEqualTo("Mərci");
				assertThat(dish.ingredients()).isEqualTo("qırmızı mərci, limon");
				assertThat(dish.price()).isEqualByComparingTo("4");
				assertThat(dish.picture()).isEqualTo(photo);
			});
		});
	}

	@Test
	void aPictureOnItsOwnLineGoesWithTheDishUnderIt() {
		byte[] photo = {9};
		List<Category> menu = MenuTextParser.parse(List.of(
			new SourceLine(List.of(""), photo), SourceLine.of("Kabab 15"), SourceLine.of("Lülə 14")));

		assertThat(menu.get(0).dishes()).extracting(Dish::picture).containsExactly(photo, null);
	}

	@Test
	void theSameHeadingTwiceIsOneCategory() {
		List<Category> menu = parse("İÇKİLƏR", "Su 1", "Desert", "Tort 5", "içkilər:", "Şirə 3");

		assertThat(menu).extracting(Category::name).containsExactly("İçkilər", "Desert");
		assertThat(menu.get(0).dishes()).extracting(Dish::name).containsExactly("Su", "Şirə");
	}

	@Test
	void aFileWithoutPricesGivesNoMenu() {
		assertThat(parse("Xoş gəlmisiniz", "Bizim haqqımızda")).isEmpty();
	}
}
