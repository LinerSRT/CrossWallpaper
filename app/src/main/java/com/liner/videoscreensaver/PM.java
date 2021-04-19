package com.liner.videoscreensaver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.gson.Gson;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ALL")
@SuppressLint("StaticFieldLeak")
public class PM {
    private static PM PM;
    private SharedPreferences sharedPreferences;

    public static void init(Context context, String preferenceName) {
        PM = new PM();
        if (PM.sharedPreferences == null) {
            PM.sharedPreferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        }
    }

    public static void init(Context context) {
        init(context, context.getPackageName());
    }

    public static void put(String key, Object value) {
        checkInitialization();
        SharedPreferences.Editor editor = PM.sharedPreferences.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof byte[]) {
            editor.putString(key, Base64.encodeToString((byte[]) value, Base64.DEFAULT));
        }
        editor.apply();
    }

    public static <T> T get(String key, Object defValue) {
        checkInitialization();
        Object result = defValue;
        if (defValue instanceof String) {
            result = PM.sharedPreferences.getString(key, (String) defValue);
        } else if (defValue instanceof Integer) {
            result = PM.sharedPreferences.getInt(key, (Integer) defValue);
        } else if (defValue instanceof Boolean) {
            result = PM.sharedPreferences.getBoolean(key, (Boolean) defValue);
        } else if (defValue instanceof Float) {
            result = PM.sharedPreferences.getFloat(key, (Float) defValue);
        } else if (defValue instanceof Long) {
            result = PM.sharedPreferences.getLong(key, (Long) defValue);
        } else if (defValue instanceof byte[]) {
            result = Base64.decode(PM.sharedPreferences.getString(key, ""), Base64.DEFAULT);
        }
        return (T) result;
    }

    public static <T> T putObject(String key, final T object, boolean toFile) {
        checkInitialization();
        put(key, new Gson().toJson(object));
        return null;
    }

    public static <T> T putObject(String key, final T object) {
        return putObject(key, object, false);
    }

    public static <T> T getObject(String key, Class<T> object, boolean fromFile) {
        checkInitialization();
        return new Gson().fromJson((String) get(key, ""), object);
    }

    public static <T> T getObject(String key, Class<T> object) {
        return getObject(key, object, false);
    }

    public static <T> T putList(String key, final T list, boolean toFile) {
        return putObject(key, list, true);
    }

    public static <T> T putList(String key, final T list) {
        return putObject(key, list);
    }

    public static <T> List<T> getList(String key, Class<T> clazz, boolean fromFile) {
        checkInitialization();
        List<T> list = new Gson().fromJson((String) get(key, ""), new ListTypeToken<>(clazz));
        if (list == null)
            list = new ArrayList<>();
        return list;
    }

    public static <T> List<T> getList(String key, Class<T> clazz) {
        return getList(key, clazz, false);
    }

    public static void putSet(String key, Set<String> set) {
        checkInitialization();
        SharedPreferences.Editor editor = PM.sharedPreferences.edit();
        editor.putStringSet(key, set);
        editor.apply();
    }

    public static Set<String> getSet(String key) {
        checkInitialization();
        return PM.sharedPreferences.getStringSet(key, null);

    }

    public static void putMap(String key, HashMap<?, ?> map) {
        checkInitialization();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            put("map_" + key + "_" + entry.getKey(), entry.getValue());
        }
    }

    public static HashMap<?, ?> getMap(String key) {
        checkInitialization();
        HashMap<String, Object> hashMap = new HashMap<>();
        for (String preferenceKey : PM.sharedPreferences.getAll().keySet()) {
            if (preferenceKey.contains(key))
                hashMap.put(preferenceKey.replace("map_" + key + "_", ""), get(preferenceKey, ""));
        }
        return hashMap;
    }

    public static void clearAll() {
        checkInitialization();
        SharedPreferences.Editor editor = PM.sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public static void clear(String key) {
        checkInitialization();
        SharedPreferences.Editor editor = PM.sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public static void clearMap(String key) {
        checkInitialization();
        for (String preferenceKey : PM.sharedPreferences.getAll().keySet()) {
            if (preferenceKey.replace("map_" + key + "_", "").equalsIgnoreCase(key)) {
                clear(preferenceKey);
            }
        }
    }

    public static String dump(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Preference dump:").append("\n");
        for (int i = 0; i < PM.sharedPreferences.getAll().keySet().size(); i++) {
            String key = String.valueOf(PM.sharedPreferences.getAll().keySet().toArray()[i]);
            String value = String.valueOf(PM.sharedPreferences.getAll().entrySet().toArray()[i]).replace(key+"=", "");
            stringBuilder.append("\t\t- key=\"").append(key).append("\", value=\"").append(value).append("\"").append("\n");

        }
        return stringBuilder.toString();
    }

    private static void checkInitialization() {
        if (PM == null)
            throw new NullPointerException("Warning! Did you init manager in your Application class?");
    }

    public static class ListTypeToken<T> implements ParameterizedType {
        private Class<?> clazz;

        public ListTypeToken(Class<T> wrapper) {
            this.clazz = wrapper;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{clazz};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }
}