[English](./README.md) | [中文](./README.zh-CN.md) | **日本語** | [Español](./README_es.md)

[__中国語__![zh-cn](https://img.shields.io/badge/lang-zh--cn-green.svg)](README.zh-CN.md)

# LAN World プラグアンドプレイ

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

## 依存ライブラリ

**Fabric: [Fabric Loader](https://fabricmc.net/use/), [Fabric API](https://modrinth.com/mod/fabric-api)**.

**Quilt: [Quilt Loader](https://quiltmc.org/install/), [QFAPI/QSL](https://modrinth.com/mod/qsl)**.

**Forge: [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/)**.

**NeoForge: [NeoForge](https://projects.neoforged.net/neoforged/neoforge/)**.

## ダウンロード

CurseForge : [https://www.curseforge.com/minecraft/mc-mods/mcwifipnp](https://www.curseforge.com/minecraft/mc-mods/mcwifipnp)

Modrinth : [https://modrinth.com/mod/mcwifipnp](https://modrinth.com/mod/mcwifipnp)

MC百科 : [https://www.mcmod.cn/class/4498.html](https://www.mcmod.cn/class/4498.html)

GitHub : [https://github.com/Satxm/mcwifipnp](https://github.com/Satxm/mcwifipnp)

## はじめに

**このブランチはMinecraft 26.2向けです！**

オリジナルのMinecraft GUIスタイルを採用し、公式のMojangマッピングが使用されています。

* [TheGlitch76/mcpnp](https://github.com/TheGlitch76/mcpnp) を改良したものです。
* UPnPモジュールは [adolfintel/WaifUPnP](https://github.com/adolfintel/WaifUPnP) および [RetGal/WaifUPnP](https://github.com/RetGal/WaifUPnP) から採用しています。
* `Online Mode` と `UUID Fix` は [Rikka0w0/LanServerProperties](https://github.com/rikka0w0/LanServerProperties) から取り入れています。

## スクリーンショット

<div align="center">

![GUI](https://cdn.modrinth.com/data/cached_images/7679311208018ad159099824b623f9ed76292975.jpeg)

</div>

## 使用方法

1. `Online Mode`ボタンには3つのオプションがあります：
 - `Enable`：Mojangサーバーデータベースでログイン情報を確認し、正規のMicrosoftアカウントを持つプレイヤーのみの参加を許可します。
 - `Disable`：検証を行わず、オフラインプレイヤーを含む誰でも参加できます。
 - `Disable + UUID Fixer`：上記と同様ですが、UUID Fixerを有効にします。UUID Fixerのデフォルト動作では、プレイヤーの名前がMojangサーバーに登録されている場合、「Online Mode」と同様に公式の一意なUUIDが使用されます。例外を設定するには `/uuidfixer force`コマンドを使用します。このモードは、「Online Mode」から「Offline Mode」へ切り替える際にバックパックやインベントリを保持したい場合に役立ちます。
 - 対応するコマンドは `/onlinemode` および `/uuidfixer enabled` です。

2. コマンド `/uuidfixer` は、「Disable + UUID Fixer」モードにおいてユーザー名がUUIDにどのようにマッピングされるかを制御します。
 - コマンド `/uuidfixer enabled` でUUIDフィクサーの有効/無効を切り替えます。
 - コマンド `/uuidfixer list` ですべてのマッピング規則を一覧表示します。
 - コマンド `/uuidfixer force` で新しい規則を追加したり、既存の規則を更新したりできます。
 - コマンド `/uuidfixer remove` で既存の規則を削除します。
 - コマンド `/uuidfixer test` で特定のユーザー名に適用されているポリシーを確認できます。

3. サーバーのポート番号を変更でき、ルーターがUPnPをサポートしている場合は、このポートをUPnPを使ってパブリックネットワークにマップするかどうかを選択できます。
GUIのボタンまたは`/upnp`コマンドを使用して、UPnPサポートの有効化と無効化を切り替えられます。

4. PvPの有効化または無効化ができます。

5. サーバーのMotD（サーバーリストにおけるサーバー名の下に表示されるメッセージ）を変更できます。

6. プレイヤーが自分のワールドに参加する際の権限を制御できます。/opおよび/deopコマンドを使用してOPリストを管理でき、/whitelistコマンドを使ってホワイトリストを作成し、それを利用してワールドに参加できるプレイヤーを制御することも可能です。

7. プレイヤーをブラックリストに追加するには `/ban` コマンドを使用します。IPアドレスをブラックリストに追加するには `/ban-ip` コマンドを、ブラックリストに登録されているプレイヤーの一覧を表示するには `/banlist` コマンドを利用します。ブラックリストからプレイヤーを削除するには `/pardon` コマンド、IPアドレスを削除するには `/pardon-ip` コマンドを使います。

8. 設定内容はファイルに記録され、次回起動時に自動的に読み込まれます。

9. このモッドはユーザーのIPアドレスを取得でき、友人にそのIPアドレス（ローカルIPv4、グローバルIPv4、またはIPv6など）をクリップボードにコピーするかどうかを選択できます。IPアドレスを友人に提供するためのものです。より詳細なIP情報を確認したい場合は、/ipコマンドを使用できます。

10. サーバーが起動した後でも上記の設定のほとんどは変更できますが、一部のオプションは新たに参加するプレイヤーにのみ適用されます。

11. 左下角のボタンをクリックすることで、元の「LANに開放」画面に戻ることもできます。

12. 専用サーバーにインストールした場合、利用できるのは「UUID Fixer」機能のみです。サーバーのルートフォルダーに`uuid_fixer.json`が存在する場合にのみ有効になります。専用サーバー側では`/uuidfixer`コマンドが利用可能です。その他の機能は一切ありません。

## 開発者向け
### Fabricアーティファクトのコンパイル
```
git clone https://github.com/Satxm/mcwifipnp.git
cd mcwifipnp
.\gradlew.bat build
.\gradlew.bat :fabric:runClient
```
対応するアーティファクトをビルドするには、`fabric` の代わりに `forge`、`neoforge`、または `quilt` を使用してください。

### Eclipse
開発を開始するには、ルートフォルダをEclipse内のgradleプロジェクトとしてインポートします。
初期の移植や開発を高速化するために、`settings.gradle`内の一部のターゲットを無効にすることをお勧めします。
