package fr.orinstone.orinstoneteams;

import java.util.HashMap;
import java.util.Map;

public final class SmallCaps {
    private static final Map<Character, Character> MAP = new HashMap<>();

    static {
        put('a', 'ᴀ'); put('b', 'ʙ'); put('c', 'ᴄ'); put('d', 'ᴅ'); put('e', 'ᴇ');
        put('f', 'ꜰ'); put('g', 'ɢ'); put('h', 'ʜ'); put('i', 'ɪ'); put('j', 'ᴊ');
        put('k', 'ᴋ'); put('l', 'ʟ'); put('m', 'ᴍ'); put('n', 'ɴ'); put('o', 'ᴏ');
        put('p', 'ᴘ'); put('q', 'ǫ'); put('r', 'ʀ'); put('s', 'ꜱ'); put('t', 'ᴛ');
        put('u', 'ᴜ'); put('v', 'ᴠ'); put('w', 'ᴡ'); put('x', 'x'); put('y', 'ʏ');
        put('z', 'ᴢ');
    }

    private static void put(char normal, char small) {
        MAP.put(normal, small);
        MAP.put(Character.toUpperCase(normal), small);
    }

    private SmallCaps() {}

    public static String convert(String input) {
        StringBuilder out = new StringBuilder();
        boolean color = false;
        for (char c : input.toCharArray()) {
            if (c == '&' || c == '§') {
                color = true;
                out.append(c);
                continue;
            }
            if (color) {
                color = false;
                out.append(c);
                continue;
            }
            out.append(MAP.getOrDefault(c, c));
        }
        return out.toString();
    }
}
