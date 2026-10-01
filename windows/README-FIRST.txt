UNTAMED REALMS - Windows quick start
====================================

You only need the normal Minecraft Launcher (Java Edition) and an internet connection.
Everything else (Java, NeoForge, all mods) is downloaded automatically.

PLAY ON THIS PC (server + game on the same laptop)
--------------------------------------------------
1. Double-click  Start-Server.bat
   - Leave the window open. First start takes a few minutes; it is ready when you see "Done".
   - If Windows asks "Do you want to allow Java...?" click Allow.

2. Close the Minecraft Launcher, then double-click  Install-Modpack.bat
   - Wait until it says "Done!". (Only needed once, and again after updates.)

3. Open the Minecraft Launcher, choose "Untamed Realms" next to PLAY, press PLAY.

4. In game: Multiplayer -> Direct Connection -> type  localhost  -> Join Server.

5. In the server window type:  op YourMinecraftName   (gives you admin commands)

TEST WORLD (everything in one place)
------------------------------------
Double-click  Start-Test-World.bat  instead of Start-Server.bat (only one of the two can run at a
time; join with  localhost  as usual). It uses the same server and mods but a separate save,
"server\test-world", so your normal world is never touched.
- You spawn on a floating TEST HUB above the starting village, as admin, with a CONTROL BOOK:
  click its lines to reset your character, set every skill, learn all spells, start or skip quests,
  change time/weather/game mode, or jump down to the real village and back.
- Forge Yard: every station, materials, all weapons by tier, armor stands, rings & amulets.
- Magic: spell tomes, skill books, enchanting, training dummies.
- Town Square: every NPC (quests, shops, trainers), two notice boards, quest items and food.
- Wilds: trees to fell, a ripe farm, a fishing pond, an ore wall, tools, and a mob arena with
  click-to-spawn signs.
- Broke something? Control book -> "Rebuild the hub" restocks everything.
- Reset-Test-World.bat starts the test world over (like Reset-World.bat does for the normal one).

UPDATING
--------
Nothing to download: Start-Server.bat and Install-Modpack.bat check for a newer build every time
you run them and update this folder automatically (your world in "server\world" is kept).
After an update, run Install-Modpack.bat again so the game has the same mods as the server.
The launcher always keeps exactly one "Untamed Realms" installation.

START OVER (fresh world for testing)
------------------------------------
Stop the server (type  stop ), then double-click  Reset-World.bat . It moves the current world and
every character in it to server\old-worlds (the 3 newest are kept); the next Start-Server.bat
creates a brand-new world with a new seed, and you pick a class again when you join.

FRIENDS
-------
Friends run only Install-Modpack.bat and connect to your IP address. They can only reach a
server on your laptop if you forward port 25565 on your router (or use a tool like playit.gg).

IF WINDOWS BLOCKS THE FILES
---------------------------
"Windows protected your PC": click "More info" -> "Run anyway".
If nothing happens, right-click the .zip -> Properties -> tick "Unblock" -> OK, then extract again.

WHERE THINGS ARE
----------------
Game files:   %APPDATA%\.minecraft\untamed-realms   (your normal Minecraft is untouched)
Server files: the "server" folder next to this file
Keys in game: K skills, J journal, R cast spell, hold Z spell wheel, N spellbook, Left Alt dodge roll,
              B backpack, M map. Keys you change yourself are kept.
