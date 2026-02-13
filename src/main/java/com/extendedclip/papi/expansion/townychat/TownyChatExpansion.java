package com.extendedclip.papi.expansion.townychat;

import com.gmail.nossr50.api.ChatAPI;
import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.TownyChatFormatter;
import com.palmergames.bukkit.TownyChat.channels.ChannelsHolder;
import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.TownyChat.events.PlayerJoinChatChannelEvent;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezzedup.discordsrv.staffchat.StaffChatAPI;
import me.clip.placeholderapi.expansion.Cleanable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TownyChatExpansion extends PlaceholderExpansion implements Listener, Cleanable {
  private final String VERSION = this.getClass().getPackage().getImplementationVersion();
  private Map<String, TownyChatExpansion.ChatPlayer> players;

  public @NotNull String getIdentifier() {
    return "townychat";
  }

  public String getRequiredPlugin() {
    return "TownyChat";
  }

  public @NotNull String getAuthor() {
    return "clip";
  }

  public @NotNull String getVersion() {
    return this.VERSION;
  }

  public TownyChatExpansion() {
    Bukkit.getPluginManager().registerEvents(this, Towny.getPlugin());
  }

  public String onPlaceholderRequest(Player p, String identifier) {
    switch (identifier) {
      case "channel_tag" -> {
        return this.getChatPlayer(p).getTag();
      }

      case "channel_name" -> {
        @Nullable String altChannel = AlternativePluginRequestHandler.INSTANCE.handle(p, identifier);
        if (altChannel != null) {
          return altChannel;
        }

        String s = this.getChatPlayer(p).getChannel();
        if (!s.isEmpty()) {
          s = s.substring(0, 1).toUpperCase() + s.substring(1);
        } else {
          s = "Global";
        }

        return s;
      }
      case "message_color" -> {
        @Nullable String altColor = AlternativePluginRequestHandler.INSTANCE.color(p);
        if (altColor != null) {
          return altColor;
        }

        String channel = this.getChatPlayer(p).getChannel();
        if (!channel.isEmpty() && !channel.equalsIgnoreCase("global")) {
          return this.getChatPlayer(p).getColor();
        }
        return "";
      }

      default -> {
        return TownyUniverseRequestHandler.INSTANCE.handle(p, identifier);
      }
    }
  }

  public void cleanup(Player player) {
    if (this.players != null) {
      this.players.remove(player.getName());
    }
  }

  private void updatePlayer(String player, String ch, String tag, String cc) {
    if (this.players == null) {
      this.players = new HashMap<>();
    }

    if (this.players.containsKey(player) && this.players.get(player) != null) {
      this.players.get(player).setChannel(ch);
      this.players.get(player).setTag(tag);
      this.players.get(player).setColor(cc);
    } else {
      TownyChatExpansion.ChatPlayer pl = new TownyChatExpansion.ChatPlayer(player, ch, tag, cc);
      this.players.put(player, pl);
    }
  }

  private TownyChatExpansion.ChatPlayer getChatPlayer(Player p) {
    return this.players != null && this.players.containsKey(p.getName()) && this.players.get(p.getName()) != null
            ? this.players.get(p.getName())
            : new TownyChatExpansion.ChatPlayer(p.getName(), "", "", "");
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(PlayerJoinChatChannelEvent e) {
    Player p = e.getPlayer();
    new ChannelsHolder(Chat.getPlugin(Chat.class));
    String tag = "";
    String channel = "";
    String msgColor = "";
    if (e.getChannel() != null) {
      tag = e.getChannel().getChannelTag();
      channel = e.getChannel().getName();
      msgColor = e.getChannel().getMessageColour();
    }

    this.updatePlayer(p.getName(), channel, tag, msgColor);
  }

  public static class ChatPlayer {
    private String player;
    private String channel;
    private String tag;
    private String color;

    public ChatPlayer(String player, String channel, String tag, String color) {
      this.setPlayer(player);
      if (channel != null) {
        this.setChannel(channel);
      } else {
        this.setChannel("");
      }

      if (tag != null) {
        this.setTag(tag);
      } else {
        this.setTag("");
      }

      if (color != null) {
        this.setColor(color);
      } else {
        this.setColor("");
      }
    }

    public String getPlayer() {
      return this.player;
    }

    public void setPlayer(String player) {
      this.player = player;
    }

    public String getChannel() {
      return this.channel == null ? "" : this.channel;
    }

    public void setChannel(String channel) {
      this.channel = channel;
    }

    public String getTag() {
      return this.tag == null ? "" : this.tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public String getColor() {
      return this.color == null ? "" : this.color;
    }

    public void setColor(String color) {
      this.color = color;
    }
  }

  public interface RequestHandler {
    @Nullable String handle(Player p, String identifier);
  }
  public interface ChatRequestHandler extends RequestHandler {
    @Nullable String color(Player p);
  }

  public static class TownyUniverseRequestHandler implements RequestHandler {

    public static final TownyUniverseRequestHandler INSTANCE = new TownyUniverseRequestHandler();


    @Override
    public @Nullable String handle(Player p, String identifier) {
      try {
        Resident r = TownyUniverse.getInstance().getResident(p.getName());
        if (r == null) return "";

        return switch (identifier) {
          case "world" -> String.format(ChatSettings.getWorldTag(), p.getWorld().getName());
          case "town" -> r.hasTown() ? r.getTown().getName() : "&7None";
          case "townformatted" -> TownyChatFormatter.formatTownTag(r, false, true);
          case "towntag" -> TownyChatFormatter.formatTownTag(r, false, false);
          case "towntagoverride" -> TownyChatFormatter.formatTownTag(r, true, false);
          case "nation" -> r.hasNation() ? r.getTown().getNation().getName() : "";
          case "nationformatted" -> TownyChatFormatter.formatNationTag(r, false, true);
          case "nationtag" -> TownyChatFormatter.formatNationTag(r, false, false);
          case "nationtagoverride" -> TownyChatFormatter.formatNationTag(r, true, false);
          case "townytag" -> TownyChatFormatter.formatTownyTag(r, false, false);
          case "townyformatted" -> TownyChatFormatter.formatTownyTag(r, false, true);
          case "townytagoverride" -> TownyChatFormatter.formatTownyTag(r, true, false);
          case "title" -> r.hasTitle() ? r.getTitle() : "";
          case "surname" -> r.hasSurname() ? r.getSurname() : "";
          case "townynameprefix" -> r.getNamePrefix();
          case "townynamepostfix" -> r.getNamePostfix();
          case "townyprefix" -> r.hasTitle() ? r.getTitle() : r.getNamePrefix();
          case "townypostfix" -> r.hasSurname() ? r.getSurname() : r.getNamePostfix();
          case "townycolor" ->
                  r.isMayor() ? ChatSettings.getMayorColour() : (r.isKing() ? ChatSettings.getKingColour() : ChatSettings.getResidentColour());
          case "group" -> TownyUniverse.getInstance().getPermissionSource().getPlayerGroup(p);
          case "permprefix" -> TownyUniverse.getInstance().getPermissionSource().getPrefixSuffix(r, "prefix");
          case "permsuffix" -> TownyUniverse.getInstance().getPermissionSource().getPrefixSuffix(r, "suffix");
          case "channeltag" -> TownyChatFormatter.formatTownyTag(r, false, false);
          default -> null;
        };
      } catch (NotRegisteredException var7) {
        return "";
      }
    }
  }


  public static class AlternativePluginRequestHandler implements ChatRequestHandler {

    public static final AlternativePluginRequestHandler INSTANCE = new AlternativePluginRequestHandler();

    // Order matters
    private final List<ChatRequestHandler> otherHandlers = List.of(
            new DiscordSRVStaffChatRequestHandler(),
            new McMMORequestHandler()
    );


    @Override
    public String handle(Player p, String identifier) {
      if (p.getTicksLived() < 150) {
        return null; // Wait for other plugins to finish hooks
      }

      for (RequestHandler otherHandler : otherHandlers) {
        String result = otherHandler.handle(p, identifier);
        if (result != null) {
          return result;
        }
      }
      return null;
    }

    @Override
    public @Nullable String color(Player p) {
      if (p.getTicksLived() < 150) {
        return null; // Wait for other plugins to finish hooks
      }

      for (ChatRequestHandler otherHandler : otherHandlers) {
        String result = otherHandler.color(p);
        if (result != null) {
          return result;
        }
      }
      return null;
    }
  }


  public static class McMMORequestHandler implements ChatRequestHandler {

    private Boolean isEnabled = null;

    @Override
    public @Nullable String handle(Player p, String identifier) {
      if (isEnabled() && ChatAPI.isUsingPartyChat(p)) {
        return "Party";
      }
      return null;
    }

    @Override
    public @Nullable String color(Player p) {
      if (isEnabled() && ChatAPI.isUsingPartyChat(p)) {
        return "&#FCB75C";
      }
      return null;
    }

    private boolean isEnabled() {
      if (this.isEnabled == null) {
        this.isEnabled = Bukkit.getPluginManager().getPlugin("mcMMO") != null;
      }
      return this.isEnabled;
    }
  }


  public static class DiscordSRVStaffChatRequestHandler implements ChatRequestHandler {

    private Boolean isEnabled = null;
    private Plugin cachedInstance = null;

    @Override
    public @Nullable String handle(Player p, String identifier) {
      if (!isEnabled()) {
        return null;
      }

      if (instance().data().isAutomaticStaffChatEnabled(p)) {
        return "Staff";
      }

      return null;
    }

    @Override
    public @Nullable String color(Player p) {
      if (!isEnabled()) {
        return null;
      }

      if (instance().data().isAutomaticStaffChatEnabled(p)) {
        return "&#a1ff68";
      }

      return null;
    }

    private boolean isEnabled() {
      if (this.isEnabled == null) {
        this.isEnabled = Bukkit.getPluginManager().getPlugin("DiscordSRV-Staff-Chat") != null;
      }
      return this.isEnabled;
    }

    private StaffChatAPI instance() {
      if (this.cachedInstance == null) {
        this.cachedInstance = Bukkit.getPluginManager().getPlugin("DiscordSRV-Staff-Chat");
      }
      return (StaffChatAPI) this.cachedInstance;
    }
  }
}
