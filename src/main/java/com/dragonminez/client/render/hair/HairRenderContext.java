package com.dragonminez.client.render.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairStyleSlot;

public final class HairRenderContext {
	public enum Mode {
		WORLD,
		EDITOR_PREVIEW,
		MENU_PREVIEW
	}

	public interface Preview {
		CustomHair style();

		HairStyleSlot slot();

		boolean physicsEnabled();

		HairEntityState simulationState();

		float highlight(CustomHair.HairFace face, int index, int segment);

		boolean isHidden(CustomHair.HairFace face, int index);

		HairPickRecorder pickRecorder();
	}

	private static Mode mode = Mode.WORLD;
	private static Preview preview;

	private HairRenderContext() {}

	public static Mode mode() {
		return mode;
	}

	public static Preview preview() {
		return preview;
	}

	public static Scope editorPreview(Preview editorPreview) {
		return new Scope(Mode.EDITOR_PREVIEW, editorPreview);
	}

	public static Scope menuPreview() {
		return new Scope(Mode.MENU_PREVIEW, null);
	}

	public static final class Scope implements AutoCloseable {
		private final Mode previousMode;
		private final Preview previousPreview;

		private Scope(Mode newMode, Preview newPreview) {
			this.previousMode = mode;
			this.previousPreview = preview;
			mode = newMode;
			preview = newPreview;
		}

		public void close() {
			mode = previousMode;
			preview = previousPreview;
		}
	}
}
