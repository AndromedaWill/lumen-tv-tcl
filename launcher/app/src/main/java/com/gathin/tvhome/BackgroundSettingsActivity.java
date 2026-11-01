package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Local background selection and bounded SAF imports. No storage/account permission. */
public final class BackgroundSettingsActivity extends Activity {
    private int pendingPhase;
    private boolean importing;
    private TextView status;
    private static final int PICK=71;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(state!=null)pendingPhase=state.getInt("phase");
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xFF101722);
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);int pad=dp(34);rows.setPadding(pad,dp(18),pad,dp(24));scroll.addView(rows);setContentView(scroll);
        TextView title=new TextView(this);title.setText(R.string.personal_background_title);title.setTextSize(28);title.setTextColor(-1);rows.addView(title);
        status=new TextView(this);status.setTextColor(0xFFBBC8D8);status.setTextSize(14);status.setPadding(0,dp(8),0,dp(12));rows.addView(status);
        add(rows,"Por horario · Alba → noche",()->choose(PersonalBackground.SCHEDULE,-1));
        add(rows,"Imagen fija · Elegir variante",()->new AlertDialog.Builder(this).setTitle("Imagen fija").setItems(PersonalBackground.PHASES,(d,which)->choose(PersonalBackground.FIXED,which)).show());
        add(rows,"Importar imagen del televisor o USB…",()->new AlertDialog.Builder(this).setTitle("¿Qué variante quieres reemplazar?").setItems(PersonalBackground.PHASES,(d,which)->{
            pendingPhase=which;Intent pick=ImagePickerIntent.resolve(this);
            if(pick==null){status.setText(R.string.file_picker_unavailable);return;}
            try{startActivityForResult(pick,PICK);}catch(ActivityNotFoundException|SecurityException e){status.setText(R.string.file_picker_unavailable);}

        }).show());
        add(rows,"Fondos aéreos y del tema · Conservar catálogo",()->choose(PersonalBackground.LEGACY,-1));
        add(rows,"Transiciones · Activar/desactivar",()->{
            boolean off=PersonalBackground.prefs(this).getBoolean("no_transition",false);
            PersonalBackground.prefs(this).edit().putBoolean("no_transition",!off).apply();setResult(RESULT_OK);refresh();
        });
        add(rows,"Restablecer colección original",()->new AlertDialog.Builder(this).setMessage("Se quitarán las copias importadas del launcher. Los archivos originales no se modifican.").setPositiveButton("Restablecer",(d,w)->{
            for(int i=0;i<6;i++)PersonalBackground.imported(this,i).delete();setResult(RESULT_OK);refresh();
        }).setNegativeButton(android.R.string.cancel,null).show());
        refresh();rows.getChildAt(2).requestFocus();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void add(LinearLayout rows,String label,Runnable click){
        TextView v=new TextView(this);v.setText(label);v.setTextColor(-1);v.setTextSize(17);v.setGravity(android.view.Gravity.CENTER_VERTICAL);
        v.setPadding(dp(16),0,dp(16),0);v.setBackgroundResource(R.drawable.settings_choice_bg);v.setFocusable(true);v.setOnClickListener(view->{if(!importing)click.run();});
        rows.addView(v,new LinearLayout.LayoutParams(-1,dp(46)));
    }
    private void refresh(){status.setText("05:00 Alba · 07:00 Amanecer · 09:00 Día · 16:00 Atardecer · 18:30 Anochecer · 20:30 Noche\n"+
            (PersonalBackground.prefs(this).getBoolean("no_transition",false)?"Transiciones desactivadas":"Transiciones suaves activadas"));}
    private void choose(String mode,int phase){PersonalBackground.prefs(this).edit().putString("mode",mode).putInt("phase",phase).apply();setResult(RESULT_OK);finish();}
    @Override protected void onSaveInstanceState(Bundle b){b.putInt("phase",pendingPhase);super.onSaveInstanceState(b);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=PICK||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();int slot=pendingPhase;Context app=getApplicationContext();WeakReference<BackgroundSettingsActivity> owner=new WeakReference<>(this);
        importing=true;status.setText("Importando y comprobando imagen…");
        new Thread(()->importImage(app,uri,slot,owner),"home-image-import").start();
    }
    private static void importImage(Context app,Uri uri,int slot,WeakReference<BackgroundSettingsActivity> owner){
        File temp=null;boolean success=false;
        try{
            temp=File.createTempFile("wallpaper-",".tmp",app.getCacheDir());
            try(InputStream input=app.getContentResolver().openInputStream(uri);java.io.OutputStream out=Files.newOutputStream(temp.toPath())){
                if(input==null)throw new java.io.IOException("No input");byte[] buffer=new byte[32768];long total=0;int n;
                while((n=input.read(buffer))!=-1){total+=n;if(total>24*1024*1024)throw new java.io.IOException("Too large");out.write(buffer,0,n);}
            }
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(temp.getPath(),bounds);
            if(bounds.outWidth<640||bounds.outHeight<360||bounds.outWidth>8192||bounds.outHeight>8192)throw new java.io.IOException("Dimensions");
            float aspect=(float)bounds.outWidth/bounds.outHeight;
            if(aspect<1.5f||aspect>2.1f||!("image/jpeg".equals(bounds.outMimeType)||"image/png".equals(bounds.outMimeType)||"image/webp".equals(bounds.outMimeType)))throw new java.io.IOException("Format/aspect");
            BitmapFactory.Options check=new BitmapFactory.Options();check.inSampleSize=1;
            while(bounds.outWidth/check.inSampleSize>64||bounds.outHeight/check.inSampleSize>64)check.inSampleSize*=2;
            android.graphics.Bitmap preview=BitmapFactory.decodeFile(temp.getPath(),check);
            if(preview==null)throw new java.io.IOException("Corrupt pixels");preview.recycle();
            Files.move(temp.toPath(),PersonalBackground.imported(app,slot).toPath(),StandardCopyOption.REPLACE_EXISTING);success=true;
        }catch(Exception|OutOfMemoryError ignored){/* Keep the previous valid image and mode. */}
        finally{if(temp!=null)temp.delete();}
        boolean imported=success;
        new Handler(Looper.getMainLooper()).post(()->{
            BackgroundSettingsActivity activity=owner.get();if(activity==null||activity.isDestroyed()||activity.isFinishing())return;
            activity.importing=false;
            if(imported){activity.setResult(RESULT_OK);activity.choose(PersonalBackground.FIXED,slot);}
            else{activity.refresh();Toast.makeText(activity,R.string.image_import_failed,Toast.LENGTH_LONG).show();}
        });
    }
}
