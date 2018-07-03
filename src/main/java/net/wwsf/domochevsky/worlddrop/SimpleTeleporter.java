package net.wwsf.domochevsky.worlddrop;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

// Staying as close to vanilla mechanics minus the actual portal
public class SimpleTeleporter extends Teleporter
{

    public SimpleTeleporter(WorldServer world)
    {
        super(world);
    }

	// Copying and modifying vanilla functions, so we don't try to come out of a portal on dimension change.
    @Override
    public void placeInPortal(Entity entity, float yaw)
    {
        int i = MathHelper.floor(entity.posX);
        int j = MathHelper.floor(entity.posY) - 1;
        int k = MathHelper.floor(entity.posZ);
        entity.setLocationAndAngles(i, j, k, entity.rotationYaw, 0.0F);
    }

    @Override
    public void removeStalePortalLocations(long totalWorldTime)
    {
        /* do nothing */
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, float yaw)
    {
        placeInPortal(entity, yaw);
        return true;
    }

}
