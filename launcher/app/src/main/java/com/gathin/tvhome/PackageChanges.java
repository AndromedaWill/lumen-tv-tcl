package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.ChangedPackages;

/** Public API 26+ change cursor, scoped to this activity/process; no polling or background receiver. */
final class PackageChanges {
    private int sequence;
    boolean changed(Context context){
        try{
            ChangedPackages changes=context.getPackageManager().getChangedPackages(sequence);
            if(changes==null)return false;
            sequence=changes.getSequenceNumber();
            return !changes.getPackageNames().isEmpty();
        }catch(RuntimeException unsupported){return true;}
    }
}
