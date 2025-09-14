package dev.colbster937.scuffed;

import java.io.File;
import java.lang.reflect.Field;

import com.mojang.minecraft.server.MinecraftServer;

import dev.colbster937.scuffed.server.ScuffedMinecraftServer;
import dev.colbster937.scuffed.server.ScuffedPlayer;

public class ScuffedUtils {
    public static String formatEnabledDisabled(boolean value) {
        return value ? "ENABLED" : "DISABLED";
    }

    public static boolean isCommand(String commandString, String checkCommand) {
        String[] command = commandString.split(" ");
        return command[0].equalsIgnoreCase(checkCommand);
    }

    public static int isLoginCommand(String commandString) {
        if (ScuffedUtils.isCommand(commandString, "/login") || ScuffedUtils.isCommand(commandString, "/log") || ScuffedUtils.isCommand(commandString, "/l")) {
            return 1;
        } else if (ScuffedUtils.isCommand(commandString, "/register") || ScuffedUtils.isCommand(commandString, "/reg") || ScuffedUtils.isCommand(commandString, "/r")) {
            return 2;
        } else {
            return 0;
        }
    }

    public static boolean isRegistered(String player) {
        File file = new File("users", player + ".txt");
        return file.exists();
    }

    public static boolean isAdmin(MinecraftServer mc, String player) {
        return mc.admins.containsPlayer(player);
    }

    public static boolean isAdmin(MinecraftServer mc, ScuffedPlayer player) {
        return isAdmin(mc, player.player.name) && player.loggedIn;
    }

    public static int[] getLevelSize(String size) {
        int[] sizeInt = new int[3];
        String[] sizeStr = size.split("x");
        if (sizeStr.length != 3) {
            return new int[] { 256, 64, 256 };
        }
        for (int i = 0; i < 3; i++) {
            try {
                sizeInt[i] = Integer.parseInt(sizeStr[i]);
            } catch (NumberFormatException e) {
                return new int[] { 256, 64, 256 };
            }
        }
        return sizeInt;
    }


    public static Object getField(Object o, String name) {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                ScuffedMinecraftServer.logger.warning("Failed to get " + name + " (" + e.getMessage() + ")");
                return null;
            }
        }
        ScuffedMinecraftServer.logger.warning("No such field: " + name);
        return null;
    }


    public static void setField(Object o, String name, String value) {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                Class<?> t = f.getType();
                if (t == int.class) f.setInt(o, Integer.parseInt(value));
                else if (t == boolean.class) f.setBoolean(o, Boolean.parseBoolean(value));
                else if (t == long.class) f.setLong(o, Long.parseLong(value));
                else if (t == double.class) f.setDouble(o, Double.parseDouble(value));
                else if (t == float.class) f.setFloat(o, Float.parseFloat(value));
                else if (t == short.class) f.setShort(o, Short.parseShort(value));
                else if (t == byte.class) f.setByte(o, Byte.parseByte(value));
                else f.set(o, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                ScuffedMinecraftServer.logger.warning("Failed to set " + name + " = " + value + " (" + e.getMessage() + ")");
                return;
            }
        }
        ScuffedMinecraftServer.logger.warning("No such field: " + name);
    }

	public static String formatDouble(double value) {
		if (value == (int) value) {
			return String.valueOf((int) value);
		} else {
			return String.valueOf(value);
		}
	}
}
