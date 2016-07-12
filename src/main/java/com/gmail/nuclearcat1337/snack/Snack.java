package com.gmail.nuclearcat1337.snack;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.HTTP429Exception;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;

import java.io.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mr_Little_Kitty on 7/10/2016.
 */
@Mod(modid = Snack.MODID, name = Snack.MODNAME, version = Snack.MODVERSION)
public class Snack
{
    public static final String MODID = "snack";
    public static final String MODNAME = "Snack";
    public static final String MODVERSION = "1.0.0";
    private static final Logger logger = Logger.getLogger(MODID);

    private static final String PLACEHOLDER = "PLACEHOLDER";
    private static final String SETTINGS_SLACK_TOKEN = "slack-token";
    private static final String SETTINGS_SLACK_CHANNEL = "slack-channel";
    private static final String SETTINGS_DISCORD_TOKEN = "discord-token";
    private static final String SETTINGS_DISCORD_CHANNEL = "discord-channel";

    //A regular expression that matches all 3 of the snitch alert messages (log in, log out, enter)
    private static final Pattern snitchAlertPattern =
            Pattern.compile("\\s*\\*\\s*([^\\s]*)\\s\\b(?:entered snitch at|logged out in snitch at|logged in to snitch at)\\b\\s*([^\\s]*)\\s\\[([^\\s]*)\\s([-\\d]*)\\s([-\\d]*)\\s([-\\d]*)\\]");

    //The slack session to post messages to
    private SlackSession slackSession = null;

    //The discord session to post messages to
    private IDiscordClient discordSession = null;

    //The token used to connect to the slack slackSession
    private String slackToken = null;

    //The discord token used to connect to the discord server
    private String discordToken = null;

    //The slack channel to post messages to
    private String slackChannel = null;

    //The discord channel to post messages to
    private String discordChannel = null;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        //Load the channel and token from the settings file
        boolean success = loadSettings(new File(Minecraft.getMinecraft().mcDataDir,"mods/Snack/settings.txt"));

        //If we loaded the token and channel, continue
        if(!success || ((slackToken == null || slackChannel == null) && (discordToken == null && discordChannel == null)))
        {
            logger.info("[Snack] SHUTTING DOWN because could not load (the slack token and the slack channel) or (the discord token and the discord channel)");
            //By returning at this point we dont register any listeners and are effectively shut down
            return;
        }

        //Slack session
        try
        {
            //If we loaded the necessary data to create a slack session
            if(slackToken != null && slackSession != null)
            {
                //Create the Slack session
                slackSession = SlackSessionFactory.createWebSocketSlackSession(slackToken);

                //Open the connection
                slackSession.connect();

                //Send a message in the slack channel to alert that we've started up the mod
                sendSlackMessage("A client using Snack has started and connected to this channel.");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            logger.info("[Snack] WARNING: Failed to create a slack session!");

            //Make sure the session is null so checks later on are valid
            slackSession = null;
        }

        //Discord session
        try
        {
            //If we loaded the necessary data to create a discord session
            if(discordToken != null && discordChannel != null)
            {
                //Create the Discord session
                discordSession = new ClientBuilder().withToken(discordToken).login();
            }
        }
        catch (DiscordException e)
        {
            e.printStackTrace();
            logger.info("[Snack] WARNING: Failed to create a discord session!");

            //Maker sure the session is null so checks later on are valid
            discordSession = null;
        }

        //If we couldn't setup either a slack session or a discord session, then return so we dont register and listeners and effectively are shut down
        if(slackSession == null && discordSession == null)
            return;

        MinecraftForge.EVENT_BUS.register(this);

        logger.info("[Snack] Successfully completed Snack setup and connected to Slack");
    }

