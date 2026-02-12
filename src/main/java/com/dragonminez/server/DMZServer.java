package com.dragonminez.server;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.server.commands.*;
import com.dragonminez.server.world.feature.OverworldFeatures;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class DMZServer {

    public static void init() {
        LogUtil.info(Env.SERVER, "Initializing DragonMineZ Server...");
    }

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		StatsCommand.register(dispatcher);
		BonusCommand.register(dispatcher);
		EffectsCommand.register(dispatcher);
		SkillsCommand.register(dispatcher);
		PointsCommand.register(dispatcher);
		MasteryCommand.register(dispatcher);
		LocateCommand.register(dispatcher);
		PartyCommand.register(dispatcher);
		StoryCommand.register(dispatcher);
		ReviveCommand.register(dispatcher);

		LogUtil.info(Env.SERVER, "DragonMineZ Commands Registered");
	}
}

