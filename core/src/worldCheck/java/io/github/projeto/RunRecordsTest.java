package io.github.projeto;

import com.badlogic.gdx.Preferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Exercises record updates/reloads without OpenGL or the player's real save file. */
public final class RunRecordsTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Map<String, Object> saved = new HashMap<>();
        int[] flushes = {0};
        Preferences preferences = (Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(),
            new Class<?>[]{Preferences.class}, (proxy, method, values) -> {
                String name = method.getName();
                if (name.equals("flush")) { flushes[0]++; return null; }
                if (name.startsWith("put")) { saved.put((String) values[0], values[1]); return proxy; }
                if (name.startsWith("get")) return saved.getOrDefault((String) values[0], values[1]);
                throw new UnsupportedOperationException(name);
            });
        RunRecords records = new RunRecords(preferences);
        check(records.getBestScore() == 0 && records.getBestTimeDisplay().equals("00:00"), "First launch records");
        check(records.submit(45, 91.75), "First score record not detected");
        check(flushes[0] == 1, "A completed run must be written once immediately");
        RunRecords reloaded = new RunRecords(preferences);
        check(reloaded.getBestScore() == 45 && reloaded.getBestTimeDisplay().equals("01:31"), "Records lost on reload");
        check(!reloaded.submit(20, 32), "Worse run replaced a score record");
        check(flushes[0] == 1, "Unchanged records unnecessarily flushed");
        check(!reloaded.submit(40, 130), "Time-only record was treated as a score record");
        check(reloaded.getBestScore() == 45 && reloaded.getBestTimeDisplay().equals("02:10"), "Independent time record failed");
        check(reloaded.submit(60, 12), "Higher score not detected");
        check(reloaded.getBestTimeDisplay().equals("02:10"), "Shorter run erased the time record");
        int writes = flushes[0];
        reloaded.submit(-10, Double.NaN);
        reloaded.submit(0, Double.POSITIVE_INFINITY);
        reloaded.submit(0, -100);
        check(flushes[0] == writes, "Invalid results altered records");
        reloaded.submit(0, 3600);
        check(new RunRecords(preferences).getBestTimeDisplay().equals("60:00"), "Long records lose minutes");
        System.out.println("Run record checks passed: first launch, flush, reload, independent records, invalid values and long runs.");
    }
}
