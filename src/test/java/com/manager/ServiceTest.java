package com.manager;


import com.chat.util.entity.User;
import com.chat.util.json.JsonObjectFactory;
import com.chat.util.json.JsonProtocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ServiceTest {
    private ZMQ.Socket requester;
    private ZMQ.Context context;
    private User user;

    @Before
    public void init() {
        context = ZMQ.context(1);
        requester = context.socket(ZMQ.REQ);
        requester.connect("tcp://localhost:16000");
        user = new User(1, "kek", "");
    }

    @Test
    public void testCreateCommand() throws Exception {
        JsonProtocol objectFromJson = createRoom();
        assertNotNull(objectFromJson);
        assertEquals("toUser", objectFromJson.getCommand());
    }

    private JsonProtocol createRoom() throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonProtocol createRoom = new JsonProtocol<>("createRoom", user);
        createRoom.setFrom(String.valueOf(user.getId()));
        createRoom.setTo("roomManager");
        requester.send(JsonObjectFactory.getJsonString(createRoom));
        String response = requester.recvStr();
        JsonProtocol<Long[]> objectFromJson = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
        objectFromJson.setFrom(String.valueOf(objectFromJson.getAttachment()[0]));
        objectFromJson.setTo(String.valueOf(user.getId()));
        return objectFromJson;
    }

    @Test
    public void testRemoveCommand() throws Exception {
        JsonProtocol jsonCreatedRoom = createRoom();
        assertNotNull(jsonCreatedRoom);

        JsonProtocol jsonProtocol = new JsonProtocol<>("removeRoom", user);
        jsonProtocol.setTo(jsonCreatedRoom.getFrom());
        jsonProtocol.setFrom(String.valueOf(user.getId()));

        requester.send(JsonObjectFactory.getJsonString(jsonProtocol));

        String deleteRoomString = requester.recvStr();
        JsonProtocol jsonDeletedRoom = JsonObjectFactory.getObjectFromJson(deleteRoomString, JsonProtocol.class);
        assertNotNull(jsonDeletedRoom);
        assertEquals("toUser", jsonDeletedRoom.getCommand());
        assertEquals(jsonCreatedRoom.getFrom(), jsonDeletedRoom.getFrom());
    }


    @Test
    public void testAddRemoveUserCommand() throws Exception {
        JsonProtocol jsonCreatedRoom = createRoom();
        assertNotNull(jsonCreatedRoom);

        User userPek = new User(4, "pek", "");
        updateUser(jsonCreatedRoom, userPek, "addUserToRoom");

        updateUser(jsonCreatedRoom, userPek, "removeUserFromRoom");

    }

    private void updateUser(JsonProtocol jsonCreatedRoom, User userPek, String command) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonProtocol jsonProtocol = new JsonProtocol<>(command, userPek);
        jsonProtocol.setTo("roomManager:" + jsonCreatedRoom.getFrom());
        jsonProtocol.setFrom(String.valueOf(userPek.getId()));

        requester.send(JsonObjectFactory.getJsonString(jsonProtocol));
        String response = requester.recvStr();

        JsonProtocol fromJson = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
        assertNotNull(fromJson);
        assertEquals("toUser", fromJson.getCommand());
        assertEquals(userPek.getId(), Integer.parseInt(fromJson.getTo()));
        assertEquals(jsonProtocol.getTo(), fromJson.getFrom());
    }

    @Test
    public void testBadCommand() throws Exception {
        User user = new User();
        JsonProtocol jsonProtocol = new JsonProtocol<>("poop", user);
        jsonProtocol.setTo(null);
        requester.send(JsonObjectFactory.getJsonString(jsonProtocol));

        String response = requester.recvStr();
        JsonProtocol objectFromJson = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
        assertNotNull(objectFromJson);
        assertEquals(user.getId(), Integer.parseInt(objectFromJson.getTo()));
        assertEquals("0", objectFromJson.getFrom());
    }

    @Test
    public void testRemoveUserFromAllRooms() throws Exception {
        JsonProtocol jsonCreatedRoom = createRoom();
        updateUser(jsonCreatedRoom, user, "addUserToRoom");
        jsonCreatedRoom = createRoom();
        updateUser(jsonCreatedRoom, user, "addUserToRoom");

        JsonProtocol jsonProtocol = new JsonProtocol<>("removeUserFromAllRooms", user);
        jsonProtocol.setFrom(String.valueOf(user.getId()));
        jsonProtocol.setTo("roomManager:15000");

        requester.send(jsonProtocol.toString());
        String response = requester.recvStr();
        JsonProtocol objectFromJson = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
        assertNotNull(objectFromJson);
        assertEquals(user.getId(), Integer.parseInt(objectFromJson.getTo()));
        assertEquals("roomManager:15000", objectFromJson.getFrom());
    }

    @Test
    public void testGetAllRooms() throws Exception {
        JsonProtocol<User> jsonProtocol = new JsonProtocol<>("getAllRooms", user);
        jsonProtocol.setFrom(String.valueOf(user.getId()));
        jsonProtocol.setTo("roomManager:15000");

        requester.send(jsonProtocol.toString());
        String response = requester.recvStr();
        JsonProtocol protocol = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);

        assertEquals(user.getId(), Integer.parseInt(protocol.getTo()));
        System.out.println(Arrays.toString((Long[]) protocol.getAttachment()));
        assertTrue(((Long[]) protocol.getAttachment()).length >= 1);
    }

    @Test
    public void testGetAllUsers() throws Exception {
        JsonProtocol jsonCreatedRoom = createRoom();
        User userJek = new User(2, "jek", "");
        updateUser(jsonCreatedRoom, userJek, "addUserToRoom");

        User userGeg = new User(3, "geg", "");
        updateUser(jsonCreatedRoom, userGeg, "addUserToRoom");

        JsonProtocol<User> jsonProtocol = new JsonProtocol<>("getAllUsers", user);
        jsonProtocol.setFrom(String.valueOf(user.getId()));
        jsonProtocol.setTo("roomManager");

        requester.send(jsonProtocol.toString());
        String response = requester.recvStr();
        JsonProtocol protocol = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);

        assertEquals(user.getId(), Integer.parseInt(protocol.getTo()));
        System.out.println(Arrays.toString((Long[]) protocol.getAttachment()));
        assertTrue(((Long[]) protocol.getAttachment()).length >= 1);
    }


    @After
    public void clear() {
        requester.close();
        context.close();
    }
}