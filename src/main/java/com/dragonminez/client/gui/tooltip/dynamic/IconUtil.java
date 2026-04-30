package com.dragonminez.client.gui.tooltip.dynamic;

import net.minecraft.network.chat.Component;

public final class IconUtil {
	private IconUtil() {}

	public static boolean isIconGlyph(int cp) {
		return (cp >= 0xE000 && cp <= 0xF8FF);
	}

	public static int[] firstIconSpan(String s) {
		int formattingStart = -1;

		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			int len = Character.charCount(cp);

			if (cp == '§') {
				if (formattingStart == -1) formattingStart = i;

				if (i + 1 < s.length()) {
					int nextCp = s.codePointAt(i + 1);
					if (nextCp == 'x' || nextCp == 'X') {
						i += 2;
						for (int k = 0; k < 6 && i < s.length(); k++) {
							if (s.charAt(i) == '§' && i + 1 < s.length()) i += 2;
							else break;
						}
						continue;
					} else {
						i += 2;
						continue;
					}
				} else {
					i++;
					continue;
				}
			}

			if (isIconGlyph(cp)) {
				int actualStart = (formattingStart >= 0) ? formattingStart : i;
				return new int[]{ actualStart, i + len };
			}

			formattingStart = -1;
			i += len;
		}

		return new int[]{ -1, -1 };
	}

	public static String stripSectionCodes(String s) {
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			int len = Character.charCount(cp);

			if (cp == '§') {
				if (i + 1 < s.length()) {
					int nextCp = s.codePointAt(i + 1);
					if (nextCp == 'x' || nextCp == 'X') {
						i += 2;
						for (int k = 0; k < 6 && i < s.length(); k++) {
							if (s.codePointAt(i) == '§' && i + 1 < s.length()) i += 2;
							else break;
						}
						continue;
					} else {
						i += 2;
						continue;
					}
				} else {
					i++;
					continue;
				}
			}
			out.appendCodePoint(cp);
			i += len;
		}
		return out.toString();
	}

	public static Component processIcon(Component originalAttr, Component translatedStat) {
		String raw = originalAttr.getString();
		int[] span = firstIconSpan(raw);

		if (span[0] >= 0) {
			String icon = raw.substring(span[0], span[1]);

			return Component.empty().append(Component.literal(icon).withStyle(style -> style.withColor(0xFFFFFF))).append(Component.literal(" ")).append(translatedStat);
		}
		return translatedStat;
	}

	public static Component getAttributeNameWithoutIcon(Component attributeComponent) {
		String raw = attributeComponent.getString();
		int[] span = firstIconSpan(raw);
		if (span[0] >= 0) {
			String restRaw = raw.substring(0, span[0]) + raw.substring(span[1]);
			String rest = stripSectionCodes(restRaw).replaceFirst("^\\s+", "");
			return Component.literal(rest);
		}
		return attributeComponent;
	}
}