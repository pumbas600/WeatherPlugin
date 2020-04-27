# WeatherPlugin

WeatherPlugin is a **sponge** plugin which allows users to set their personal weather and plot weather independent of 
the server weather.

A player's personal weather is saved even after they quit the server, but not after a server restart.

**Note:** This plugin was designed with the understanding that the server using it will always have clear weather. 
if your server weather is not always clear, this plugin may not function as intended.

Some of the features can be seen [here](https://youtu.be/iO_Yi8CaJ_8)

## Dependecies:
WeatherPlugin requires:

> Packetgate plugin needs to be added to the server.
More information, including how to install packetgate can be found [here](https://github.com/CrushedPixel/PacketGate)

> PlotSquared plugin needs to be added to the server for plot functionality to work correctly.
**Note:** The plotsquared plugin has been modified for specific usage of this plugin through the addition of an Integer WeatherFlag. 
This can be found in the /libs file.

## Commands:

```
/pweather set [WeatherType]
```
***Requires:*** *weatherplugin.command.set permission*

***Description:*** Sets a players personal weather.

```
/pweather plot [WeatherType]
```
***Requires:*** *weatherplugin.command.plot permission + to be added to the plot*

***Description:*** Toggles plot weather overriding your personal weather. 

```
/pweather toggle
```
***Requires:*** *weatherplugin.command.toggle permission*

***Description:*** Displays information regarding the current player, the plot they're currently in, and other information
useful for debugging any issues involved with this plugin.

```
/pweather debug
```
***Requires:*** *weatherplugin.command.debug permission*

***Description:*** Sets a plot's weather. 

```
/pweather globaltoggle
```
***Requires:*** *weatherplugin.command.globalweather permission*

***Description:*** An administrative command which toggles WeatherPlugin for all users. This will send a clear packet initally to each player and will prevent any further packets from being sent while toggled. This will not remove any players personal weather settings, and once untoggled, their weather will be set to that once again.

## Weather Types:

Each weather type supports a range of aliases. These are as follows:

RAIN:
- rain
- rainy
- raining

Please note that snow and rain are interchangeable and are biome dependent. For more information on this, check out [this](https://minecraft.gamepedia.com/Snowfall)

SNOW:
- snow
- snowing

CLEAR:
- clear
- reset
- sunny
- undo

LIGHTNING: (Only the lightning bolts)
- lightning
- thunder

LIGHTNINGSTORM: (Lightning bolts and rain - this is the normal lightning that is experienced in Minecraft)
- lightningstorm
- thunderstorm
- storm
