package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.datagen.builder.PatternRecipeBuilder;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.server.recipes.PatternRecipe;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import java.util.function.Consumer;

public class DMZPatternRecipeProvider {
	private static final Map<Character, ItemLike> PALETTE = new LinkedHashMap<>();

	static {
		PALETTE.put('W', Items.WHITE_DYE);
		PALETTE.put('S', Items.LIGHT_GRAY_DYE);
		PALETTE.put('G', Items.GRAY_DYE);
		PALETTE.put('K', Items.BLACK_DYE);
		PALETTE.put('N', Items.BROWN_DYE);
		PALETTE.put('R', Items.RED_DYE);
		PALETTE.put('O', Items.ORANGE_DYE);
		PALETTE.put('Y', Items.YELLOW_DYE);
		PALETTE.put('L', Items.LIME_DYE);
		PALETTE.put('E', Items.GREEN_DYE);
		PALETTE.put('C', Items.CYAN_DYE);
		PALETTE.put('A', Items.LIGHT_BLUE_DYE);
		PALETTE.put('B', Items.BLUE_DYE);
		PALETTE.put('U', Items.PURPLE_DYE);
		PALETTE.put('M', Items.MAGENTA_DYE);
		PALETTE.put('P', Items.PINK_DYE);
		PALETTE.put('Q', Items.GOLD_NUGGET);
	}

	private final Consumer<FinishedRecipe> consumer;
	private final Map<String, String> usedGrids = new HashMap<>();

	public DMZPatternRecipeProvider(Consumer<FinishedRecipe> consumer) {
		this.consumer = consumer;
	}

