package com.manager;


import com.chat.util.entity.User;
import com.chat.util.json.JsonObjectFactory;
import com.chat.util.json.JsonProtocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

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
        user = new User(1, "kek", "kek");
    }

    @Test
    public void testCreateCommand() throws Exception {
        JsonProtocol objectFromJson = createRoom();
        assertNotNull(objectFromJson);
        assertEquals("toUser", objectFromJson.getCommand());
        assertTrue(Long.parseLong(objectFromJson.getFrom()) > 0L);
    }

    private JsonProtocol createRoom() throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonProtocol createRoom = new JsonProtocol<>("createLobby", user);
        createRoom.setFrom(String.valueOf(user.getId()));
        createRoom.setTo("roomManager:2357");
        requester.send(JsonObjectFactory.getJsonString(createRoom));
        String response = requester.recvStr();
        return JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
    }

    @Test
    public void testRemoveCommand() throws Exception {
        JsonProtocol jsonCreatedRoom = createRoom();
        assertNotNull(jsonCreatedRoom);

        JsonProtocol jsonProtocol = new JsonProtocol<>("removeRoom", user);
        jsonProtocol.setTo("roomManager:" + jsonCreatedRoom.getFrom());
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

        User userPek = new User(2, "pek", "pek");
        updateUser(jsonCreatedRoom, userPek, "addUserToRoom");

        updateUser(jsonCreatedRoom, userPek, "removeUserFromRoom");

    }

    private void updateUser(JsonProtocol jsonCreatedRoom, User userPek, String command) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonProtocol jsonProtocol = new JsonProtocol<>(command, userPek);
        jsonProtocol.setTo("roomManager:" + jsonCreatedRoom.getFrom());
        jsonProtocol.setFrom(String.valueOf(userPek.getId()));

        requester.send(JsonObjectFactory.getJsonString(jsonProtocol));
        String response = requester.recvStr();

        JsonProtocol<User> fromJson = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
        assertNotNull(fromJson);
        assertEquals("toUser", fromJson.getCommand());
        assertEquals(userPek.getId(), fromJson.getAttachment().getId());
        assertEquals(userPek.getId(), Integer.parseInt(fromJson.getTo()));
        assertEquals(jsonCreatedRoom.getFrom(), fromJson.getFrom());
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
        assertEquals(-1L, Integer.parseInt(objectFromJson.getFrom()));
    }


    @After
    public void clear() {
        requester.close();
        context.close();
    }
}