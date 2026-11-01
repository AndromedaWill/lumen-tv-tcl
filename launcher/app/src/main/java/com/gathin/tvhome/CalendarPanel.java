package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Small offline calendar: left/right changes month, Back closes. No calendar/account permission. */
final class CalendarPanel extends Dialog {
    private final Activity activity;
    private YearMonth month=YearMonth.now();
    private LinearLayout body;
    private final int previousId=android.view.View.generateViewId(),nextId=android.view.View.generateViewId();
    CalendarPanel(Activity activity){super(activity);this.activity=activity;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size){
        TextView v=new TextView(activity);v.setText(value);v.setTextSize(size);v.setTextColor(Color.WHITE);
        v.setTypeface(activity.getResources().getFont(R.font.inter_medium));
        v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);return v;
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);Window w=getWindow();if(w==null)return;
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(18),dp(22),dp(18));
        body.setBackground(TvDesign.panel(activity));setContentView(body);
        w.setLayout(dp(380),WindowManager.LayoutParams.WRAP_CONTENT);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams p=w.getAttributes();p.dimAmount=TvDesign.dim(activity);p.gravity=Gravity.CENTER;w.setAttributes(p);render();
    }
    private void render(){
        android.view.View focused=body.findFocus();int focusId=focused==null?nextId:focused.getId();
        body.removeAllViews();Locale locale=activity.getResources().getConfiguration().getLocales().get(0);
        TextView title=text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",locale)),20);TvDesign.typography(title,TvDesign.Type.SECTION);
        LinearLayout heading=new LinearLayout(activity);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView previous=arrow("‹",previousId,-1),next=arrow("›",nextId,1);
        previous.setContentDescription(activity.getString(R.string.calendar_previous));next.setContentDescription(activity.getString(R.string.calendar_next));
        previous.setNextFocusRightId(nextId);previous.setNextFocusLeftId(previousId);
        next.setNextFocusLeftId(previousId);next.setNextFocusRightId(nextId);
        heading.addView(previous,new LinearLayout.LayoutParams(dp(38),dp(42)));
        heading.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        heading.addView(next,new LinearLayout.LayoutParams(dp(38),dp(42)));
        body.addView(heading);
        (focusId==previousId?previous:next).requestFocus();
        LinearLayout days=new LinearLayout(activity);
        for(int i=1;i<=7;i++)days.addView(text(java.time.DayOfWeek.of(i).getDisplayName(TextStyle.SHORT,locale),12),new LinearLayout.LayoutParams(0,dp(32),1));
        body.addView(days);int offset=month.atDay(1).getDayOfWeek().getValue()-1;LocalDate today=LocalDate.now();
        for(int row=0;row<6;row++){
            LinearLayout line=new LinearLayout(activity);
            for(int col=0;col<7;col++){
                int day=row*7+col-offset+1;boolean valid=day>=1&&day<=month.lengthOfMonth();
                TextView cell=text(valid?Integer.toString(day):"",16);
                if(valid&&month.atDay(day).equals(today)){
                    android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
                    bg.setColor(TvDesign.color(activity,R.color.ds_selected_fill));bg.setCornerRadius(dp(14));bg.setStroke(dp(1),TvDesign.color(activity,R.color.ds_accent));cell.setBackground(bg);cell.setContentDescription("Hoy, "+day);
                }
                line.addView(cell,new LinearLayout.LayoutParams(0,dp(32),1));
            }body.addView(line);
        }
    }
    private TextView arrow(String glyph,int id,int direction){
        TextView arrow=text(glyph,32);arrow.setId(id);arrow.setFocusable(true);arrow.setClickable(true);
        TvDesign.control(activity,arrow,20);
        arrow.setOnClickListener(v->{month=month.plusMonths(direction);render();});
        return arrow;
    }
}
