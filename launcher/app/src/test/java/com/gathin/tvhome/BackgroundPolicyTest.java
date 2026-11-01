package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class BackgroundPolicyTest {
    @Test public void threeArbitraryImagesCoverMidnight(){List<Integer> t=Arrays.asList(420,840,1200);assertEquals(2,BackgroundPolicy.scheduledIndex(t,0));assertEquals(0,BackgroundPolicy.scheduledIndex(t,420));assertEquals(1,BackgroundPolicy.scheduledIndex(t,1199));assertEquals(2,BackgroundPolicy.scheduledIndex(t,1200));}
    @Test public void reorderedCollectionKeepsClockAssociation(){assertEquals(2,BackgroundPolicy.scheduledIndex(Arrays.asList(1200,420,840),900));}
    @Test(expected=IllegalArgumentException.class)public void duplicateTimesRejected(){BackgroundPolicy.validateSchedule(Arrays.asList(60,60));}
    @Test(expected=IllegalArgumentException.class)public void oneImageIsNotSchedule(){BackgroundPolicy.validateSchedule(Collections.singletonList(300));}
    @Test public void twelveImagesSupported(){List<Integer> t=new ArrayList<>();for(int i=0;i<12;i++)t.add(i*120);assertEquals(11,BackgroundPolicy.scheduledIndex(t,1439));}
    @Test public void randomNeverImmediatelyRepeats(){Random r=new Random(1);for(int n=2;n<13;n++)for(int i=0;i<100;i++)assertNotEquals(i%n,BackgroundPolicy.nextIndex(i%n,n,true,r));}
    @Test public void sequentialOrderWraps(){assertEquals(0,BackgroundPolicy.nextIndex(2,3,false,new Random()));assertEquals(1,BackgroundPolicy.nextIndex(0,3,false,new Random()));}
    @Test public void firstRandomChoiceCanUseEveryImage(){Set<Integer> all=new HashSet<>();Random r=new Random(2);for(int i=0;i<100;i++)all.add(BackgroundPolicy.nextIndex(-1,3,true,r));assertEquals(3,all.size());}
}
