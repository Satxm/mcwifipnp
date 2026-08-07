[English](./README.md) | [中文](./README.zh-CN.md) | [日本語](./README_ja.md) | **Español**

[__Chino__![zh-cn](https://img.shields.io/badge/lang-zh--cn-green.svg)](README.zh-CN.md)

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

## Dependencias

**Fabric: [Fabric Loader](https://fabricmc.net/use/), [Fabric API](https://modrinth.com/mod/fabric-api)**.

**Quilt: [Quilt Loader](https://quiltmc.org/install/), [QFAPI/QSL](https://modrinth.com/mod/qsl)**.

**Forge: [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/)**.

**NeoForge: [NeoForge](https://projects.neoforged.net/neoforged/neoforge/)**.

## Descarga

CurseForge: [https://www.curseforge.com/minecraft/mc-mods/mcwifipnp](https://www.curseforge.com/minecraft/mc-mods/mcwifipnp)

Modrinth: [https://modrinth.com/mod/mcwifipnp](https://modrinth.com/mod/mcwifipnp)

Enciclopedia de MC: [https://www.mcmod.cn/class/4498.html](https://www.mcmod.cn/class/4498.html)

GitHub: [https://github.com/Satxm/mcwifipnp](https://github.com/Satxm/mcwifipnp)

## Introducción

**Esta rama es para Minecraft 26.2.**

Utiliza el estilo de interfaz gráfica de Minecraft estándar, y emplea las asignaciones oficiales de Mojang.

* Modificado a partir de [TheGlitch76/mcpnp](https://github.com/TheGlitch76/mcpnp)  
* Módulo UPnP de [adolfintel/WaifUPnP](https://github.com/adolfintel/WaifUPnP) y [RetGal/WaifUPnP](https://github.com/RetGal/WaifUPnP).  
* Funcionalidades “Online Mode” y “UUID Fix” de [Rikka0w0/LanServerProperties](https://github.com/rikka0w0/LanServerProperties).

## Capturas de pantalla

<div align="center">

![GUI](https://cdn.modrinth.com/data/cached_images/7679311208018ad159099824b623f9ed76292975.jpeg)

</div>

## Uso

1. El botón “Modo en línea” tiene tres opciones: 
 - `Activar`: verificar la información de inicio de sesión contra la base de datos del servidor de Mojang, permitiendo unirse únicamente a jugadores con cuenta real de Microsoft.
 - `Desactivar`: sin verificación, permitiendo que cualquier persona, incluidos los jugadores fuera de línea, se una.
 - `Desactivar + Corrector de UUID`: similar al anterior, activa el Corrector de UUID. El comportamiento por defecto de este corrector es que, si el nombre de un jugador está registrado en el servidor de Mojang, se utilizará el UUID único oficial, al igual que en el “Modo en línea”. Se pueden añadir excepciones mediante el comando `/uuidfixer force`. Este modo resulta útil para conservar las mochilas e inventarios al pasar del “Modo en línea” al “Modo fuera de línea”.
 - Los comandos correspondientes son `/onlinemode` y `/uuidfixer enabled`.

2. El comando `/uuidfixer` controla cómo se asignan los nombres de usuario a los UUID en el modo «Disable + UUID Fixer».
 - El comando `/uuidfixer enabled` activa o desactiva el corrector de UUID.
 - El comando `/uuidfixer list` muestra todas las reglas de asignación.
 - El comando `/uuidfixer force` agrega una nueva regla o actualiza una existente.
 - El comando `/uuidfixer remove` elimina una regla existente.
 - El comando `/uuidfixer test` permite verificar la política aplicada a un nombre de usuario.

3. Permite cambiar el número de puerto del servidor, y usted puede decidir si desea mapear dicho puerto a la red pública mediante UPnP (si su router admite UPnP). Utilice el botón de la interfaz gráfica o el comando `/upnp` para activar o desactivar la compatibilidad con UPnP.

4. Permite activar o desactivar el PvP.

5. Permite cambiar el mensaje por defecto del servidor (el mensaje que se muestra debajo del nombre del servidor en la lista de servidores).

6. Permite controlar los permisos de otros jugadores cuando se unen a su mundo. Utilice los comandos `/op` y `/deop` para gestionar la lista de jugadores con privilegios especiales. También puede usar el comando `/whitelist` para crear una lista blanca y emplearla para controlar qué jugadores pueden unirse a su mundo.

7. Puede utilizar el comando `/ban` para poner en lista negra a los jugadores. Use el comando `/ban-ip` para agregar direcciones IP a la lista negra. Utilice el comando `/banlist` para ver la lista de jugadores en lista negra. Aplique el comando `/pardon` para eliminar jugadores de la lista negra, y use el comando `/pardon-ip` para quitar direcciones IP de dicha lista.

8. Sus configuraciones se guardarán en un archivo y se cargarán automáticamente la próxima vez.

9. Este mod puede obtener su dirección IP, y usted puede decidir si copiarla (ya sea IPv4 local, IPv4 global o IPv6) al portapapeles para compartirla con sus amigos. La orden `/ip` permite obtener más información sobre la IP.

10. Puede modificar la mayoría de las configuraciones anteriores una vez que el servidor esté en marcha, pero algunas opciones solo se aplican a los jugadores que se unan por primera vez.

11. También puede volver a la pantalla estándar “Abrir para LAN” haciendo clic en el botón de la esquina inferior izquierda.

12. Cuando se instala en un servidor dedicado, solo está disponible la función de `UUID Fixer`. Solo se puede activar si existe el archivo `uuid_fixer.json` en la carpeta raíz del servidor. En el lado del servidor dedicado está disponible el comando `/uuidfixer`. Este mod no realiza ninguna otra acción.

## Para desarrolladores
### Compilar artefactos de Fabric
```
git clone https://github.com/Satxm/mcwifipnp.git
cd mcwifipnp
.\gradlew.bat build
.\gradlew.bat :fabric:runClient
```
Reemplace `fabric` por `forge`, `neoforge` o `quilt` para compilar los artefactos correspondientes.

### Eclipse
Importe la carpeta raíz como proyecto Gradle en Eclipse para comenzar el desarrollo.
Es posible que desee desactivar algunos objetivos en `settings.gradle` para acelerar el proceso inicial de adaptación/desarrollo.
