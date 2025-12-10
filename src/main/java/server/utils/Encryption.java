package server.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Encryption {
    private final String data;
    public Encryption(String link) {
        this.data = link;
    }
    public String returnSHA1 () throws NoSuchAlgorithmException {
        MessageDigest sh1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sh1.digest(getData().getBytes());
        return HexFormat.of().formatHex(digest);
    }

    public String getData() {
        return data;
    }
    @Override
    public String toString(){
        return "[ " + data + "]";
    }
}
