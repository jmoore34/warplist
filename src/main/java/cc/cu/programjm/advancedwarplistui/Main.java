package cc.cu.programjm.advancedwarplistui;

import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import mkremins.fanciful.FancyMessage;
import org.omg.CORBA.NO_PERMISSION;
import org.sqlite.date.ExceptionUtils;

/**
 * Created by programjm on 6/19/2017.
 */
public class Main extends JavaPlugin
{
    static final String WARP_PERMISSION = "warplist.warp";
    static final String OVERRIDE_OTHER_SETWARP_PERMISSION = "warplist.setwarp.other";
    static final String USER_WARP_COUNT_SECTION = "playerWarpCounts";
    static final String WARP_OTHER_PERMISSION = "warplist.warp.other";
    static final String WARP_SPECIFIC = "warplist.warp.";
    static final String RECEIVE_STACK_TRACE = "warplist.stacktrace";
    static final String VIEW_HIDDEN = "warplist.warplist.hidden";
    static final String WARP_PRIVATE = "warplist.warp.private";
    static final String SETWARP_HIDDEN = "warplist.setwarp.hidden";
    static final String SETWARP_PRIVATE = "warplist.setwarp.private";

    static final int UNINITIALIZED = -1;

    static
    {
        ConfigurationSerialization.registerClass(Warp.class);
        ConfigurationSerialization.registerClass(Warp.Access.class);
        ConfigurationSerialization.registerClass(ConfigurationSerializablePriorityQueue.class);
    }

    protected JavaPlugin plugin;
    protected YamlConfiguration yml;
    protected File dataFile;
    private ArrayList<Warp> allWarps;
    private ArrayList<Warp> warpsByName;
    private ArrayList<Warp> warpsByRecentVisits;
    private ArrayList<Warp> warpsByTotalVisits;
    private ArrayList<Warp> warpsByCreation;
    private ArrayList<Warp> reverseWarpsByCreation;
    private ArrayList<Warp> warpsByMostFavorited;

