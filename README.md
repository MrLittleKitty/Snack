# Snitch + Slack = Snack
Snack --- A Minecraft Forge mod that posts Snitch messages to Slack

Current supported Minecraft version: **Minecraft 1.10**

#Builds
Minecraft 1.10 --- [Mediafire](https://www.mediafire.com/?17y6w8pj5b83n15)

#How to use this mod
1. Get a Slack session token. [This article is helpful.](https://www.fullstackpython.com/blog/build-first-slack-bot-python.html)
2. Put the compiled .jar for this mod file into your Minecraft mods folder
3. Start your Minecraft client and then shut it down
4. Open the settings.txt file in /mods/Snack
5. Replace the word PLACEHOLDER in "token=PLACEHOLDER" with your Slack session Token from Step #1
6. Replace the word PLACEHOLDER in "channel=PLACEHOLDER" with the name of the channel you want your Snitch messages to be posted in (The name of the channel must be all lower case) (The Slack account you are using MUST be a member of that channel and have permission to post messages)
7. Save the settings.txt file
8. Start up your Minecraft client and connect to a server with the JukeAlert plugin on it
9. Enjoy having your Slack channel spammed with messages as people enter your Snitches

#Building the mod yourself
1. Setup MinecraftForge
2. Setup Gradle
3. Run the build.gradle file from this repository
4. Follow the instructions on using the mod with your compiled .jar file

#Credits
This mod uses the [Simple Slack API Library](https://github.com/Ullink/simple-slack-api/blob/master/README.md)
Thanks to Ullink for making that library!

#License
This mod is licensed under the [MIT License](https://github.com/MrLittleKitty/Snack/blob/master/license.txt)
