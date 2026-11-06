package com.dragonminez.common.init.item.consumables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SenzuBeanItem extends FoodItem {

	public enum SenzuType {
		ALL(true, true, true),
		HEALTH(true, false, false),
		KI(false, true, false),
		STAMINA(false, false, true);

		private final boolean health;
		private final boolean ki;
		private final boolean stamina;

		SenzuType(boolean health, boolean ki, boolean stamina) {
			this.health = health;
			this.ki = ki;
			this.stamina = stamina;
		}

		public boolean restoresHealth() { return health; }
		public boolean restoresKi() { return ki; }
		public boolean restoresStamina() { return stamina; }
	}

	private static final List<SenzuBeanItem> ALL = new ArrayList<>();

	private final SenzuType senzuType;

	public SenzuBeanItem(SenzuType senzuType) {
		super(20, 0.0f, 16);
		this.senzuType = senzuType;
		ALL.add(this);
	}

	public SenzuType getSenzuType() {
		return senzuType;
	}

	public static List<SenzuBeanItem> all() {
		return Collections.unmodifiableList(ALL);
	}
}
