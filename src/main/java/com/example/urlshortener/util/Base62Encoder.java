package com.example.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class Base62Encoder {

    // The Base62 Alphabet: 10 digits + 26 uppercase + 26 lowercase = 62 characters
    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_ALPHABET.length();
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Encodes a base-10 integer (e.g., a database sequence ID) into a Base62 string.
     * 
     * @param id The base-10 integer ID.
     * @return The Base62 encoded string.
     */
    public String encode(long id) {
        if (id == 0) {
            return String.valueOf(BASE62_ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        long current = id;
        
        while (current > 0) {
            int remainder = (int) (current % BASE);
            sb.append(BASE62_ALPHABET.charAt(remainder));
            current /= BASE;
        }
        
        // The modulo operation gives us the least significant digit first, 
        // so we must reverse the string builder to get the correct representation.
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back into its original base-10 integer (long).
     * 
     * @param shortCode The Base62 encoded string.
     * @return The original base-10 integer ID.
     * @throws IllegalArgumentException if the shortCode contains invalid characters.
     */
    public long decode(String shortCode) {
        long id = 0;
        
        for (int i = 0; i < shortCode.length(); i++) {
            char c = shortCode.charAt(i);
            int value = BASE62_ALPHABET.indexOf(c);
            
            if (value == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            
            id = id * BASE + value;
        }
        
        return id;
    }

    /**
     * Generates a random Base62 string of the specified length.
     * (Retained for collision-based randomized alias generation).
     * 
     * @param length The desired length of the short code.
     * @return A random Base62 encoded string.
     */
    public String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_ALPHABET.charAt(RANDOM.nextInt(BASE)));
        }
        return sb.toString();
    }
}