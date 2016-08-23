package com.manager.service;

import com.chat.util.entity.User;
import com.chat.util.json.JsonObjectFactory;
import com.chat.util.json.JsonProtocol;
import com.manager.command.Command;
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
                Optional<JsonProtocol<User>> objectFromJson = Optional.ofNullable(JsonObjectFactory
                        .getObjectFromJson(request, JsonProtocol.class));
                String command = objectFromJson.map(JsonProtocol::getCommand).orElse("");
                Command method = commands.getOrDefault(command, (user, id) -> -1L);

                long requestTo = Long.parseLong(objectFromJson.map(JsonProtocol::getTo).orElse("0"));
                User user = objectFromJson.map(JsonProtocol::getAttachment).orElseGet(User::new);
                long roomId = method.execute(user, requestTo);

                JsonProtocol<User> reply = new JsonProtocol<>(TO_USER, user);
                reply.setFrom(String.valueOf(roomId));
                reply.setTo(String.valueOf(user.getId()));

                responder.send(JsonObjectFactory.getJsonString(reply));
            }
        }
    }
}
