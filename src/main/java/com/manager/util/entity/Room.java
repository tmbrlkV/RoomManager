package com.manager.util.entity;


import com.chat.util.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {
    private static final Logger logger = LoggerFactory.getLogger(Room.class);
    private long id;
    private List<User> users = new ArrayList<>();

    public Room(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public boolean addUser(User user) {
        if (!users.contains(user)) {
            user.setPassword("");
            users.add(user);
            logger.debug("User {}, {} added.", user.getId(), user.getLogin());
            return true;
        }
        return false;
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public boolean removeUser(User user) {
        logger.debug("User {}, {} removed.", user.getId(), user.getLogin());
        return users.remove(user);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Room room = (Room) o;

        return id == room.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
