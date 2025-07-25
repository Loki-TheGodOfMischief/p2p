package common;

import java.io.*;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private String from;
    private String content;
    private LocalDateTime timestamp;
    private String to; // recipient username for private messages (null for group)

    public Message(String from, String to, String content) {
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getFrom() { return from; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getTo() { return to; }
}
