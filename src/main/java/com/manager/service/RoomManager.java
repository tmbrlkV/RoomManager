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

    public Room createRoom() {
        Room room = new Room(Instant.now().getEpochSecond());
        rooms.add(room);
        logger.debug("Rooms size: {}.", rooms.size());
        return room;
    }

    public Room createLobby(long roomId) {
        if (roomId <= 0L) {
            return createRoom();
        }
        Room room = new Room(roomId);
        rooms.add(room);
        logger.debug("Rooms size: {}.", rooms.size());
        return room;
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public Room removeRoom(long roomId) {
        Optional<Room> foundRoom = findRoom(roomId);
        foundRoom.map(rooms::remove).orElse(false);
        return foundRoom.orElse(new Room(-1L));
    }

    public Room addUserToRoom(User user, long roomId) {
        Optional<Room> foundRoom = findRoom(roomId);
        foundRoom.map(room -> room.addUser(user)).orElse(false);
        return foundRoom.orElse(new Room(-1L));
    }

    public Room removeUserFromRoom(User user, long roomId) {
        Optional<Room> foundRoom = findRoom(roomId);
        foundRoom.map(room -> room.removeUser(user)).orElse(false);
        return foundRoom.orElse(new Room(-1L));
    }

    private Optional<Room> findRoom(long roomId) {
        return rooms.stream().filter(room -> room.getId() == roomId).findFirst();
    }
}
