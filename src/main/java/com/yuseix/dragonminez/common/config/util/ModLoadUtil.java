package com.yuseix.dragonminez.common.config.util;

import com.yuseix.dragonminez.common.Reference;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModLoadUtil {

    public static void forEachMod(Consumer<IModInfo> onFetch) {
        final ModList list = ModList.get();
        final List<IModInfo> mods = ModLoadUtil.sortModList(list.getMods());
        for (IModInfo mod : mods) {
            onFetch.accept(mod);
        }
    }

    private static List<IModInfo> sortModList(List<IModInfo> mods) {
        List<IModInfo> formattedList = new ArrayList<>();
        IModInfo baseMod = null;
        for (IModInfo mod : mods) {
            final String modId = mod.getModId();
            if ("minecraft".equals(modId) || "forge".equals(modId)) {
                continue;
            }
            if (Reference.MOD_ID.equals(modId)) {
                baseMod = mod;
            } else {
                formattedList.add(mod);
            }
        }
        if (baseMod != null) {
            formattedList.add(baseMod);
        }
        return formattedList;
    }
}
