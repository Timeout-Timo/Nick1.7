package de.timeout.nick.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

import de.timeout.nick.Nick;
import de.timeout.nick.utils.MojangGameProfile;
import de.timeout.nick.utils.Reflections;
import net.minecraft.util.com.mojang.authlib.GameProfile;

public class DisguiseManager implements Listener {

	private static Nick main = Nick.plugin;
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player p = event.getEntity();
		if(NickManager.isInProgress(p)) {
			event.setDeathMessage(null);
			event.setKeepInventory(true);
			event.setKeepLevel(true);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		
		Player[] onlinePlayers = Bukkit.getServer().getOnlinePlayers();
		for(int i = 0; i < onlinePlayers.length; i++) {
			Player pl = onlinePlayers[i];
				if(main.isNicked(pl) && p.canSee(pl)) {
					NickManager.sendNickPackets(pl, main.getNickname(pl), true, p);
				}
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		if(main.isNicked(p)) {
			NickManager.usedNames.remove(main.getNickname(p));
			main.removeNick(p);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onCommandDisguise(PlayerCommandPreprocessEvent event) {
		String[] split = event.getMessage().substring(1).split(" ");
		for(int i = 0 ; i < split.length; i++) {
			String s = split[i];
			if(NickManager.usedNames.contains(s.toLowerCase())) {
				Player name = main.getNickedPlayer(s);
				split[i] = name.getName();
			} else if(Bukkit.getServer().getOfflinePlayer(s).isOnline()) {
				Player p = Bukkit.getServer().getPlayer(s);
				if(main.isNicked(p))event.setCancelled(true);
			}
		}
		event.setMessage("/" + String.join(" ", split));
	}
	
	/*
	 * Called when a nicked player dies. Refresh spawnpacket
	 */
	public static void registerPacketListener() {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, new PacketType[] {PacketType.Play.Server.NAMED_ENTITY_SPAWN}) {
			
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				WrappedGameProfile gp = packet.getGameProfiles().read(0);
				
				Player p = Bukkit.getServer().getPlayer(gp.getUUID());
				if(main.isNicked(p)) {
					String nick = main.getNickname(p);
					
					System.out.println(nick);
					System.out.println(p.getName());
					
					MojangGameProfile profile = NickManager.getProfile(nick);
					if(profile == null)profile = new MojangGameProfile(nick);
					
					GameProfile prof = profile.getProfile();
					Reflections.setField(Reflections.getField(GameProfile.class, "name"), prof, nick);
					Reflections.setField(Reflections.getField(GameProfile.class, "id"), prof, p.getUniqueId());
					
					packet.getGameProfiles().write(0, WrappedGameProfile.fromHandle(profile.getProfile()));
				}
			}
		});
	}
}
