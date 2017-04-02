package net.wwsf.domochevsky.worlddrop;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;


@Mod(modid="dropchevsky", name="World Drop", version="b15", acceptableRemoteVersions = "*", canBeDeactivated = true, acceptedMinecraftVersions="[1.9,1.11)")
public class Main
{
	private static boolean disablePortals;	// If this is true then we're stopping fire from igniting regular portals in all dimensions


	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		//this.configDir = event.getModConfigurationDirectory();	// The directory we're saving our config to. Remembering that for later
		DropHandler.setConfigDir(event.getModConfigurationDirectory().getPath());

		MinecraftForge.EVENT_BUS.register(new Event_Listener());

		// TODO: Config for whether or not to disable regular portals
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());	// Starting config

		config.load();	// And loading it up

		this.disablePortals = config.get("portals", "Should I prevent the ignition of portals (eg with flint and steel) in all dimensions? (default false)", false).getBoolean();

		config.save();// Done with config, saving it
	}


	@EventHandler
	public void serverLoad(FMLServerStartingEvent event)	// Server's starting up, so registering all commands now and loading our drops in
	{
		//this.worldFolder = event.getServer().getFolderName();
		DropHandler.setWorldFolder(event.getServer().getFolderName());	// Handing over where we currently are. This changes on a per-server base

		DropHandler.loadDefaultDrops();	// World-independent
		DropHandler.loadDrops();		// World-specific

		event.registerServerCommand(new Command_SetDrop());	// For chat-added drops
	}


	@EventHandler
	public void serverStopping(FMLServerStoppingEvent event)
	{
		// The server's stopping. Writing our config to disk

		DropHandler.saveDrops();
		DropHandler.clearMountSet();
	}


	/*public void modDisabled(FMLModDisabledEvent event)
	{
		// We've been disabled, so... do we need to do anything about this? No world is loaded at the moment.
	}*/


	// Just for convenience.
	// Package access.
	static void sendConsoleMessage(String msg)
	{
		if (msg == null || msg.isEmpty()) { return; }

		System.out.println(msg);
	}


	static boolean disablePortalIgnition() { return disablePortals; }
}
