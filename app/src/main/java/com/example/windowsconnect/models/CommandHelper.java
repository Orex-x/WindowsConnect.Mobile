package com.example.windowsconnect.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;


public class CommandHelper {

    public static String createCommand(Command command, Object obj, boolean isSmallFile) {
        if(isSmallFile){
            ObjectMapper mapper = new ObjectMapper();
            String jsonObj = null;
            try {
                jsonObj = mapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {

            }
            return  "{\"command\" : " + "\"" + command + "\"" + ", \"value\" : " + jsonObj + "}";
        }else{
            return  "{\"command\" : " + "\"" + command + "\"" + ", \"value\" : " + obj + "}";
        }
    }
}
