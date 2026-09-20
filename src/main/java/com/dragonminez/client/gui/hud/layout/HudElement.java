package com.dragonminez.client.gui.hud.layout;

public enum HudElement {
	MAIN("main", false),
	L2_HEALTH("legacy2_health", false),
	L2_KI("legacy2_ki", false),
	L2_STAMINA("legacy2_stamina", false),
	MC_LEFT("minecraft_left", false),
	MC_RIGHT("minecraft_right", false),
	PARTY("party", false),
	RESERVE("reserve", true),
	RAGE("rage", true);

	private final String id;
	private final boolean meter;

	HudElement(String id, boolean meter) {
		this.id = id;
		this.meter = meter;
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
