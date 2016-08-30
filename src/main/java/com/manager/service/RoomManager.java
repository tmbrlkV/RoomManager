package com.manager.service;

import com.chat.util.entity.User;
import com.manager.util.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class RoomManager {
    private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);

    private List<Room> rooms = new LinkedList<>();
    private List<User> users = new LinkedList<>();
    private static Room lobby;

    public Room createRoom(User user) {
        Instant now = Instant.now();
        Room room = new Room(now.getEpochSecond() + now.getNano());
        rooms.add(room);
        users.add(user);
        logger.debug("Rooms size: {}.", rooms.size());
        return room;
    }

    public Room createLobby(long roomId) {
        if (lobby == null) {
            lobby = new Room(roomId);
            rooms.add(lobby);
            logger.debug("Rooms size: {}.", rooms.size());
        }
        return lobby;
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public Room removeRoom(long roomId) {
        Optional<Room> foundRoom = findRoom(roomId);
        foundRoom.map(rooms::remove).orElse(false);
        logger.debug("Found room {}", foundRoom);
        return foundRoom.orElse(new Room(-1L));
    }

    public Room addUserToRoom(User user, long roomId) {
        Optional<Room> foundRoom = findRoom(roomId);
        foundRoom.map(room -> {
            System.err.println(user);
            room.addUser(user);
            users.add(user);
            return true;
        }).orElse(false);
        logger.debug("Found room: {}", foundRoom);
        return foundRoom.orElse(new Room(-1L));
    }

    public Room removeUserFromRoom(User user, long roomId) {
        Optional<Room> foundRoom = findRoom(roomId);
        foundRoom.map(room -> room.removeUser(user)).orElse(false);
        Room room = foundRoom.orElse(new Room(-1L));
        if (!room.equals(lobby) && room.getUsers().isEmpty()) {
            room = removeRoom(room.getId());
            logger.debug("Room {} was removed.", room);
        }
        return room;
    }

    public Room removeUserFromAllRooms(User user) {
        logger.debug("Rooms size {}", rooms.size());
        rooms.forEach(room -> {
            logger.debug("Try to remove {} from {}", room, rooms);
            if (room.getUsers().contains(user)) {
                removeUserFromRoom(user, room.getId());
                logger.debug("User {} was removed from {}", user, room);
            }
        });

        users.remove(user);
        logger.debug("Rooms {}", rooms);
        return lobby;
    }

    private Optional<Room> findRoom(long roomId) {
        return rooms.parallelStream().filter(room -> room.getId() == roomId).findFirst();
    }
}
