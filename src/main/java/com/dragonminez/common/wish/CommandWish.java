package com.dragonminez.common.wish;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

public class CommandWish extends Wish {
    private final String[] commands;

    public CommandWish(String name, String description, String... commands) {
        super(name, description);
        this.commands = commands;
    }

    @Override
    public void grant(ServerPlayer player) {
        for (String command : commands) {
            String parsedCommand = command.replace("%player%", player.getName().getString());
            player.getServer().getCommands().performPrefixedCommand(player.getServer().createCommandSourceStack(), parsedCommand);
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "command");
        json.addProperty("name", getName());
        json.addProperty("description", getDescription());
        JsonArray commandsArray = new JsonArray();
        for (String command : commands) {
            commandsArray.add(command);
        }
        json.add("commands", commandsArray);
        return json;
    }
}
