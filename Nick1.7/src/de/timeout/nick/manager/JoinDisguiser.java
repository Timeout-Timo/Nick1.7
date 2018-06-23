package de.timeout.nick.manager;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import de.timeout.nick.DatabaseManager;
import de.timeout.nick.Nick;
import de.timeout.nick.events.PlayerNickEvent;
import de.timeout.nick.utils.SQLManager;

/*	Abläufe beim PlayerLogin.
 * 
 * 1. AsyncPlayerPreLoginEvent
 * 2. PlayerLoginEvent
 * 3. PacketPlayOutPlayerInfo
 * 4. PacketPlayOutNamedEntitySpawn
 */
public class JoinDisguiser implements Listener {
	
		
	private static Nick main = Nick.plugin;
	private static HashMap<UUID, String> nickCache = new HashMap<UUID, String>();
	
	@EventHandler
	public void registerPlayer(AsyncPlayerPreLoginEvent event) {
		if(!main.sqlEnabled()) {
			List<UUID> list = DatabaseManager.getNickedList();
			if(list.contains(event.getUniqueId())) {
				nickCache.put(event.getUniqueId(), NickManager.getRandomNick());
			}
		} else if(SQLManager.isInDatabase(event.getUniqueId()))nickCache.put(event.getUniqueId(), NickManager.getRandomNick());
	}
	
	@EventHandler
	public void cancelNick(PlayerLoginEvent event) {
		Player p = event.getPlayer();
		if(nickCache.containsKey(p.getUniqueId())) {
			PlayerNickEvent e = new PlayerNickEvent(p, nickCache.get(p.getUniqueId()));
			main.getServer().getPluginManager().callEvent(e);
			if(!e.isCancelled()) {
				nickCache.replace(p.getUniqueId(), e.getNick());
				NickManager.usedNames.add(e.getNick());
				main.addNick(p, e.getNick());
			} else nickCache.remove(p.getUniqueId());
		}
	}
	
	@EventHandler
	public void nickNickedPlayer(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		if(nickCache.containsKey(p.getUniqueId())) {
			Bukkit.getScheduler().runTaskLater(main, new Runnable() {
				
				@SuppressWarnings("deprecation")
				@Override
				public void run() {
					NickManager.sendNickPackets(p, nickCache.get(p.getUniqueId()), false, Bukkit.getOnlinePlayers());
					nickCache.remove(p.getUniqueId());
				}
			}, 3);
			event.setJoinMessage(event.getJoinMessage().replace(p.getName(), nickCache.get(p.getUniqueId())));
		}
	}
}
