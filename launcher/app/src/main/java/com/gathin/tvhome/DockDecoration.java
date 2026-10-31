package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Draws the tvOS Liquid Glass band behind the first (dock) row and adds a gap below it. */
public class DockDecoration extends RecyclerView.ItemDecoration {

    private final int dockSize;
    private final float density;
    private final boolean libraryBelowFold;
    private final LumenDockDrawable band;
    private final RectF rect = new RectF();
    private final Rect bounds = new Rect();
    private final android.graphics.Path glassClip = new android.graphics.Path();
    private final android.graphics.Paint cachedPaint = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
    private android.graphics.Bitmap cachedBand;
    private android.graphics.drawable.Drawable cachedSource;
    private final int[] windowPosition = new int[2];

    DockDecoration(int dockSize, float density, boolean libraryBelowFold) {
        this.dockSize = dockSize;
        this.density = density;
        this.libraryBelowFold = libraryBelowFold;
        band = new LumenDockDrawable(density);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && pos < dockSize) {
            outRect.top = (int) (6 * density);
            // A separate reveal gap, excluded from dock placement: the library must not leave clipped
            // fragments at the bottom of the resting Home screen. Down still scrolls to real rows.
            outRect.bottom = (int) ((libraryBelowFold ? 10 : 12) * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        boolean any = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || pos >= dockSize) continue;
            RecyclerView.ViewHolder vh = parent.getChildViewHolder(child);
            View card = vh instanceof AppAdapter.Holder ? ((AppAdapter.Holder) vh).card : child.findViewById(R.id.card);
            if (card == null) continue;
            float l = child.getLeft() + card.getLeft();
            float t = child.getTop() + card.getTop() + child.getTranslationY();
            float r = l + card.getWidth();
            float b = t + card.getHeight();
            if (!any) {
                rect.set(l, t, r, b);
                any = true;
            } else {
                rect.union(l, t, r, b);
            }
        }
        if (!any) return;
        // The same margin on all four sides, so the gap above an icon matches the gap beside it. A focused end
        // icon shifts inward as it grows (TvFocusMotion.edgeShift), so it stays inside this margin too.
        float pad = 20 * density;
        rect.inset(-pad, -20 * density);
        View root = parent.getRootView().findViewById(R.id.root);
        android.graphics.drawable.Drawable source = root == null ? null : root.getBackground();
        bounds.set(0, 0, Math.round(rect.width()), Math.round(rect.height()));
        if (cachedBand == null || cachedBand.getWidth() != bounds.width()
                || cachedBand.getHeight() != bounds.height() || cachedSource != source) {
            // Rasterize once per layout/background, rather than stencil, sample and shade on every
            // animation frame. The translucent material travels with the dock while scrolling.
            cachedBand = android.graphics.Bitmap.createBitmap(bounds.width(), bounds.height(),
                    android.graphics.Bitmap.Config.ARGB_8888);
            Canvas layer = new Canvas(cachedBand);
            glassClip.reset();
            glassClip.addRoundRect(0, 0, bounds.width(), bounds.height(), 37 * density, 37 * density,
                    android.graphics.Path.Direction.CW);
            layer.clipPath(glassClip);
            band.setBounds(bounds);
            band.draw(layer);
            cachedSource = source;
        }
        c.drawBitmap(cachedBand, rect.left, rect.top, cachedPaint);
    }
}
