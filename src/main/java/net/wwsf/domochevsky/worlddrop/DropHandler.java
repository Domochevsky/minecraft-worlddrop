package net.wwsf.domochevsky.worlddrop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class DropHandler
{
	private static ArrayList<_Drop> drops = new ArrayList<_Drop>();	// The actual drops, in a neat list
	private static HashMap<Entity, Entity> toRemount = new HashMap<Entity, Entity>();	// Reconnecting rider with mount after they arrive
	private static ArrayList<Entity> entityInTeleport = new ArrayList<Entity>();


	private static String configDir;	// The recommended config directory (/config/)
	private static String worldFolder;	// The name of the world. Should be unique, if I saw this right

	protected static void setConfigDir(String dir)
	{
		if (dir == null || dir.isEmpty())
		{
			Main.sendConsoleMessage("[World Drop] Warning: I wasn't given a config folder. I don't know where to put my stuff. D:");
			return;
		}

		configDir = dir;
	}


	public static void setWorldFolder(String dir)
	{
		if (dir == null || dir.isEmpty())
		{
			Main.sendConsoleMessage("[World Drop] Warning: I wasn't given a world folder. I don't know where to put my stuff. D:");
			return;
		}

		worldFolder = dir;
	}


	static int getNumberOfDrops()
	{
		return drops.size();
	}


	// --- Now for the drops themselves ---


	// Saving all drops to file
	// package access
	static void saveDrops()
	{
		String path = configDir + "/worlddrop/";
		File configFile = new File(path);

		configFile.mkdirs();	// Creating the folder first

		configFile = new File(path + "worlddrop_" + worldFolder + ".dat");

		try
		{
			FileOutputStream saveFile = new FileOutputStream(configFile);			// Saving to the current profile
			ObjectOutputStream saveHandle = new ObjectOutputStream(saveFile);		// Creating the file handle

			saveHandle.writeObject(drops);	// Saving the whole shebang

			saveHandle.close();	// Done saving all drops
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		finally
		{
			Main.sendConsoleMessage("[World Drop] Saved " + drops.size() + " drop(s) to file for world '" + worldFolder + "'.");
		}
	}


	// package access
	static void loadDrops()
	{
		loadDrops(configDir + "/worlddrop/worlddrop_" + worldFolder + ".dat", false);
	}


	// Valid for all worlds
	// package access
	static void loadDefaultDrops()
	{
		loadDrops(configDir + "/worlddrop/worlddrop_default.dat", true);
	}


	private static void loadDrops(String path, boolean fromDefault)
	{
		try
		{
			if(new File(path).isFile())	// Exists
			{
				FileInputStream loadFile = new FileInputStream(path); 				// Loading the passed in profile
				ObjectInputStream loadHandle = new ObjectInputStream(loadFile);		// Creating the file handle

				Object obj = loadHandle.readObject();

				if (obj instanceof _Drop[])	// Legacy
				{
					addDropsOld((_Drop[]) obj);
				}
				else	// Making a bit of an assumption here (namely that this is ArrayList<_Drop>)
				{
					addDrops((ArrayList<_Drop>) obj);
				}

				loadHandle.close();
			}
			else
			{
				// Doesn't exist. Now what? Should be fine to load no drops on first start.
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		catch (ClassNotFoundException e) { e.printStackTrace(); }
		finally
		{
			if (fromDefault)
			{
				Main.sendConsoleMessage("[World Drop] Loaded " + drops.size() + " drops from default file.");
			}
			else
			{
				Main.sendConsoleMessage("[World Drop] Loaded " + drops.size() + " drops for world '" + worldFolder + "'.");
			}
		}
	}


	// Adds the passed in drops to the actual drop array.
	// This is the old version, here for legacy purposes.
	private static void addDropsOld(_Drop[] fromDrops)
	{
		//if (this.drops == null) { this.drops = new _Drop[50]; }	// Init
		if (fromDrops == null) { return; }

		int dropCount = 0;

		while (dropCount < fromDrops.length && fromDrops[dropCount] != null)
		{
			if (!isEqualToExistingDrop(fromDrops[dropCount]))
			{
				drops.add(fromDrops[dropCount]);
				//this.addSingleDrop(fromDrops[dropCount]);
			}
			// else, already have this exact value

			dropCount += 1;
		}
	}


	private static void addDrops(ArrayList<_Drop> fromDrops)
	{
		if (fromDrops == null) { return; }

		Iterator<_Drop> it = fromDrops.iterator();
		_Drop importDrop;

		while (it.hasNext())
		{
			importDrop = it.next();

			if (!isEqualToExistingDrop(importDrop))
			{
				drops.add(importDrop);
			}
			// else, already have this exact drop
		}
	}


	private static boolean isEqualToExistingDrop(_Drop drop)
	{
		int dropCount = 0;

		Iterator<_Drop> it = drops.iterator();
		_Drop currentDrop;

		while (it.hasNext())
		{
			currentDrop = it.next();

			if (drop.forceCoords == currentDrop.forceCoords &&
					drop.isClimb == currentDrop.isClimb &&
					drop.heightFrom == currentDrop.heightFrom &&
					drop.heightTo == currentDrop.heightTo &&
					drop.posX == currentDrop.posX &&
					drop.posZ == currentDrop.posZ &&
					drop.worldFromID == currentDrop.worldFromID &&
					drop.worldToID == currentDrop.worldToID)
			{
				return true;	// All parts are the same
			}
		}

		return false;
	}


	// --- Now for dealing with players themselves ---


	// Package access
	static boolean checkPlayerDrop(EntityPlayer player)
	{
		Iterator<_Drop> it = drops.iterator();
		_Drop currentDrop;

		while (it.hasNext())
		{
			currentDrop = it.next();

			double posY = player.posY;

			if(entityInTeleport.contains(player)) { continue; }
			if (player.world.provider.getDimension() != currentDrop.worldFromID) { continue; } // Not the relevant dimension/world
			if (currentDrop.isClimb) { 
				if (posY < currentDrop.heightFrom) { continue; } // Not at or below the height required to drop from
			} else {
				if (posY > currentDrop.heightFrom) { continue; } // Not at or below the height required to drop from
			}	
			if (!Event_Listener.isGraceTimerExpired(player)) { continue; }
	
			if (!DimensionManager.isDimensionRegistered(currentDrop.worldToID)) // Target dimension doesn't seem to exist, so not doing this
			{
				Main.sendConsoleMessage("[World Drop] Target dimension " + currentDrop.worldToID + " doesn't seem to exist. Not letting " + player.getName() + " drop.");
				continue;
			}

			Main.sendConsoleMessage("[World Drop] Teleporting " + player.getName() + " to dimension " + currentDrop.worldToID + " at posY: " + currentDrop.heightTo);
			player.removePassengers();			// TODO: Clutch for now. Right now the player is assumed to be the highest rider in the stack.
			moveEntity(currentDrop, player);	// Checks out. Let's do this.

			secureTargetLocation(player.world, player);
			Event_Listener.addPlayerGraceTimer(player);
			Event_Listener.addPlayerDamageGraceTimer(player);

			return true;	// Successful drop
		}

		return false;
	}

	private static void moveEntity(_Drop drop, Entity entity)
	{

		Entity ridden = entity.getRidingEntity();	// Need to keep track if the player is riding something, since they get dismounted on transfer
		entity.dismountRidingEntity();				// Begone in either case, to be remounted later

		if (drop.worldFromID != drop.worldToID) 	// Off you go (Traveling to a different dimension)
		{
			if (entity instanceof EntityPlayerMP)
			{
				Transition.transferPlayerToDimension((EntityPlayerMP) entity, drop.worldToID, drop.heightTo);
			}
			else
			{
				entity = Transition.transferRegularEntityToDimension(entity, drop.worldToID, drop.heightTo);	// A new entity comes back at the end
			}
		}
		// else, is in the same dimension, so no need to travel

		if (entity == null)
		{
			Main.sendConsoleMessage("[WORLD DROP] Something went wrong when transfering an non-player entity to another dimension! Send help! D:");
			Main.sendConsoleMessage("[WORLD DROP] The failed drop: FROM dim " + drop.worldFromID + " at height " + drop.heightFrom + " TO dim " + drop.worldToID + " at height " + drop.heightTo +
					". isClimb: " + drop.isClimb + " / toPoint: " + drop.forceCoords + " (" + drop.posX + "/" + drop.posZ +")");
			return;
		}


		if (drop.forceCoords)
		{
			// Forcing a specific entry position
			entity.setPositionAndUpdate(drop.posX, drop.heightTo, drop.posZ);
		}

		//entity.setVelocity(velX, velY, velZ);	// Velocity should be maintained. Probably. EDIT: Fuck that noise, this is a client-only method. :|

		// What about mounts?
		if (ridden != null)
		{
			// All the way down!
			moveEntity(drop, ridden);

			// Remounting in a couple ticks
			toRemount.put(entity, ridden);
		}
	}


	// Go time!
	/*private static void movePlayer(_Drop drop, EntityPlayer player)
	{
		Entity ridden = player.getRidingEntity();	// Need to keep track if the player is riding something, since they get dismounted on transfer
		player.dismountRidingEntity();				// Begone in either case, to be remounted later

		// Keeping track of how fast you went before
		double velX = player.motionX;
		double velY = player.motionY;
		double velZ = player.motionZ;

		if (drop.worldFromID != drop.worldToID) 	// Off you go (Traveling to a different dimension)
		{
			player.changeDimension(drop.worldToID);
			//Transition.transferPlayerToDimension((EntityPlayerMP) player, drop.worldToID, drop.heightTo);
		}
		// else, is in the same dimension, so no need to travel

		if (drop.forceCoords)
		{
			// Forcing a specific entry position
			player.setPositionAndUpdate(drop.posX, drop.heightTo, drop.posZ);
		}
		else
		{
			// Just setting your position, so you come out at the right height
			player.setPositionAndUpdate(player.posX, drop.heightTo, player.posZ);
		}

		player.setVelocity(velX, velY, velZ);	// Velocity should be maintained. Probably.

		// What about mounts?
		if (ridden != null)
		{
			if (ridden instanceof EntityPlayer)
			{
				movePlayer(drop, (EntityPlayer) ridden);	// All the way down
			}
			else
			{
				moveMount(drop, ridden);
			}

			// Remounting in a couple ticks
			toRemount.put(player, ridden);
		}
	}


	// The rider has been moved. What about their mount?
	private static void moveMount(_Drop drop, Entity entity)
	{
		Entity ridden = entity.getRidingEntity();
		entity.dismountRidingEntity();				// Begone in either case, to be remounted later

		double velX = entity.motionX;
		double velY = entity.motionY;
		double velZ = entity.motionZ;

		if (drop.worldFromID != drop.worldToID) 	// Off you go (Traveling to a different dimension)
		{
			entity.changeDimension(drop.worldToID);
			//Transition.transferEntityToDimension(entity, drop.worldToID, drop.heightTo);
		}
		// else, is in the same dimension, so no need to travel

		if (drop.forceCoords) 	// Forcing a specific entry position
		{
			entity.setPosition(drop.posX, drop.heightTo, drop.posZ);
		}
		else	// Just setting your position, so you come out at the right height
		{
			entity.setPosition(entity.posX, drop.heightTo, entity.posZ);
		}

		entity.setVelocity(velX, velY, velZ);	// Velocity should be maintained. Probably.

		// What about mounts of mounts?
		if (ridden != null)
		{
			if (ridden instanceof EntityPlayer)
			{
				movePlayer(drop, (EntityPlayer) ridden);	// All the way down
			}
			else
			{
				moveMount(drop, ridden);
			}

			// Remounting a couple ticks later
			toRemount.put(entity, ridden);
		}
	}*/


	// This tries to create a safe space for someone to stand in upon arrival.
	// Only removes a small selection of blocks, like dirt and stone
	private static void secureTargetLocation(World world, Entity entity)
	{
		BlockPos pos = new BlockPos(entity);

		clearSpace(world, pos);

		pos = pos.up();
		clearSpace(world, pos);

		pos = pos.up();
		clearSpace(world, pos);
	}


	// Removing blocks if they're part of a list of "natural" things.
	private static void clearSpace(World world, BlockPos pos)
	{
		if (world.isAirBlock(pos)) { return; }	// Already clear

		IBlockState state = world.getBlockState(pos);

		if (state == null) { return; }

		//if (state.isNormalCube()) { return; }

		if (state.getBlock() == Blocks.DIRT)
		{
			world.setBlockToAir(pos);
		}
		else if (state.getBlock() == Blocks.STONE)
		{
			world.setBlockToAir(pos);
		}
		else if (state.getBlock() == Blocks.SAND)
		{
			world.setBlockToAir(pos);
		}
		else if (state.getBlock() == Blocks.GRASS)
		{
			world.setBlockToAir(pos);
		}
		else if (state.getBlock() == Blocks.SANDSTONE)
		{
			world.setBlockToAir(pos);
		}
		else if (state.getBlock() == Blocks.GRAVEL)
		{
			world.setBlockToAir(pos);
		}
		else if (state.getBlock() == Blocks.NETHERRACK)
		{
			world.setBlockToAir(pos);
		}
		else if (state.getBlock() == Blocks.END_STONE)
		{
			world.setBlockToAir(pos);
		}
		// else, not touching that stuff
	}


	// Anyone in need of remounting?
	static void checkRemounts()
	{
		Set<Entry<Entity,Entity>> mountSet = toRemount.entrySet();

		if (mountSet.isEmpty()) { return; }	// oh ok, nevermind

		Iterator<Entry<Entity, Entity>> it = mountSet.iterator();

		while (it.hasNext())
		{
			Entry<Entity, Entity> entry = it.next();

			// Back onto your steed, valorous rider
			if (entry.getKey().startRiding(entry.getValue()))
			{
				Main.sendConsoleMessage("[WORLD DROP] Remounted someone after a dimension change.");
			}
			else
			{
				Main.sendConsoleMessage("[WORLD DROP] Tried to remount someone after a dimension change, but that didn't work for some reason.");
			}
		}

		toRemount.clear();	// Done.
	}


	// Likely called when the server stops, to remove any ties to old entities.
	static void clearMountSet()
	{
		toRemount.clear();
	}


	// --- Dealing with chat command input now ---


	// Displaying all known drops to the one who asked
	static void showDrops(ICommandSender sender)
	{
		sender.sendMessage(new TextComponentString("[World Drop] Known drops:"));

		ListIterator<_Drop> it = drops.listIterator();
		_Drop drop;

		while (it.hasNext())
		{
			drop = it.next();

			if (drop.isClimb) { continue; }

			String desc = "[World Drop] ID " + (it.nextIndex() - 1) + ": From world " + drop.worldFromID +
					" (height " + drop.heightFrom + " and below) to world " + drop.worldToID + " (height " + drop.heightTo + ").";

			if (drop.forceCoords)
			{
				desc += " Entry point is set to X " + drop.posX + " / Z " + drop.posZ + ".";
			}

			sender.sendMessage(new TextComponentString(desc));
		}

		sender.sendMessage(new TextComponentString("[World Drop] Known climbs:"));

		it = drops.listIterator();	// Reset

		while (it.hasNext())
		{
			drop = it.next();

			if (!drop.isClimb) { continue; }

			String desc = "[World Drop] ID " + (it.nextIndex() - 1) + ": From world " + drop.worldFromID +
					" (height " + drop.heightFrom + " and above) to world " + drop.worldToID + " (height " + drop.heightTo + ").";

			if (drop.forceCoords)
			{
				desc += " Entry point is set to X " + drop.posX + " / Z " + drop.posZ + ".";
			}

			sender.sendMessage(new TextComponentString(desc));
		}
	}


	// Comes in with a whole bunch of chat arguments, waiting to be sorted into a drop
	static void sortInput(ICommandSender sender, String worldFrom, String worldTo, String heightFrom, String heightTo, String doClimb)
	{
		int worldFromID = getIntegerFromString(worldFrom);
		int worldToID = getIntegerFromString(worldTo);

		int heightFromNum = getIntegerFromString(heightFrom);
		int heightToNum = getIntegerFromString(heightTo);

		boolean doClimbBool = Boolean.parseBoolean(doClimb);

		_Drop drop = new _Drop(worldFromID, worldToID, heightFromNum, heightToNum, doClimbBool);
		drops.add(drop);

		if (doClimbBool)
		{
			sender.sendMessage(new TextComponentString("[World Drop] Created a climb from world " + worldFromID + " (Height " +
					heightFromNum + " and above) to " + "world " + worldToID + " (Height " + heightToNum + ")."));
		}
		else
		{
			sender.sendMessage(new TextComponentString("[World Drop] Created a drop from world " + worldFromID + " (Height " +
					heightFromNum + " and below) to " + "world " + worldToID + " (Height " + heightToNum + "). "));
		}
	}


	// They want an existing drop to exit at a specific x y z coord, not just the default drop
	static void sortPoint(ICommandSender sender, String drop, String isEnabled, String posX, String posZ)
	{
		int dropID = getIntegerFromString(drop);

		if (dropID < 0 || dropID >= drops.size())
		{
			sender.sendMessage(new TextComponentString("[World Drop] Drop " + dropID + " doesn't seem to exist. Doing nothing."));
			return;
		}

		_Drop knownDrop = drops.get(dropID);

		boolean enable = Boolean.parseBoolean(isEnabled);
		int coordX = getIntegerFromString(posX);
		int coordZ = getIntegerFromString(posZ);

		knownDrop.forceCoords = enable;
		knownDrop.posX = coordX;
		knownDrop.posZ = coordZ;

		if (!knownDrop.forceCoords)
		{
			sender.sendMessage(new TextComponentString("[World Drop] Point entry for Drop " + dropID + " has been disabled."));
		}
		else
		{
			sender.sendMessage(new TextComponentString("[World Drop] Point entry for Drop " + dropID +
					" has been enabled and set to X " + knownDrop.posX + " and Z " + knownDrop.posZ + "."));
		}
	}


	private static int getIntegerFromString(String str)
	{
		int value = 0;	// Default value. Not sure if this should be something other than 0

		if (str == null || str.isEmpty()) { return value; }

		try
		{
			value = Integer.parseInt(str);
		}
		catch (NumberFormatException e)
		{
			// Nuthin'
		}

		return value;
	}


	// Someone wants a drop gone
	static void sortRemove(ICommandSender sender, String dropString)
	{
		int dropID = getIntegerFromString(dropString);

		if (dropID >= 0 && dropID < drops.size())
		{
			drops.remove(dropID);
			sender.sendMessage(new TextComponentString("[World Drop] Removed drop/climb with ID " + dropID + "."));
		}
		else
		{
			sender.sendMessage(new TextComponentString("[World Drop] That doesn't seem to be a valid entry for removal."));
		}
	}
}
