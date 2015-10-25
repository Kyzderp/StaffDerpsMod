package io.github.kyzderp.staffderpsmod;

import io.github.kyzderp.staffderpsmod.config.StaffDerpsConfig;

import java.io.File;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.OutboundChatListener;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderEventBroker.ReturnValue;
import com.mumfrey.liteloader.modconfig.ConfigStrategy;
import com.mumfrey.liteloader.modconfig.ExposableOptions;

/**
 * What more derps do you want?
 *
 * @author Kyzeragon
 */
@ExposableOptions(strategy = ConfigStrategy.Versioned, filename="staffderpsmod.json")
public class LiteModStaffDerps implements Tickable, ChatFilter, OutboundChatFilter, PostRenderListener
{
	///// FIELDS /////
	private static KeyBinding leftBinding;
	private static KeyBinding rightBinding;
	private static KeyBinding summonBinding;

	private CompassMath compassMath;
	private SeeInvisible invis;
	private PetOwner owner;
	private LBFilter lbfilter;
	private MobSummoner summoner;

	private boolean showOwner;
	private int grabCooldown;

	private StaffDerpsConfig config;

	///// METHODS /////
	public LiteModStaffDerps() {}

	@Override
	public String getName() { return "Staff Derps"; }

	@Override
	public String getVersion() { return "1.2.0"; }

