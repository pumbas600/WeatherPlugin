package com.darwinreforged.pweather;

import com.google.inject.Inject;
import com.intellectualcrafters.plot.flag.Flags;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListener;
import eu.crushedpixel.sponge.packetgate.api.registry.PacketGate;
import net.minecraft.network.play.server.SPacketSpawnGlobalEntity;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import com.darwinreforged.pweather.Core.Weather;


import java.util.Optional;
import java.util.UUID;

@Plugin(
    id = "weatherplugin",
    name = "Weatherplugin",
    description = "A plugin which sets personal and plot weather",
    dependencies = {@Dependency(id = "packetgate"), @Dependency(id = "forge")})
public class WeatherPlugin
{

  @Inject private Logger logger;

  private static WeatherPlugin plugin;

  public static WeatherPlugin getPlugin(){
      return plugin;
  }

  @Listener
  public void onServerStart(GameStartedServerEvent event) {
    Optional<PacketGate> packetGateOptional = Sponge.getServiceManager().provide(PacketGate.class);
    if (packetGateOptional.isPresent()) {
      logger.info("PacketGate is present");

      initialiseCommand();
      Sponge.getEventManager().registerListeners(this, new Listeners());
      logger.info("Weather Plugin successfull initialised");
    } else {
      logger.error(
          "PacketGate is not installed. This is required for Weather Plugin to function correctly");
    }

    WeatherPlugin.plugin = this;
  }

  private void initialiseCommand() {
    CommandSpec plotWeatherCommand =
        CommandSpec.builder()
            .description(Text.of("Set the weather of your plot"))
            .permission("weatherplugin.command.plot")
            .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("weather"))))
            .executor(new PlotWeatherCommand())
            .build();

    CommandSpec playerWeatherCommand =
        CommandSpec.builder()
            .description(Text.of("Set your personal weather"))
            .permission("weatherplugin.command.set")
            .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("weather"))))
            .executor(new PlayerWeatherCommand())
            .build();

    CommandSpec toggleGlobalWeatherCommand =
        CommandSpec.builder()
            .description(Text.of("Toggles the weather of all players personal weather"))
            .permission("weatherplugin.command.globalweather")
            .arguments(GenericArguments.none())
            .executor(
                (src, args) -> {
                  if (!(src instanceof Player)) {
                    src.sendMessage(Text.of("This command can only be executed by a player"));
                    return CommandResult.success();
                  }

                  // If true, this prevents any packets from being intercepted.
                  Player player = (Player) src;
                  Core.globalWeatherOff = !Core.globalWeatherOff;
                  Core.sendMessage(player, "Global weather: " + (Core.globalWeatherOff ? "OFF" : "ON"));

                  if (Core.getLightningPlayers().size() != 0) {
                      if (Core.globalWeatherOff) {
                          LightningManager.stopLightningScheduler();
                      }
                      else {
                          LightningManager.startlightningScheduler();
                      }
                  }

                  for (Player onlinePlayer : Sponge.getServer().getOnlinePlayers())
                  {
                      UUID uuid = onlinePlayer.getUniqueId();
                      if (Core.globalWeatherOff)
                      {
                          Core.sendMessage(onlinePlayer, "Sorry, but pweather has been disabled");
                          PlotPlayer plotPlayer = PlotPlayer.wrap(onlinePlayer);
                          if (plotPlayer.getCurrentPlot() != null) {
                              Plot plot = plotPlayer.getCurrentPlot();
                              //Sends packet to all online players, setting their weather to server's weather
                              //Player has weather set in the plot they're currently in or has their personal weather set
                              if (plot.getFlag(Flags.WEATHER, 0) > 0 || Core.playerWeatherContains(uuid)) {
                                  Core.sendPlayerWeatherPacket(uuid, Weather.RESET, true);
                              }
                          }
                      }
                      else {
                          Core.sendMessage(onlinePlayer, "Pweather has been renabled!");
                          Weather weather = Core.getPlayersWeather(uuid);
                          if (weather == Weather.RAINING || weather == Weather.LIGHTNINGSTORM) {
                              Core.sendPlayerWeatherPacket(uuid, Weather.RAINING);
                          }

                      }
                  }
                  return CommandResult.success();
                })
            .build();

      CommandSpec togglePlotWeatherCommand =
              CommandSpec.builder()
                      .description(Text.of("Toggles plot weather overriding personal weather"))
                      .permission("weatherplugin.command.toggle")
                      .arguments(GenericArguments.none())
                      .executor(
                              (src, args) -> {
                                  if (!(src instanceof  Player)) return CommandResult.success();
                                  Player player = (Player) src;
                                  UUID uuid = player.getUniqueId();
                                  if (Core.toggledPlayersContains(uuid)) {
                                      Core.removeToggledPlayer(uuid);
                                      Core.sendMessage(player, "Disable plot weather override set to: OFF");
                                      return CommandResult.success();
                                  }
                                  Core.addToggledPlayer(uuid);
                                  Core.sendMessage(player, "Disable plot weather override set to: ON");
                                  return CommandResult.success();
                              }
                      ).build();

    CommandSpec debugCommand =
            CommandSpec.builder()
                    .description(Text.of("Prints out relevant information about the player"))
                    .permission("weatherplugin.command.debug")
                    .child(toggleGlobalWeatherCommand, "globaltoggle")
                    .child(togglePlotWeatherCommand, "toggle")
                    .executor(
                        (src, args) -> {
                            if (!(src instanceof Player)) return CommandResult.success();
                            Player player = (Player) src;
                            Core.sendMessage(player, "Current Weather Type: " +
                                    Core.getPlayersWeather(player.getUniqueId()));
                            Core.sendMessage(player, "Is Lightning Player: " +
                                    Core.lightningPlayersContains(player.getUniqueId()));
                            Core.sendMessage(player, LightningManager.lightningSchedulerStatus());

                            //If the players in a plot, send what type of weather it has too:
                            PlotPlayer plotPlayer = PlotPlayer.wrap(player);
                            if (plotPlayer.getCurrentPlot() != null) {
                                Plot plot = plotPlayer.getCurrentPlot();
                                int weatherType = plot.getFlag(Flags.WEATHER, 0);
                                Core.sendMessage(player, "Plot Weather set to: " + Core.Weather.of(weatherType));
                            }
                            return CommandResult.success();
                        }
                    ).build();



    Sponge.getCommandManager().register(this, debugCommand, "weatherdebug", "dweather");
    Sponge.getCommandManager().register(this, playerWeatherCommand, "weatherplugin", "pweather");
    Sponge.getCommandManager().register(this, plotWeatherCommand, "plotweather");
  }
}
