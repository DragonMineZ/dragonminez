package com.dragonminez.core.common.config.util;

import com.dragonminez.mod.common.Reference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;

public class ModLoadUtil {

  public static void forEachMod(BiConsumer<ModList, IModInfo> onFetch) {
    final ModList list = ModList.get();
    final List<IModInfo> mods = ModLoadUtil.sortModList(list.getMods());
    for (IModInfo mod : mods) {
      onFetch.accept(list, mod);
    }
  }

  public static Path getModPath(ModList mods, String modIdentifier) {
    final IModFileInfo modFileInfo = mods.getModFileById(modIdentifier);
    if (modFileInfo == null) {
      return null;
    }
    final IModFile modFile = modFileInfo.getFile();
    return modFile.getFilePath();
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
