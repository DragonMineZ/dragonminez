package com.dragonminez.client.gui.tutorial;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class TutorialStep {
	private static final Object[] NO_ARGS = new Object[0];

	private final String titleKey;
	private final String textKey;
	private final Supplier<Object[]> args;
	private final Supplier<List<TutorialRect>> highlights;
	private final List<TutorialButton> buttons;
	private final BooleanSupplier condition;
	private final Runnable onEnter;
	private final Runnable onExit;
	private final String nextKey;
	private final boolean passthrough;
	private final boolean skippable;
	private final boolean centered;
	private final float padding;
	private final float width;

	private TutorialStep(Builder builder) {
		this.titleKey = builder.titleKey;
		this.textKey = builder.textKey;
		this.args = builder.args;
		this.highlights = builder.highlights;
		this.buttons = List.copyOf(builder.buttons);
		this.condition = builder.condition;
		this.onEnter = builder.onEnter;
		this.onExit = builder.onExit;
		this.nextKey = builder.nextKey;
		this.passthrough = builder.passthrough;
		this.skippable = builder.skippable;
		this.centered = builder.centered;
		this.padding = builder.padding;
		this.width = builder.width;
	}

	public static Builder of(String textKey) {
		return new Builder(textKey);
	}

	public String titleKey() { return titleKey; }
	public String textKey() { return textKey; }
	public Object[] args() { return args != null ? args.get() : NO_ARGS; }
	public List<TutorialButton> buttons() { return buttons; }
	public String nextKey() { return nextKey; }
	public boolean passthrough() { return passthrough; }
	public boolean skippable() { return skippable; }
	public boolean centered() { return centered; }
	public float padding() { return padding; }
	public float width() { return width; }

	public boolean applies() {
		return condition == null || condition.getAsBoolean();
	}

	public List<TutorialRect> resolveHighlights() {
		if (highlights == null) return List.of();
		List<TutorialRect> resolved = highlights.get();
		if (resolved == null) return List.of();
		List<TutorialRect> valid = new ArrayList<>(resolved.size());
		for (TutorialRect rect : resolved) {
			if (rect != null && rect.width() > 0.0f && rect.height() > 0.0f) valid.add(rect);
		}
		return valid;
	}

	public void enter() {
		if (onEnter != null) onEnter.run();
	}

	public void exit() {
		if (onExit != null) onExit.run();
	}

	public static final class Builder {
		private final String textKey;
		private String titleKey;
		private Supplier<Object[]> args;
		private Supplier<List<TutorialRect>> highlights;
		private final List<TutorialButton> buttons = new ArrayList<>();
		private BooleanSupplier condition;
		private Runnable onEnter;
		private Runnable onExit;
		private String nextKey;
		private boolean passthrough;
		private boolean skippable = true;
		private boolean centered;
		private float padding = 3.0f;
		private float width;

		private Builder(String textKey) {
			this.textKey = textKey;
		}

		public Builder title(String titleKey) {
			this.titleKey = titleKey;
			return this;
		}

		public Builder args(Supplier<Object[]> args) {
			this.args = args;
			return this;
		}

		public Builder highlight(Supplier<List<TutorialRect>> highlights) {
			this.highlights = highlights;
			return this;
		}

		public Builder highlightOne(Supplier<TutorialRect> highlight) {
			this.highlights = () -> {
				TutorialRect rect = highlight.get();
				return rect == null ? List.of() : List.of(rect);
			};
			return this;
		}

		public Builder button(TutorialButton button) {
			this.buttons.add(button);
			return this;
		}

		public Builder when(BooleanSupplier condition) {
			this.condition = condition;
			return this;
		}

		public Builder onEnter(Runnable onEnter) {
			this.onEnter = onEnter;
			return this;
		}

		public Builder onExit(Runnable onExit) {
			this.onExit = onExit;
			return this;
		}

		public Builder next(String nextKey) {
			this.nextKey = nextKey;
			return this;
		}

		public Builder passthrough() {
			this.passthrough = true;
			return this;
		}

		public Builder centered() {
			this.centered = true;
			return this;
		}

		public Builder noSkip() {
			this.skippable = false;
			return this;
		}

		public Builder padding(float padding) {
			this.padding = padding;
			return this;
		}

		public Builder width(float width) {
			this.width = width;
			return this;
		}

		public TutorialStep build() {
			return new TutorialStep(this);
		}
	}
}
