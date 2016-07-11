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

import java.io.*;
import java.util.logging.LogManager;
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

    //A regular expression that matches all 3 of the snitch alert messages (log in, log out, enter)
    private static final Pattern snitchAlertPattern =
            Pattern.compile("\\s*\\*\\s*([^\\s]*)\\s\\b(?:entered snitch at|logged out in snitch at|logged in to snitch at)\\b\\s*([^\\s]*)\\s\\[([^\\s]*)\\s([-\\d]*)\\s([-\\d]*)\\s([-\\d]*)\\]");

    //The slack session to post messages to
    private SlackSession session = null;

    //The token used to connect to the slack session
    private String token = null;

    //The slack channel to post messages to
    private String channel = null;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        //Load the channel and token from the settings file
        boolean success = loadSettings(new File(Minecraft.getMinecraft().mcDataDir,"mods/Snack/settings.txt"));

        //If we loaded the token and channel, continue
        if(!success || token == null || channel == null)
        {
            logger.info("[Snack] SHUTTING DOWN because could not load token or channel");
            //By returning at this point we dont register any listeners and are effectively shut down
            return;
        }

        try
        {
            //Create the slack session
            session = SlackSessionFactory.createWebSocketSlackSession(token);

            //Open the connection
            session.connect();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            logger.info("[Snack] SHUTTING DOWN because could not create slack session");

            //By returning at this point we stop any events from being registed and are effectively shut down
            return;
        }

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
                    //Get the channel that we are supposed to send messages to
                    SlackChannel slackChannel = session.findChannelByName(channel);

                    //Send the alert message to that channel
                    session.sendMessage(slackChannel,message);
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
                    if(tokens[0].equalsIgnoreCase("token"))
                    {
                        token = tokens[1];
                        //If this is the default value, dont use it
                        if(token.equalsIgnoreCase("placeholder"))
                            token = null;
                    }
                    else if(tokens[0].equalsIgnoreCase("channel")) //See if this is the line with the slack channel name
                    {
                        channel = tokens[1];
                        //If this is the default value, dont use it
                        if(channel.equalsIgnoreCase("placeholder"))
                            channel = null;
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
                file.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("token=PLACEHOLDER");
                writer.write(System.lineSeparator());
                writer.write("channel=PLACEHOLDER");
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
