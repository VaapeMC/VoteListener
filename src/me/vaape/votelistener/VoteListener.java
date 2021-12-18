package me.vaape.votelistener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import me.vaape.rewards.Rewards;
import net.md_5.bungee.api.ChatColor;

public class VoteListener extends JavaPlugin implements Listener{
	
	public static VoteListener plugin;
	private FileConfiguration config = this.getConfig();
	
	public void onEnable() {
		plugin = this;
		loadConfiguration();
		getLogger().info(ChatColor.GREEN + "VoteListener has been enabled!");
		getServer().getPluginManager().registerEvents(this, this);
		saveConfig();
	}
	
	public void onDisable(){
		saveConfig();
		plugin = null;
	}
	
	public void loadConfiguration() {
		final FileConfiguration config = this.getConfig();
		config.set("streaks.UUID.server start time", new Date().getTime());
		config.options().copyDefaults(true);
		saveConfig();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("vote")) {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Vote using the following links to receive special rewards!");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "1. " + ChatColor.BLUE + "https://tinyurl.com/aauhw874");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "2. " + ChatColor.BLUE + "https://tinyurl.com/yyvudn4e");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "3. " + ChatColor.BLUE + "https://tinyurl.com/pandcwef");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "4. " + ChatColor.BLUE + "https://tinyurl.com/a6an4bvp");
		}
		else if (label.equalsIgnoreCase("votestreak")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				UUID UUID = player.getUniqueId();
				
				long lastVoteInMillis = config.getLong("streaks." + UUID + ".last vote time");

				Date now = new Date();
				long nowInMillis = now.getTime();				
				long timeSinceLastVote = nowInMillis - lastVoteInMillis;
				
				player.sendMessage(ChatColor.BLUE + "Your daily vote streak is: " + ChatColor.GRAY + config.getInt("streaks." + UUID + ".streak"));
				player.sendMessage(ChatColor.BLUE + "Last voted: " + ChatColor.GRAY + String.format("%d hours %d minutes ago",
						TimeUnit.MILLISECONDS.toHours(timeSinceLastVote), //Total number of full hours
						TimeUnit.MILLISECONDS.toMinutes(timeSinceLastVote) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeSinceLastVote)))); //(Total minutes) - (Total number of full hours in minutes)
				
			}

			else {
				sender.sendMessage(ChatColor.RED + "Only players can use this command.");
			}
		}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerVote (VotifierEvent event) {
		Vote vote = event.getVote();
		
		if (vote.getUsername() == null) {
			return;
		}
		
		String username = vote.getUsername();
		
		OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(username);
		
		String reward = Rewards.getInstance().pickReward();
		Rewards.getInstance().giveReward(reward, offlinePlayer, false);
		Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Vote] " + ChatColor.BLUE + username + " used /vote and received " + ChatColor.ITALIC + Rewards.getInstance().getConfig().get("rewards." + reward + ".name") + "!");
		if (offlinePlayer.isOnline()) {
			Player player = (Player) offlinePlayer;
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.65f, 1f);
		}
		
		
		
		//Vote streaks
		UUID UUID = offlinePlayer.getUniqueId();
		
		//Calculate time since last voted vote each vote address
		Date now = new Date();
		long nowInMillis = now.getTime();
		long lastVoteInMillis = config.getLong("streaks." + UUID + ".last vote time");
		long timeSinceLastVote = nowInMillis - lastVoteInMillis;
		
		//Handle streak increment or reset
		//Must be longer than 24 hours so can only increase streak once a day
		//If longer than 48 hours then streak resets
		//24 hours = 86400000
		//48 hours = 172800000   
		if (timeSinceLastVote >= 86400000 && timeSinceLastVote <= 172800000) { //If increasing streak increase streak by 1, and set last vote time to now
			config.set("streaks." + UUID + ".last vote time", nowInMillis);
			config.set("streaks." + UUID + ".streak", config.getInt("streaks." + UUID + ".streak") + 1);
			
			if (offlinePlayer.isOnline()) {
				
				Player player = (Player) offlinePlayer;
				player.sendMessage(ChatColor.LIGHT_PURPLE + "[Vote] " + ChatColor.BLUE + "Daily vote streak increase! Your streak is now at " + config.getInt("streaks." + UUID + ".streak") + "!");
			}
		}
		else if (timeSinceLastVote >= 172800000) { //If longer than 48 hours since last vote, set streak to 1, and set last vote time to now
			config.set("streaks." + UUID + ".last vote time", nowInMillis);
			config.set("streaks." + UUID + ".streak", 1);
		}
		saveConfig();
		
		//This stops players getting the votestreak reward more than once in one vote session
		boolean hasRecievedReward = false;
		if (timeSinceLastVote < 45000000) { //12.5 Hours
			hasRecievedReward = true;
			return;
		}
			
		if (!hasRecievedReward) {
			switch (config.getInt("streaks." + UUID + ".streak")) {
			case 5:
				Rewards.getInstance().giveReward("5_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + " 5 days " + ChatColor.BLUE + ChatColor.BOLD +
						"in a row and received a 5 day streak token!");
				break;
			case 10:
				Rewards.getInstance().giveReward("10_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + " 10 days " + ChatColor.BLUE + ChatColor.BOLD +
						"in a row and received a 10 day streak token!");
				break;
			case 20:
				Rewards.getInstance().giveReward("25_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.BOLD + " 25 days " + ChatColor.BLUE + ChatColor.BOLD +
						"in a row and received a 25 day streak token!");
				break;
			case 35:
				Rewards.getInstance().giveReward("35_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.BOLD + " 50 days " + ChatColor.BLUE + ChatColor.BOLD +
						"in a row and received a 50 day streak token!");
				break;
			case 50:
				Rewards.getInstance().giveReward("50_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.BOLD + " 75 days " + ChatColor.BLUE + ChatColor.BOLD +
						"in a row and received a 75 day streak token!");
				break;
			case 75:
				Rewards.getInstance().giveReward("75_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 100 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 100 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				break;
			case 100:
				Rewards.getInstance().giveReward("100_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 150 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 150 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				break;
			case 125:
				Rewards.getInstance().giveReward("125_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 200 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 200 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
			case 150:
				Rewards.getInstance().giveReward("150_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 250 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 250 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				break;
			case 175:
				Rewards.getInstance().giveReward("175_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 300 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 300 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
			case 200:
				Rewards.getInstance().giveReward("200_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 350 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 350 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				break;
			case 400:
				Rewards.getInstance().giveReward("400_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 400 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 400 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
			case 450:
				Rewards.getInstance().giveReward("450_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 450 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 450 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				break;
			case 500:
				Rewards.getInstance().giveReward("500_day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 500 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 500 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				break;
			case 1000:
				Rewards.getInstance().giveReward("1000 day_streak", offlinePlayer, false);
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[VoteStreaks] " + ChatColor.BLUE + ChatColor.BOLD + username +
						" has voted " + ChatColor.RED + ChatColor.MAGIC + "!" + ChatColor.RED + ChatColor.BOLD + " 1000 days " + ChatColor.RED +
						ChatColor.MAGIC + "!"+ ChatColor.BLUE + ChatColor.BOLD + "in a row and received a 1000 day streak token!");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				Bukkit.broadcastMessage("");
				break;
			default:
		}
		}
		
		switch (config.getInt(UUID + ".streak")) {
			case 5:
				break;
			case 10:
				break;
			case 25:
				break;
			case 50:
				break;
			case 100:
				break;
			case 200:
				break;
			case 500:
				break;
			case 1000:
				break;
			default:
		}
	}
}
