package com.darwinreforged.pweather;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import com.darwinreforged.pweather.Core.Weather;

import java.util.Optional;
import java.util.UUID;

public class PlayerWeatherCommand implements CommandExecutor {
  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
    if (!(src instanceof Player)) {
      src.sendMessage(Text.of("This command can only be executed by players"));
      return CommandResult.success();
    }

    Player player = (Player) src;
    UUID uuid = player.getUniqueId();

    Optional<String> optionalWeather = args.getOne("weather");
    if (optionalWeather.isPresent()) {
      String commandWeather = optionalWeather.get();
      Weather weather = Core.parseWeather(commandWeather);

      switch(weather)
      {
        case UNKNOWN:
          Core.sendMessage(
                  player,
                  "That weather type is unknown. If you feel this is an error, "
                          + "feel free to let a staff member know");
          return CommandResult.success();

        case RESET:
          // set weather to clear
          Core.sendPlayerWeatherPacket(uuid, Weather.RESET);
          if (Core.playerWeatherContains(uuid)) Core.removePlayerWeather(uuid);

          break;

        case RAINING:
          Core.addToPlayerWeather(uuid, Weather.RAINING);
          Core.sendPlayerWeatherPacket(uuid, Weather.RAINING);

          break;

        case LIGHTNING:
          //Lightning bolts are sent to players in this arrayList
          Core.sendPlayerWeatherPacket(uuid, Weather.RESET);
          Core.addToPlayerWeather(uuid, Weather.LIGHTNING);

          break;

        case LIGHTNINGSTORM:
          Core.addToPlayerWeather(uuid, Weather.LIGHTNINGSTORM);
          //Set player's weather to raining
          Core.sendPlayerWeatherPacket(uuid, Weather.RAINING);
          break;
      }
      Core.sendMessage(player, "Personal Weather set to: " + weather.getDisplayName());
      return CommandResult.success();

    } else {
      Core.sendMessage(player, "Valid weather types: rain, rainy, raining, snowing, snow, lightning, thunder, storm, lightningstorm, thunderstorm, reset, clear, sunny, undo");
    }

    return CommandResult.empty();
  }
}
