package de.timeout.nick.manager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

import de.timeout.nick.ConfigManager;
import de.timeout.nick.Nick;
import de.timeout.nick.utils.MojangGameProfile;
import de.timeout.nick.utils.Reflections;
import net.minecraft.util.com.mojang.authlib.GameProfile;

public class NickManager {

	private static Nick main = Nick.plugin;
	private static ProtocolManager manager = ProtocolLibrary.getProtocolManager();
	public static MojangGameProfile steveProfile = new MojangGameProfile("MHF_Alex");
	
	public static List<String> usedNames = new ArrayList<String>();
	private static List<Player> nickedPlayerCache = new ArrayList<Player>();
	private static HashMap<String, MojangGameProfile> profileCache = new HashMap<String, MojangGameProfile>();
				
	private static Class<?> entityplayerClass = Reflections.getNMSClass("EntityPlayer");
	private static Class<?> entityhumanClass = Reflections.getNMSClass("EntityHuman");
	private static Class<?> packetplayoutnamedentityspawnClass = Reflections.getNMSClass("PacketPlayOutNamedEntitySpawn");
	
	private static Field nameField = Reflections.getField(GameProfile.class, "name");
	private static Field uuidField = Reflections.getField(GameProfile.class, "id");
	
	private static Field profileField = Reflections.getField(packetplayoutnamedentityspawnClass, "b");
	
	private static String nickedPrefix = main.getConfig().getString("nickprefix").replaceAll("&", "§");
	
	public static String getRandomNick() {
		List<String> list = ConfigManager.getNicks().getStringList("nicks");
		String name = list.isEmpty() ? null : list.get((int) (Math.random() * (list.size() -1)));
		if(name != null) {
			while(usedNames.contains(name))name = list.get((int) (Math.random() * (list.size() -1)));
			return name;
		} else throw new NullPointerException("Nicklist cannot be null");
	}
	
	public static void sendNickPackets(Player player, String nick, boolean disguise, Player... sendedPlayers) {
		MojangGameProfile profile = profileCache.containsKey(nick) ? profileCache.get(nick) : new MojangGameProfile(nick);
		sendPackets(player, nick, profile, disguise, sendedPlayers, nickedPrefix);
	}
	
	public static void sendUnnickPackets(Player player, Player... sendedPlayers) {
		MojangGameProfile profile = profileCache.containsKey(player.getName().toLowerCase()) ? profileCache.get(player.getName().toLowerCase()) : new MojangGameProfile(player);
		sendPackets(player, player.getName(), profile, false, sendedPlayers, "");
	}

	@SuppressWarnings("deprecation")
	private static void sendPackets(Player player, String nick, MojangGameProfile profile, boolean disguise, Player[] sendedPlayers, String prefix) {
		if(!disguise)nickedPlayerCache.add(player);
		try {
			cache(profile);
			Object ep = Reflections.getEntityPlayer(player);
			
			GameProfile prof = profile.getProfile();
			Reflections.setField(nameField, prof, nick);
			Reflections.setField(uuidField, prof, player.getUniqueId());
			
			PacketContainer despawn = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
			despawn.getIntegerArrays().write(0, new int[] {(int) entityplayerClass.getMethod("getId").invoke(ep)});
			
			for(Player p : Bukkit.getServer().getOnlinePlayers()) manager.sendServerPacket(p, despawn);
			Bukkit.getScheduler().runTaskLater(main, new Runnable() {
				
				@Override
				public void run() {
					try {
						Object spawn = packetplayoutnamedentityspawnClass.getConstructor(entityhumanClass).newInstance(ep);
						Reflections.setField(profileField, spawn, prof);
						
						PacketContainer packet = PacketContainer.fromPacket(spawn);
						for(Player p : Bukkit.getServer().getOnlinePlayers()) {
							if(!p.getName().equalsIgnoreCase(player.getName()))manager.sendServerPacket(p, packet);
						}
						player.setDisplayName(prefix + nick);
						player.setPlayerListName(prefix + nick);
						player.setCustomName(nick);
						nickedPlayerCache.remove(player);
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchMethodException | SecurityException e) {
						e.printStackTrace();
					}
				}
			}, 2);
		} catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			
		}
	}
	
	public static void cache(MojangGameProfile profile) {
		if(profileCache.containsKey(profile.getName().toLowerCase())) profileCache.replace(profile.getName().toLowerCase(), profile);
		else profileCache.put(profile.getName().toLowerCase(), profile);
	}
	
	public static boolean isInProgress(Player player) {
		return nickedPlayerCache.contains(player);
	}
	
	public static MojangGameProfile getProfile(String name) {
		return profileCache.get(name);
	}
}
