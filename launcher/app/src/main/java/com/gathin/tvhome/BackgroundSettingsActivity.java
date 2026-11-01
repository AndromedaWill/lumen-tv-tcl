package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Remote-first gallery: preview, three modes and a collection edited without rebuilding the APK. */
public final class BackgroundSettingsActivity extends Activity {
    private BackgroundStore store;
    private final BackgroundMedia media=new BackgroundMedia();
    private FrameLayout root;
    private LinearLayout options,thumbs;
    private ImageView preview;
    private TextView caption,status;
    private String selected,pendingReplace,editingMode;
    private boolean pendingVideo;
    private int imports;
    private boolean ownWrite;
    private Dialog activeDialog;
    private static final int PICK=71;
    private final SharedPreferences.OnSharedPreferenceChangeListener collectionChanged=(p,key)->{
        if("collection".equals(key)&&!ownWrite&&imports==0&&root!=null){store=BackgroundStore.load(this);renderOptions();renderCollection();showPreview();}
    };
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);store=BackgroundStore.load(this);selected=saved==null?store.fixed:saved.getString("selected",store.fixed);
        editingMode=saved==null?store.mode:saved.getString("editing",store.mode);
        if(BackgroundStore.LEGACY.equals(editingMode))editingMode=BackgroundStore.FIXED;
        if(saved!=null){pendingReplace=saved.getString("replace");pendingVideo=saved.getBoolean("video");}
        root=new FrameLayout(this);root.setId(R.id.root);root.setBackground(TvDesign.screen(this));setContentView(root);
        TextView title=text("Fondos",28);TvDesign.typography(title,TvDesign.Type.TITLE);place(title,36,20,500,42);
        TextView sub=text("Tu colección · imágenes y vídeos locales",13);sub.setTextColor(0xFFBAC6D8);place(sub,38,66,470,24);
        preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);round(preview,20);place(preview,36,104,456,257);
        preview.setFocusable(true);preview.setClickable(true);preview.setContentDescription("Vista previa del fondo. OK para editar");preview.setOnClickListener(v->edit());
        preview.setOnFocusChangeListener((v,focused)->focusFrame(v,focused,20));
        caption=text("",18);caption.setSingleLine(true);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);place(caption,38,367,450,28);
        options=new LinearLayout(this);options.setOrientation(LinearLayout.VERTICAL);place(options,516,100,408,290);
        status=text("",12);status.setTextColor(0xFFBAC6D8);status.setMaxLines(2);place(status,516,380,408,34);
        HorizontalScrollView rail=new HorizontalScrollView(this);rail.setHorizontalScrollBarEnabled(false);rail.setClipToPadding(false);
        thumbs=new LinearLayout(this);thumbs.setPadding(dp(5),dp(8),dp(5),dp(8));rail.addView(thumbs);place(rail,31,420,898,100);
        renderOptions();renderCollection();showPreview();options.getChildAt(0).requestFocus();
    }
    private void place(View v,int x,int y,int w,int h){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(w),dp(h));p.leftMargin=dp(x);p.topMargin=dp(y);root.addView(v,p);}
    private TextView text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setTypeface(getResources().getFont(R.font.inter_medium));v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(false);return v;}
    private void round(View v,int radius){v.setClipToOutline(true);v.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View view,android.graphics.Outline o){o.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(radius));}});}
    private void focusFrame(View v,boolean focus,int radius){
        if(!focus){v.setForeground(null);return;}
        android.graphics.drawable.GradientDrawable rim=new android.graphics.drawable.GradientDrawable();rim.setColor(Color.TRANSPARENT);rim.setCornerRadius(dp(radius));rim.setStroke(dp(1.5f),0xFFE0ECFC);v.setForeground(rim);
    }
    private TextView button(LinearLayout parent,String value,Runnable action){
        TextView v=text(value,14);v.setId(View.generateViewId());v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);v.setMarqueeRepeatLimit(-1);v.setPadding(dp(14),0,dp(14),0);v.setFocusable(true);v.setClickable(true);
        TvDesign.typography(v,TvDesign.Type.CONTROL);TvDesign.control(this,v,TvDesign.radius(this,R.dimen.ds_radius_control));
        v.setOnFocusChangeListener((view,focus)->v.setSelected(focus));
        v.setOnClickListener(view->{if(imports==0&&BackgroundMedia.pendingImports()==0)action.run();else status.setText("Importación en curso…");});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(34));p.bottomMargin=dp(5);parent.addView(v,p);return v;
    }
    private void renderOptions(){
        options.removeAllViews();LinearLayout modes=new LinearLayout(this);options.addView(modes,new LinearLayout.LayoutParams(-1,dp(38)));
        String[] names={"Fijo","Por horario","Presentación"},values={BackgroundStore.FIXED,BackgroundStore.SCHEDULE,BackgroundStore.SLIDESHOW};
        for(int i=0;i<3;i++){String mode=values[i];TextView b=button(modes,(mode.equals(editingMode)?"● ":"")+names[i],()->{editingMode=mode;renderOptions();options.getChildAt(1).requestFocus();});LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)b.getLayoutParams();p.width=0;p.weight=1;p.rightMargin=dp(i<2?5:0);}
        button(options,BackgroundStore.FIXED.equals(editingMode)?"Usar este fondo":BackgroundStore.SCHEDULE.equals(editingMode)?"Activar horario":"Iniciar presentación",this::apply);
        if(BackgroundStore.SCHEDULE.equals(editingMode)){
            BackgroundStore.Item item=current();
            if(item.video)button(options,"Usar este vídeo en modo Fijo",()->{editingMode=BackgroundStore.FIXED;apply();renderOptions();});
            else{
                button(options,"Hora · "+time(item.minute)+(item.scheduled?"  ✓":"  · Excluido"),()->editTime(item));
                button(options,item.scheduled?"Quitar esta imagen del horario":"Incluir esta imagen en el horario",()->toggleSchedule(item));
            }
        }else if(BackgroundStore.SLIDESHOW.equals(editingMode)){
            button(options,"Intervalo · "+interval(),()->choose("Intervalo",new String[]{"15 segundos","30 segundos","1 minuto","5 minutos","15 minutos","30 minutos","1 hora","Personalizado…"},i->{
                if(i<7){store.intervalSeconds=new int[]{15,30,60,300,900,1800,3600}[i];save();renderOptions();}
                else numberDialog("Intervalo personalizado","Minutos",store.intervalSeconds/60,1440,"Segundos",store.intervalSeconds%60,59,(m,s)->{int seconds=m*60+s;if(seconds<10||seconds>86400)throw new IllegalArgumentException("Elige entre 10 segundos y 24 horas.");store.intervalSeconds=seconds;save();renderOptions();});
            }));
            button(options,store.random?"Orden · Aleatorio":"Orden · Secuencial",()->{store.random=!store.random;save();renderOptions();});
        }else{
            button(options,"Editar este fondo…",this::edit);
            button(options,"Fundido · "+(store.transitions?"Activado":"Desactivado"),()->{store.transitions=!store.transitions;save();renderOptions();});
        }
        button(options,"Añadir imágenes o vídeo…",()->choose("Añadir a tu colección",new String[]{"Importar imágenes…","Importar un vídeo…","Aéreos ya descargados"},i->{if(i<2)pick(i==1,null);else aerials();}));
        button(options,"Colección · "+store.items.size()+" fondos",()->choose("Colección",new String[]{"Editar fondo seleccionado","Restablecer las seis imágenes iniciales","Conservar fondos del tema heredado"},i->{if(i==0)edit();else if(i==1)confirm("Se quitarán las copias importadas de esta colección; los archivos originales y vídeos aéreos no se modifican.",this::reset);else{store.mode=BackgroundStore.LEGACY;save();status.setText("Fondos del tema seleccionados. Pulsa Atrás para volver a Inicio.");}}));
        status.setText(BackgroundStore.SCHEDULE.equals(editingMode)?store.images(true).size()+" imágenes en horario · entre 2 y 12 · hora local":BackgroundStore.SLIDESHOW.equals(editingMode)?store.images(false).size()+" imágenes · solo mientras Inicio es visible":current().video?"Vista previa estática · el vídeo se reproduce en Inicio":"Elige un fondo de tu colección.");
    }
    private void renderCollection(){
        thumbs.removeAllViews();for(BackgroundStore.Item item:store.items){
            FrameLayout tile=new FrameLayout(this);tile.setFocusable(true);tile.setClickable(true);tile.setContentDescription(item.label+(item.video?". Vídeo":". Imagen")+(item.scheduled?". "+time(item.minute):""));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(136),dp(80));lp.rightMargin=dp(10);thumbs.addView(tile,lp);
            ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);round(image,12);FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(-1,dp(60));tile.addView(image,ip);
            TextView name=text(item.label+(item.scheduled?" · "+time(item.minute):""),10);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(18));np.topMargin=dp(62);tile.addView(name,np);
            media.preview(this,item,image,dp(136),dp(76));
            tile.setOnFocusChangeListener((v,focused)->{focusFrame(image,focused,12);name.setTextColor(focused?Color.WHITE:0xFFB6C4D7);if(focused){boolean changed=!item.id.equals(selected);selected=item.id;showPreview();if(changed)renderOptions();}});
            tile.setOnClickListener(v->{selected=item.id;edit();});tile.setNextFocusUpId(preview.getId()==View.NO_ID?assignPreviewId():preview.getId());
        }
    }
    private int assignPreviewId(){preview.setId(View.generateViewId());return preview.getId();}
    private BackgroundStore.Item current(){BackgroundStore.Item item=store.find(selected);if(item!=null)selected=item.id;return item;}
    private void showPreview(){BackgroundStore.Item item=current();if(item==null)return;caption.setText(item.label+(item.video?"  · Vídeo":""));int width=Math.min(960,dp(456));media.preview(this,item,preview,width,Math.round(width*9f/16));}
    private void save(){ownWrite=true;try{store.save(this);}finally{ownWrite=false;}setResult(RESULT_OK);}
    private void toggleSchedule(BackgroundStore.Item x){
        if(!x.scheduled&&store.images(true).size()>=12){status.setText("El horario admite un máximo de 12 imágenes.");return;}
        x.scheduled=!x.scheduled;save();renderOptions();renderCollection();
    }
    private void apply(){
        try{if(BackgroundStore.SCHEDULE.equals(editingMode))BackgroundPolicy.validateSchedule(BackgroundStore.starts(store.images(true)));
            if(BackgroundStore.SLIDESHOW.equals(editingMode)&&store.images(false).isEmpty())throw new IllegalArgumentException("Añade al menos una imagen a la presentación.");
            store.mode=editingMode;store.fixed=current().id;save();status.setText("Aplicado · Pulsa Atrás para verlo en Inicio.");
        }catch(IllegalArgumentException invalid){status.setText(invalid.getMessage());}
    }
    private void edit(){
        if(imports>0||BackgroundMedia.pendingImports()>0){status.setText("Importación en curso…");return;}
        BackgroundStore.Item x=current();if(x==null)return;
        List<String> labels=new ArrayList<>(Arrays.asList("Usar como fondo fijo","Reemplazar por imagen…","Reemplazar por vídeo…"));
        if(!x.video){labels.add("Hora · "+time(x.minute));labels.add(x.scheduled?"Excluir del horario":"Incluir en horario");}
        labels.addAll(Arrays.asList("Mover antes","Mover después","Eliminar de la colección"));
        choose(x.label,labels.toArray(new String[0]),i->{
            if(i==0){editingMode=BackgroundStore.FIXED;apply();renderOptions();return;}
            if(i==1||i==2){pick(i==2,x.id);return;}
            if(!x.video&&i==3){editTime(x);return;}if(!x.video&&i==4){toggleSchedule(x);return;}
            int offset=x.video?3:5,index=store.items.indexOf(x);
            if(i-offset<2){int target=Math.max(0,Math.min(store.items.size()-1,index+(i-offset==0?-1:1)));Collections.swap(store.items,index,target);save();renderCollection();}
            else if(store.items.size()==1)status.setText("Conserva al menos un fondo en tu colección.");
            else confirm("¿Quitar «"+x.label+"»? Su archivo original no se modifica.",()->{store.items.remove(x);selected=store.items.get(Math.min(index,store.items.size()-1)).id;save();media.discardUnused(this,Collections.singletonList(x));renderOptions();renderCollection();showPreview();});
        });
    }
    private void editTime(BackgroundStore.Item x){
        numberDialog("Hora de inicio","Hora",x.minute/60,23,"Minutos",x.minute%60,59,(h,m)->{x.minute=h*60+m;save();renderOptions();renderCollection();});
    }
    private void numberDialog(String title,String first,int a,int maxA,String second,int b,int maxB,java.util.function.BiConsumer<Integer,Integer> apply){
        Dialog d=dialog(title);LinearLayout body=(LinearLayout)d.findViewById(android.R.id.content).getTag();int[] values={a,b},limits={maxA,maxB};
        Runnable accept=()->{try{apply.accept(values[0],values[1]);d.dismiss();preview.requestFocus();}catch(IllegalArgumentException bad){Toast.makeText(this,bad.getMessage(),Toast.LENGTH_LONG).show();}};
        LinearLayout row=new LinearLayout(this);body.addView(row);TextView[] fields=new TextView[2];
        for(int i=0;i<2;i++){int index=i;LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);column.setGravity(Gravity.CENTER);column.setFocusable(true);column.setBackgroundResource(R.drawable.settings_choice_bg);
            TextView label=text(i==0?first:second,12);label.setGravity(Gravity.CENTER);column.addView(label);fields[i]=text(String.format(Locale.ROOT,"%02d",values[i]),34);fields[i].setGravity(Gravity.CENTER);column.addView(fields[i]);TextView arrows=text("↑  ↓",12);arrows.setGravity(Gravity.CENTER);column.addView(arrows);
            column.setBackground(new FrostedControlDrawable(this,column,false,16));column.setOnFocusChangeListener((v,focus)->{column.setBackground(new FrostedControlDrawable(this,column,focus,16));int color=TvDesign.color(this,focus?R.color.ds_text_focused:R.color.ds_text_primary);for(int j=0;j<column.getChildCount();j++)((TextView)column.getChildAt(j)).setTextColor(color);});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(100),1);p.setMargins(dp(4),dp(12),dp(4),dp(12));row.addView(column,p);
            column.setContentDescription((i==0?first:second)+". Arriba y abajo para cambiar; OK para guardar");
            column.setOnKeyListener((v,key,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN){values[index]=Math.floorMod(values[index]+(key==KeyEvent.KEYCODE_DPAD_UP?1:-1),limits[index]+1);fields[index].setText(String.format(Locale.ROOT,"%02d",values[index]));return true;}
                if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){accept.run();return true;}return false;
            });
        }
        button(body,"Guardar",accept);d.show();row.getChildAt(0).requestFocus();
    }
    private String time(int minute){return String.format(Locale.getDefault(),"%02d:%02d",minute/60,minute%60);}
    private String interval(){return store.intervalSeconds<60?store.intervalSeconds+" s":store.intervalSeconds/60+" min";}
    private Dialog dialog(String title){
        Dialog d=new Dialog(this);activeDialog=d;d.setOnDismissListener(ignored->{if(activeDialog==d)activeDialog=null;});d.requestWindowFeature(Window.FEATURE_NO_TITLE);ScrollView scroll=new ScrollView(this);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(14),dp(18),dp(14));body.setBackground(TvDesign.panel(this));body.addView(text(title,18));scroll.addView(body);d.setContentView(scroll);
        d.findViewById(android.R.id.content).setTag(body);Window w=d.getWindow();if(w!=null){w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));w.setLayout(dp(340),-2);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=w.getAttributes();p.dimAmount=TvDesign.dim(this);w.setAttributes(p);}return d;
    }
    private void choose(String title,String[] values,java.util.function.IntConsumer action){Dialog d=dialog(title);LinearLayout body=(LinearLayout)d.findViewById(android.R.id.content).getTag();for(int i=0;i<values.length;i++){int at=i;button(body,values[i],()->{d.dismiss();action.accept(at);});}d.show();body.getChildAt(1).requestFocus();}
    private void confirm(String message,Runnable action){choose(message,new String[]{"Confirmar","Cancelar"},i->{if(i==0)action.run();});}
    private void pick(boolean video,String replace){
        if(replace==null&&store.items.size()>=BackgroundStore.LIMIT){status.setText("Colección completa: elimina un fondo antes de añadir otro.");return;}
        Intent intent=ImagePickerIntent.resolve(this,video);if(intent==null){status.setText(R.string.file_picker_unavailable);return;}
        pendingVideo=video;pendingReplace=replace;if(!video&&replace==null)intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        try{startActivityForResult(intent,PICK);}catch(ActivityNotFoundException|SecurityException unavailable){status.setText(R.string.file_picker_unavailable);}
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);if(req!=PICK||result!=RESULT_OK||data==null)return;
        List<Uri> uris=new ArrayList<>();if(data.getClipData()!=null){for(int i=0;i<Math.min(BackgroundStore.LIMIT,data.getClipData().getItemCount());i++)uris.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)uris.add(data.getData());
        if(pendingReplace!=null&&uris.size()>1)uris=uris.subList(0,1);if(uris.isEmpty())return;imports=uris.size();status.setText("Importando y comprobando…");
        String replace=pendingReplace;Context app=getApplicationContext();WeakReference<BackgroundSettingsActivity> owner=new WeakReference<>(this);
        for(Uri uri:uris)media.importMedia(app,uri,pendingVideo,replace,(item,error)->{
            String chosen=null;
            if(item!=null)chosen=item.id;
            BackgroundSettingsActivity a=owner.get();if(a==null||a.isDestroyed())return;a.imports=Math.max(0,a.imports-1);a.store=BackgroundStore.load(a);if(chosen!=null)a.selected=chosen;a.setResult(RESULT_OK);a.renderOptions();a.renderCollection();a.showPreview();a.status.setText(error!=null?error:a.imports==0?"Importación completada · archivos originales intactos":"Importando…");
        });
    }
    private void aerials(){if(store.items.size()>=BackgroundStore.LIMIT){status.setText("Colección completa.");return;}List<String[]> all=AerialBackdrop.catalog(this);if(all.isEmpty()){status.setText("No hay vídeos aéreos descargados. Puedes importar tu propio vídeo.");return;}
        choose("Aéreos guardados",all.stream().map(v->v[1]).toArray(String[]::new),i->{String[] a=all.get(i);BackgroundStore.Item item=new BackgroundStore.Item(UUID.randomUUID().toString(),a[1],"aerial:"+a[0],true,false,0);store.items.add(item);selected=item.id;save();renderCollection();showPreview();});}
    private void reset(){List<BackgroundStore.Item> previous=new ArrayList<>(store.items);store=BackgroundStore.defaults(this);store.mode=BackgroundStore.SCHEDULE;for(int i=0;i<store.items.size();i++)store.items.get(i).source="asset:"+(i+1);selected=store.fixed;editingMode=store.mode;save();media.discardUnused(this,previous);renderOptions();renderCollection();showPreview();}
    @Override protected void onSaveInstanceState(Bundle b){b.putString("selected",selected);b.putString("editing",editingMode);b.putString("replace",pendingReplace);b.putBoolean("video",pendingVideo);super.onSaveInstanceState(b);}
    @Override protected void onStart(){super.onStart();BackgroundStore.prefs(this).registerOnSharedPreferenceChangeListener(collectionChanged);}
    @Override protected void onStop(){BackgroundStore.prefs(this).unregisterOnSharedPreferenceChangeListener(collectionChanged);super.onStop();}
    @Override protected void onDestroy(){if(activeDialog!=null)activeDialog.dismiss();media.close();if(preview!=null)preview.setImageDrawable(null);if(thumbs!=null)thumbs.removeAllViews();super.onDestroy();}
}
