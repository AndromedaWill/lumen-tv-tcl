package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

/** Shared entry to the existing aerial downloader. Virtualized rows; no media decoded by this menu. */
final class AerialStoreDialog extends Dialog {
    private final Activity activity;
    private final Consumer<AerialDownloads.Aerial> choose;
    private final List<AerialDownloads.Aerial> entries=new ArrayList<>();
    private RecyclerView list;
    private TextView status;
    private android.app.AlertDialog confirmation;
    private final Rows adapter=new Rows();
    AerialStoreDialog(Activity activity,Consumer<AerialDownloads.Aerial> choose){super(activity);this.activity=activity;this.choose=choose;}
    private int dp(int value){return TvDesign.dp(activity,value);}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(14),dp(18),dp(14));
        body.setBackground(TvDesign.overlayPanel(activity,body));
        TextView title=new TextView(activity);TvDesign.typography(title,TvDesign.Type.SECTION);title.setText(R.string.aerial_store_title);body.addView(title);
        status=new TextView(activity);TvDesign.typography(status,TvDesign.Type.META);status.setTextColor(TvDesign.color(activity,R.color.ds_text_secondary));status.setPadding(0,dp(8),0,dp(8));body.addView(status);
        list=new RecyclerView(activity);list.setLayoutManager(new LinearLayoutManager(activity));list.setItemAnimator(null);list.setAdapter(adapter);
        body.addView(list,new LinearLayout.LayoutParams(-1,dp(260)));setContentView(body);
    }
    @Override public void show(){
        super.show();Window window=getWindow();
        if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(dp(410),-2);window.setDimAmount(TvDesign.dim(activity));}
        load();
    }
    private void load(){
        status.setText(R.string.aerials_loading);status.setFocusable(true);status.setOnClickListener(null);status.requestFocus();
        WeakReference<AerialStoreDialog> owner=new WeakReference<>(this);
        AerialDownloads.loadCatalog(activity.getApplicationContext(),values->{
            AerialStoreDialog dialog=owner.get();if(dialog==null||!dialog.isShowing())return;
            if(values==null||values.isEmpty()){
                dialog.status.setText(R.string.aerials_offline);dialog.status.setOnClickListener(v->dialog.load());return;
            }
            dialog.entries.clear();dialog.entries.addAll(values);dialog.adapter.notifyDataSetChanged();
            dialog.status.setText(R.string.aerial_store_hint);dialog.status.setFocusable(false);
            dialog.list.post(()->{RecyclerView.ViewHolder first=dialog.list.findViewHolderForAdapterPosition(0);if(first!=null)first.itemView.requestFocus();});
        });
    }
    private void remove(AerialDownloads.Aerial aerial){
        if(!AerialDownloads.onTv(activity,aerial))return;
        BackgroundStore store=BackgroundStore.load(activity);
        boolean used=false;
        for(BackgroundStore.Item item:store.items)if(item.source.equals("aerial:"+aerial.file))used=true;
        if(BackgroundStore.LEGACY.equals(store.mode)){
            String legacy=AerialBackdrop.chosen(activity,activity.getString(R.string.aerial_backdrop));
            used|=aerial.file.equals(legacy)||AerialBackdrop.CYCLE.equals(legacy);
        }
        if(used){status.setText(R.string.aerial_store_in_use);return;}
        confirmation=new android.app.AlertDialog.Builder(activity).setTitle(aerial.title).setMessage(R.string.aerial_store_delete)
                .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.aerial_store_remove,(dialog,which)->{
                    WeakReference<AerialStoreDialog> owner=new WeakReference<>(this);
                    AerialDownloads.removeAsync(activity.getApplicationContext(),aerial,()->{
                        AerialStoreDialog menu=owner.get();if(menu==null||!menu.isShowing())return;
                        menu.status.setText(menu.activity.getString(R.string.aerials_removed,aerial.title));
                        int index=menu.entries.indexOf(aerial);if(index>=0)menu.adapter.notifyItemChanged(index);
                    });
                }).create();
        confirmation.setOnDismissListener(dialog->confirmation=null);confirmation.show();
    }
    @Override public void dismiss(){if(confirmation!=null)confirmation.dismiss();super.dismiss();}
    private final class Rows extends RecyclerView.Adapter<Row> {
        @Override public int getItemCount(){return entries.size();}
        @Override public Row onCreateViewHolder(@NonNull ViewGroup parent,int type){
            TextView text=new TextView(activity);TvDesign.typography(text,TvDesign.Type.BODY);text.setGravity(Gravity.CENTER_VERTICAL);text.setPadding(dp(14),dp(6),dp(14),dp(6));
            text.setSingleLine(true);text.setEllipsize(android.text.TextUtils.TruncateAt.END);text.setMarqueeRepeatLimit(-1);text.setFocusable(true);text.setClickable(true);
            RecyclerView.LayoutParams params=new RecyclerView.LayoutParams(-1,dp(48));params.bottomMargin=dp(6);text.setLayoutParams(params);
            TvDesign.control(activity,text,TvDesign.radius(activity,R.dimen.ds_radius_control));
            text.setOnFocusChangeListener((v,on)->{text.setSelected(on);text.setEllipsize(on?android.text.TextUtils.TruncateAt.MARQUEE:android.text.TextUtils.TruncateAt.END);});
            return new Row(text);
        }
        @Override public void onBindViewHolder(@NonNull Row holder,int position){
            AerialDownloads.Aerial aerial=entries.get(position);TextView text=(TextView)holder.itemView;
            String state=aerial.file.equals(AerialDownloads.downloading)?activity.getString(R.string.aerials_percent,AerialDownloads.downloadPercent)
                    :activity.getString(AerialDownloads.onTv(activity,aerial)?R.string.aerials_on_tv:R.string.aerials_get);
            text.setText(aerial.title+"  ·  "+state);text.setContentDescription(aerial.title+". "+state);
            text.setOnLongClickListener(v->{remove(aerial);return true;});
            text.setOnClickListener(v->{
                if(AerialDownloads.onTv(activity,aerial)){if(choose!=null){dismiss();choose.accept(aerial);}return;}
                WeakReference<AerialStoreDialog> owner=new WeakReference<>(AerialStoreDialog.this);
                AerialDownloads.download(activity.getApplicationContext(),aerial,(percent,message)->{
                    AerialStoreDialog dialog=owner.get();if(dialog==null||!dialog.isShowing())return;
                    if(percent<0)dialog.status.setText(message);
                    else if(percent>100)dialog.status.setText(dialog.activity.getString(R.string.aerials_done,aerial.title));
                    int index=dialog.entries.indexOf(aerial);if(index>=0)dialog.adapter.notifyItemChanged(index);
                });
            });
        }
    }
    private static final class Row extends RecyclerView.ViewHolder { Row(View view){super(view);} }
}
