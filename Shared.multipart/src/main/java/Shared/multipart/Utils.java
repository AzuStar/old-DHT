package Shared.multipart;

/**
 * Utils
 */
public final class Utils {

    public static final int KEY_BITS = 8;

    public static int Hash(String input, int mod) {
        int hash = 0;

        for (int i = 0; i < input.length(); i++)
            hash = hash * 31 + (int) input.charAt(i);

        if (hash < 0)
            hash = hash * -1;

        return hash % ((int) Math.pow(2, mod));
    }
    

}