package net.wwsf.domochevsky.worlddrop;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class Command_SetDrop implements ICommand
{
	@Override
	public String getCommandName() { return "worlddrop"; }


	@Override
	public String getCommandUsage(ICommandSender sender)
	{
		return "worlddrop set [I:World ID from] [I:World ID to] (I:Height from) (I:Height to) (B:isClimb) OR worlddrop remove [drop ID] OR worlddrop show";
	}


	@Override
	public int compareTo(ICommand arg0)
	{
		return 0;	// Whazzat for?
	}


	@Override
	public List<String> getCommandAliases()
	{
		List aliases = new ArrayList();

		aliases.add("worlddrop");
		aliases.add("wdrop");

		return aliases;
	}


	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		if (args.length == 0)	// No arguments given. I can't work with this
	    {
			this.showHelp(sender);
	    }
		else if (args.length == 1 && args[0].equalsIgnoreCase("show"))	// Displaying all known drops in chat
		{
			DropHandler.showDrops(sender);
		}

		else if (args.length == 1 && args[0].equalsIgnoreCase("save"))	// Saving our current drops to file
		{
			sender.addChatMessage(new TextComponentString("[World Drop] Saving all drops to file."));
			DropHandler.saveDrops();
		}

		else if (args.length == 2 && args[0].equalsIgnoreCase("remove"))	// Two arguments given. Could be "remove"
	    {
			DropHandler.sortRemove(sender, args[1]);
	    }

		else if(args.length == 3 && args[0].equalsIgnoreCase("set"))	// Three argument given.
	    {
			sender.addChatMessage(new TextComponentString("[World Drop] 3 arguments given. Assuming them to be world IDs. Also making a bunch of other assumptions."));
			DropHandler.sortInput(sender, args[1], args[2], "" + 0, "" + 256, "" + false);
	    }

		else if(args.length == 4 && args[0].equalsIgnoreCase("set"))	// Four argument given. Getting there
	    {
			sender.addChatMessage(new TextComponentString("[World Drop] Four arguments given. Assuming them to be world IDs and FROM height. Making assumptions about TO height."));
			DropHandler.sortInput(sender, args[1], args[2], args[3], "" + 256, "" + false);
	    }

		else if(args.length == 5 && args[0].equalsIgnoreCase("set"))	// Five argument given. Solid
	    {
			sender.addChatMessage(new TextComponentString("[World Drop] Five arguments given. Assuming them to be world IDs and FROM/TO heights."));
			DropHandler.sortInput(sender, args[1], args[2], args[3], args[4], "" + false);
	    }

		else if(args.length == 5 && args[0].equalsIgnoreCase("point"))	// Five argument given. Solid
	    {
			sender.addChatMessage(new TextComponentString("[World Drop] Five arguments given. Setting the entry point for an existing drop."));
			DropHandler.sortPoint(sender, args[1], args[2], args[3], args[4]);
	    }

		else if(args.length == 6 && args[0].equalsIgnoreCase("set"))	// Six argument given. Excellent
	    {
			sender.addChatMessage(new TextComponentString("[World Drop] Six arguments given. Assuming them to be world IDs, FROM/TO heights and whether or not to do a drop or climb."));
			DropHandler.sortInput(sender, args[1], args[2], args[3], args[4], args[5]);
	    }

		else
		{
			this.showHelp(sender);
		}
	}


	private void showHelp(ICommandSender sender)
	{
		sender.addChatMessage(new TextComponentString("[World Drop] Known drops/climbs: " + DropHandler.getNumberOfDrops()));
		sender.addChatMessage(new TextComponentString("[World Drop] Usage:"));
		sender.addChatMessage(new TextComponentString("[World Drop] /worlddrop set [I:World ID from] [I:World ID to] (I:Height from) (I:Height to) (B:isClimb) - Set up a drop/climb for this server."));
		sender.addChatMessage(new TextComponentString("[World Drop] /worlddrop remove [Drop ID] - Remove a known drop."));
		sender.addChatMessage(new TextComponentString("[World Drop] /worlddrop show - See all known drops for this server."));
		sender.addChatMessage(new TextComponentString("[World Drop] /worlddrop save - Save all drops to disk."));
		sender.addChatMessage(new TextComponentString("[World Drop] /worlddrop point [I:Drop ID] [B:isEnabled] [I:Coord X] [I:Coord Z] - Change an existing drop to exit at a specific X/Z position."));
	}


	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		if (sender instanceof EntityPlayerMP)			// Is this a player?
		{
			EntityPlayerMP player = (EntityPlayerMP) sender;

			if (server.getPlayerList().canSendCommands(player.getGameProfile()))
			{
				return true;	// Player is allowed to send commands, which seems to be the local equivalent of op
			}

			else if (player.capabilities.isCreativeMode)
			{
				return true;	// Yeah that works too
			}

			else
			{
				return false; 	// Not on the op list, so can't use it
			}
		}

		return true;	// Not a player, so assuming it to be the console
	}


	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos)
	{
		return null;
	}


	@Override
	public boolean isUsernameIndex(String[] args, int index)
	{
		return false;
	}
}
