package com.darwinreforged.pweather;

import com.intellectualcrafters.plot.flag.Flags;
import com.intellectualcrafters.plot.object.Plot;
import com.plotsquared.sponge.events.PlayerEnterPlotEvent;
import com.plotsquared.sponge.events.PlayerLeavePlotEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent.Join;

import com.darwinreforged.pweather.Core.Weather;
import org.spongepowered.api.scheduler.Task;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Listeners {

  @Listener(order = Order.LATE)
  public void onPlotEnter(PlayerEnterPlotEvent event) {
    if (Core.globalWeatherOff) return;

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    if (Core.toggledPlayersContains(uuid)) return;

    Plot plot = event.getPlot();

    int weatherType = plot.getFlag(Flags.WEATHER, 0);
    // Plot has no weather
    if (weatherType <= 0) return;

    Weather plotWeather = Core.Weather.of(weatherType);
    Weather playersWeather = Core.getPlayersWeather(uuid);

    if (plot.getWorldName().replaceAll(",", ";").equals(plot.getId().toString())) {
      Task.builder()
          .delay(500, TimeUnit.MILLISECONDS)
          .execute(() -> determinePacketToSend(uuid, playersWeather, plotWeather))
          .name("WeatherTask")
          .submit(WeatherPlugin.getPlugin());
    }
    else
    {
      determinePacketToSend(uuid, playersWeather, plotWeather);
    }
  }

  @Listener
  public void onPlotExit(PlayerLeavePlotEvent event) {
    if (Core.globalWeatherOff) return;

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    Plot plot = event.getPlot();

    int weatherType = plot.getFlag(Flags.WEATHER, 0);
    // Plot has no weather
    if (weatherType <= 0) return;

    Weather plotWeather = Core.Weather.of(weatherType);
    Weather playersWeather = Core.getPlayersWeather(uuid);

    determinePacketToSend(uuid, plotWeather, playersWeather);
  }

  @Listener
  public void onPlayerJoin(Join event, @First Player player) {
    if (Core.globalWeatherOff || !Core.playerWeatherContains(player.getUniqueId())) return;
    UUID uuid = player.getUniqueId();

    switch (Core.getPlayersWeather(uuid)) {
      case RAINING:
        Core.sendPlayerWeatherPacket(uuid, Weather.RAINING);
        break;
      case LIGHTNING:
        Core.addLightningPlayer(uuid);
        break;
      case LIGHTNINGSTORM:
        Core.sendPlayerWeatherPacket(uuid, Weather.RAINING);
        Core.addLightningPlayer(uuid);
        break;
    }
  }

  @Listener
  public void onPlayerLeave(ClientConnectionEvent.Disconnect event, @First Player player) {
    Core.removeLightningPlayer(player.getUniqueId());
  }

  private void determinePacketToSend(UUID uuid, Weather from, Weather to) {
    // No need to send any weather packets
    if (from == to) return;

    switch (to) {
      case RESET:
        Core.sendPlayerWeatherPacket(uuid, Weather.RESET);
        Core.removeLightningPlayer(uuid);
        break;

      case RAINING:
        if (from != Weather.LIGHTNINGSTORM) {
          Core.sendPlayerWeatherPacket(uuid, Weather.RAINING);
        }
        Core.removeLightningPlayer(uuid);

        break;

      case LIGHTNING:
        // You only need to send a clear packet if its raining
        if (from != Weather.RESET) {
          Core.sendPlayerWeatherPacket(uuid, Weather.RESET);
        }
        Core.addLightningPlayer(uuid);
        break;

      case LIGHTNINGSTORM:
        // You only need to send a raining packet if its clear
        if (from != Weather.RAINING) {
          Core.sendPlayerWeatherPacket(uuid, Weather.RAINING);
        }
        Core.addLightningPlayer(uuid);
        break;
    }
  }
}
