// GlassUI.java
// Glassmorphism ("frosted glass") panel style for in-game UI.
// Put this file in your mod's UI package and change the package line to match.
//
// HOW IT WORKS
// Real blur needs a shader. This uses the cheap, fast version that works
// everywhere: translucent fill + light gradient + soft border + top highlight
// + drop shadow + rounded corners.
//
// HOW TO CONNECT IT
// GlassUI draws through the small Painter interface below. In your mod,
// implement Painter by calling the mod's own draw functions (for BBS, its
// batcher: a filled box and a vertical gradient box). Example:
//
//   Painter p = new Painter() {
//       public void rect(int x1, int y1, int x2, int y2, int argb) {
//           context.batcher.box(x1, y1, x2, y2, argb);
//       }
//   };
//   GlassUI.panel(p, x, y, w, h, 6, hovered);
//
// (Method names can differ between versions. Check the batcher class in the repo.)

public final class GlassUI
{
    /** Minimal drawing interface. Colors are ARGB ints (0xAARRGGBB). x2/y2 are exclusive. */
    public interface Painter
    {
        void rect(int x1, int y1, int x2, int y2, int argb);
    }

    /* ---- Style (tweak these to change the look) ---- */
    public static int SHADOW      = 0x33000000; // soft dark shadow
    public static int FILL_TOP    = 0x4DFFFFFF; // ~30% white at the top
    public static int FILL_BOTTOM = 0x1AFFFFFF; // ~10% white at the bottom
    public static int BORDER      = 0x66FFFFFF; // thin light border
    public static int HIGHLIGHT   = 0x99FFFFFF; // bright line along the top edge
    public static int HOVER_GLOW  = 0x22FFFFFF; // extra brightness when hovered
    public static int SHADOW_SIZE = 3;

    private GlassUI() {}

    /** Draws a full glass panel. radius = corner size in pixels (0 = square). */
    public static void panel(Painter p, int x, int y, int w, int h, int radius, boolean hovered)
    {
        radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));

        /* Shadow: a few offset, fading layers below and to the right */
        for (int i = SHADOW_SIZE; i >= 1; i--)
        {
            int a = ((SHADOW >>> 24) / (i + 1)) << 24 | (SHADOW & 0xFFFFFF);

            shape(p, x + 1, y + 2, w + i - 1, h + i - 1, radius + 1, a, a, 0, 0);
        }

        /* Body + border */
        int top = hovered ? add(FILL_TOP, HOVER_GLOW) : FILL_TOP;
        int bottom = hovered ? add(FILL_BOTTOM, HOVER_GLOW) : FILL_BOTTOM;

        shape(p, x, y, w, h, radius, top, bottom, BORDER, HIGHLIGHT);
    }

    /** A small glass button-style panel (smaller radius, brighter when hovered). */
    public static void button(Painter p, int x, int y, int w, int h, boolean hovered)
    {
        panel(p, x, y, w, h, 4, hovered);
    }

    /* ---- Internals ---- */

    /** Draws a rounded rectangle row by row (vertical gradient fill, 1px border, top highlight). */
    private static void shape(Painter p, int x, int y, int w, int h, int r, int topColor, int bottomColor, int border, int highlight)
    {
        for (int row = 0; row < h; row++)
        {
            int inset = cornerInset(row, h, r);
            int left = x + inset;
            int right = x + w - inset;
            int yy = y + row;
            float t = h <= 1 ? 0 : row / (float) (h - 1);
            int fill = lerp(topColor, bottomColor, t);

            boolean edgeRow = row == 0 || row == h - 1;

            if (border != 0 && edgeRow)
            {
                /* Top row gets the highlight, bottom row gets the plain border */
                p.rect(left, yy, right, yy + 1, row == 0 && highlight != 0 ? highlight : border);
            }
            else if (border != 0)
            {
                p.rect(left, yy, left + 1, yy + 1, border);
                p.rect(right - 1, yy, right, yy + 1, border);
                p.rect(left + 1, yy, right - 1, yy + 1, fill);
            }
            else
            {
                p.rect(left, yy, right, yy + 1, fill);
            }
        }
    }

    /** How many pixels to cut from each side of this row to make a round corner. */
    private static int cornerInset(int row, int h, int r)
    {
        if (r <= 0) return 0;

        int d = row < r ? r - row : (row >= h - r ? row - (h - r - 1) : 0);

        if (d <= 0) return 0;

        double dy = d - 0.5;

        return (int) Math.round(r - Math.sqrt(Math.max(0, r * (double) r - dy * dy)));
    }

    private static int lerp(int a, int b, float t)
    {
        int aa = (int) (((a >>> 24) & 0xFF) + (((b >>> 24) & 0xFF) - ((a >>> 24) & 0xFF)) * t);
        int rr = (int) (((a >>> 16) & 0xFF) + (((b >>> 16) & 0xFF) - ((a >>> 16) & 0xFF)) * t);
        int gg = (int) (((a >>> 8) & 0xFF) + (((b >>> 8) & 0xFF) - ((a >>> 8) & 0xFF)) * t);
        int bb = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);

        return aa << 24 | rr << 16 | gg << 8 | bb;
    }

    /** Adds the alpha of glow onto color (keeps the color's RGB). */
    private static int add(int color, int glow)
    {
        int a = Math.min(255, (color >>> 24) + (glow >>> 24));

        return a << 24 | (color & 0xFFFFFF);
    }
}