    @SubscribeEvent
    public void chatParser(ClientChatReceivedEvent event)
    {
        if(event != null && event.getMessage() != null)
        {
            //Get the message we got in chat
            String message = event.getMessage().getUnformattedText();

            //Get the Matcher object for the regular expression that matches snitch alerts
            Matcher matcher = snitchAlertPattern.matcher(message);

            //If the message matches a snitch alert, send it in slack
            if(matcher.matches())
            {
                try
                {
                    //Send the message in slack, if applicable
                    sendSlackMessage(message);

                    //Send the message in discord, if applicable
                    sendDiscordMessage(message);
                }
                catch(Exception e)
                {
                    logger.info("[Snack] WARNING: Could not post alert message to Slack.");

                    //If we cant post messages anymore, unregister this and effectively shut down
                    MinecraftForge.EVENT_BUS.unregister(this);
                }
            }
        }
    }

    //Sends a message in the Slack channel specified in the settings file
    private void sendSlackMessage(String message)
    {
        //Make sure we have a valid slack session to send a message to
        if(slackSession != null)
        {
            try
            {
                //Get the channel that we are supposed to send messages to
                SlackChannel slackChannel = slackSession.findChannelByName(this.slackChannel);

                //Send the alert message to that channel
                slackSession.sendMessage(slackChannel, message);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                logger.info("[Snack] WARNING: Failed to send message in Slack and encountered an error!");
            }
        }
    }

    private void sendDiscordMessage(String message)
    {
        //Make sure we have a valid discord session to send a message to
        if(discordSession != null)
        {
            try
            {
                //Build the message builder with the appropriate channel and session, then send the message (thats what build() does)
                new MessageBuilder(discordSession).withChannel(discordChannel).withContent(message).build();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                logger.info("[Snack] WARNING: Failed to send message in Discord and encountered an error!");
            }
        }
    }

    private boolean loadSettings(File file)
    {
        //See if the settings file exists
        if(file.exists())
        {
            try
            {
                //Attempt to load the values from the settings file
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = null;
                while((line = reader.readLine()) != null)
                {
                    String[] tokens = line.split("=");
                    //See if this is the line with the slack token
                    if(tokens[0].equalsIgnoreCase(SETTINGS_SLACK_TOKEN))
                    {
                        slackToken = tokens[1];
                        //If this is the default value, dont use it
                        if(slackToken.equalsIgnoreCase(PLACEHOLDER))
                            slackToken = null;
                    }
                    else if(tokens[0].equalsIgnoreCase(SETTINGS_SLACK_CHANNEL)) //See if this is the line with the slack channel name
                    {
                        slackChannel = tokens[1];
                        //If this is the default value, dont use it
                        if(slackChannel.equalsIgnoreCase(PLACEHOLDER))
                            slackChannel = null;
                    }
                    if(tokens[0].equalsIgnoreCase(SETTINGS_DISCORD_TOKEN)) //See is this is the line with the discord token
                    {
                        discordToken = tokens[1];
                        //If this is the default value, dont use it
                        if(discordToken.equalsIgnoreCase(PLACEHOLDER))
                            discordToken = null;
                    }
                    else if(tokens[0].equalsIgnoreCase(SETTINGS_DISCORD_CHANNEL)) //See if this is the line with the discord channel name
                    {
                        discordChannel = tokens[1];
                        //If this is the default value, dont use it
                        if(discordChannel.equalsIgnoreCase(PLACEHOLDER))
                            discordChannel = null;
                    }
                }
                //Always close your asset streams, kids.
                reader.close();
                return true;
            }
            catch(Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
        else
        {
            //If the settings file doesnt exist, create it and set the default values
            try
            {
                //If the "Snack" directory doesnt exist it needs to be created
                File directory = new File(Minecraft.getMinecraft().mcDataDir,"mods/Snack");
                if(!directory.exists())
                    directory.mkdir();

                file.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(SETTINGS_SLACK_TOKEN+"="+PLACEHOLDER);
                writer.write(System.lineSeparator());
                writer.write(SETTINGS_SLACK_CHANNEL+"="+PLACEHOLDER);
                writer.write(System.lineSeparator());
                writer.write(SETTINGS_DISCORD_TOKEN+"="+PLACEHOLDER);
                writer.write(System.lineSeparator());
                writer.write(SETTINGS_DISCORD_CHANNEL+"="+PLACEHOLDER);
                writer.flush();
                writer.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            } return false;
        }
    }
}
