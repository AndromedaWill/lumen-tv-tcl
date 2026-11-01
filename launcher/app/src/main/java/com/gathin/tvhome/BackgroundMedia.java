package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.widget.ImageView;
import java.io.*;
import java.nio.file.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;

/** Bounded private imports and sampled previews. File/codec work stays outside the UI thread. */
final class BackgroundMedia implements AutoCloseable {
    private static final java.util.concurrent.atomic.AtomicInteger imports=new java.util.concurrent.atomic.AtomicInteger();
    static int pendingImports(){return imports.get();}
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(0,1,5,TimeUnit.SECONDS,new LinkedBlockingQueue<>(),r->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},"home-media-io"));
    private final android.util.LruCache<String,Bitmap> cache=new android.util.LruCache<String,Bitmap>(4*1024*1024){@Override protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
    private final List<Future<?>> jobs=new ArrayList<>();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    private static final class Ticket {volatile boolean cancelled;}
    interface Imported {void complete(BackgroundStore.Item item,String error);}
    void discardUnused(Context context,List<BackgroundStore.Item> items){Context app=context.getApplicationContext();worker.execute(()->BackgroundStore.discardUnused(app,items));}
    void preview(Context context,BackgroundStore.Item item,ImageView view,int width,int height){
        if(closed)return;Object old=view.getTag();if(old instanceof Ticket)((Ticket)old).cancelled=true;
        Ticket ticket=new Ticket();view.setTag(ticket);String key=item.source+":"+width+"x"+height;
        Bitmap cached=cache.get(key);if(cached!=null){view.setImageBitmap(cached);return;}view.setImageDrawable(null);
        Context app=context.getApplicationContext();WeakReference<ImageView> target=new WeakReference<>(view);
        jobs.removeIf(Future::isDone);jobs.add(worker.submit(()->{
            if(closed||ticket.cancelled||Thread.currentThread().isInterrupted())return;Bitmap bitmap=null;
            try{
                if(item.video){File f=BackgroundStore.file(app,item);if(f!=null){MediaMetadataRetriever r=new MediaMetadataRetriever();try{
                    r.setDataSource(f.getPath());bitmap=Build.VERSION.SDK_INT>=27?r.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,width,height):r.getFrameAtTime(0);
                }finally{r.release();}}}
                else{
                    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
                    try(InputStream in=BackgroundStore.open(app,item)){BitmapFactory.decodeStream(in,null,o);}
                    o.inSampleSize=1;while(o.outWidth/(o.inSampleSize*2)>=width&&o.outHeight/(o.inSampleSize*2)>=height)o.inSampleSize*=2;
                    o.inJustDecodeBounds=false;o.inScaled=false;try(InputStream in=BackgroundStore.open(app,item)){bitmap=BitmapFactory.decodeStream(in,null,o);}
                }
                if(bitmap!=null&&(bitmap.getWidth()>width||bitmap.getHeight()>height)){
                    float scale=Math.min(1f,Math.max(width/(float)bitmap.getWidth(),height/(float)bitmap.getHeight()));
                    Bitmap scaled=Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(bitmap.getWidth()*scale)),Math.max(1,Math.round(bitmap.getHeight()*scale)),true);
                    if(scaled!=bitmap)bitmap.recycle();bitmap=scaled;
                }
            }catch(Exception|OutOfMemoryError invalid){ }
            Bitmap ready=bitmap;main.post(()->{
                if(closed||ticket.cancelled){if(ready!=null)ready.recycle();return;}
                if(ready!=null)cache.put(key,ready);ImageView dest=target.get();if(dest!=null&&dest.getTag()==ticket)dest.setImageBitmap(ready);
            });
        }));
    }
    void importMedia(Context context,Uri uri,boolean video,String replacement,Imported callback){
        imports.incrementAndGet();Context app=context.getApplicationContext();worker.execute(()->{
            BackgroundStore.Item item=null;String error=null;File temp=null;
            try{
                File dir=BackgroundStore.directory(app);temp=File.createTempFile("import-",".tmp",dir);long max=(video?256L:24L)*1024*1024;
                try(InputStream in=app.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){
                    if(in==null)throw new IOException("No se puede leer el archivo.");byte[] buffer=new byte[32768];long total=0;int n;
                    while((n=in.read(buffer))!=-1){total+=n;if(total>max)throw new IOException(video?"Vídeo demasiado grande: máximo 256 MB.":"Imagen demasiado grande: máximo 24 MB.");if(dir.getUsableSpace()<32L*1024*1024)throw new IOException("No hay suficiente espacio libre.");out.write(buffer,0,n);}
                }
                if(video){MediaMetadataRetriever r=new MediaMetadataRetriever();try{
                    r.setDataSource(temp.getPath());int w=Integer.parseInt(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)),h=Integer.parseInt(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    if(w<320||h<180||w>4096||h>2160||Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))<=0)throw new IOException("Vídeo no compatible.");
                    Bitmap test=Build.VERSION.SDK_INT>=27?r.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,160,90):r.getFrameAtTime(0);
                    if(test==null)throw new IOException("No se puede decodificar el vídeo.");test.recycle();
                }finally{r.release();}}
                else{
                    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(temp.getPath(),o);
                    if(o.outWidth<640||o.outHeight<360||o.outWidth>8192||o.outHeight>8192||o.outWidth/(float)o.outHeight<1.5f||o.outWidth/(float)o.outHeight>2.1f
                            ||!("image/jpeg".equals(o.outMimeType)||"image/png".equals(o.outMimeType)||"image/webp".equals(o.outMimeType)))throw new IOException("Usa JPEG, PNG o WebP horizontal, de 640×360 a 8192×8192.");
                    o.inSampleSize=1;while(o.outWidth/o.inSampleSize>160||o.outHeight/o.inSampleSize>90)o.inSampleSize*=2;o.inJustDecodeBounds=false;
                    Bitmap test=BitmapFactory.decodeFile(temp.getPath(),o);if(test==null)throw new IOException("La imagen está dañada.");test.recycle();
                }
                String id=UUID.randomUUID().toString(),name=video?"Vídeo importado":"Imagen importada";
                try(android.database.Cursor c=app.getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){
                    if(c!=null&&c.moveToFirst())name=Cards.cleanLabel(c.getString(0),name);
                }catch(RuntimeException noDisplayName){ }
                File dest=new File(dir,id+(video?".video":".image"));Files.move(temp.toPath(),dest.toPath(),StandardCopyOption.REPLACE_EXISTING);
                item=new BackgroundStore.Item(id,name,"file:"+dest.getName(),video,!video,540);
                BackgroundStore.acceptImport(app,item,replacement);
            }catch(Exception|OutOfMemoryError bad){error=bad instanceof IOException?bad.getMessage():"No se pudo importar. Se conserva la colección anterior.";}
            finally{if(temp!=null)temp.delete();}
            BackgroundStore.Item result=error==null?item:null;String message=error;main.post(()->{imports.decrementAndGet();callback.complete(result,message);});
        });
    }
    @Override public void close(){closed=true;for(Future<?> job:jobs)job.cancel(true);jobs.clear();worker.shutdown();cache.evictAll();}
}
