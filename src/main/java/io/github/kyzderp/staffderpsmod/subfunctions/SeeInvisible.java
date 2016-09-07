package io.github.kyzderp.staffderpsmod.subfunctions;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.util.math.AxisAlignedBB;

public class SeeInvisible {
	
	private AxisAlignedBB bb;

	public SeeInvisible(){}
	
	private List getPlayers()
	{
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		this.bb = new AxisAlignedBB(player.posX - 32, player.posY - 32, player.posZ - 32, 
				player.posX + 32, player.posY + 32, player.posZ + 32);
		return Minecraft.getMinecraft().theWorld.getEntitiesWithinAABB(EntityPlayer.class, this.bb);
	}
	
	public List<EntityPlayer> getInvsPlayers()
	{
		List playerList = new ArrayList<EntityPlayer>();
		
		for (Object player: this.getPlayers())
		{
			if (((EntityPlayer) player).isInvisible())
			{
				playerList.add(player);
			}
		}
		return playerList;
	}

	public String getInvsString() {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		List invsPlayers = this.getInvsPlayers();
		if (invsPlayers.contains(player))
			invsPlayers.remove(player);
		int n = invsPlayers.size();
		List sortedInvsPlayers = new ArrayList();
		String result = "";
		
		for (int j = 0; j < n; j++)
		{
			int currentDist = 100;
			Entity currentPlayer = null;
			//get closest
			for (int i = 0; i < invsPlayers.size(); i++)
			{
				int dist = this.distanceToMob((Entity) invsPlayers.get(i), player);
				if ( dist < currentDist)
				{
					currentDist = dist;
					currentPlayer = (Entity)invsPlayers.get(i);
				}
			}
			result += currentPlayer.getName() + "(" + currentDist + "m) ";
			invsPlayers.remove(currentPlayer);
		}
		return result;
	}
	
	private int distanceToMob(Entity e1, Entity e2)
	{
		return (int) Math.sqrt(Math.pow((e1.posX - e2.posX), 2)
				+ Math.pow((e1.posY - e2.posY), 2)
				+ Math.pow((e1.posZ - e2.posZ), 2));
	}
}