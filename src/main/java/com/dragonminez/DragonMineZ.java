package com.dragonminez;

import com.dragonminez.client.DMZClient;
import com.dragonminez.common.DMZCommon;
import com.dragonminez.server.DMZServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

@Mod(Reference.MOD_ID)
public class DragonMineZ {

	public DragonMineZ() {
		checkIncompatibility("legendarytooltips", "Legendary Tooltips");
		checkIncompatibility("epicfight", "Epic Fight");
		checkIncompatibility("bettercombat", "Better Combat");

		LogUtil.info(Env.COMMON, "Initializing DragonMineZ...");

		DMZCommon.init();

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> DMZClient::init);
		DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> DMZServer::init);

		LogUtil.info(Env.COMMON, "DragonMineZ initialized successfully");
	}

	private void checkIncompatibility(String modId, String modName) {
		if (ModList.get().isLoaded(modId)) {
			String jarName = modId + ".jar";
			IModFileInfo fileInfo = ModList.get().getModFileById(modId);
			if (fileInfo != null && fileInfo.getFile() != null) jarName = fileInfo.getFile().getFileName();
			throw new IllegalStateException("§cIncompatibility Error: §eDragonMineZ §ccannot be loaded alongside §e" + modName + "§c. Please remove the §6" + jarName + " §cfile from your mods folder to continue.");
		}
	}
}