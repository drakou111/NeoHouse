package com.drakou111.neohouse.factories.params;

public class TitleParams {
    public String title = null;
    public String subtitle = null;
    public int fadeIn = 1;
    public int stay = 5;
    public int fadeOut = 1;
    public String note = null;

    public TitleParams title(String v)    { this.title = v; return this; }
    public TitleParams subtitle(String v) { this.subtitle = v; return this; }
    public TitleParams fadeIn(int v)      { this.fadeIn = v; return this; }
    public TitleParams stay(int v)        { this.stay = v; return this; }
    public TitleParams fadeOut(int v)     { this.fadeOut = v; return this; }
    public TitleParams note(String v)     { this.note = v; return this; }
}
