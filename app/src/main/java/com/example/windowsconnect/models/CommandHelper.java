package com.example.windowsconnect.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;


public class CommandHelper {

    public static String toJson(Object obj){
        ObjectMapper mapper = new ObjectMapper();
        String jsonObj;
        try {
            jsonObj = mapper.writeValueAsString(obj);
            return jsonObj;
        } catch (JsonProcessingException e) {
        }
        return "";
    }

/*    public static String createCommand(int command, Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonObj = null;
        try {
            jsonObj = mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
        }
        return  "{\"command\" : " + "\"" + command + "\"" + ", \"value\" : " + jsonObj + "}";
    }

    public static String createCommand(Command command, int x, int y, int action, int pointer) {
        return  "{\"command\": \""+command+"\", \"value\": {\"x\": " + x + ",\"y\": " + y + ", \"action\": " + action + ", \"pointer\" : " + pointer + "}}";
    }


    public static String createCommand(Command command) {
        return  "{\"command\": \""+command+"\", \"value\": \"\"}";
    }*/
}
