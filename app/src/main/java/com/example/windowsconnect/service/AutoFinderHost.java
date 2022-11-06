package com.example.windowsconnect.service;

import com.example.windowsconnect.models.Command;
import com.example.windowsconnect.models.CommandHelper;
import com.example.windowsconnect.models.Device;

import java.util.logging.Handler;

public class AutoFinderHost {

    public static class MyThread extends Thread{
        Device device;

        public MyThread(Device device){
            this.device = device;
        }

        @Override
        public void run() {
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

    public static void Find(Device device){
      new MyThread(device).start();
    }
}
