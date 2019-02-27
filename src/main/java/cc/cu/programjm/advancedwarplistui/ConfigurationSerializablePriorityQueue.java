package cc.cu.programjm.advancedwarplistui;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.*;

/**
 * Created by progr on 6/23/2017.
 */
public class ConfigurationSerializablePriorityQueue<T> extends PriorityQueue<T> implements ConfigurationSerializable
{
    public Map<String, Object> serialize()
    {
        HashMap map = new HashMap<String, Object>(1);
        map.put("values", this.toArray());
        return map;
    }

    public ConfigurationSerializablePriorityQueue(Map<String, Object> map)
    {
        if (map.containsKey("values"))
        {
            List<T> values = (List<T>) map.get("values");
            this.addAll(values);
        }

        else

        {
            throw new NullArgumentException("map");
        }
    }

    public ConfigurationSerializablePriorityQueue()
    {
        super();
    }

    public ConfigurationSerializablePriorityQueue deserialize(Map<String, Object> map)
    {
        return new ConfigurationSerializablePriorityQueue(map);
    }

    public ConfigurationSerializablePriorityQueue valueOf(Map<String, Object> map)
    {
        return new ConfigurationSerializablePriorityQueue(map);
    }
}
