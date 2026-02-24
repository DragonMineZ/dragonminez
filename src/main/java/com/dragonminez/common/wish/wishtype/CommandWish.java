package com.dragonminez.common.wish.wishtype;

import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;

public class CommandWish extends Wish {
    private final String[] commands;

    public CommandWish(String name, String description, String... commands) {
        super(name, description, "command");
        this.commands = commands;
    }

    @Override
    public void grant(ServerPlayer player) {
        for (String command : commands) {
            String parsedCommand = command.replace("%player%", player.getName().getString());
            player.getServer().getCommands().performPrefixedCommand(player.getServer().createCommandSourceStack().withPermission(4), parsedCommand);
        }
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, CommandWish.class);
    }
}
