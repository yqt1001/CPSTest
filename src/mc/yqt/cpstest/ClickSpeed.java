package mc.yqt.cpstest;

import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;

public class ClickSpeed extends JavaPlugin implements Listener {

	private static LinkedList<Test> tests = new LinkedList<Test>();
	public static ProtocolManager PM;
	
	@Override
	public void onEnable() {
		PM = ProtocolLibrary.getProtocolManager();
		
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		this.createPacketListener();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		//clicks per second command
		if(cmd.getName().equalsIgnoreCase("cps"))
		{
			
			//command sender must be a player!
			if(!(sender instanceof Player))
			{
				sender.sendMessage("You must be a player to do this!");
				return true;
			}
			
			Player p = (Player) sender;
			
			if(isInTest(p) != null)
				//if player has a test currently active, don't start another
				p.sendMessage("§cYou can't run multiple tests at once!");
			else
				//start a new test
				tests.add(new Test(p));
			
			
			return true;
		}
		
		return false;
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		//on move, teleport the entity to new location
		Test t;
		if((t = isInTest(e.getPlayer())) != null)
			t.sendZombieTPPacket();
		
	}
	
	@EventHandler
	public void onTeleport(PlayerTeleportEvent e) {
		//on teleport cancel the click speed test
		Test t;
		if((t = isInTest(e.getPlayer())) != null)
		{
			t.stop();
			e.getPlayer().sendMessage("§cYou can't teleport during a click speed test! Test failed.");
		}
	}
	
	//method to create protocollib packet listener
	public void createPacketListener() {
		PM.addPacketListener(new PacketAdapter(this, PacketType.Play.Client.USE_ENTITY) 
		{
			@Override
			public void onPacketReceiving(PacketEvent e) 
			{
				//handle incoming packet
				if(e.getPacketType().equals(PacketType.Play.Client.USE_ENTITY) && e.getPacket().getEntityUseActions().read(0).equals(EntityUseAction.ATTACK))
				{
					//make sure it's valid for a click speed test
					Test t;
					
					if((t = isInTest(e.getPlayer())) != null)
					{
						if(t.getClicks() == 0)
							//if first click, start test
							t.startTest();
						else
							//else increment num of clicks
							t.incrementClicks();
					}
				}
			}
		});
	}

	/* STATIC METHODS */
	//checks if a test for this user currently exists
	public static Test isInTest(Player p) {
		for(Test t : tests)
			if(t.getPlayer().equals(p))
				return t;
			
		return null;
	}
		
	//removes a specified test from active tests list
	public static void removeTest(Test t) {
		Iterator<Test> it = tests.iterator();
		
		while(it.hasNext())
			if(it.next().equals(t))
			{
				it.remove();
				return;
			}
	}
	
	//gets location of zombie; right ahead of the player
	public static Location getZombieLocation(Player p) {
		return p.getLocation().getDirection().multiply(1).add(p.getLocation().toVector()).toLocation(p.getWorld());
	}
}