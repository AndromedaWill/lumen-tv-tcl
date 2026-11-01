package com.gathin.tvhome;

/** Preserve UI detail up to the original photo's native resolution; never invent pixels. */
final class BackdropSize {
    final int width, height, sample;
    private BackdropSize(int width, int height, int sample) {
        this.width=width; this.height=height; this.sample=sample;
    }
    static BackdropSize forViewport(int uiWidth, int uiHeight, int sourceWidth, int sourceHeight) {
        if (uiWidth<=0 || uiHeight<=0 || sourceWidth<=0 || sourceHeight<=0)
            throw new IllegalArgumentException("Invalid background dimensions");
        double scale=Math.min(1d, Math.min((double)sourceWidth/uiWidth, (double)sourceHeight/uiHeight));
        int width=Math.max(1,(int)Math.round(uiWidth*scale));
        int height=Math.max(1,(int)Math.round(uiHeight*scale));
        int sample=1;
        while (sourceWidth/(sample*2)>=width && sourceHeight/(sample*2)>=height) sample*=2;
        return new BackdropSize(width,height,sample);
    }
}
