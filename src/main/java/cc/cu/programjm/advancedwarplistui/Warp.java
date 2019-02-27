package cc.cu.programjm.advancedwarplistui;

import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.omg.CORBA.NO_PERMISSION;

import java.lang.reflect.Type;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

/**
 * Created by progr on 6/21/2017.
 */
public class Warp implements ConfigurationSerializable
{
    private Location location;
    private UUID ownerUUID;
    private Date creation;
    private int visits;
    private ConfigurationSerializablePriorityQueue<Date> recentVisits;
    private String description;
    private double cost;
    private Access access;
    private String name;
    private boolean featured;
    private ArrayList<String> favoritedUUIDStrings;
    //TODO lastVisit

    public ArrayList<String> getFavoritedUUIDStrings()
    {
        return favoritedUUIDStrings;
    }

    public void setFavoritedUUIDs(ArrayList<String> favoritedUUIDStrings)
    {
        this.favoritedUUIDStrings = favoritedUUIDStrings;
    }

    public void incrementVisits()
    {
        visits++;
    }

    public final static int RECENT_VISIT_COUNT = 5;

    public boolean isFeatured()
    {
        return featured;
    }

    public void setFeatured(boolean featured)
    {
        this.featured = featured;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Warp warp = (Warp) o;

        return name.equals(warp.name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    public Date getCreation()
    {

        return creation;
    }

    public void setCreation(Date creation)
    {
        this.creation = creation;
    }


    public String getName()
    {
        return name;
    }

    public void setFavoritedUUIDStrings(ArrayList<String> favoritedUUIDStrings)
    {
        this.favoritedUUIDStrings = favoritedUUIDStrings;
    }

    public void setName(String name)
    {
        this.name = name;
    }


    public Warp(String name, Location location, UUID ownerUUID, Date creation, UUID owner)
    {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.creation = creation;
        this.visits = 0;
        this.recentVisits = new ConfigurationSerializablePriorityQueue<>();
        this.description = "";
        this.cost = 0.0;
        this.access = Access.PUBLIC;
        this.name = name;
        this.featured = false;
        this.ownerUUID = owner;
        this.favoritedUUIDStrings = new ArrayList<String>();
    }


    public Location getLocation()
    {
        return location;
    }

    public void setLocation(Location location)
    {
        this.location = location;
    }

    public UUID getOwnerUUID()
    {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID)
    {
        this.ownerUUID = ownerUUID;
    }

    public int getVisits()
    {
        return visits;
    }

    public void setVisits(int visits)
    {
        this.visits = visits;
    }

    public ConfigurationSerializablePriorityQueue<Date> getRecentVisits()
    {
        return recentVisits;
    }

    public void setRecentVisits(ConfigurationSerializablePriorityQueue<Date> recentVisits)
    {
        this.recentVisits = recentVisits;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public double getCost()
    {
        return cost;
    }

    public void setCost(double cost)
    {
        this.cost = cost;
    }

    public Access getAccess()
    {
        return access;
    }

    public void setAccess(Access access)
    {
        this.access = access;
    }

    @Override
    public Map<String, Object> serialize()
    {
        Map map = new HashMap<String, Object>();
        map.put("cost", cost);
        map.put("creation", creation);
        map.put("description", description);
        map.put("location", location);
        map.put("name", name);
        map.put("recentVisits", recentVisits);
        map.put("visits", visits);
        map.put("featured", featured);
        map.put("owner",ownerUUID.toString());
        map.put("favoritedUUIDStrings", favoritedUUIDStrings);
        map.put("access", access.serialize());
        return map;
    }


    public Warp(Map<String, Object> map)
    {
        cost = Double.parseDouble(map.getOrDefault("cost", 0.0).toString());
        creation = (Date) map.getOrDefault("creation", new Date());
        description = (String) map.getOrDefault("description", "");
        location = (Location) map.getOrDefault("location",new Location(getServer().getWorlds().get(0),0,0,0));
        name = (String) map.getOrDefault("name","unnamed_warp");
        recentVisits = (ConfigurationSerializablePriorityQueue<Date>) map.getOrDefault("recentVisits", new ConfigurationSerializablePriorityQueue<Date>());
        visits = (int) map.getOrDefault("visits",0);
        featured = (boolean) map.getOrDefault("featured",false);
        ownerUUID = UUID.fromString((String)map.getOrDefault("owner", new UUID(0,0).toString()));
        favoritedUUIDStrings = (ArrayList<String>) map.getOrDefault("favoritedUUIDStrings", new ArrayList<String>());
        access = Access.valueOf(map.getOrDefault("access", Access.PUBLIC).get("name"));
    }

    public static Object deserialize(Map<String, Object> map)
    {
        return new Warp(map);
    }

    public static Object valueOf(Map<String, Object> map)
    {
        return new Warp(map);
    }


    public enum WarpComparatorTypes implements Comparator<Warp>
    {
        COST {
            @Override
            public int compare(Warp o1, Warp o2)
            {
                return Double.compare(o1.getCost(), o2.getCost());
            }
        },
        CREATION {
            @Override
            public int compare(Warp o1, Warp o2)
            {
                return o1.getCreation().compareTo(o2.getCreation());
            }
        },
        NAME {
            @Override
            public int compare(Warp o1, Warp o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        },
        RECENT_VISITS {
            @Override
            public int compare(Warp o1, Warp o2)
            {
                Date warp1Visit = o1.getRecentVisits().size() > 0 ? o1.getRecentVisits().peek() : new Date(0L);
                Date warp2Visit = o2.getRecentVisits().size() > 0 ? o2.getRecentVisits().peek() : new Date(0L);
                return (warp1Visit.compareTo(warp2Visit));
            }
        },
        VISITS {
            @Override
            public int compare(Warp o1, Warp o2)
            {
                return Integer.compare(o1.getVisits(), o2.getVisits());
            }
        },
        FEATURED {
            @Override
            public int compare(Warp o1, Warp o2)
            {
                if (o1.isFeatured() == o2.isFeatured())
                    return 0;
                else
                    return o1.isFeatured() ? 1 : -1;
            }
        },
        FAVORITED {
            @Override
            public int compare(Warp o1, Warp o2)
            {
                return Integer.compare(o1.getFavoritedUUIDStrings().size(), o2.getFavoritedUUIDStrings().size());
            }
        };

    }

    public static Comparator<Warp> getCombinedComparator(WarpComparatorTypes... comparators)
    {
        return new Comparator<Warp>()
        {
            @Override
            public int compare(Warp o1, Warp o2)
            {
                int result = 0;
                for (Comparator<Warp> c : comparators)
                {
                    int comparison = c.compare(o1, o2);
                    if (comparison != 0)
                    {
                        result = comparison;
                        break;
                    }
                    // ( else, continue; )
                }
                return result;
            }
        };
    }

    public static final String NO_PERMISSION = "";
    @SerializableAs("Access")
    public enum Access implements ConfigurationSerializable
    {

        PUBLIC ("Public", "All normal users can view and access this warp.", NO_PERMISSION, NO_PERMISSION, NO_PERMISSION),
        HIDDEN ("Hidden", "Normal users will not see the warp on the warplist, but they can still access it by name.",Main.VIEW_HIDDEN, NO_PERMISSION, Main.SETWARP_HIDDEN),
        PRIVATE ("Private","Normal users cannot see nor access the warp.",Main.WARP_PRIVATE, Main.WARP_PRIVATE, Main.SETWARP_PRIVATE);
        private final String description;
        private final String viewPermission;
        private final String warpPermission;
        private final String editPermission;
        private final String label;

        public String getLabel()
        {
            return this.label;
        }

        public String getDescription()
        {
            return description;
        }

        public String getViewPermission()
        {
            return viewPermission;
        }

        public String getWarpPermission()
        {
            return warpPermission;
        }

        public String getEditPermission()
        {
            return editPermission;
        }

        public boolean hasViewPermission()
        {
            return !viewPermission.equals(NO_PERMISSION);
        }

        public boolean hasEditPermission()
        {
            return !editPermission.equals(NO_PERMISSION);
        }

        public boolean hasWarpPermission()
        {
            return !warpPermission.equals(NO_PERMISSION);
        }

        Access(String label, String description, String viewPermission, String warpPermission, String editPermission)
        {
            this.label = label;
            this.description = description;
            this.viewPermission = viewPermission;
            this.warpPermission = warpPermission;
            this.editPermission = editPermission;
        }

        @Override
        public Map<String, Object> serialize()
        {
            HashMap map = new HashMap<String, Object>(1);
            map.put("name",this.name());
            return map;
        }
        public static Access deserialize(Map<String, Object> map)
        {
            if (map.containsKey("name"))
            {
                return Access.valueOf((String)map.get("name"));
            }
            else
            {
                throw new NullArgumentException("map");
            }
        }


    }

    public enum Property
    {
        NAME {
            public String getName() {return "Name";}
            public Object getValue(Warp warp) {return warp.getName();}
            public void setValue(Warp warp, Object o) {warp.setName((String) o);}
            public String getPermission() {return NO_PERMISSION;}
            public Type getType() {return Type.STRING;}
        },
        DESCRIPTION {
            public String getName() {return "Description";}
            public Object getValue(Warp warp) {return warp.getDescription();}
            public void setValue(Warp warp, Object o) {warp.setDescription((String) o);}
            public String getPermission() {return NO_PERMISSION;}
            public Type getType() {return Type.STRING;}
        },
        ACCESS {
            public String getName() {return "Access";}
            public Object getValue(Warp warp) {return warp.getAccess();}
            public void setValue(Warp warp, Object o) {warp.setAccess(Access.valueOf((String) o));}
            public String getPermission() {return Main.VIEW_HIDDEN;}
            public Type getType() {return Type.ACCESS;}
        };

        abstract String getName();
        abstract Object getValue(Warp warp);
        abstract void setValue(Warp warp, Object o) throws ClassCastException;
        abstract String getPermission();
        abstract Type getType();
        final static String NO_PERMISSION = "";

        public enum Type
        {
            STRING, ACCESS, BOOLEAN;
        }
    }
}
