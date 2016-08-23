package com.manager.command;


import com.chat.util.entity.User;

@FunctionalInterface
public interface Command {

    long execute(User user, long roomId);

    String CREATE_ROOM = "createRoom";
    String REMOVE_ROOM = "removeRoom";
    String ADD_USER_TO_ROOM = "addUserToRoom";
    String REMOVE_USER_FROM_ROOM = "removeUserFromRoom";
    String TO_USER = "toUser";
    String DEFAULT = "";
}
