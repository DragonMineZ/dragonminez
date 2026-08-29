package com.dragonminez.mixin.sable;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Only apply Sable soft-compat mixins when Sable is on the classpath.
 */
public final class SableMixinPlugin implements IMixinConfigPlugin {

	private static final String SABLE_MARKER = "dev.ryanhcode.sable.Sable";
	private boolean sablePresent;

	@Override
	public void onLoad(String mixinPackage) {
		try {
			Class.forName(SABLE_MARKER, false, getClass().getClassLoader());
			sablePresent = true;
		} catch (ClassNotFoundException | NoClassDefFoundError e) {
			sablePresent = false;
		}
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return sablePresent;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
