package me.earth.earthhack.impl.commands;

import me.earth.earthhack.api.command.Command;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.commands.util.CommandDescriptions;
import me.earth.earthhack.impl.util.text.ChatUtil;
import me.earth.earthhack.impl.util.text.TextColor;

public class SoundCommand extends Command implements Globals
{
    public SoundCommand()
    {
        super(new String[][]{{"sound"}});
        CommandDescriptions.register(this, "Reloads the SoundSystem.");
    }

    @Override
    public void execute(String[] args)
    {
        try
        {
            mc.getSoundManager().reloadSounds();
            ChatUtil.sendMessage(TextColor.GREEN + "Reloaded SoundSystem.", getName());
        }
        catch (Exception e)
        {
            ChatUtil.sendMessage(TextColor.RED
                                    + "Couldn't reload sound: "
                                    + e.getMessage(), getName());
            e.printStackTrace();
        }
    }

}
