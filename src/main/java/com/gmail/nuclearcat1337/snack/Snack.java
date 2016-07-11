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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    private static final Pattern snitchAlertPattern =
            Pattern.compile("\\s*\\*\\s*([^\\s]*)\\s\\b(?:entered snitch at|logged out in snitch at|logged in to snitch at)\\b\\s*([^\\s]*)\\s\\[([^\\s]*)\\s([-\\d]*)\\s([-\\d]*)\\s([-\\d]*)\\]");
    private static final Logger logger = Logger.getLogger(MODID);

    private SlackSession session = null;

    private String token = null;
    private String channel = null;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        boolean success = loadSettings(new File(Minecraft.getMinecraft().mcDataDir,"mods/Snack/settings.txt"));

        if(!success || token == null || channel == null)
        {
            logger.info("[Snack] SHUTTING DOWN because could not load token or channel");
            return;
        }

        session = SlackSessionFactory.createWebSocketSlackSession(token);
        try
        {
            session.connect();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            logger.info("[Snack] SHUTTING DOWN because could not create slack session");
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
            String message = event.getMessage().getUnformattedText();
            Matcher matcher = snitchAlertPattern.matcher(message);
            if(matcher.matches())
            {
                SlackChannel slackChannel = session.findChannelByName(channel);

                session.sendMessage(slackChannel,message);
            }
        }
    }

    private boolean loadSettings(File file)
    {
        if(file.exists())
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = null;
                while((line = reader.readLine()) != null)
                {
                    String[] tokens = line.split("=");
                    if(tokens[0].equalsIgnoreCase("token"))
                    {
                        token = tokens[1];
                    }
                    else if(tokens[0].equalsIgnoreCase("channel"))
                    {
                        channel = tokens[1];
                    }
                }
                reader.close();
                return true;
            }
            catch(Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
        else return false;
    }
}
