package mc.yqt.cpstest;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.TitleAction;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class Test {

	/* test info */
	private Player p; 
	private int clicks = 0;
	private int time = 0;
	private long startms;
	private BukkitTask test;
	
	/* entity info */
	private int EID;
	private Location loc;
	
	//construct the test
	public Test(Player p) {
		this.p = p;
		
		//send packet of an invisible giant zombie for the player to hit
		this.sendZombieSpawnPacket();
		
		//send message to player
		this.p.sendMessage("§eThe test is ready, start clicking to start the 10 second countdown!");
	}
	
	public Player getPlayer() {
		return this.p;
	}
	
	public int getClicks() {
		return this.clicks;
	}
	
	public int getEID() {
		return this.EID;
	}
	
	//start test loop, runs once every second for 10 seconds
	public void startTest() {
		this.startms = System.currentTimeMillis();
		this.sendTitleCreatePackets();
		this.incrementClicks();
		this.p.sendMessage("§a§lTest started. Good luck!");
		
		this.test = new BukkitRunnable() {
			
			@Override
			public void run()
			{
				if(time > 10)
				{
					stop();
					
					//broadcast results
					p.sendMessage("§eThe test is over.");
					p.sendMessage("§eYou clicked §a§l" + clicks + " §etimes and got §a§l"
							+ ((double) clicks / 10.0) + " clicks per second§e!");
				}
				else
					Test.sendTitleUpdatePacket(p, time);
				
				time++;
				
				/* a cps autoban check would go here */
			}
		}.runTaskTimer(ClickSpeed.getPlugin(ClickSpeed.class), 0L, 20L);
	}
	
	//stops test loop
	public void stopTest() {
		this.test.cancel();
	}
	
	//increment click counter
	public void incrementClicks() {
		this.clicks++;
		this.sendSubtitleUpdatePacket();
	}
	
	//handler to shut down tests
	public void stop() {
		this.stopTest();
		this.sendZombieDestroyPacket();
		ClickSpeed.removeTest(this);
	}
	
	//sends the initial starting zombie creation packet
	public void sendZombieSpawnPacket() {
		
		//set data watcher metadata
		WrappedDataWatcher dw = new WrappedDataWatcher();
		dw.setObject(0, (byte) 32); //sets metadata of zombie to invisibile
		dw.setObject(4, (byte) 1); //sets zombie to silent
		
		
		//set entity data
		this.EID = new Random().nextInt();
		this.loc = ClickSpeed.getZombieLocation(this.p);
		
		//create entity spawn packet
		PacketContainer packet = ClickSpeed.PM.createPacket(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
		
		packet.getIntegers().write(0, this.EID); //in game entity ID
		packet.getIntegers().write(1, 53); //giant zombie entity type ID
		packet.getIntegers().write(2, (int) Math.floor(this.loc.getX() * 32.0D)); //entity X pos
		packet.getIntegers().write(3, (int) Math.floor((this.loc.getY() + 1) * 32.0D)); //entity Y pos
		packet.getIntegers().write(4, (int) Math.floor(this.loc.getZ() * 32.0D)); //entity Z pos
		packet.getBytes().write(0, (byte) 0); //entity yaw (irrelevant)
		packet.getBytes().write(1, (byte) 0); //entity head pitch (irrelevant)
		packet.getBytes().write(2, (byte) 0); //entity head yaw (irrelevant)
		packet.getIntegers().write(5, 0); //entity velocity X
		packet.getIntegers().write(6, 0); //entity velocity Y
		packet.getIntegers().write(7, 0); //entity velocity Z
		packet.getDataWatcherModifier().write(0, dw); //entity metadata
		
		//try to send packet to client
		try {
			ClickSpeed.PM.sendServerPacket(this.p, packet);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			
			//if failed, cancel the test
			this.p.sendMessage("§cFailed to start click speed test!");
			ClickSpeed.removeTest(this);
		}
	}
	
	//sends a packet that tells the client to teleport the zombie
	public void sendZombieTPPacket() {
		//update zombie location
		this.loc = ClickSpeed.getZombieLocation(this.p);
		
		PacketContainer packet = ClickSpeed.PM.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
		
		packet.getIntegers().write(0, this.EID); //in game entity ID
		packet.getIntegers().write(1, (int) Math.floor(this.loc.getX() * 32.0D)); //entity X pos
		packet.getIntegers().write(2, (int) Math.floor((this.loc.getY() + 1) * 32.0D)); //entity Y pos
		packet.getIntegers().write(3, (int) Math.floor(this.loc.getZ() * 32.0D)); //entity Z pos
		packet.getBytes().write(0, (byte) 0); //entity pitch (irrelevant)
		packet.getBytes().write(1, (byte) 0); //entity yaw (irrelevant)
		packet.getBooleans().write(0, false); //entity is not necessarily on ground
		
		try {
			ClickSpeed.PM.sendServerPacket(this.p, packet);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			
			//if failed, cancel the test
			this.p.sendMessage("§cProtocol code error, test failed!");
			this.stop();
		}
		
		
	}
	
	//sends a packet that destroys the zombie
	public void sendZombieDestroyPacket() {
		PacketContainer packet = ClickSpeed.PM.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		
		packet.getIntegerArrays().write(0, new int[]{this.EID}); //integer array of entities to destroy, just one
		
		try {
			ClickSpeed.PM.sendServerPacket(this.p, packet);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	//sends the title creation packets
	public void sendTitleCreatePackets() {
		//create blank title packet
		PacketContainer p1 = ClickSpeed.PM.createPacket(PacketType.Play.Server.TITLE);
		p1.getTitleActions().write(0, TitleAction.TITLE); //tells client we are setting the title
		p1.getChatComponents().write(0, WrappedChatComponent.fromText("")); //write blank title
		
		//create subtitle packet
		PacketContainer p2 = ClickSpeed.PM.createPacket(PacketType.Play.Server.TITLE);
		p2.getTitleActions().write(0, TitleAction.SUBTITLE); //tells client we are setting subtitle
		p2.getChatComponents().write(0, WrappedChatComponent.fromText("§e0 | 0.0 cps")); //write intro subtitle
		
		//create main subtitle packets, will run for the duration of the test
		PacketContainer p3 = ClickSpeed.PM.createPacket(PacketType.Play.Server.TITLE);
		p3.getTitleActions().write(0, TitleAction.TIMES); //tells client we are setting the times
		p3.getIntegers().write(0, 0); //fade in for 0 ticks
		p3.getIntegers().write(1, 40); //stay on screen for 12 seconds (every time title packet gets sent, time is reset)
		p3.getIntegers().write(2, 10); //fade out for 10 ticks
		
		try {
			ClickSpeed.PM.sendServerPacket(this.p, p1);
			ClickSpeed.PM.sendServerPacket(this.p, p2);
			ClickSpeed.PM.sendServerPacket(this.p, p3);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			
			//if failed, cancel the test
			this.p.sendMessage("§cProtocol code error, test failed!");
			this.stop();
		}
	}
	
	//update title
	public static void sendTitleUpdatePacket(Player p, int time) {
		PacketContainer packet = ClickSpeed.PM.createPacket(PacketType.Play.Server.TITLE);
		packet.getTitleActions().write(0, TitleAction.TITLE); //tells client we are setting title
		packet.getChatComponents().write(0, WrappedChatComponent.fromText(((time < 7) ? "§e" : "§c") + (10 - time) + " seconds")); //write updated title
		
		try {
			ClickSpeed.PM.sendServerPacket(p, packet);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	//update subtitle
	public void sendSubtitleUpdatePacket() {
		PacketContainer packet = ClickSpeed.PM.createPacket(PacketType.Play.Server.TITLE);
		packet.getTitleActions().write(0, TitleAction.SUBTITLE); //tells client we are setting subtitle
		packet.getChatComponents().write(0, WrappedChatComponent.fromText(((this.clicks < 75) ? "§e" : ((this.clicks < 100) ? "§d" : "§c")) + this.clicks + " | " 
													+ Math.round((double) this.clicks / ((double) (System.currentTimeMillis() - this.startms) / 1000) * 10.0) / 10.0 + " cps")); //write updated subtitle
		
		try {
			ClickSpeed.PM.sendServerPacket(this.p, packet);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			
			//if failed, cancel the test
			this.p.sendMessage("§cProtocol code error, test failed!");
			this.stop();
		}
	}
}
