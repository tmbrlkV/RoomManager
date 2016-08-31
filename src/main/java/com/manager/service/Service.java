package com.manager.service;

import com.chat.util.entity.User;
import com.chat.util.json.JsonObjectFactory;
import com.chat.util.json.JsonProtocol;
import com.manager.command.Command;
import com.manager.util.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.manager.command.Command.*;

public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    private static Pattern pattern = Pattern.compile("([a-zA-Z]+:?)((\\d+):?){0,2}");
    private static RoomManager roomManager = new RoomManager();
    private static Map<String, Command> commands = new HashMap<String, Command>() {{
        put(CREATE_ROOM, (user, id) -> {
            long roomId = roomManager.createRoom(user).getId();
            logger.debug("***ROOM_ID {}***", roomId);
            roomManager.addUserToRoom(user, roomId);
            return new Long[]{roomId};
        });
        put(REMOVE_ROOM, (user, id) -> new Long[]{roomManager.removeRoom(id).getId()});
        put(ADD_USER_TO_ROOM, (user, id) -> new Long[]{roomManager.addUserToRoom(user, id).getId()});
        put(REMOVE_USER_FROM_ROOM, (user, id) -> new Long[]{roomManager.removeUserFromRoom(user, id).getId()});
        put(REMOVE_USER_FROM_ALL_ROOMS, (user, id) -> new Long[]{roomManager.removeUserFromAllRooms(user).getId()});
        put(GET_ALL_ROOMS, (user, id) -> roomManager.getRooms().stream().map(Room::getId).toArray(Long[]::new));
        put(GET_ALL_USERS, (user, id) -> roomManager.getUsers().parallelStream().map(User::getId)
                .map(Integer::longValue).toArray(Long[]::new));
        put(DEFAULT, (user, id) -> new Long[]{-1L});
    }};

    private static long getRoomId(String keyTo) {
        Matcher matcher = pattern.matcher(keyTo);
        if (matcher.matches()) {
            logger.debug("Matcher: {}", matcher);
            String group = matcher.group(3);
            logger.debug("Group: {}", group);
            if (group != null) {
                return Long.parseLong(group);
            }
        }
        return 0L;
    }

    private static Room initLobby() {
        return roomManager.createLobby(15000);
    }

    public static void main(String[] args) {
        try (ZMQ.Context context = ZMQ.context(1)) {
            ZMQ.Socket responder = context.socket(ZMQ.REP);
            responder.bind("tcp://*:16000");
            Room lobby = initLobby();
            logger.debug("Lobby initialized: {}", lobby);

            while (!Thread.currentThread().isInterrupted()) {
                logger.debug("Start");
                String request = responder.recvStr();
                logger.debug("Request: {}", request);
                Optional<JsonProtocol<User>> objectFromJson = Optional.ofNullable(JsonObjectFactory
                        .getObjectFromJson(request, JsonProtocol.class));
                String command = objectFromJson.map(JsonProtocol::getCommand).orElse("");
                logger.debug("Command: {}", command);
                Command method = commands.getOrDefault(command, (user, id) -> new Long[]{-1L});

                String requestTo = objectFromJson.map(JsonProtocol::getTo).orElse("0");
                logger.debug("RequestTo: {}", requestTo);
                User user = objectFromJson.map(JsonProtocol::getAttachment).orElseGet(User::new);
                logger.debug("Before execute");
                Long[] roomId = method.execute(user, getRoomId(requestTo));

                JsonProtocol<Long[]> reply = new JsonProtocol<>(TO_USER, roomId);
                reply.setFrom("roomManager:" + roomId[0]);
                reply.setTo(String.valueOf(user.getId()));

                responder.send(reply.toString());
                logger.debug("Send {}", reply);
            }
        }
    }
}
