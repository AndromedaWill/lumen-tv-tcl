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
    private final LiquidGlassDrawable band;
    private final RectF rect = new RectF();
    private final Rect bounds = new Rect();
    private final android.graphics.Path glassClip = new android.graphics.Path();
    private final int[] windowPosition = new int[2];

    DockDecoration(int dockSize, float density, boolean libraryBelowFold) {
        this.dockSize = dockSize;
        this.density = density;
        this.libraryBelowFold = libraryBelowFold;
        // A quiet surface behind the icons, without luminous outlines or simulated thickness.
        band = new ReflectiveSurfaceDrawable(28f, 0x48101620);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && pos < dockSize) {
            outRect.top = (int) (6 * density);
            // A separate reveal gap, excluded from dock placement: the library must not leave clipped
            // fragments at the bottom of the resting Home screen. Down still scrolls to real rows.
            outRect.bottom = (int) ((12 + (libraryBelowFold ? 28 : 0)) * density);
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
        float pad = 24 * density;
        rect.inset(-pad, -pad);
        View root = parent.getRootView().findViewById(R.id.root);
        if (root != null && root.getBackground() instanceof ScenicBackdropDrawable) {
            glassClip.reset();
            glassClip.addRoundRect(rect, 28 * density, 28 * density, android.graphics.Path.Direction.CW);
            int save = c.save();
            c.clipPath(glassClip);
            parent.getLocationInWindow(windowPosition);
            c.translate(-windowPosition[0], -windowPosition[1]);
            ((ScenicBackdropDrawable) root.getBackground()).drawFrost(c);
            c.restoreToCount(save);
        }
        // Bounds at the origin, moved with the canvas: the band slides with every scroll frame, and
        // onBoundsChange rebuilds the gradients, so only a size change may reset them.
        bounds.set(0, 0, Math.round(rect.width()), Math.round(rect.height()));
        if (!bounds.equals(band.getBounds())) band.setBounds(bounds);
        c.save();
        c.translate(rect.left, rect.top);
        band.draw(c);
        c.restore();
    }
}
