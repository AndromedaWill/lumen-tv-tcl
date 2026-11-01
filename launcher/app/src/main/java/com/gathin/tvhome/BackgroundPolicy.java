package com.gathin.tvhome;
import java.util.List;
import java.util.Random;

/** Clock and collection rules independent of Android, including wrap through midnight. */
final class BackgroundPolicy {
    static void validateSchedule(List<Integer> starts){
        if(starts.size()<2||starts.size()>12)throw new IllegalArgumentException("El horario necesita entre 2 y 12 imágenes.");
        java.util.Set<Integer> unique=new java.util.HashSet<>();
        for(int minute:starts)if(minute<0||minute>=1440||!unique.add(minute))throw new IllegalArgumentException("Cada imagen necesita una hora distinta.");
    }
    static int scheduledIndex(List<Integer> starts,int minute){
        validateSchedule(starts);
        if(minute<0||minute>=1440)throw new IllegalArgumentException("Hora inválida");
        int selected=0,best=1441;
        for(int i=0;i<starts.size();i++){int age=(minute-starts.get(i)+1440)%1440;if(age<best){best=age;selected=i;}}
        return selected;
    }
    static int nextIndex(int current,int size,boolean random,Random rng){
        if(size<1)return -1;if(size==1)return 0;
        if(!random)return (current+1+size)%size;
        if(current<0||current>=size)return rng.nextInt(size);
        int pick=rng.nextInt(size-1);return pick>=current&&current>=0?pick+1:pick;
    }
}
