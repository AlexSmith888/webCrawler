package client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class RequestBody implements Runnable {
    private String message;
    private String server;
    private Integer portNumber;

    RequestBody(String message, String server, int port) {
        this.message = message;
        this.server = server;
        this.portNumber = port;
    }

    @Override
    public void run() {
        byte[] dataToBeSend = message.getBytes(StandardCharsets.UTF_8);
        try (Socket remote = new Socket(server, portNumber)) {
            InputStream in = remote.getInputStream();
            OutputStream out = remote.getOutputStream();

            out.write(dataToBeSend);
            remote.shutdownOutput();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int val ;
            while ((val = in.read()) != -1) {
                buffer.write(val);
            }
            System.out.println(buffer.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
