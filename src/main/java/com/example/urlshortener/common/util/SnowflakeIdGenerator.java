package com.example.urlshortener.common.util;

import org.springframework.stereotype.Component;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;

@Component
public class SnowflakeIdGenerator {

    // Snowflake Algorithm Constants
    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    // Custom Epoch (e.g., Jan 1, 2024) to maximize the lifespan of the generator
    private static final long CUSTOM_EPOCH = 1704067200000L;

    private final long nodeId;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public SnowflakeIdGenerator() {
        this.nodeId = createNodeId();
    }

    /**
     * Generates a globally unique 64-bit ID.
     * Thread-safe and distributed-safe.
     */
    public synchronized long nextId() {
        long currentTimestamp = timestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock! Clock moved backwards.");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted for this millisecond, block until next millisecond
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            // Reset sequence for the new millisecond
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        // Pack the bits together into a single 64-bit long
        return (currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS))
                | (nodeId << SEQUENCE_BITS)
                | sequence;
    }

    private long timestamp() {
        return Instant.now().toEpochMilli() - CUSTOM_EPOCH;
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }

    /**
     * Generates a node ID based on the machine's MAC address to prevent 
     * ID collisions when running multiple instances of the app.
     */
    private long createNodeId() {
        long id = 0;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                }
            }
            id = sb.toString().hashCode() & MAX_NODE_ID;
        } catch (Exception ex) {
            // Fallback to a random number if MAC address cannot be read
            id = (new SecureRandom().nextInt() & MAX_NODE_ID);
        }
        return id;
    }
}