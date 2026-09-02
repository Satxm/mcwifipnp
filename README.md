[__中文__![zh-cn](https://img.shields.io/badge/lang-zh--cn-green.svg)](README.zh-CN.md)

# LAN World Plug-n-Play

<div align="center">

[![1][1]][2] [![3][3]][4] [![5][5]][6] [![7][7]][8]

</div>

[1]: https://img.shields.io/modrinth/dt/RTWpcTBp?label=Modrinth%0aDownloads&logo=modrinth&style=flat&color=45A35F&labelcolor=2D2D2D
[2]: https://modrinth.com/mod/mcwifipnp

[3]: https://img.shields.io/curseforge/dt/450250?label=CurseForge%0aDownloads&logo=curseforge&style=flat&color=E36639&labelcolor=2D2D2D
[4]: https://www.curseforge.com/minecraft/mc-mods/mcwifipnp

[5]: https://img.shields.io/badge/Available%20for-%201.15%20to%201.21-47376F?logo=files&color=377BCB&labelcolor=2D2D2D
[6]: https://modrinth.com/mod/mcwifipnp/versions

[7]: https://img.shields.io/github/license/Satxm/mcwifipnp?label=License&logo=github&style=flat&color=E51050&labelcolor=2D2D2D
[8]: https://github.com/satxm/mcwifipnp

## Dependencies

**Fabric: [Fabric Loader](https://fabricmc.net/use/), [Fabric API](https://modrinth.com/mod/fabric-api)**.

**Quilt: [Quilt Loader](https://quiltmc.org/install/), [QFAPI/QSL](https://modrinth.com/mod/qsl)**.

**Forge: [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/)**.

**NeoForge: [NeoForge](https://projects.neoforged.net/neoforged/neoforge/)**.

## Download

CurseForge : [https://www.curseforge.com/minecraft/mc-mods/mcwifipnp](https://www.curseforge.com/minecraft/mc-mods/mcwifipnp)

Modrinth : [https://modrinth.com/mod/mcwifipnp](https://modrinth.com/mod/mcwifipnp)

MC百科 : [https://www.mcmod.cn/class/4498.html](https://www.mcmod.cn/class/4498.html)

GitHub : [https://github.com/Satxm/mcwifipnp](https://github.com/Satxm/mcwifipnp)

## Introduction

**This Branch is for Minecraft 26.2!**

Uses the vanilla Minecraft GUI style, Uses the official mojang mappings.

* Modified from [TheGlitch76/mcpnp](https://github.com/TheGlitch76/mcpnp)
* UPnP module from [adolfintel/WaifUPnP](https://github.com/adolfintel/WaifUPnP) & [RetGal/WaifUPnP](https://github.com/RetGal/WaifUPnP).
* `Online Mode` and `UUID Fix` from[Rikka0w0/LanServerProperties](https://github.com/rikka0w0/LanServerProperties).

## Screenshots

<div align="center">

![GUI](https://cdn.modrinth.com/data/cached_images/3c2d2a4a0f0d88bbc79fe654945d7827ccc91aca.jpeg)

</div>

## Usage

1. The `Online Mode` button has three options: 
 - `Enable`: verify login information against the Mojang server database, only allow players with genuine Microsoft account to join.
 - `Disable`: no verification, allows anyone, including offline players to join.
 - `Disable + UUID Fixer`: Similar to above, enables the UUID Fixer. The default behavior of UUID Fixer is that, if a player has his name on the Mojang server, the official unique UUID will be used, just like in the "Online Mode". Exceptions can be added using the `/uuidfixer force` command. This mode can be useful to preserve backpacks and inventories when switching from "Online Mode" to "Offline Mode".
 - The corresponding commands are `/onlinemode` and `/uuidfixer enabled`.

2. The command `/uuidfixer` controls how usernames are mapped to UUIDs in the `Disable + UUID Fixer` mode.
 - command `/uuidfixer enabled` toggles the UUID fixer on and off.
 - command `/uuidfixer list` lists all mapping rules.
 - command `/uuidfixer force` adds a new rule or update an existing rule.
 - command `/uuidfixer remove` removes an existing rule.
 - command `/uuidfixer test` allows you to check the policy applied to a username.

3. Allows you to change server's port number and you can choose whether to map this port to the public network using UPnP (if your router supports UPnP).
Use the GUI button or the `/upnp` command to toggle UPnP support on and off.

4. Allows you to enable or disable pvp.

5. Allows you to change server motd (The message displayed below the server name in the server list).

6. Allows you control other players' permissions when they join your world. Use `/op` and `/deop` commands to manipulate the OP list. You can also use command `/whitelist` to build a whitelist and use it to control players who can join your world.

7. You can use command `/ban` to blacklist players. Use command `/ban-ip` to add IP addresses to the blacklist. Use command `/banlist` to list blacklisted players. Use command `/pardon` to remove players from the blacklist, use command `/pardon-ip` to remove IP addresses from the blacklist

8. Your settings will be recorded in a file, and it will be automatically loaded next time.

9. This mod can get your IP address, and you can choose whether to copy the IP address (such as local IPv4, globe IPv4 or IPv6) to the clipboard. in order to provide the IP address to your friends. The command `/ip` can retrieve more IP information.

10. You can change most of the above settings once the server starts, but some options are only applied to newly joined players.

11. You can also go back to the vanilla `Open to Lan` screen by clicking in the button on the bottom-left corner.

12. When installed to a dedicated server, only the `UUID Fixer` function is available. It can only be enabled if `uuid_fixer.json` exists in the root server folder. The `/uuidfixer` command is available on the dedicated server side. This mod does nothing otherwise.

## For Developers
### Compile Fabric Artifacts
```
git clone https://github.com/Satxm/mcwifipnp.git
cd mcwifipnp
.\gradlew.bat build
.\gradlew.bat :fabric:runClient
```
Replace `fabric` with `forge`, `neoforge`, or `quilt` to build the corresponding artifacts.

### Eclipse
Import the root folder as a gradle project in Eclipse to start the development.
You may want to disable some targets in `settings.gradle` to speed up the initial porting/development.
