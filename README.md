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

**Fabric: Requires [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api)**.

**Quilt: Requires [Quilt Loader](https://quiltmc.org/install/) and [QFAPI/QSL](https://modrinth.com/mod/qsl)**.

**Forge: Requires [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/)**.

**NeoForge：Requires [NeoForge](https://projects.neoforged.net/neoforged/neoforge/)**.

## Download - 下载

CurseForge : [https://www.curseforge.com/minecraft/mc-mods/mcwifipnp](https://www.curseforge.com/minecraft/mc-mods/mcwifipnp)

Modrinth : [https://modrinth.com/mod/mcwifipnp](https://modrinth.com/mod/mcwifipnp)

MC百科 : [https://www.mcmod.cn/class/4498.html](https://www.mcmod.cn/class/4498.html)

GitHub 源码 : [https://github.com/Satxm/mcwifipnp](https://github.com/Satxm/mcwifipnp)

## Introduction - 简介

**This Branch is for Minecraft 1.20.6+ only!**

**这个分支仅适用于 Minecraft 版本 1.20.6+ ！**

Uses the vanilla Minecraft GUI style, Uses the official mojang mappings.

使用Minecraft原生界面样式，使用Mojang官方混淆表。

Modified from [TheGlitch76/mcpnp](https://github.com/TheGlitch76/mcpnp) project and UPnP module from [adolfintel/WaifUPnP](https://github.com/adolfintel/WaifUPnP).

修改自[TheGlitch76/mcpnp](https://github.com/TheGlitch76/mcpnp)项目，UPnP模块来自[adolfintel/WaifUPnP](https://github.com/adolfintel/WaifUPnP)。

## Screenshots - 界面截图

<div align="center">

![GUI EN-US](https://github.com/Satxm/images/raw/main/en.jpg)

![GUI ZH-CN](https://github.com/Satxm/images/raw/main/zh.jpg)

</div>

## What Can It Do - 它的作用

For the `Oline Mode` button, there are now three options: 
 - `Enable`: enable genuine verification, which will verify login information against the Mojang server database, only allowing players who login with a Microsoft account to join, 
 - `Disable`: not verify login information, allows offline players to join, 
 - `Disable + UUID Fixer`: Attempt to match the Mojang server user name with the player name for offline mode players to obtain a unique UUID, Meanwhile, UUIDs are retained for users logging in with Microsoft accounts, It can also prevent the loss of backpack and inventory items.

对于`正版验证`按钮，现在有三个选项：
 - `启用`：启用正版验证，将会比对Mojang服务器数据库验证登录信息，即只允许使用微软帐户登录的玩家加入；
 - `禁用`：即不验证登录信息，允许使用离线模式登录的玩家加入；
 - `禁用 + 修复UUID`：尝试使用离线模式登录的玩家名匹配Mojang服务器用户名称以获取唯一UUID，同时为使用微软帐户登录的用户保留UUID，它也可以防止背包和物品栏内容丢失。

Added a new command `/forceoffline` to control whether players are forced into offline mode without obtaining UUIDs from Mojang servers. 
 - `/forceoffline list` command can list players who in the force offline list, 
 - `/forceoffline add` command can add players to the force offline list, 
 - `/forceoffline remove` command can remove players from the force offline list.

添加了一个新命令 `/forceoffline` 以便于控制玩家是否强制玩家为离线模式，不从 Mojang 服务器获取 UUID。
 - `/forceoffline list` 命令可以查看列表中玩家，
 - `/forceoffline add` 命令可以添加玩家到列表，
 - `/forceoffline remove` 命令可以从列表中移除玩家。

Added `UUID Fixer module`, which allows offline players to obtain a unique UUID from the Mojang server, keeping the UUID fixed and not changing due to client changes.

添加了 `UUID 修复模块`，对于离线玩家，可以使离线玩家从 Mojang 服务器获取唯一的 UUID，使 UUID 固定，不会因为客户端变化而变化。

Allows you to change and lock the port number of the LAN world and you can choose whether to map this port to the public network using UPnP (if your router supports UPnP).

允许你修改并锁定局域网世界的端口号，并选择是否映射这个端口使用UPnP映射到公网（如果你的路由器支持UPnP）。

Automatically select game mode according to your game, allows you to enable or disable pvp.

根据你的游戏，自动选择游戏模式，允许你启用或禁用PVP。

Allows you to change server motd (Which is the message that is displayed in the server list of the client, below the name).

允许你自定义MOTD（是玩家客户端的多人游戏服务器列表中显示的服务器信息，显示于名称下方）。

Allows you control other players' op permissions when they join your world, and you can use `/op` and `/deop` commands to do that. You can use command `/whitelist` to build a whitelist, than use it to control players who can join your world.

你可以控制其他玩家加入时是否有op权限、是否可以作弊，你也可以使用 `/op` `/deop` 命令进行控制。你可以使用 `/whitelist` 命令构建白名单，然后用其控制其他玩家进是否允许加入你的游戏世界。

You can use command `/ban` to add players to the blacklist, use command `/ban-ip` to add IP addresses to the blacklist, use command `/banlist` to list players who in the blacklist, use command `/pardon` to remove players from the blacklist, use command `/pardon-ip` to remove IP addresses from the blacklist

你可以使用 `/ban` 来封禁玩家、 使用 `/ban-ip` 来封禁 IP 地址、 `/banlist` 命令可以查看封禁的玩家列表；你可以使用 `/pardon` 来解封玩家、 使用 `/pardon-ip` 来解封 IP地址。

Your settings will be recorded in a file, and it will be automatically loaded next time.

本模组可以自动保存配置文件，并且下次加载世界时会自动载入配置。

This mod can get your IP address, and you can choose whether to copy the IP address (such as local IPv4, globe IPv4 or IPv6) to the clipboard. in order to provide the IP address to your friends.

本模组可以获取你的IP地址（比如本地 IPv4，公网 IPv4 或 IPv6），而且你可以选择是否复制IP到剪切板，以方便联机使用。
