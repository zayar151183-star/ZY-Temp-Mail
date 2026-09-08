package com.zayar.tempbox;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final int BG=Color.rgb(7,16,30), PANEL=Color.rgb(13,25,43), INK=Color.rgb(237,244,255), MUTED=Color.rgb(145,160,184), CYAN=Color.rgb(77,226,203), BLUE=Color.rgb(91,140,255);
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final String[] providers={"https://api.mail.tm","https://api.mail.gw"};
    private TextView email,status,meta,count; private LinearLayout inbox; private Button copy,refresh,newMail;
    private String token="",provider=""; private boolean english=false; private final Runnable poll=new Runnable(){public void run(){loadMessages();ui.postDelayed(this,10000);}};

    @Override public void onCreate(Bundle b){super.onCreate(b);english=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("english",false);buildUi();createMailbox();}
    @Override protected void onDestroy(){ui.removeCallbacks(poll);io.shutdownNow();super.onDestroy();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),Color.rgb(32,48,74));return d;}
    private TextView text(String s,int sp,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setLineSpacing(0,1.18f);return v;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private String tr(String my,String en){return english?en:my;}

    private void buildUi(){
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(22),dp(18),dp(30));scroll.addView(root);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);TextView brand=text("T   TempBox",21,INK);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));Button language=new Button(this);language.setText(english?"မြန်မာ":"English");language.setTextColor(INK);language.setTextSize(12);language.setBackground(bg(Color.rgb(16,30,49),12));bar.addView(language,lp(dp(92),dp(42)));root.addView(bar);language.setOnClickListener(v->{getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("english",!english).apply();recreate();});
        TextView tag=text("●  PRIVATE · FAST · FREE",12,CYAN);tag.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tagLp=lp(-1,-2);tagLp.topMargin=dp(48);root.addView(tag,tagLp);
        TextView title=text(tr("ယာယီ အီးမေးလ်\nချက်ချင်း ရယူပါ","Temporary Email\nReady Instantly"),38,INK);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams titleLp=lp(-1,-2);titleLp.setMargins(0,dp(10),0,dp(12));root.addView(title,titleLp);
        TextView sub=text(tr("Website များတွင် sign up လုပ်ရန် တစ်ခါသုံး inbox ကို အခမဲ့အသုံးပြုပါ။","Use a free disposable inbox for website sign-ups and keep spam away."),15,MUTED);sub.setGravity(Gravity.CENTER);root.addView(sub);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(17),dp(18),dp(17),dp(18));card.setBackground(bg(PANEL,20));LinearLayout.LayoutParams cardLp=lp(-1,-2);cardLp.topMargin=dp(32);root.addView(card,cardLp);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=text(tr("သင့်ယာယီအီးမေးလ်","Your temporary email"),14,INK);label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));status=text(tr("ဖန်တီးနေသည်…","Creating…"),12,CYAN);row.addView(status);card.addView(row);
        LinearLayout addrRow=new LinearLayout(this);addrRow.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams addrLp=lp(-1,-2);addrLp.topMargin=dp(10);card.addView(addrRow,addrLp);
        email=text(tr("လိပ်စာဖန်တီးနေသည်…","Creating address…"),16,INK);email.setGravity(Gravity.CENTER_VERTICAL);email.setPadding(dp(14),0,dp(10),0);email.setBackground(bg(Color.rgb(7,18,31),13));addrRow.addView(email,new LinearLayout.LayoutParams(0,dp(58),1));
        copy=new Button(this);copy.setText("COPY");copy.setTextColor(Color.WHITE);copy.setTextSize(11);copy.setEnabled(false);copy.setBackground(bg(BLUE,13));LinearLayout.LayoutParams copyLp=lp(dp(68),dp(58));copyLp.leftMargin=dp(9);addrRow.addView(copy,copyLp);
        newMail=new Button(this);newMail.setText(tr("＋  Email အသစ်ဖန်တီးမယ်","＋  Create new email"));newMail.setTextColor(BG);newMail.setTypeface(Typeface.DEFAULT,Typeface.BOLD);newMail.setBackground(bg(CYAN,13));LinearLayout.LayoutParams newLp=lp(-1,dp(50));newLp.topMargin=dp(14);card.addView(newMail,newLp);
        refresh=new Button(this);refresh.setText(tr("↻  Inbox Refresh","↻  Refresh inbox"));refresh.setTextColor(INK);refresh.setEnabled(false);refresh.setBackground(bg(Color.rgb(16,30,49),13));LinearLayout.LayoutParams refLp=lp(-1,dp(48));refLp.topMargin=dp(9);card.addView(refresh,refLp);
        LinearLayout inboxCard=new LinearLayout(this);inboxCard.setOrientation(LinearLayout.VERTICAL);inboxCard.setBackground(bg(PANEL,20));LinearLayout.LayoutParams icLp=lp(-1,-2);icLp.topMargin=dp(18);root.addView(inboxCard,icLp);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(18),dp(17),dp(18),dp(15));LinearLayout heads=new LinearLayout(this);heads.setOrientation(LinearLayout.VERTICAL);TextView ih=text("Inbox",21,INK);ih.setTypeface(Typeface.DEFAULT,Typeface.BOLD);heads.addView(ih);meta=text(tr("စာဝင်လာပါက ဒီနေရာမှာ ပြပါမယ်","New messages will appear here"),12,MUTED);heads.addView(meta);head.addView(heads,new LinearLayout.LayoutParams(0,-2,1));count=text("0",13,INK);count.setGravity(Gravity.CENTER);count.setBackground(bg(Color.rgb(28,48,80),9));head.addView(count,lp(dp(34),dp(34)));inboxCard.addView(head);
        inbox=new LinearLayout(this);inbox.setOrientation(LinearLayout.VERTICAL);inbox.setPadding(dp(10),dp(12),dp(10),dp(18));inboxCard.addView(inbox);showEmpty();
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Temp email",email.getText()));Toast.makeText(this,tr("ကူးယူပြီးပါပြီ","Email copied"),Toast.LENGTH_SHORT).show();});
        newMail.setOnClickListener(v->createMailbox());refresh.setOnClickListener(v->loadMessages());setContentView(scroll);
    }
    private void showEmpty(){inbox.removeAllViews();TextView e=text(tr("✉\nစာမဝင်သေးပါ\n\n၁၀ စက္ကန့်တိုင်း အလိုအလျောက် စစ်ဆေးပေးပါမယ်။","✉\nNo messages yet\n\nWe check for new mail every 10 seconds."),14,MUTED);e.setGravity(Gravity.CENTER);e.setPadding(0,dp(30),0,dp(20));inbox.addView(e,lp(-1,-2));}
    private void setBusy(boolean b,String s){status.setText(s);newMail.setEnabled(!b);refresh.setEnabled(!b&&!token.isEmpty());}
    private void createMailbox(){ui.removeCallbacks(poll);token="";copy.setEnabled(false);email.setText(tr("လိပ်စာဖန်တီးနေသည်…","Creating address…"));setBusy(true,tr("ဖန်တီးနေသည်…","Creating…"));io.execute(()->{Exception last=null;for(String base:providers){try{JSONObject domains=get(base+"/domains?page=1","GET",null,null);JSONArray a=domains.optJSONArray("hydra:member");if(a==null||a.length()==0)throw new Exception(tr("Domain မရပါ","No domain available"));String domain=a.getJSONObject(0).getString("domain"),address=random(17)+"@"+domain,password="Tb!"+random(22)+"9";JSONObject body=new JSONObject().put("address",address).put("password",password);get(base+"/accounts","POST",body.toString(),null);JSONObject auth=get(base+"/token","POST",body.toString(),null);String t=auth.getString("token");provider=base;token=t;ui.post(()->{email.setText(address);copy.setEnabled(true);setBusy(false,tr("အသုံးပြုနိုင်ပါပြီ","Ready to use"));loadMessages();ui.postDelayed(poll,10000);});return;}catch(Exception e){last=e;}}Exception err=last;ui.post(()->{email.setText(tr("Email မဖန်တီးနိုင်ပါ","Could not create email"));setBusy(false,tr("ချိတ်ဆက်မှု မအောင်မြင်ပါ","Connection failed"));Toast.makeText(this,err==null?tr("ဝန်ဆောင်မှု မရပါ","Service unavailable"):err.getMessage(),Toast.LENGTH_LONG).show();});});}
    private void loadMessages(){if(token.isEmpty())return;refresh.setEnabled(false);io.execute(()->{try{JSONObject d=get(provider+"/messages?page=1","GET",null,token);JSONArray list=d.optJSONArray("hydra:member");ui.post(()->render(list==null?new JSONArray():list));}catch(Exception e){ui.post(()->meta.setText(tr("Inbox စစ်ဆေးမရပါ","Could not check inbox")));}finally{ui.post(()->refresh.setEnabled(true));}});}
    private void render(JSONArray list){inbox.removeAllViews();count.setText(String.valueOf(list.length()));meta.setText(list.length()>0?(english?list.length()+" message(s)":list.length()+" စောင် ဝင်ထားပါသည်"):tr("စာဝင်လာပါက ဒီနေရာမှာ ပြပါမယ်","New messages will appear here"));if(list.length()==0){showEmpty();return;}for(int i=0;i<list.length();i++){JSONObject m=list.optJSONObject(i);if(m==null)continue;String id=m.optString("id"),subject=m.optString("subject","(No subject)"),from=m.optJSONObject("from")==null?"Unknown":m.optJSONObject("from").optString("address","Unknown");TextView v=text("✉   "+from+"\n      "+subject,14,INK);v.setPadding(dp(10),dp(14),dp(10),dp(14));v.setBackground(bg(Color.rgb(18,35,58),10));LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(8);inbox.addView(v,p);v.setOnClickListener(x->openMessage(id));}}
    private void openMessage(String id){io.execute(()->{try{JSONObject m=get(provider+"/messages/"+URLEncoder.encode(id,"UTF-8"),"GET",null,token);String from=m.optJSONObject("from")==null?"Unknown":m.optJSONObject("from").optString("address"),subject=m.optString("subject","(No subject)"),body=m.optString("text",tr("စာသားမပါသော email ဖြစ်ပါသည်။","This email has no text content."));ui.post(()->new AlertDialog.Builder(this).setTitle(subject).setMessage("From: "+from+"\n\n"+body).setPositiveButton(tr("ပိတ်မယ်","Close"),null).show());}catch(Exception e){ui.post(()->Toast.makeText(this,tr("စာကို ဖွင့်မရပါ","Could not open message"),Toast.LENGTH_SHORT).show());}});}
    private JSONObject get(String url,String method,String body,String bearer)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod(method);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestProperty("Accept","application/ld+json, application/json");c.setRequestProperty("Content-Type","application/json");if(bearer!=null)c.setRequestProperty("Authorization","Bearer "+bearer);if(body!=null){c.setDoOutput(true);try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();StringBuilder s=new StringBuilder();if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line);}if(code>=400)throw new Exception("Server error "+code);return new JSONObject(s.toString());}
    private String random(int n){String c="abcdefghjkmnpqrstuvwxyz23456789";StringBuilder s=new StringBuilder();Random r=new Random();for(int i=0;i<n;i++)s.append(c.charAt(r.nextInt(c.length())));return s.toString();}
}
