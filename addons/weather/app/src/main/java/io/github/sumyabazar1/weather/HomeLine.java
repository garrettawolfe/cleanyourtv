package io.github.sumyabazar1.weather;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The weather as one line under Home's clock (the launcher's game_day panel): "City · Partly cloudy" over
 * "48°  ·  H 52°  L 39°". The launcher queries when Home comes back and when the row ends, so the row ends
 * REFRESH_MS from now and each query past that age fetches Open-Meteo once in the background, then notifies.
 * Nothing runs while Home is not on screen. Holds only what Home shows: no links, accounts or position.
 */
public final class HomeLine extends ContentProvider {
    static final String AUTHORITY = "io.github.sumyabazar1.weather.home";
    static final Uri TURNS = Uri.parse("content://" + AUTHORITY + "/turns");
    private static final String[] COLUMNS =
            {"top_line", "word", "start_ms", "end_ms", "colour", "calendar", "turn_seconds", "headline"};
    private static final long STALE_MS = 3 * 60 * 60_000L;
    private static final int COLOUR = 0xFF4FC3F7;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicBoolean fetching = new AtomicBoolean();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        Context ctx = getContext();
        MatrixCursor c = new MatrixCursor(COLUMNS);
        SharedPreferences place = ctx.getSharedPreferences("place", Context.MODE_PRIVATE);
        if (!place.contains("name")) return c;
        SharedPreferences home = ctx.getSharedPreferences("home_line", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long at = home.getLong("at", 0);
        boolean samePlace = place.getString("name", "").equals(home.getString("place", null));
        if (!samePlace || now - at >= Forecast.REFRESH_MS) fetch(ctx, place, home);
        if (samePlace && now - at < STALE_MS) {
            c.addRow(new Object[] {home.getString("top", ""), "", at, now + Forecast.REFRESH_MS, COLOUR, "weather", 30,
                    home.getString("headline", "")});
        }
        c.setNotificationUri(ctx.getContentResolver(), TURNS);
        return c;
    }

    private void fetch(Context ctx, SharedPreferences place, SharedPreferences home) {
        if (!fetching.compareAndSet(false, true)) return;
        String name = place.getString("name", "");
        double lat = Double.longBitsToDouble(place.getLong("lat", 0));
        double lon = Double.longBitsToDouble(place.getLong("lon", 0));
        boolean f = ctx.getSharedPreferences("options", Context.MODE_PRIVATE).getBoolean("fahrenheit",
                "f".equalsIgnoreCase(ctx.getString(R.string.temperature_unit).trim()));
        io.execute(() -> {
            try {
                Forecast.Report r = Forecast.fetch(lat, lon, Forecast.MIN_DAYS);
                String top = name + "  ·  " + ctx.getString(Forecast.sky(r.code, r.day));
                String headline = Forecast.degrees(r.tempC, f) + "°";
                if (!Double.isNaN(r.todayMaxC)) {
                    headline += "    H " + Forecast.degrees(r.todayMaxC, f) + "°   L " + Forecast.degrees(r.todayMinC, f) + "°";
                }
                home.edit().putString("place", name).putString("top", top).putString("headline", headline)
                        .putLong("at", System.currentTimeMillis()).apply();
                ctx.getContentResolver().notifyChange(TURNS, null);
            } catch (Exception offline) {
                // Keep the last line until it is STALE_MS old; the next query tries again.
            } finally {
                fetching.set(false);
            }
        });
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
