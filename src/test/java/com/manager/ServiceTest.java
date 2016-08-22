package com.manager;

import com.manager.util.entity.User;
import com.manager.util.json.JsonObject;
import com.manager.util.json.JsonObjectFactory;
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
        JsonObject objectFromJson = createRoom();
        assertNotNull(objectFromJson);
        assertEquals("toUser", objectFromJson.getCommand());
        assertTrue(Long.parseLong(objectFromJson.getFrom()) > 0L);
    }

    private JsonObject createRoom() throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonObject createRoom = new JsonObject("createRoom", user);
        requester.send(JsonObjectFactory.getJsonString(createRoom));
        String response = requester.recvStr();
        return JsonObjectFactory.getObjectFromJson(response, JsonObject.class);
    }

    @Test
    public void testRemoveCommand() throws Exception {
        JsonObject jsonCreatedRoom = createRoom();
        assertNotNull(jsonCreatedRoom);

        JsonObject jsonObject = new JsonObject("removeRoom", user);
        jsonObject.setTo(jsonCreatedRoom.getFrom());
        jsonObject.setFrom(String.valueOf(user.getId()));

        requester.send(JsonObjectFactory.getJsonString(jsonObject));

        String deleteRoomString = requester.recvStr();
        JsonObject jsonDeletedRoom = JsonObjectFactory.getObjectFromJson(deleteRoomString, JsonObject.class);
        assertNotNull(jsonDeletedRoom);
        assertEquals("toUser", jsonDeletedRoom.getCommand());
        assertEquals(jsonCreatedRoom.getFrom(), jsonDeletedRoom.getFrom());
    }


    @Test
    public void testAddRemoveUserCommand() throws Exception {
        JsonObject jsonCreatedRoom = createRoom();
        assertNotNull(jsonCreatedRoom);

        User userPek = new User(2, "pek", "pek");
        updateUser(jsonCreatedRoom, userPek, "addUserToRoom");

        updateUser(jsonCreatedRoom, userPek, "removeUserFromRoom");

    }

    private void updateUser(JsonObject jsonCreatedRoom, User userPek, String command) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonObject jsonObject = new JsonObject(command, userPek);
        jsonObject.setTo(jsonCreatedRoom.getFrom());
        jsonObject.setFrom(String.valueOf(userPek.getId()));

        requester.send(JsonObjectFactory.getJsonString(jsonObject));
        String response = requester.recvStr();

        JsonObject jsonAddedUser = JsonObjectFactory.getObjectFromJson(response, JsonObject.class);
        assertNotNull(jsonAddedUser);
        assertEquals("toUser", jsonAddedUser.getCommand());
        assertEquals(userPek.getId(), jsonAddedUser.getUser().getId());
        assertEquals(userPek.getId(), Integer.parseInt(jsonAddedUser.getTo()));
        assertEquals(jsonCreatedRoom.getFrom(), jsonAddedUser.getFrom());
    }

    @Test
    public void testBadCommand() throws Exception {
        User user = new User();
        JsonObject jsonObject = new JsonObject("poop", user);
        jsonObject.setTo(null);
        requester.send(JsonObjectFactory.getJsonString(jsonObject));

        String response = requester.recvStr();
        JsonObject objectFromJson = JsonObjectFactory.getObjectFromJson(response, JsonObject.class);
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