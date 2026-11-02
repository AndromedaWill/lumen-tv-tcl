package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Rect;
import android.widget.ScrollView;

/** Transparent scroll edges: Android masks only the two narrow edge strips, not the screen.
 * No backdrop, bitmap, RenderEffect or recurring worker. Focus keeps a margin beyond the mask.
 */
final class EdgeFadeScrollView extends ScrollView {
    private final Rect focusRect = new Rect();
    private final int clearance;

    EdgeFadeScrollView(Context context) {
        super(context);
        clearance = TvDesign.dp(context, 4);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(TvDesign.dp(context, 18));
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setClipChildren(true);
        setClipToPadding(true);
        // A new D-pad target must never wait for scrolling to become readable. Focus itself
        // still uses the shared motion tokens; touch/fling retains the native scroll path.
        setSmoothScrollingEnabled(false);
    }

    @Override public int getSolidColor() { return 0; } // Alpha mask, never paint a grey edge.

    @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        focusRect.set(rect);
        focusRect.inset(0, -clearance);
        return super.computeScrollDeltaToGetChildRectOnScreen(focusRect);
    }

    @Override protected void onMeasure(int width, int height) {
        int available = Math.max(TvDesign.dp(getContext(), 180),
                Math.round(getResources().getDisplayMetrics().heightPixels * 806f / 1080f)
                        - TvDesign.dp(getContext(), 96));
        super.onMeasure(width, MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST));
    }
}