    private void stop()
    {
        getLogger().warning(ChatColor.RED + "Shutting down to prevent further errors.");
        getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void onEnable()
    {
        plugin = this;


        dataFile = new File("plugins/AdvancedWarplistUI/warps.yml");
        if (!dataFile.exists())
        {
            try
            {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e)
            {
                getLogger().log(Level.SEVERE, "Unable to create a config file!", e);
                e.printStackTrace();
                getServer().broadcast(e.toString(), RECEIVE_STACK_TRACE);
                stop();
            }

        }
        yml = new YamlConfiguration();
        yml = YamlConfiguration.loadConfiguration(dataFile);

        if (!yml.contains("Note"))
            yml.set("Note", "Manual editing of this file may cause problems and is not recommended. Instead, use the in-game /setwarp, /ediwarp, and /deletewarp commands.");
        fullResort();
        getLogger().info(getDescription().getName() + " has been enabled.");
    }

    public void fullResort()
    {
        allWarps = getWarps();
        warpsByName = new ArrayList<Warp>(allWarps);
        warpsByRecentVisits = new ArrayList<Warp>(allWarps);
        warpsByTotalVisits = new ArrayList<Warp>(allWarps);
        warpsByCreation = new ArrayList<Warp>(allWarps);
        warpsByMostFavorited = new ArrayList<Warp>(allWarps);

        Collections.sort(warpsByName, Warp.WarpComparatorTypes.NAME);
        Collections.sort(warpsByRecentVisits, Warp.getCombinedComparator(Warp.WarpComparatorTypes.RECENT_VISITS, Warp.WarpComparatorTypes.NAME));
        Collections.sort(warpsByTotalVisits, Warp.getCombinedComparator(Warp.WarpComparatorTypes.VISITS, Warp.WarpComparatorTypes.NAME));
        Collections.sort(warpsByCreation, Warp.getCombinedComparator(Warp.WarpComparatorTypes.CREATION, Warp.WarpComparatorTypes.NAME));
        Collections.sort(warpsByMostFavorited, Warp.getCombinedComparator(Warp.WarpComparatorTypes.FAVORITED, Warp.WarpComparatorTypes.NAME));
        reverseWarpsByCreation = new ArrayList<Warp>(warpsByCreation);
        Collections.reverse(reverseWarpsByCreation);
    }

    protected boolean save()
    {
        if (!dataFile.exists())
        {
            try
            {
                dataFile.createNewFile();
            } catch (IOException e)
            {
                getLogger().log(Level.SEVERE, "Unable to create a config file!", e);
                return false;
            }

        }
        try
        {
            yml.save(dataFile);
        } catch (IOException e)
        {
            getLogger().log(Level.SEVERE, "Unable to save!", e);
            return false;

        }
        return true;
    }


    @Override
    public void onDisable()
    {
        getLogger().info(getDescription().getName() + " has been disabled.");
    }

    @NotNull
    protected ArrayList<Warp> getWarps() throws ClassCastException
    {
        ArrayList warps;

        if (!yml.contains("warps"))
        {
            yml.set("warps", new ArrayList<Warp>());
            save();
        }
        warps = (ArrayList<Warp>) yml.get("warps");
        return warps;
    }

    /**
     * Warp and player must exist.
     */
    private void warpPlayer(Player player, Warp warp)
    {
        player.teleport(warp.getLocation());
        warp.incrementVisits();
        if (warp.getRecentVisits().size() <= Warp.RECENT_VISIT_COUNT)
            warp.getRecentVisits().poll();
        warp.getRecentVisits().add(new Date());
        save();
        Collections.sort(warpsByRecentVisits, Warp.getCombinedComparator(Warp.WarpComparatorTypes.RECENT_VISITS, Warp.WarpComparatorTypes.NAME));
        Collections.sort(warpsByTotalVisits, Warp.getCombinedComparator(Warp.WarpComparatorTypes.VISITS, Warp.WarpComparatorTypes.NAME));
    }

    private boolean allowAccess(Player p, Warp w)
    {
        return !p.hasPermission("-" + WARP_SPECIFIC + w.getName())
                && (
                !w.getAccess().hasWarpPermission()
                        || p.hasPermission(w.getAccess().getWarpPermission())
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        /*try
        {*/
        if (label.equalsIgnoreCase("setwarp"))
        {
            if (sender instanceof Player)
            {
                if (args.length == 1)
                {
                    Player player = (Player) sender;
                    Date now = new Date();
                    Warp createdWarp = new Warp(args[0], player.getLocation(), player.getUniqueId(), now, player.getUniqueId());
                    boolean warpExists = warpExists(args[0]);
                    Warp existingWarp = null;
                    if (warpExists)
                        existingWarp = getExistingWarp(args[0]);
                    boolean refuseAccess = false;
                    if (warpExists)
                    {
                        UUID warpOwnerUUID = existingWarp.getOwnerUUID();
                        boolean isOwner = warpOwnerUUID.equals(player.getUniqueId());
                        refuseAccess = !isOwner && !player.hasPermission(OVERRIDE_OTHER_SETWARP_PERMISSION);
                    }
                    if (refuseAccess)
                    {
                        player.sendMessage(ChatColor.RED + "You are not the owner of this warp.");
                    } else
                    {
                        //check if the player has enough warps left
                        boolean hasANumberPermission = false;
                        int userTotalNumberAllowedWarps = 0;
                        int ownedWarps;
                        //1. check how many total warps the user can have
                        Set<PermissionAttachmentInfo> playerPerms = player.getEffectivePermissions();
                        if (!playerPerms.isEmpty())
                            for (PermissionAttachmentInfo permInfo : playerPerms)
                            {
                                String permName = permInfo.getPermission().toLowerCase();
                                if (permName.contains(WARP_PERMISSION))
                                {
                                    int permNumberWarps = UNINITIALIZED;
                                    String lastPart = permName.substring(permName.lastIndexOf('.'));
                                    boolean failedToCast = false;
                                    try
                                    {
                                        permNumberWarps = Integer.parseInt(lastPart);
                                    } catch (NumberFormatException nfe)
                                    {
                                        failedToCast = true;
                                    }

                                    if (!failedToCast)
                                    {
                                        hasANumberPermission = true;
                                        userTotalNumberAllowedWarps = Math.max(userTotalNumberAllowedWarps, permNumberWarps);
                                    }
                                }
                            }
                        //2. check how many warps the user already has, if applicable
                        String configurationSection = USER_WARP_COUNT_SECTION + "." + player.getUniqueId();
                        ownedWarps = 0;
                        if (!getWarps().isEmpty())
                            for (Warp w : getWarps())
                                if (w.getOwnerUUID().equals(player.getUniqueId()))
                                    ownedWarps++;
                        if (hasANumberPermission && ownedWarps >= userTotalNumberAllowedWarps)
                        {
                            sender.sendMessage(ChatColor.RED + "You have already used up your allocated " + userTotalNumberAllowedWarps + " warps.");
                            return true;
                        } else
                        {
                            String msg;
                            boolean success;
                            if (warpExists)
                            {
                                for (Warp w : getWarps())
                                    if (w.getName().equalsIgnoreCase(args[0]))
                                    {
                                        getWarps().remove(w);
                                        break;
                                    }


                                getWarps().add(createdWarp);
                                success = save();
                                if (success)
                                {
                                    msg = ChatColor.YELLOW + "Changed the location of the warp " + args[0] + ". ";
                                } else
                                    msg = ChatColor.RED + "Unable to edit the location of the warp" + args[0];
                            } else
                            {
                                getWarps().add(createdWarp);
                                fullResort();
                                success = save();
                                if (success)
                                {
                                    msg = ChatColor.YELLOW + "Created warp " + args[0] + ". ";
                                } else
                                    msg = ChatColor.RED + "Unable to create the warp.";
                            }
                            if (success)
                            {
                                msg += ChatColor.WHITE + "\nEdit the warp's properties using the following command:";
                                msg += ChatColor.GRAY;
                                msg += "\n/editwarp <warpname> <property> [value]";
                                msg += "\nProperties: name, description, private";
                                ownedWarps++;
                                if (hasANumberPermission)
                                    msg += "\nYou have used " + (ownedWarps) + " out of your allocated " + userTotalNumberAllowedWarps + " warps.";
                            }

                            sender.sendMessage(msg);
                            return true;
                        }
                    }

                } else
                {
                    sender.sendMessage(ChatColor.RED + "Usage: /setwarp <warp name>");
                    return true;
                }
            } else
            {
                sender.sendMessage(ChatColor.RED + "Only in-game players can use this command.");
                return true;
            }
        } else if (label.equalsIgnoreCase("warp"))
        {
            if (args.length == 1)
            {
                if (sender instanceof Player)
                {
                    Warp warp = getExistingWarp(args[0]);
                    if (warp == null)
                        sender.sendMessage(ChatColor.RED + "Warp \"" + args[0] + "\" not found.");
                    else
                    {
                        Player player = (Player) sender;
                        if (allowAccess(player, warp))
                        {
                            warpPlayer(player, warp);
                            sender.sendMessage("Warped to warp " + warp.getName());
                        } else
                        {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to access this warp.");
                        }
                    }
                } else
                {
                    sender.sendMessage(ChatColor.RED + "Usage: /warp <player> <warp name>");
                }
            } else if (args.length == 2)
            {
                if (!sender.hasPermission(WARP_OTHER_PERMISSION))
                {
                    sender.sendMessage(ChatColor.RED + "Usage: /warp <warp name>");
                } else
                {
                    Player selectedPlayer = null;
                    boolean playerFound = false;
                    for (Player p : getServer().getOnlinePlayers())
                    {
                        if (p.getName().equalsIgnoreCase(args[0]))
                        {
                            selectedPlayer = p;
                            playerFound = true;
                            break;
                        }
                    }
                    if (!playerFound)
                    {
                        sender.sendMessage(ChatColor.RED + "Unable to find player " + args[0]);
                        return true;
                    } else
                    {
                        if (!warpExists(args[1]))
                            sender.sendMessage(ChatColor.RED + "Warp \"" + args[1] + "\" not found.");
                        else
                        {
                            Warp warp = getExistingWarp(args[1]);
                            if (allowAccess((Player) sender, warp))
                            {
                                warpPlayer(selectedPlayer, warp);
                                sender.sendMessage("Warped " + selectedPlayer.getDisplayName() + " to warp " + warp.getName());
                            } else
                                sender.sendMessage(ChatColor.RED + "ERROR: You do not have access to this warp.");
                        }
                    }
                }
            } else
            {
                if (sender instanceof ConsoleCommandSender)
                    sender.sendMessage(ChatColor.RED + "Usage: /warp <player> <warp name>");
                else if (sender.hasPermission(WARP_OTHER_PERMISSION))
                {
                    sender.sendMessage(ChatColor.RED + "Usage: /warp [player] <warp name>");
                } else sender.sendMessage(ChatColor.RED + "Usage: /warp <warp name>");
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("delwarp"))
        {
            Player player = (Player) sender;
            Date now = new Date();
            Warp existingWarp = getExistingWarp(args[0]);
            boolean refuseAccess = false;
            if (existingWarp != null)
            {
                UUID warpOwnerUUIDUUID = existingWarp.getOwnerUUID();
                boolean isOwner = warpOwnerUUIDUUID.equals(player.getUniqueId());
                refuseAccess = !isOwner && !player.hasPermission(OVERRIDE_OTHER_SETWARP_PERMISSION);
                if (refuseAccess)
                {
                    sender.sendMessage(ChatColor.RED + "Error: You are not the owner of this warp.");
                } else
                {
                    getWarps().remove(existingWarp);
                    boolean success = save();
                    if (success)
                        sender.sendMessage("Removed warp " + args[0]);
                    else
                        sender.sendMessage(ChatColor.RED + "Error: An error occured while removing the warp. Please contact a server administrator.");
                }
            } else
            {
                sender.sendMessage(ChatColor.RED + "Warp " + args[0] + " not found.");
            }
        } else if (command.getName().equalsIgnoreCase("warplist"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "Only in-game users can use this command.");
                return true;
            }
            Player player = (Player) sender;

            if (args.length == 1 && (args[0].equalsIgnoreCase("about") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("version")))
            {
                player.performCommand("/version AdvancedWarplistUI");
                return true;
            }


            // /warplist [startIndex] [warpTypeOrdinal] [sortTypeOrdinal] [action] [action - warp name]
            int startIndex = args.length >= 4 ? NumberUtils.toInt(args[0], 0) : 0;
            int currentSortOrdinal = args.length >= 4 ? NumberUtils.toInt(args[1], 0) : 0;
            int currentFilterOrdinal = args.length >= 4 ? NumberUtils.toInt(args[2], 0) : 0;
            int currentAccessOrdinal = args.length >= 4 ? NumberUtils.toInt(args[3], 0) : 0;
            SortType currentSort = SortType.values()[currentSortOrdinal];
            FilterType currentFilter = FilterType.values()[currentFilterOrdinal];
            Warp.Access currentAccess = Warp.Access.values()[currentAccessOrdinal];
            if (!player.hasPermission(currentAccess.getViewPermission()))
            {
                currentAccess = Warp.Access.PUBLIC;
                currentAccessOrdinal = 0;
            }
            if (args.length == 6)
            {
                String action = args[4];
                String target = args[5];
                if (action.equalsIgnoreCase("addfavorite"))
                {
                    for (Warp w : getWarps())
                    {
                        if (w.getName().equalsIgnoreCase(target))
                        {
                            w.getFavoritedUUIDStrings().add((player.getUniqueId().toString()));
                            break;
                        }
                    }
                } else if (action.equalsIgnoreCase("removefavorite"))
                {
                    for (Warp w : getWarps())
                    {
                        if (w.getName().equalsIgnoreCase(target))
                        {
                            w.getFavoritedUUIDStrings().remove(((Player) sender).getUniqueId().toString());
                            break;
                        }
                    }
                }
                Collections.sort(warpsByMostFavorited, Warp.getCombinedComparator(Warp.WarpComparatorTypes.FAVORITED, Warp.WarpComparatorTypes.NAME));
                save();
            }

            ArrayList<Warp> sortedWarps = getWarps(currentSort, currentFilter, currentAccess, sender);

            FancyMessage header = new FancyMessage("Warp List").color(ChatColor.AQUA);
            FancyMessage warps = new FancyMessage("");
            final int NUMBER_OF_LINES = 18;
            int currentWarpIndex = startIndex;
            for (int line = 0; line < NUMBER_OF_LINES; line++)
            {
                if (currentWarpIndex >= sortedWarps.size())
                    break;
                else
                {
                    Warp warp = sortedWarps.get(currentWarpIndex);
                    warps.then((currentWarpIndex + 1) + ". ");
                    if (warp.getCost() == 0)
                    {
                        warps.then(warp.getName())
                                .color(ChatColor.BLUE)
                                .style(ChatColor.UNDERLINE)
                                .command("/warp " + warp.getName())
                                .tooltip(ChatColor.BLUE + "Click to warp to " + warp.getName() + ". (free)");
                    }
                    warps.then(" ");
                    warps.then("❤");

                    boolean favorite = false;
                    for (String UUIDString : warp.getFavoritedUUIDStrings())
                    {
                        UUID compareUUID = UUID.fromString(UUIDString);
                        if (compareUUID.equals(player.getUniqueId()))
                        {
                            favorite = true;
                        }
                    }
                    if (favorite)
                    {
                        warps.color(ChatColor.RED);
                        warps.tooltip(ChatColor.GRAY + "Click to remove favorite.");
                        warps.command(String.format("/warplist %d %d %d %d removefavorite %s", startIndex, currentSortOrdinal, currentFilterOrdinal, currentAccessOrdinal, warp.getName()));
                    } else
                    {
                        warps.color(ChatColor.GRAY);
                        warps.tooltip(ChatColor.GRAY + "Click to add favorite.");
                        warps.command(String.format("/warplist %d %d %d %d addfavorite %s", startIndex, currentSortOrdinal, currentFilterOrdinal, currentAccessOrdinal, warp.getName()));
                    }
                    currentWarpIndex++;
                    if (currentWarpIndex <= sortedWarps.size() - 1)
                        warps.then("\n");
                }
            }

            FancyMessage options = new FancyMessage("Sort: ");
            for (SortType s : SortType.values())
            {
                options.then("[" + s.getLabel() + "]");
                if (!s.needsPermission() || sender.hasPermission(s.getPermission()))
                {
                    if (s.ordinal() == currentSortOrdinal)
                        options.color(ChatColor.DARK_PURPLE);
                    else
                        options.color(ChatColor.BLUE);
                    options.command(String.format("/warplist %d %d %d %d", startIndex, s.ordinal(), currentFilterOrdinal, currentAccessOrdinal));
                    options.tooltip("Sort by " + s.getLabel().toLowerCase());
                    options.then(" ");
                }

            }

            options.then("\nFilter: ")
                    .color(ChatColor.WHITE);
            for (FilterType f : FilterType.values())
            {
                options.then("[" + f.getLabel() + "]");
                if (f.ordinal() == currentFilterOrdinal)
                    options.color(ChatColor.DARK_PURPLE);
                else
                    options.color(ChatColor.BLUE);
                options.command(String.format("/warplist %d %d %d %d", startIndex, currentSortOrdinal, f.ordinal(), currentAccessOrdinal));
                options.tooltip("Filter warps by " + f.getLabel().toLowerCase());
                options.then(" ");
            }
            header.send(sender);
            options.send(sender);
            warps.send(sender);

        } else if (command.getName().equalsIgnoreCase("editwarp"))
        {
            if (args.length == 0)
            {
                sender.sendMessage("Usage: /editwarp <warp> [property] [value]");
            } else if (args.length == 1)
            {
                String warpArg = args[0];
                if (warpExists(warpArg))
                {
                    boolean showEditButtons = sender instanceof Player;
                    Warp warp = getExistingWarp(warpArg);
                    FancyMessage header = new FancyMessage("Properties of " + warp.getName())
                            .style(ChatColor.UNDERLINE);
                    FancyMessage properties = new FancyMessage("");
                    for (Warp.Property property : Warp.Property.values())
                    {
                        switch (property.getType())
                        {
                            case STRING:
                                if (property.getPermission().equals(Warp.Property.NO_PERMISSION) || sender.hasPermission(property.getPermission()))
                                {

                                    properties.then("\n" + property.getName() + ": ");
                                    properties.then("[Edit]")
                                            .color(ChatColor.BLUE)
                                            .tooltip(ChatColor.WHITE + "Click to edit the warp's " + property.getName().toLowerCase())
                                            .suggest(String.format("/editwarp %s %s ", warp.getName(), property.getName().toLowerCase()));
                                }
                                break;
                            case ACCESS:
                                if (sender.hasPermission(SETWARP_HIDDEN) || sender.hasPermission(SETWARP_PRIVATE))
                                {
                                    properties.then("\n" + property.getName() + ": ");
                                    for (Warp.Access access : Warp.Access.values())
                                    {
                                        if (sender.hasPermission(access.getEditPermission()))
                                        {
                                            properties.then("[" + access.getLabel() + "]")
                                                    .tooltip(access.getDescription())
                                                    .command(String.format("/editwarp %s %s %s", warp.getName(), property.getName(), access.name()));
                                            if (access.equals((Warp.Access) property.getValue(warp)))
                                            {
                                                properties.color(ChatColor.DARK_PURPLE);
                                            } else
                                                properties.color(ChatColor.BLUE);
                                            properties.then(" ");
                                        }
                                    }
                                }
                                break;
                        }
                    }
                    header.send(sender);
                    properties.send(sender);
                    return true;
                } else
                {
                    sender.sendMessage(ChatColor.RED + "Unable to find warp " + warpArg);
                }
            }
        }


        /*} catch (Exception e)
        {
            if (!sender.hasPermission(RECEIVE_STACK_TRACE))
                sender.sendMessage(ChatColor.RED + "ERROR: An error occurred while processing the command. Please contact a server administrator.");

            String errorString = "";
            errorString += ChatColor.RED + "An exception occured when " +sender.getName() + " executed command /" + command.getName() + " ";
            for (String s : args)
                errorString += s+" ";
            errorString+="\n" + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
            getServer().broadcast(errorString, RECEIVE_STACK_TRACE);
            getLogger().log(Level.SEVERE, errorString);
        }*/
        return true;
    }


    protected Warp getExistingWarp(String name)
    {
        ArrayList<Warp> warps = getWarps();
        for (Warp warp : warps)
        {
            if (warp.getName().equalsIgnoreCase(name))
                return warp;
        }
        return null;
    }

    protected boolean warpExists(String name)
    {
        ArrayList<Warp> warps = getWarps();
        for (Warp warp : warps)
        {
            if (warp.getName().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }


    public ArrayList<Warp> getWarpsByName()
    {
        return warpsByName;
    }

    public ArrayList<Warp> getWarpsByRecentVisits()
    {
        return warpsByRecentVisits;
    }

    public ArrayList<Warp> getWarpsByTotalVisits()
    {
        return warpsByTotalVisits;
    }

    public ArrayList<Warp> getWarpsByCreation()
    {
        return warpsByCreation;
    }

    public interface Sort
    {
        public String getLabel();

        public boolean needsPermission();

        public String getPermission();
    }

    public interface Filter
    {
        String getLabel();
    }

    public enum SortType implements Sort
    {
        NAME
                {
                    public String getLabel()
                    {
                        return "Name";
                    }

                    public boolean needsPermission()
                    {
                        return false;
                    }

                    public String getPermission()
                    {
                        return null;
                    }
                },
        OLD
                {
                    public String getLabel()
                    {
                        return "Old";
                    }

                    public boolean needsPermission()
                    {
                        return false;
                    }

                    public String getPermission()
                    {
                        return null;
                    }
                },
        NEW
                {
                    public String getLabel()
                    {
                        return "New";
                    }

                    public boolean needsPermission()
                    {
                        return false;
                    }

                    public String getPermission()
                    {
                        return null;
                    }
                },
        TOP
                {
                    public String getLabel()
                    {
                        return "Visits";
                    }

                    public boolean needsPermission()
                    {
                        return false;
                    }

                    public String getPermission()
                    {
                        return null;
                    }
                },
        HOT
                {
                    public String getLabel()
                    {
                        return "Hot";
                    }

                    public boolean needsPermission()
                    {
                        return false;
                    }

                    public String getPermission()
                    {
                        return null;
                    }
                },
        FAVORITED
                {
                    public String getLabel()
                    {
                        return "Most favorited";
                    }

                    public boolean needsPermission()
                    {
                        return false;
                    }

                    public String getPermission()
                    {
                        return null;
                    }
                };
    }

    public enum FilterType implements Filter
    {
        ALL
                {
                    public String getLabel()
                    {
                        return "All";
                    }
                },
        FEATURED
                {
                    public String getLabel()
                    {
                        return "Featured";
                    }
                },
        OWNED
                {
                    public String getLabel()
                    {
                        return "Yours";
                    }
                },
        FAVORITED
                {
                    public String getLabel()
                    {
                        return "Favorited";
                    }
                },
    }


    public ArrayList<Warp> getReverseWarpsByCreation()
    {
        return reverseWarpsByCreation;
    }

    public ArrayList<Warp> getWarpsByMostFavorited()
    {
        return warpsByMostFavorited;
    }

    public ArrayList<Warp> getWarps(SortType s, FilterType f, Warp.Access access, CommandSender sender)
    {
        ArrayList<Warp> sorted = null;
        switch (s)
        {
            case NAME:
                sorted = getWarpsByName();
            case OLD:
                sorted = getWarpsByCreation();
                break;
            case NEW:
                sorted = getReverseWarpsByCreation();
                break;
            case TOP:
                sorted = getWarpsByRecentVisits();
                break;
            case HOT:
                sorted = getWarpsByTotalVisits();
                break;
            case FAVORITED:
                sorted = getWarpsByMostFavorited();

        }
        ArrayList<Warp> filtered = new ArrayList<Warp>(sorted);

        filtered.removeIf((Warp w) -> !w.getAccess().equals(access));


        switch (f)
        {
            case ALL:
                break;
            case FEATURED:
                filtered.removeIf((Warp w) -> !w.isFeatured());
                break;
            case FAVORITED:
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    filtered.removeIf((Warp w) -> !w.getFavoritedUUIDStrings().contains(p.getUniqueId().toString()));
                }
                break;
            case OWNED:
                if (sender instanceof Player)
                {
                    Player p = (Player) sender;
                    filtered.removeIf((Warp w) -> !w.getOwnerUUID().equals(p.getUniqueId()));
                }
                break;
        }
        return filtered;
    }


}

