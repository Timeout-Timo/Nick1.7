package de.timeout.nick;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.com.google.gson.JsonArray;
import net.minecraft.util.com.google.gson.JsonIOException;
import net.minecraft.util.com.google.gson.JsonObject;
import net.minecraft.util.com.google.gson.JsonParser;
import net.minecraft.util.com.google.gson.JsonSyntaxException;

public class DatabaseManager {
	
	private static Nick main = Nick.plugin;
	
	private static File nickedplayersFile = null;

	public static void loadNicked() {
		try {
			if(nickedplayersFile == null)nickedplayersFile = new File(main.getDataFolder().getPath() + "/database", "nickedPlayers.json");
			if(!nickedplayersFile.exists())nickedplayersFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static File getNickedPlayerFile() {
		loadNicked();
		return nickedplayersFile;
	}
	
	public static void cacheNicked() {
		if(!main.sqlEnabled()) {
			loadNicked();
			JsonArray array = new JsonArray();
			List<UUID> list = main.getNickedPlayers();
			
			list.forEach(uuid -> {
				JsonObject obj = new JsonObject();
				obj.addProperty("uuid", uuid.toString());
				array.add(obj);
			});
				
			try {
				PrintWriter writer = new PrintWriter(getNickedPlayerFile());
				writer.write(array.toString());
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static List<UUID> getNickedList() {
		List<UUID> list = new ArrayList<UUID>();
		try {
			JsonArray array = new JsonParser().parse(new FileReader(getNickedPlayerFile())).getAsJsonArray();
			for(int i = 0; i < array.size(); i++) list.add(UUID.fromString(array.get(i).getAsJsonObject().get("uuid").getAsString()));
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
		}
		return list;
	}
}
