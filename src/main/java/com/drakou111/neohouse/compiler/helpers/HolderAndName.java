package com.drakou111.neohouse.compiler.helpers;


import com.drakou111.neohouse.compiler.enums.Holder;

public class HolderAndName {
    public final Holder holder;
    public final String name;

    public HolderAndName(Holder holder, String name) {
        this.holder = holder;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HolderAndName other)) return false;
        return this.holder == other.holder && this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return holder.hashCode() * 31 + name.hashCode();
    }
}