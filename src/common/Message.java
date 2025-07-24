package common;

import java.io.*;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private String from;
    private String content;
    private LocalDateTime timestamp;

    public Message(String from, String content) {
        this.from = from;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getFrom() { return from; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