	@Override
	public void init(File configPath)
	{
		this.compassMath = new CompassMath(Minecraft.getMinecraft());
		this.invis = new SeeInvisible();
		this.owner = new PetOwner();
		this.config = new StaffDerpsConfig();
		this.lbfilter = new LBFilter();
		this.summoner = new MobSummoner(this.config);

		this.showOwner = false;

		this.leftBinding = new KeyBinding("key.compass.left", -97, "key.categories.litemods");
		this.rightBinding = new KeyBinding("key.compass.right", -96, "key.categories.litemods");
		this.summonBinding = new KeyBinding("key.summon", Keyboard.CHAR_NONE, "key.categories.litemods");

		LiteLoader.getInput().registerKeyBinding(leftBinding);
		LiteLoader.getInput().registerKeyBinding(rightBinding);
		LiteLoader.getInput().registerKeyBinding(this.summonBinding);
	}

	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath) {}

	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock)
	{
		if (inGame && minecraft.currentScreen == null && Minecraft.isGuiEnabled())
		{
			if (LiteModStaffDerps.summonBinding.isPressed())
			{
				this.summoner.summon();
			}

			if (LiteModStaffDerps.leftBinding.isPressed())
				this.compassMath.jumpTo();
			else if (LiteModStaffDerps.rightBinding.isPressed())
				this.compassMath.passThrough();

			if (this.config.getSeeInvisibleOn())
			{
				FontRenderer fontRender = minecraft.fontRendererObj;
				String invsPlayers = this.invis.getInvsString();
				fontRender.drawStringWithShadow("Hidden players: " 
						+ invsPlayers, 0, 0, 0xFFAA00);
			}

			if (this.config.getSeePetOwnerOn())
			{
				FontRenderer fontRender = minecraft.fontRendererObj;
				String dogs = this.owner.getDogOwners();
				String cats = this.owner.getCatOwners();
				fontRender.drawStringWithShadow("Dogs: " 
						+ dogs, 0, 0, 0xFFAA00);
				fontRender.drawStringWithShadow("Cats: " 
						+ cats, 0, 10, 0xFFAA00);
			}
		}
	}

	@Override
	public boolean onSendChatMessage(String message)
	{
		String[] tokens = message.trim().split(" ");
		if (tokens[0].equalsIgnoreCase("/staffderps") || tokens[0].equalsIgnoreCase("/sd"))
		{
			while (message.matches(".*  .*"))
				message = message.replaceAll("  ", " ");
			tokens = message.split(" ");
			if (tokens.length < 2)
			{
				this.logMessage("Staff Derps [v" + this.getVersion() + "] by Kyzeragon", false);
				this.logMessage("Type /sd help or /staffderps help for commands.", false);
				return false;
			}
			else if (tokens[1].equalsIgnoreCase("grab"))
			{
				this.logError("The /sd grab command is in ItemSorter mod now. Use /grab instead");
			}
			else if (tokens[1].equalsIgnoreCase("invis") || tokens[1].equalsIgnoreCase("invisible"))
			{
				if (tokens.length < 3)
					this.logError("Usage: /sd invis <on|off>");
				else if (tokens[2].equalsIgnoreCase("on"))
				{
					this.config.setSeeInvisibleOn(true);
					this.logMessage("See through invisibility: ON", true);
				}
				else if (tokens[2].equalsIgnoreCase("off"))
				{
					this.config.setSeeInvisibleOn(false);
					this.logMessage("See through invisibility: OFF", true);
				}
				else
					this.logError("Usage: /sd invis <on|off>");
			}
			else if (tokens[1].equalsIgnoreCase("pet"))
			{ // TODO: make it clearer which pet it is
				if (tokens.length < 3)
					this.logError("Usage: /sd pet <on|off|copy>");
				else if (tokens[2].equalsIgnoreCase("on")) {
					this.config.setSeePetOwnerOn(true);
					this.logMessage("Displaying pets in 2 block radius", true);
				}
				else if (tokens[2].equalsIgnoreCase("off")) {
					this.config.setSeePetOwnerOn(false);
					this.logMessage("Pet display: OFF", true);
				}
				else if (tokens[2].equalsIgnoreCase("copy")) {
					String result = this.owner.getRandomOwner();
					if (result == null || result == "")
						this.logError("No owners for any pets in range!");
					else {
						this.logMessage("Owner name is " + result, true);
					}
				}
				else
					this.logError("Usage: /sd pet <on|off|copy>");
			}
			else if (tokens[1].equalsIgnoreCase("chunk") || tokens[1].equalsIgnoreCase("c"))
			{
				int first = 10000000;
				int second = 10000000;
				for (int i = 0; i < tokens.length; i++)
				{
					if (tokens[i].matches("-?[0-9]*"))
					{
						if (first != 10000000)
						{
							second = Integer.parseInt(tokens[i]) * 16 + 8;
							first = first * 16 + 8;
							Minecraft.getMinecraft().thePlayer.sendChatMessage(
									"/tppos " + first + " 100 " + second);
							return false;
						}
						else
							first = Integer.parseInt(tokens[i]);
					}
				}
				this.logError("Usage: /sd chunk <x> <y>");
			}
			else if (tokens[1].equalsIgnoreCase("tp"))
			{
				message = message.replaceAll("\\.[0-9]*", ""); // get rid of decimals
				if (tokens.length == 3 && tokens[2].contains("/")) // try to split with / . ,
					message = message.replaceAll("/", " ");
				else if (tokens.length == 3 && tokens[2].contains("."))
					message = message.replaceAll(".", " ");
				else if (tokens.length == 3 && tokens[2].contains(","))
					message = message.replaceAll(",", " ");

				String result = "";
				char prevChar = 'a';
				for (int i = 7; i < message.length(); i++) // keep only -, numbers, and spaces
				{
					char c = message.charAt(i);
					if (!(prevChar == '-' && c == ' '))
						if ("-0123456789 ".contains("" + c))
							result += c;
				}
				while (result.matches(".*  .*")) // Only 1 space pl0x
					result = result.replaceAll("  ", " ");
				String[] coords = result.trim().split(" ");
				if (coords.length != 3)
				{
					for (String coord: coords)
						System.out.println("\"" + coord + "\"");
					this.logError("Invalid format: " + result);
					return false;
				}
				coords[1] = coords[1].replaceAll("-", "");

				// Sometimes people like to switch the y and the z... annoying.
				int y = Integer.parseInt(coords[1]);
				int z = Integer.parseInt(coords[2]);
				if (y > 255 && (z >= 0 && z < 256))
					result = coords[0] + " " + z + " " + y;
				else if (y > 255)
				{
					String sub = coords[1].substring(0, 3);
					if (Integer.parseInt(sub) > 255)
						sub = sub.substring(0, 2);
					result = coords[0] + " " + sub + " " + coords[2];
				}
				else
					result = coords[0] + " " + y + " " + coords[2];
				this.logMessage("Running /tppos " + result, true);
				Minecraft.getMinecraft().thePlayer.sendChatMessage("/tppos " + result);
			}
			else if (tokens[1].equalsIgnoreCase("lbf"))
			{
				this.lbfilter.handleCommand(message);
			}
			else if (tokens[1].equalsIgnoreCase("summon"))
			{
				this.summoner.setCommand(message.replaceAll("sd |staffderps ", ""));
			}
			else if (tokens[1].equalsIgnoreCase("scalar"))
			{
				if (tokens.length == 2)
					this.summoner.showScalar();
				else
					this.summoner.setScalar(tokens[2]);
			}
			else if (tokens[1].equalsIgnoreCase("help"))
			{
				String[] commands = {"invis <on|off> - See through invisibility effect.",
						"pet <on|off|copy> - Get pet owner.",
						"chunk <x> <y> - Teleport to chunk coords.",
						"tp <coordinates> - Attempts to TP to poorly formatted coords.",
						"lbf y <minY> <maxY> - Shows only lb entries within specified Y.",
						"summon <mob> ~x ~y ~z {[data]}",
						"scalar <double> - Set the scalar for shooting the mob",
				"help - This help message."};
				this.logMessage("Staff Derps [v" + this.getVersion() + "] commands (alias /staffderps)", false);
				for (String command: commands)
					this.logMessage("/sd " + command, false);
				// TODO: add wiki link
			}
			else
			{
				this.logMessage("Staff Derps [v" + this.getVersion() + "] by Kyzeragon", false);
				this.logMessage("Type /sd help or /staffderps help for commands.", false);
				return false;
			}
			return false;
		}
		return true;
	}


	/**
	 * Stops the Unknown command error from the server from displaying,
	 * and also prevents displaying of lb entries that should be filtered out
	 */
	@Override
	public boolean onChat(IChatComponent chat, String message, 
			ReturnValue<IChatComponent> newMessage) {
		if (message.matches("�r�6\\([0-9]+\\).*at .*:.*:.*"))
			return this.lbfilter.handleEntry(message);
		return true;
	}

	/**
	 * Display location of invisible players if display is on.
	 */
	@Override
	public void onPostRenderEntities(float partialTicks) 
	{
		if (!this.config.getSeeInvisibleOn())
			return;
		// TODO: display player name too
		RenderHelper.disableStandardItemLighting();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_DEPTH_TEST);

		boolean foggy = GL11.glIsEnabled(GL11.GL_FOG);
		GL11.glDisable(GL11.GL_FOG);

		GL11.glPushMatrix();

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		GL11.glTranslated(-(player.prevPosX + (player.posX - player.prevPosX) * partialTicks),
				-(player.prevPosY + (player.posY - player.prevPosY) * partialTicks),
				-(player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks));

		Tessellator tess = Tessellator.getInstance();
		List<EntityPlayer> players = this.invis.getInvsPlayers();
		for (EntityPlayer currentPlayer: players)
		{
			double x = currentPlayer.posX - 0.3;
			double y = currentPlayer.posY;
			double z = currentPlayer.posZ - 0.3;
			GL11.glLineWidth(3.0f);
			tess.getWorldRenderer().startDrawing(GL11.GL_LINE_LOOP);
			tess.getWorldRenderer().setColorRGBA(255, 0, 0, 200);
			tess.getWorldRenderer().addVertex(x, y, z);
			tess.getWorldRenderer().addVertex(x + 0.6, y, z);
			tess.getWorldRenderer().addVertex(x + 0.6, y, z + 0.6);
			tess.getWorldRenderer().addVertex(x, y, z + 0.6);
			tess.draw();

			tess.getWorldRenderer().startDrawing(GL11.GL_LINE_LOOP);
			tess.getWorldRenderer().setColorRGBA(255, 0, 0, 200);
			tess.getWorldRenderer().addVertex(x, y + 1.8, z);
			tess.getWorldRenderer().addVertex(x + 0.6, y + 1.8, z);
			tess.getWorldRenderer().addVertex(x + 0.6, y + 1.8, z + 0.6);
			tess.getWorldRenderer().addVertex(x, y + 1.8, z + 0.6);
			tess.draw();

			tess.getWorldRenderer().startDrawing(GL11.GL_LINES);
			tess.getWorldRenderer().setColorRGBA(255, 0, 0, 200);
			tess.getWorldRenderer().addVertex(x, y, z);
			tess.getWorldRenderer().addVertex(x, y + 1.8, z);

			tess.getWorldRenderer().addVertex(x + 0.6, y, z);
			tess.getWorldRenderer().addVertex(x + 0.6, y + 1.8, z);

			tess.getWorldRenderer().addVertex(x + 0.6, y, z + 0.6);
			tess.getWorldRenderer().addVertex(x + 0.6, y + 1.8, z + 0.6);

			tess.getWorldRenderer().addVertex(x, y, z + 0.6);
			tess.getWorldRenderer().addVertex(x, y + 1.8, z + 0.6);
			tess.draw();
		}

		GL11.glPopMatrix();

		// Only re-enable fog if it was enabled before we messed with it.
		// Or else, fog is *all* you'll see with Optifine.
		if (foggy)
			GL11.glEnable(GL11.GL_FOG);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);

		RenderHelper.enableStandardItemLighting();
	}

	/**
	 * Logs the message to the user
	 * @param message The message to log
	 */
	public static void logMessage(String message, boolean addPrefix)
	{
		if (addPrefix)
			message = "�8[�2StaffDerps�8] �a" + message;
		ChatComponentText displayMessage = new ChatComponentText(message);
		displayMessage.setChatStyle((new ChatStyle()).setColor(EnumChatFormatting.GREEN));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	/**
	 * Logs the error message to the user
	 * @param message The error message to log
	 */
	public static void logError(String message)
	{
		ChatComponentText displayMessage = new ChatComponentText("�8[�4!�8] �c" + message + " �8[�4!�8]");
		displayMessage.setChatStyle((new ChatStyle()).setColor(EnumChatFormatting.RED));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	@Override
	public void onPostRender(float partialTicks) {}
}