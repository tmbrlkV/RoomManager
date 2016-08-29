package com.manager.service;

import com.chat.util.entity.User;
import com.manager.util.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RoomManager {
    private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);

    private List<Room> rooms = new ArrayList<>();
    private List<User> users = new ArrayList<>();
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
//        if (room.getUsers().isEmpty()) {
//            room = removeRoom(room.getId());
//        }
        return room;
    }

    public Room removeUserFromAllRooms(User user) {
        rooms.stream().forEach(room -> {
            logger.debug("Try to remove from {}", room);
            if (room.getUsers().contains(user)) {
                removeUserFromRoom(user, room.getId());
                logger.debug("User {} was removed from {}", user, room);
            }
        });

        return lobby;
    }

    private Optional<Room> findRoom(long roomId) {
        return rooms.stream().filter(room -> room.getId() == roomId).findFirst();
    }
}
