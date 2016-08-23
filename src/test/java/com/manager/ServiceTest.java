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
        JsonProtocol<User> objectFromJson = createRoom();
        assertNotNull(objectFromJson);
        assertEquals("toUser", objectFromJson.getCommand());
        assertTrue(Long.parseLong(objectFromJson.getFrom()) > 0L);
    }

    private JsonProtocol<User> createRoom() throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonProtocol<User> createRoom = new JsonProtocol<>("createRoom", user);
        requester.send(JsonObjectFactory.getJsonString(createRoom));
        String response = requester.recvStr();
        return JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
    }

    @Test
    public void testRemoveCommand() throws Exception {
        JsonProtocol<User> jsonCreatedRoom = createRoom();
        assertNotNull(jsonCreatedRoom);

        JsonProtocol<User> jsonObject = new JsonProtocol<>("removeRoom", user);
        jsonObject.setTo(jsonCreatedRoom.getFrom());
        jsonObject.setFrom(String.valueOf(user.getId()));

        requester.send(JsonObjectFactory.getJsonString(jsonObject));

        String deleteRoomString = requester.recvStr();
        JsonProtocol<User> jsonDeletedRoom = JsonObjectFactory.getObjectFromJson(deleteRoomString, JsonProtocol.class);
        assertNotNull(jsonDeletedRoom);
        assertEquals("toUser", jsonDeletedRoom.getCommand());
        assertEquals(jsonCreatedRoom.getFrom(), jsonDeletedRoom.getFrom());
    }


    @Test
    public void testAddRemoveUserCommand() throws Exception {
        JsonProtocol<User> jsonCreatedRoom = createRoom();
        assertNotNull(jsonCreatedRoom);

        User userPek = new User(2, "pek", "pek");
        updateUser(jsonCreatedRoom, userPek, "addUserToRoom");

        updateUser(jsonCreatedRoom, userPek, "removeUserFromRoom");

    }

    private void updateUser(JsonProtocol<User> jsonCreatedRoom, User userPek, String command) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonProtocol<User> jsonObject = new JsonProtocol<>(command, userPek);
        jsonObject.setTo(jsonCreatedRoom.getFrom());
        jsonObject.setFrom(String.valueOf(userPek.getId()));

        requester.send(JsonObjectFactory.getJsonString(jsonObject));
        String response = requester.recvStr();

        JsonProtocol<User> jsonAddedUser = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
        assertNotNull(jsonAddedUser);
        assertEquals("toUser", jsonAddedUser.getCommand());
        assertEquals(userPek.getId(), jsonAddedUser.getAttachment().getId());
        assertEquals(userPek.getId(), Integer.parseInt(jsonAddedUser.getTo()));
        assertEquals(jsonCreatedRoom.getFrom(), jsonAddedUser.getFrom());
    }

    @Test
    public void testBadCommand() throws Exception {
        User user = new User();
        JsonProtocol<User> jsonObject = new JsonProtocol<>("poop", user);
        jsonObject.setTo(null);
        requester.send(JsonObjectFactory.getJsonString(jsonObject));

        String response = requester.recvStr();
        JsonProtocol<User> objectFromJson = JsonObjectFactory.getObjectFromJson(response, JsonProtocol.class);
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