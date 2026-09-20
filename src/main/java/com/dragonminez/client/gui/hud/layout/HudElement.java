package com.dragonminez.client.gui.hud.layout;

public enum HudElement {
	MAIN("main", false),
	L2_HEALTH("compact_health", false),
	L2_KI("compact_ki", false),
	L2_STAMINA("compact_stamina", false),
	MC_LEFT("minecraft_left", false),
	MC_RIGHT("minecraft_right", false),
	PARTY("party", false),
	RESERVE("reserve", true),
	RAGE("rage", true),
	SKILL_1("skill_1", false),
	SKILL_2("skill_2", false),
	SKILL_3("skill_3", false),
	SKILL_4("skill_4", false),
	TRACKED_QUEST("tracked_quest", false),
	QUEST_NOTICE("quest_notice", false),
	SCOUTER("scouter", false),
	BABA_TIMER("baba_timer", false);

	private static final HudElement[] SKILLS = {SKILL_1, SKILL_2, SKILL_3, SKILL_4};

	private final String id;
	private final boolean meter;

	HudElement(String id, boolean meter) {
		this.id = id;
		this.meter = meter;
	}

	public static HudElement skill(int row) {
		return SKILLS[row];
	}

	public static HudElement[] skills() {
		return SKILLS.clone();
	}

	public boolean isSkill() {
		return this == SKILL_1 || this == SKILL_2 || this == SKILL_3 || this == SKILL_4;
	}

	public boolean isExtra() {
		return this == TRACKED_QUEST || this == QUEST_NOTICE || this == SCOUTER || this == BABA_TIMER;
	}

	public int skillRow() {
		return ordinal() - SKILL_1.ordinal();
	}

	public String id() {
		return id;
	}

	public boolean isMeter() {
		return meter;
	}

	public String translationKey() {
		return "gui.dragonminez.hud_editor.element." + id;
	}
}
