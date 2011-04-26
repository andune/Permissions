package com.nijiko.permissions;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import com.nijiko.data.GroupStorage;
import com.nijiko.data.GroupWorld;
import com.nijiko.data.StorageFactory;
import com.nijiko.data.UserStorage;
public class ModularControl extends PermissionHandler
{
    private Map<String, UserStorage> WorldUserStorage = new HashMap<String, UserStorage>();
    private Map<String, GroupStorage> WorldGroupStorage = new HashMap<String, GroupStorage>();
    private Map<String, String> WorldUserStorageCopy = new HashMap<String, String>();
    private Map<String, String> WorldGroupStorageCopy = new HashMap<String, String>();
    
    private Map<String, Map<String, Group>> WorldGroups = new HashMap<String, Map<String, Group>>();
    private Map<String, Map<String, User>> WorldUsers = new HashMap<String, Map<String, User>>();
    private Map<String, Group> WorldBase = new HashMap<String, Group>();
//    private Configuration storageConfig;
    private String defaultWorld = "";
    
    public ModularControl(Configuration storageConfig)
    {
//        this.storageConfig = storageConfig;
        StorageFactory.setConfig(storageConfig);
    }

    @Override
    public void setDefaultWorld(String world) {
        this.defaultWorld = world;
    }

    @Override
    public boolean loadWorld(String world) throws Exception {
        if(checkWorld(world))
        {
            forceLoadWorld(world);
            return true;
        }
        return false;
    }
//    public void setConfig(Configuration config)
//    {
//        this.storageConfig = config;
//    }

    @Override
    public void forceLoadWorld(String world) throws Exception {
        UserStorage userStore = StorageFactory.getUserStorage(world);
        GroupStorage groupStore = StorageFactory.getGroupStorage(world);
        load(world,userStore,groupStore);
    }
    @Override
    public boolean checkWorld(String world) {
        return ((WorldUserStorage.get(world.toLowerCase())==null) && (WorldUserStorageCopy.get(world.toLowerCase())==null))
        ||(((WorldGroupStorage.get(world.toLowerCase())==null) && (WorldGroupStorageCopy.get(world.toLowerCase())==null)));
    }

    @Override
    public void load() throws Exception {
        this.loadWorld(defaultWorld);
    }

    
    @Override
    public void reload() {
        for(UserStorage store : WorldUserStorage.values())
        {
            store.reload();
        }
        for(GroupStorage store : WorldGroupStorage.values())
        {
            store.reload();
        }
    }

    private UserStorage getUserStorage(String world)
    {
        if(world==null)return null;
        String userParentWorld = this.WorldUserStorageCopy.get(world.toLowerCase());
        if(userParentWorld==null) userParentWorld = world;
        return this.WorldUserStorage.get(userParentWorld.toLowerCase());
    }

    private GroupStorage getGroupStorage(String world)
    {
        if(world==null)return null;
        String groupParentWorld = this.WorldGroupStorageCopy.get(world.toLowerCase());
        if(groupParentWorld==null) groupParentWorld = world;
        return this.WorldGroupStorage.get(groupParentWorld.toLowerCase());
    }
    @Override
    public boolean reload(String world) {
        UserStorage userStore = getUserStorage(world);
        GroupStorage groupStore = getGroupStorage(world);
        if(userStore==null&&groupStore==null) return false;
        if(userStore!=null)userStore.reload();
        if(groupStore!=null)groupStore.reload();
        return true;
    }

    
    public boolean has(String world, String name, String permission) {
        return permission(world,name,permission);
    }
    