	protected void generate() {
		Item z = MainItems.BLANK_PATTERN_Z.get();
		Item sup = MainItems.BLANK_PATTERN_SUPER.get();

		build(MainItems.PATTERN_GOKU_KID, z,
				"RR  RR",
				"RRKKRR",
				"KRRRYK",
				"KR#RRK",
				" RRRR ",
				"BBRRBB");
		build(MainItems.PATTERN_GOKU1, z,
				"OO  OO",
				"OOBBOO",
				"BOBBOB",
				"BO#OOB",
				" OOOO ",
				"BBBBBB");
		build(MainItems.PATTERN_GOKU_SUPER, sup,
				"OO  OO",
				"OOAAOO",
				"AWAAOA",
				"AO#OOA",
				" OOOO ",
				"AAAAAA");
		build(MainItems.PATTERN_GOKU_GT, z,
				"BB  BB",
				"BBAABB",
				"BABBAB",
				"BB#BBB",
				" BBBB ",
				"MSSSSM");
		build(MainItems.PATTERN_GOKU_WHIS, sup,
				"OO  OO",
				"OOBBOO",
				"BWBBOB",
				"BO#OOB",
				" OOOO ",
				"BBBBBB");
		build(MainItems.PATTERN_YARDRAT, z,
				"WY  YW",
				"WBYYBW",
				"BWBBWB",
				"YB#BBY",
				" BWWB ",
				"NYBBYN");
		build(MainItems.PATTERN_GOTEN, z,
				"OO  OO",
				"OOKKOO",
				"BOKKOB",
				"BO#OOB",
				" OOOO ",
				"BBKKBB");
		build(MainItems.PATTERN_GOTEN_SUPER, sup,
				"LL  LL",
				"LEEEEL",
				"ELLLLE",
				"EL#LLE",
				" LEEL ",
				"SSEESS");
		build(MainItems.PATTERN_GOHAN1, z,
				"UU  UU",
				"UUKKUU",
				"KUUUUK",
				"KU#UUK",
				" UUUU ",
				"BBUUBB");
		build(MainItems.PATTERN_GOHAN_SUPER, sup,
				"UU  UU",
				"UUKKUU",
				"KUUUUK",
				"KU#UUK",
				" UUUU ",
				"GRRRRG");
		build(MainItems.PATTERN_FUTURE_GOHAN, z,
				"OO  OO",
				"OOBBOO",
				"BOBBOB",
				"BO#OOB",
				" OBBO ",
				"B OO B");
		build(MainItems.PATTERN_TRUNKS_KID, z,
				"CC  CC",
				"CCKKCC",
				"KCCCCK",
				"KC#CCK",
				" CKKC ",
				"OKCCKO");
		build(MainItems.PATTERN_TIEN, z,
				"    EE",
				"   EE ",
				"  E#E ",
				"REEEER",
				"RRRRRR",
				"  RR  ");
		build(MainItems.PATTERN_VEGETTO, z,
				"BA  AB",
				"BBOOBB",
				"OBOOBO",
				"BBO#BB",
				" BBBB ",
				" BBBB ");
		build(MainItems.PATTERN_VEGETA_BUU, z,
				"BB  BB",
				"BBKKBB",
				"KBBBBK",
				"KB#BBK",
				" BBBB ",
				" KBBK ");
		build(MainItems.PATTERN_BLACK, sup,
				"GG  GG",
				"GGKKGG",
				"KGKKGK",
				"KG#GGK",
				" GGGG ",
				"KRRRRK");
		build(MainItems.PATTERN_VEGETA1, z,
				"YY  YY",
				"OWBBWO",
				"BWWWWB",
				"BW#WWB",
				"BSNYSB",
				"BONNOB");
		build(MainItems.PATTERN_VEGETA2, z,
				"WS  SW",
				"BWBBWB",
				"BWWWWB",
				"BW#WWB",
				"BSNYSB",
				"BKNNKB");
		build(MainItems.PATTERN_VEGETA_Z, z,
				"NO  ON",
				"BWBBWB",
				"AWWWWA",
				"AW#WWA",
				"BSNYSB",
				"BKNNKB");
		build(MainItems.PATTERN_VEGETA_SUPER, sup,
				"YS  SY",
				"CWBBWC",
				"CWWWWC",
				"CW#WWC",
				"CSNYSC",
				"KKNNKK");
		build(MainItems.PATTERN_VEGETA_WHIS, sup,
				"YK  KY",
				"KWKKWK",
				"KWWWWK",
				"KW#WWK",
				" SYYS ",
				" YYYY ");
		build(MainItems.PATTERN_BARDOCK1, z,
				"EE  EE",
				"WBWWBW",
				"WBBBBW",
				" B#BB ",
				"REEEER",
				"R KK R");
		build(MainItems.PATTERN_BARDOCK2, sup,
				"YY  YY",
				"WKWWKW",
				"KKKKKK",
				"BK#KKB",
				"BSWWSB",
				"AKKKKA");
		build(MainItems.PATTERN_TURLES, z,
				"SS  SS",
				"UBWWBU",
				"WBBBBW",
				" B#BB ",
				" BSSB ",
				" KWWK ");
		build(MainItems.PATTERN_GINE, z,
				"YY  YY",
				" WYYW ",
				"CCCCCC",
				" S#SS ",
				"MSYYSM",
				"M    M");
		build(MainItems.PATTERN_BROLY_Z, z,
				"N    N",
				"NQ  QN",
				" QQQQ ",
				"  #A  ",
				"Q    Q",
				"QA  AQ");
		build(MainItems.PATTERN_BROLY_SUPER, sup,
				"LE  EL",
				"WKWWKW",
				"WKKKKW",
				" K#KK ",
				" KLLK ",
				" WKKW ");
		build(MainItems.PATTERN_GAS, sup,
				"RY  YR",
				"YRYYRY",
				"SRYYRS",
				"SR#RRS",
				" RYYR ",
				"  YY  ");
		build(MainItems.PATTERN_PICCOLO, z,
				"UU  UU",
				"UUKKUU",
				"UUUUUU",
				" U#UU ",
				" UUUU ",
				"RBBBBR");
		build(MainItems.PATTERN_GREAT_SAIYAMAN, z,
				"ER  RE",
				"EEKKEE",
				"KELLEK",
				"KE#LEK",
				"KEEEEK",
				"RRRRRR");
		build(MainItems.PATTERN_SHIN, z,
				"RR  RR",
				"CKRRKC",
				"CCWKCC",
				"AC#KCA",
				"ACCCCA",
				"WOOOOW");
		build(MainItems.PATTERN_ZAMASU, sup,
				"YG  GY",
				"GKYYKG",
				"UKGWKU",
				"UK#GKU",
				"UKKKKU",
				" AAAA ");
		build(MainItems.PATTERN_FUSION_ZAMASU, sup,
				"RG  GR",
				"GKRRKG",
				"KGRGGK",
				"KG#GGK",
				" KRRK ",
				"WRRRRW");
		build(MainItems.PATTERN_HIT, sup,
				"KU  UK",
				"UUKKUU",
				"CUUUUC",
				"CU#UUC",
				" UUUU ",
				" KYYK ");
		build(MainItems.PATTERN_BEERUS, sup,
				" WNNW ",
				"WAWWAW",
				" WAAW ",
				" S#AS ",
				" SSSS ",
				"Y    Y");
		build(MainItems.PATTERN_WHIS, sup,
				"KWKKWK",
				"MKYYKM",
				"MKY#KM",
				"MKYYKM",
				"MKYYKM",
				" KYYK ");
		build(MainItems.PATTERN_GAMMA1, sup,
				"RR  RR",
				"OOOOOO",
				"KSOOSK",
				"OO#ROO",
				"OSOOSO",
				"NNNNNN");
		build(MainItems.PATTERN_GAMMA2, sup,
				"AA  AA",
				"OOOOOO",
				"KSOOSK",
				"OO#BOO",
				"OSOOSO",
				"NNNNNN");
		build(MainItems.PATTERN_A16, z,
				"LE  EL",
				"LLWKLL",
				"ELLLRE",
				"KL#LLK",
				" ELLE ",
				"LE  EL");
		build(MainItems.PATTERN_A17, z,
				"KR  RK",
				"KROORK",
				"KKROKK",
				"SK#KKS",
				"SKKKKS",
				"WK  KW");
		build(MainItems.PATTERN_A17_SUPER, sup,
				"EE  EE",
				"EKKKKE",
				"EKWWKE",
				"ES#WSE",
				" SSSS ",
				"EK  KE");
		build(MainItems.PATTERN_A18, z,
				"BB  BB",
				"BBKKBB",
				"KBBBBK",
				"SB#BBS",
				" KKKK ",
				"SK  KS");
		build(MainItems.PATTERN_A18_KAME, z,
				"AA  AA",
				"ABKKBA",
				"BAAAAB",
				" A#AA ",
				" AAAA ",
				" BAAB ");
		build(MainItems.PATTERN_A18_TOURNAMENT, z,
				"KK  KK",
				"KGKKGK",
				"KKKKKK",
				"SK#KKS",
				" GKKG ",
				"SK  KS");
		build(MainItems.PATTERN_A18_CELL, z,
				"SK  KS",
				"SWAAWS",
				"SKWWYK",
				" K#WK ",
				" KWWK ",
				" SWWS ");
		build(MainItems.PATTERN_ORANGE_HIGH, z,
				"SK  KS",
				"SKWWKS",
				"SKWEKS",
				"SK#KKS",
				" WWWW ",
				" ROOR ");
		build(MainItems.PATTERN_VIDEL, z,
				"WW  WW",
				"WWKKWW",
				"KWWWWK",
				" W#WW ",
				"NNWENN",
				"NN  NN");
		build(MainItems.PATTERN_AGE1000, z,
				"RR  RR",
				"RKLLKR",
				"KLLLLK",
				"KL#ELK",
				" ELLE ",
				"KEEEEK");
		build(MainItems.PATTERN_TRUNKS_Z, z,
				"UU  UU",
				"UUKKUU",
				"UUKKUU",
				"UU#KUU",
				"UUKKUU",
				"UU  UU");
		build(MainItems.PATTERN_TRUNKS_SUPER, sup,
				"AR  RA",
				"BARRAB",
				"BAYYAB",
				"BA#KAB",
				"BBKKBB",
				"AB  BA");
		build(MainItems.PATTERN_VEGETA_GT, z,
				"RR  RR",
				"RRKKRR",
				"KRRRRK",
				"KR#RRK",
				" RRRR ",
				" KRRK ");
		build(MainItems.PATTERN_MAJIN_BUU, z,
				"UU  UU",
				"KUUUUK",
				"KYKKYK",
				"YK#KKY",
				"YUUUUY",
				"YUUUUY");
		build(MainItems.PATTERN_GOGETA, sup,
				"EY  YE",
				"YEKKEY",
				"KKYYKK",
				" K#KK ",
				" GKKG ",
				" G  G ");
		build(MainItems.PATTERN_GRANOLA, sup,
				"KW  WK",
				"ESWWSE",
				" SWWS ",
				" S#YS ",
				"NNNNNN",
				"  GG  ");
		build(MainItems.PATTERN_MAJIN21, sup,
				"  NN  ",
				" KWWK ",
				"KKKKKK",
				"KK#KKK",
				"KK  KK",
				"Y    Y");
		build(MainItems.PATTERN_KALE, sup,
				"RR  RR",
				"RRRRRR",
				"KRRRRK",
				" R#RR ",
				" RRRR ",
				"Y    Y");
		build(MainItems.PATTERN_CAULIFLA, sup,
				" PMMP ",
				" MPPM ",
				" K#KK ",
				"K    K",
				"K    K",
				"K    K");
		build(MainItems.PATTERN_KEFLA, sup,
				"MM  MM",
				" MMMM ",
				"YM#MMY",
				"YMMMMY",
				" MMMM ",
				"  MM  ");
		build(MainItems.PATTERN_PRIDE_TROOPS, sup,
				"KR  RK",
				"RRKKRR",
				"RKRRKR",
				"RR#RRR",
				"WRRRRW",
				" KRRK ");

		build(MainItems.PATTERN_A13, z,
				"YY  YY",
				"YYKKYY",
				"YYKKYR",
				"YY#KYY",
				"O    O",
				"ON  NO");
		build(MainItems.PATTERN_A14, z,
				"    NN",
				"   NN ",
				"  N#  ",
				" YN   ",
				"NN   N",
				"N    N");
		build(MainItems.PATTERN_A20, z,
				"OO  OO",
				"OSOOSO",
				"OWRRWO",
				"OS#RSO",
				"YKWWKY",
				"Y    Y");
		build(MainItems.PATTERN_CAPSULE_CORP, z,
				"BB  BB",
				"BKWWKB",
				"BSWWSB",
				"BK#SKB",
				"BKSSKB",
				"BB  BB");
		build(MainItems.PATTERN_CHAOZ, z,
				"EE  EE",
				"EERREE",
				"YEEEEY",
				"YE#WEY",
				"YERREY",
				"S    S");
		build(MainItems.PATTERN_COOLER_SOLDIER, z,
				" W  WN",
				"UWLLWU",
				"KLLLLK",
				"UL#LLU",
				"UKSSKU",
				"K    K");
		build(MainItems.PATTERN_DRAGON_CLAN, sup,
				"MK  KM",
				"MUUUUM",
				"MU#UUM",
				"MUUUUM",
				" UUUU ",
				" RRRR ");
		build(MainItems.PATTERN_EVIL_BUU, z,
				"BY  YB",
				"KBKKBK",
				"KYBBYK",
				" B#BB ",
				"YBBBBY",
				"YB  BY");
		build(MainItems.PATTERN_FIGHTER, z,
				"EE  EE",
				"EEKKEE",
				"EEEEEE",
				"OE#EEO",
				" EEEE ",
				" SSSS ");
		build(MainItems.PATTERN_GERO, z,
				"NW  WN",
				"OOWWOO",
				"OOOOOO",
				"SO#OOS",
				"WOOOOW",
				"W OO W");
		build(MainItems.PATTERN_GREAT_SAIYAMAN_2, z,
				"EC  CE",
				"CCUUCC",
				"UCCCCU",
				"UC#CCU",
				" WWOW ",
				" RRRR ");
		build(MainItems.PATTERN_KIBITO, z,
				"RR  RR",
				"RRCCRR",
				"ARRRRA",
				"AR#RRA",
				" NRRN ",
				"COOOOC");
		build(MainItems.PATTERN_KING_VEGETA, z,
				"PR  RP",
				"WKBBKW",
				"WWKKWW",
				"SW#WWS",
				"KKPPKK",
				"KRRRRK");
		build(MainItems.PATTERN_MIGHTY_MAJIN, z,
				" NNNN ",
				"YYYYYY",
				"YEYYEY",
				"YY#YYY",
				"UUUUUU",
				" UUUU ");
		build(MainItems.PATTERN_MYSTIC, z,
				"WW  WW",
				"WKCCKW",
				"WCCKCW",
				" C#WC ",
				" CCCC ",
				" SSSS ");
		build(MainItems.PATTERN_RADITZ, z,
				"NN  NN",
				"NWKKWN",
				"WBBBBW",
				" B#BB ",
				" BNNB ",
				" W  W ");
		build(MainItems.PATTERN_SLUG, z,
				" OOOO ",
				"YOOOOY",
				"UYRRYU",
				"OY#UYO",
				"OYYYYO",
				" YUUY ");
		build(MainItems.PATTERN_SUPER_BUU, z,
				"Y    Y",
				"K    K",
				"K #  K",
				"K    K",
				"K    K",
				"Y    Y");
		build(MainItems.PATTERN_VERGIL, sup,
				"BB  BB",
				"BKBBKB",
				"BBKKBB",
				"BB#KBB",
				"BBKKBB",
				" BBBB ");
		build(MainItems.PATTERN_WARRIOR_CLAN, sup,
				"UU  UU",
				" UUUU ",
				" UKKU ",
				" U#UU ",
				" UUUU ",
				" SSSS ");
		build(MainItems.PATTERN_WONDER_MAJIN, z,
				" YYYY ",
				" KYYK ",
				"  UU  ",
				"WU#UUW",
				" UUUU ",
				" UUUU ");
		build(MainItems.PATTERN_XENO_GOKU, sup,
				"RR  RR",
				"RRKKRR",
				"NRKKRN",
				"NR#KRN",
				"BRKKRB",
				"BR  RB");
		build(MainItems.PATTERN_XENO_GOKU_PATREON, sup,
				"SS  SS",
				"SSKKSS",
				"GSKKSG",
				"GS#KSG",
				"WSKKSW",
				"WS  SW");
	}

