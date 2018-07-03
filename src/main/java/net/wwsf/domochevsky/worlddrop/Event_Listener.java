package net.wwsf.domochevsky.worlddrop;

import java.util.HashMap;
import java.util.Random;

import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class Event_Listener 
{
	private Random rand = new Random();
	private static HashMap<Entity, Integer> playerGraceTimers = new HashMap<Entity, Integer>();	// Grace timers for post-teleportation
	
	private static int ticksBetweenChecks = 10; // check only every 10 ticks
	private static int ticksSinceLastCheck = 0; // Counter for ticks between checks
	private static int graceTimer = 20 * 10; // 10 seconds at 20 ticks

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent event)
    {
		if (event.side == Side.CLIENT) { return; }	// Not doing this on client side
		updateGraceTimer(event.player); 			// Update the timer on the checked player
		if (ticksSinceLastCheck < ticksBetweenChecks) { 			// Check every 10 ticks
			ticksSinceLastCheck++;
			return; 
		}
		ticksSinceLastCheck = 0;					// Reset the check counter

		if (DropHandler.checkPlayerDrop(event.player)) { return; }		// Drop successful. We're done here
    }
	
	@SubscribeEvent
	public void onHurtEvent(LivingHurtEvent event)
	{
		Entity hurtEntity = event.getEntity();							// Get the hurt entity
		if (hurtEntity instanceof EntityPlayer) {						// If the entity is a player
			EntityPlayer player = (EntityPlayer)hurtEntity;				// Cast player from Entity
			if(!isGraceTimerExpired(player)){							// Check if that player has grace period timer on him
				event.setCanceled(true);								// If he does, cancel the damage
				Main.sendConsoleMessage("[World Drop] Prevented damage to " + player.getName() + " while teleporting via worlddrop during grace period.");
			}
		}
	}

	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event)
	{
		if (event.side == Side.CLIENT) { return; }		// Not doing this on client side
		if (this.rand.nextInt(100) < 99) { return; }	// Random chance to check. Not within the margin, so not doing this.

		DropHandler.checkRemounts();
	}


	// TODO: On block placed, to listen for fire and whether or not its trying to spawn a portal. Stop that if we're set to forbid it
	@SubscribeEvent
	public void onBlockPlace(PlaceEvent event)
	{
		if (event.getWorld().isRemote) { return; } 		// Not doing this on client side.
		if (!Main.disablePortalIgnition()) { return; }	// Don't care.

		IBlockState blockState = event.getPlacedBlock();	// Now, what is this...

		if (!(blockState.getBlock() instanceof BlockFire)) { return; }	// Not fire, so doesn't matter.

		// Ok, this is fire. Can it ignite a portal from where it is? Cribbing some code from BlockPortal.

		BlockPortal.Size portalSize = new BlockPortal.Size(event.getWorld(), event.getPos(), EnumFacing.Axis.X);

		if (!portalSize.isValid()) { return; }	// Not a valid position for a portal, so doesn't matter

		// Whelp, looks like this is a valid portal position. Not having it.

		event.setCanceled(true);
	}
	
	public static boolean isGraceTimerExpired(EntityPlayer player) {
		if (playerGraceTimers.containsKey(player)) { // Check if a player still has an active timer
			Integer timer = playerGraceTimers.get(player);
			if (timer > graceTimer) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}
	
	public static void addPlayerGraceTimer(EntityPlayer player) {
		if (playerGraceTimers.containsKey(player)) { // Check if a player still has an active timer
			playerGraceTimers.put(player, 0 );
		}
		playerGraceTimers.put(player, new Integer(0));
	}
	
	private static boolean updateGraceTimer(EntityPlayer player) {
		if (playerGraceTimers.containsKey(player)) { // Check if a player still has an active timer
			Integer timer = playerGraceTimers.get(player);
			if (timer <= graceTimer) {
				playerGraceTimers.put(player, timer + 1 );
			} else {
				removePlayerGraceTimer(player);
				return true;
			}
		}
		return false;
	}
	
	private static boolean removePlayerGraceTimer(EntityPlayer player) {
		if (playerGraceTimers.containsKey(player)) { // Check if a player still has an active timer		
				playerGraceTimers.remove(player);
				return true;
			}
		return false;
	}
}
