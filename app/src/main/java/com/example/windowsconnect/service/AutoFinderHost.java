package com.example.windowsconnect.service;

import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Device;

import java.util.logging.Handler;

public class AutoFinderHost {
    public static void Find(Device device){
        String message = CommandHelper.createCommand(Command.requestAddDevice, device);
        for (int i = 0; i <= 200; i++)
        {
            for (int y = 0; y <= 200; y++)
            {
                String input = "192.168."+ y + "." + i;
                UDPClient.sendMessage(message, input, String.valueOf(Settings.SEND_PORT));
            }
        }
    }
}
