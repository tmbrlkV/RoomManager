package com.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.manager.command.Command;
import com.manager.util.entity.User;
import com.manager.util.json.JsonObject;
import com.manager.util.json.JsonObjectFactory;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.manager.command.Command.*;

public class Service {
    private static RoomManager roomManager = new RoomManager();
    private static Map<String, Command> commands = new HashMap<String, Command>() {{
        put(CREATE_ROOM, (user, id) -> {
            long roomId = roomManager.createRoom().getId();
            roomManager.addUserToRoom(user, roomId);
            return roomId;
        });
        put(REMOVE_ROOM, (user, id) -> roomManager.removeRoom(id).getId());
        put(ADD_USER_TO_ROOM, (user, id) -> roomManager.addUserToRoom(user, id).getId());
        put(REMOVE_USER_FROM_ROOM, (user, id) -> roomManager.removeUserFromRoom(user, id).getId());
        put(DEFAULT, (user, id) -> -1L);
    }};


    public static void main(String[] args) {
        try (ZMQ.Context context = ZMQ.context(1)) {
            ZMQ.Socket responder = context.socket(ZMQ.REP);
            responder.bind("tcp://*:16000");

            while (!Thread.currentThread().isInterrupted()) {
                String request = responder.recvStr();
                Optional<JsonObject> objectFromJson = Optional.ofNullable(JsonObjectFactory
                        .getObjectFromJson(request, JsonObject.class));
                String command = objectFromJson.map(JsonObject::getCommand).orElse("");
                Command method = commands.getOrDefault(command, (user, id) -> -1L);

                long requestTo = Long.parseLong(objectFromJson.map(JsonObject::getTo).orElse("0"));
                User user = objectFromJson.map(JsonObject::getUser).orElseGet(User::new);
                long roomId = method.execute(user, requestTo);

                JsonObject reply = new JsonObject(TO_USER, user);
                reply.setFrom(String.valueOf(roomId));
                reply.setTo(String.valueOf(user.getId()));

                try {
                    responder.send(JsonObjectFactory.getJsonString(reply));
                } catch (JsonProcessingException e) {
                    responder.send("");
                }
            }
        }
    }
}