	protected void build(RegistryObject<Item> pattern, ItemLike blankPattern, String... rows) {
		String name = pattern.getId().getPath();
		if (rows.length != PatternRecipe.MAX_SIZE) {
			throw new IllegalStateException("Pattern recipe " + name + " must have " + PatternRecipe.MAX_SIZE + " rows");
		}

		PatternRecipeBuilder builder = PatternRecipeBuilder.shaped(pattern.get()).group(Reference.MOD_ID);
		Set<Character> used = new LinkedHashSet<>();
		int blanks = 0;
		for (String row : rows) {
			if (row.length() != PatternRecipe.MAX_SIZE) {
				throw new IllegalStateException("Pattern recipe " + name + " has a row that isn't " + PatternRecipe.MAX_SIZE + " wide: '" + row + "'");
			}
			for (char c : row.toCharArray()) {
				if (c == ' ') continue;
				if (c == '#') blanks++;
				else if (!PALETTE.containsKey(c)) throw new IllegalStateException("Pattern recipe " + name + " uses unknown symbol '" + c + "'");
				used.add(c);
			}
			builder.pattern(row);
		}
		if (blanks != 1) throw new IllegalStateException("Pattern recipe " + name + " must contain exactly one blank pattern (#)");

		for (char c : used) {
			builder.define(c, c == '#' ? blankPattern : PALETTE.get(c));
		}

		checkUnique(name, blankPattern, rows);
		builder.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private void checkUnique(String name, ItemLike blankPattern, String[] rows) {
		String prefix = blankPattern.asItem() + "|";
		String normal = prefix + shrink(rows);
		String[] mirroredRows = Arrays.stream(rows).map(r -> new StringBuilder(r).reverse().toString()).toArray(String[]::new);
		String mirrored = prefix + shrink(mirroredRows);
		String clash = usedGrids.getOrDefault(normal, usedGrids.get(mirrored));
		if (clash != null) {
			throw new IllegalStateException("Pattern recipe " + name + " has the same layout as " + clash);
		}
		usedGrids.put(normal, name);
	}

	private static String shrink(String[] rows) {
		int top = 0, bottom = rows.length - 1, left = Integer.MAX_VALUE, right = -1;
		while (top <= bottom && rows[top].isBlank()) top++;
		while (bottom >= top && rows[bottom].isBlank()) bottom--;
		for (int i = top; i <= bottom; i++) {
			String row = rows[i];
			if (row.isBlank()) continue;
			left = Math.min(left, row.length() - row.stripLeading().length());
			right = Math.max(right, row.stripTrailing().length());
		}
		StringBuilder sb = new StringBuilder();
		for (int i = top; i <= bottom; i++) sb.append(rows[i], left, right).append('/');
		return sb.toString();
	}
}
