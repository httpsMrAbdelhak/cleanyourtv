package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

/** The picture on the dock's weather tile: the current condition and temperature, or just the icon. */
final class WeatherTileArt {
    private WeatherTileArt() { }

    /** forecast may be null (no city yet, or not loaded): the tile then shows a cloud and the word. */
    static Bitmap render(Context context, int w, int h, Forecast forecast, String label) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boolean day = forecast == null || forecast.day;
        int top = Palette.light ? (day ? 0xFFD6E8FF : 0xFFC4CCE6) : (day ? 0xFF2D5C9A : 0xFF1C2440);
        int bottom = Palette.light ? (day ? 0xFFF4F8FF : 0xFFE3E7F4) : (day ? 0xFF16335C : 0xFF0D1226);
        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        WeatherCode code = forecast == null ? WeatherCode.PARTLY : WeatherCode.of(forecast.code);
        WeatherGlyph glyph = new WeatherGlyph(code, day);
        int size = Math.round(h * 0.62f);
        int pad = Math.round(w * 0.08f);
        glyph.setBounds(pad, (h - size) / 2, pad + size, (h + size) / 2);
        glyph.draw(canvas);

        paint.setColor(Palette.light ? 0xFF14171F : 0xFFF5F6FA);
        paint.setTextAlign(Paint.Align.LEFT);
        if (forecast != null) {
            paint.setTypeface(Ui.light(context));
            paint.setTextSize(h * 0.42f);
            canvas.drawText(WeatherPanel.degrees(forecast.temperature), pad * 2 + size, h * 0.62f, paint);
        } else {
            paint.setTypeface(Ui.medium(context));
            paint.setTextSize(h * 0.2f);
            canvas.drawText(label, pad * 2 + size, h * 0.57f, paint);
        }
        return bitmap;
    }
}