    @Override
    public boolean has(Player player, String permission) {
        return permission(player,permission);
    }

    
    @Override
    public boolean permission(Player player, String permission) {
        String name = player.getName();
        String worldName = player.getWorld().getName();
        return permission(worldName,name,permission);
    }

    
    public boolean permission(String world, String name, String permission)
    {
        if(name==null||name.isEmpty()||world==null||world.isEmpty()) return true;
        User user;
        try {
            user = this.safeGetUser(world, name);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }        
        return user.hasPermission(permission);
    }

    
    public String getGroupName(String world, String name) {
        Map<String,Group> groups = this.WorldGroups.get(world.toLowerCase());
        if(groups == null) return null;
        Group g = groups.get(name.toLowerCase());
        if(g == null) return null;
        return g.getName();
    }

    
    public Set<Group> getParentGroups(String world, String name) {
        try {
            return this.stringToGroups(safeGetUser(world, name).getParents());
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<Group>();
        }
    }

    
    public boolean inGroup(String world, String name, String groupWorld, String group) {
        try {
            return safeGetUser(world, name).inGroup(groupWorld, group);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    
    public boolean inSingleGroup(String world, String name,String groupWorld, String group) {
        try {
            return safeGetUser(world, name).getParents().contains(new GroupWorld(groupWorld,group));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    
    @Override
    public String getGroupPrefix(String world, String group) {
        try {
            return safeGetGroup(world, group).getPrefix();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    
    @Override
    public String getGroupSuffix(String world, String group) {
        try {
            return safeGetGroup(world, group).getSuffix();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    
    @Override
    public boolean canGroupBuild(String world, String group) {
        try {
            return safeGetGroup(world, group).canBuild();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    
    @Override
    public void save(String world) {
        UserStorage userStore = getUserStorage(world);
        GroupStorage groupStore = getGroupStorage(world);
        if(userStore!=null)userStore.save();
        if(groupStore!=null)groupStore.save();
    }

    
    @Override
    public void saveAll() {
        Collection<UserStorage> userStores = this.WorldUserStorage.values();
        for(UserStorage userStore : userStores)
        {
            userStore.save();
        }
        Collection<GroupStorage> groupStores = this.WorldGroupStorage.values();
        for(GroupStorage groupStore : groupStores)
        {
            groupStore.save();
        }
    }
    Set<Group> stringToGroups(Set<GroupWorld> raws)
    {        
        Set<Group> groupSet = new HashSet<Group>();
        for(GroupWorld raw : raws)
        {
            Map<String, Group> gMap = this.WorldGroups.get(raw.getWorld().toLowerCase());
            if(gMap != null)
            {
                Group g = gMap.get(raw.getName().toLowerCase());
                if(g != null) groupSet.add(g);
            }
        }
        return groupSet;
    }
    
    @Override
    public User safeGetUser(String world, String name) throws Exception
    {
        try
        {
            loadWorld(world);
        }
        catch(Exception e)
        {
            throw new Exception("Error creating user " + name + " in world " + world + " due to storage problems!", e);
        }
        if(this.WorldUsers.get(world.toLowerCase()) == null) this.WorldUsers.put(world.toLowerCase(), new HashMap<String, User>());
        if(this.WorldUsers.get(world.toLowerCase()).get(name.toLowerCase()) == null) this.WorldUsers.get(world.toLowerCase()).put(name.toLowerCase(), new User(this, getUserStorage(world), name, world));
        return this.WorldUsers.get(world.toLowerCase()).get(name.toLowerCase());
    }
    
    @Override
    public Group safeGetGroup(String world, String name) throws Exception
    {
        try
        {
            loadWorld(world);
        }
        catch(Exception e)
        {
            throw new Exception("Error creating group " + name + " in world " + world + " due to storage problems!", e);
        }
        if(this.WorldGroups.get(world.toLowerCase()) == null) this.WorldGroups.put(world, new HashMap<String, Group>());
        if(this.WorldGroups.get(world.toLowerCase()).get(name.toLowerCase()) == null) this.WorldGroups.get(world.toLowerCase()).put(name.toLowerCase(), new Group(this, getGroupStorage(world), name, world));
        return this.WorldGroups.get(world.toLowerCase()).get(name.toLowerCase());
    }

    Group getDefaultGroup(String world)
    {
        return this.WorldBase.get(world.toLowerCase());
    }
    
    @Override
    public Collection<User> getUsers(String world)
    {
        if(WorldUsers.get(world.toLowerCase())==null) return new HashSet<User>();
        return WorldUsers.get(world.toLowerCase()).values();
    }
    

    @Override
    public Collection<Group> getGroups(String world)
    {
        if(WorldGroups.get(world.toLowerCase())==null) return new HashSet<Group>();
        return WorldGroups.get(world.toLowerCase()).values();
    }
    
    @Override
    public User getUserObject(String world, String name)
    {
        if(WorldUsers.get(world.toLowerCase())==null) return null;
        return WorldUsers.get(world.toLowerCase()).get(name.toLowerCase());
    }
    
    @Override
    public Group getGroupObject(String world, String name)
    {
        if(WorldGroups.get(world.toLowerCase())==null) return null;
        return WorldGroups.get(world.toLowerCase()).get(name.toLowerCase());
    }
    @Override
    public String getGroup(String world, String name) {
        return this.getGroupName(world, name);
    }
    
    
    @Override
    public String[] getGroups(String world, String name) {
        Set<Group> groups;
        try {
            groups = safeGetUser(world,name).getAncestors();
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
        List<String> groupList = new ArrayList<String>(groups.size());
        for(Group g : groups)
        {
            if(g == null) continue;
            if(g.getWorld().equalsIgnoreCase(world))groupList.add(g.getName());
        }
        return groupList.toArray(new String[0]);
    }
    
    
    @Override
    public boolean inGroup(String world, String name, String group) {
        return inGroup(world,name,world,group);
    }
    
    
    @Override
    public boolean inSingleGroup(String world, String name, String group) {
        return inSingleGroup(world,name,world,group);
    }
    
    
    
    @Override
//    @Deprecated
    public void addUserPermission(String world, String user, String node) {
        try {
            safeGetUser(world,user).addPermission(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    @Override
//    @Deprecated
    public void removeUserPermission(String world, String user, String node) {
        try {
            safeGetUser(world,user).removePermission(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    
    private void load(String world, UserStorage userStore, GroupStorage groupStore)
    {
        if(userStore==null||groupStore==null) return;
        String userWorld = userStore.getWorld();
        if(!world.equalsIgnoreCase(userWorld)) this.WorldUserStorageCopy.put(world.toLowerCase(), userWorld);
        String groupWorld = groupStore.getWorld();
        if(!world.equalsIgnoreCase(groupWorld)) this.WorldGroupStorageCopy.put(world.toLowerCase(), groupWorld);
        this.WorldUserStorage.put(userStore.getWorld().toLowerCase(), userStore);
        this.WorldGroupStorage.put(groupStore.getWorld().toLowerCase(), groupStore);

        Map<String, User> users = new HashMap<String, User>();        
        Set<String> userNames = userStore.getUsers();
        for(String userName : userNames)
        {
            User user = new User(this,userStore,userName,world);
            users.put(userName.toLowerCase(), user);
        }
        WorldUsers.put(world.toLowerCase(), users);

        HashMap<String, Group> groups = new HashMap<String, Group>();        
        Set<String> groupNames = groupStore.getGroups();
        for(String groupName : groupNames)
        {
            Group group = new Group(this,groupStore,groupName,world);
            groups.put(groupName.toLowerCase(), group);
        }
        WorldGroups.put(world.toLowerCase(), groups);
    }
    @Override
    public String getGroupPermissionString(String world, String group,
            String permission) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public int getGroupPermissionInteger(String world, String group,
            String permission) {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public boolean getGroupPermissionBoolean(String world, String group,
            String permission) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public double getGroupPermissionDouble(String world, String group,
            String permission) {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public String getUserPermissionString(String world, String name,
            String permission) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public int getUserPermissionInteger(String world, String name,
            String permission) {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public boolean getUserPermissionBoolean(String world, String name,
            String permission) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public double getUserPermissionDouble(String world, String name,
            String permission) {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public String getPermissionString(String world, String name,
            String permission) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public int getPermissionInteger(String world, String name, String permission) {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public boolean getPermissionBoolean(String world, String name,
            String permission) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public double getPermissionDouble(String world, String name,
            String permission) {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public void addGroupInfo(String world, String group, String node,
            Object data) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void removeGroupInfo(String world, String group, String node) {
        // TODO Auto-generated method stub
        
    }
    
    
    
    
    

}