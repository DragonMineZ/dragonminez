package com.dragonminez.client.gui.dialogue;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
final class DialogueReplies {
	private static final int MAX_VARIANTS = 9;
	private static final String GENERIC = "gui.dragonminez.dialogue.reply.";
	private static final String DONE = "done";
	private static final RandomSource RANDOM = RandomSource.create();

	private DialogueReplies() {
	}

	static MutableComponent reply(String npcId, String event, Object... args) {
		String key = pickKey("gui.dragonminez.lines." + npcId + ".reply." + event);
		if (key == null) key = pickKey(GENERIC + event);
		if (key == null) key = pickKey(GENERIC + DONE);
		return NpcDialogueContent.line(key != null ? key : GENERIC + DONE + ".1", args);
	}

	static String pickKey(String... bases) {
		List<String> pool = new ArrayList<>();
		for (String base : bases) {
			if (I18n.exists(base)) pool.add(base);
			for (int i = 1; i <= MAX_VARIANTS; i++) {
				if (I18n.exists(base + "." + i)) pool.add(base + "." + i);
			}
		}
		return pool.isEmpty() ? null : pool.get(RANDOM.nextInt(pool.size()));
	}
}
