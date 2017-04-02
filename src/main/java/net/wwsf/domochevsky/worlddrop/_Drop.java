package net.wwsf.domochevsky.worlddrop;

import java.io.Serializable;

public class _Drop implements Serializable
{
	private static final long serialVersionUID = 1L;

	// Leaving all fields public, for speed purposes.

	public int worldFromID;	// The world we're coming from
	public int worldToID;	// The world we're going to

	// Drop heights at which we leave/arrive
	public int heightFrom;
	public int heightTo;

	public boolean isClimb;	// If this is true then we want to get UP, not down

	public boolean forceCoords;	// If this is true then we're also setting the character to exit at a specific x z position

	public int posX;
	public int posZ;


	public _Drop(int fromID, int toID, int fromHeight, int toHeight, boolean isClimb)
	{
		this.worldFromID = fromID;
		this.worldToID = toID;

		this.heightFrom = fromHeight;
		this.heightTo = toHeight;

		this.isClimb = isClimb;
	}
}
