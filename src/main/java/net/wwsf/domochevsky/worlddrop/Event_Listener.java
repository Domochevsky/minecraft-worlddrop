package net.wwsf.domochevsky.worlddrop;

import java.util.Random;

import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class Event_Listener 
{
	private Random rand = new Random();

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent event)
    {
		if (event.side == Side.CLIENT) { return; }	// Not doing this on client side

		if (this.rand.nextInt(100) < 90) { return; }	// Random chance to check. Not within the margin, so not doing this. Gives a "per player" chance to eventually check each tick

		if (DropHandler.checkPlayerDrop(event.player)) { return; }		// Drop successful. We're done here
		if (DropHandler.checkPlayerClimb(event.player)) { return; }		// Climb successful, so we're done as well
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
}
