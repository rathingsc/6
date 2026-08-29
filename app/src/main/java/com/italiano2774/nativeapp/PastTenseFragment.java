package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** v2.5 focused contrast practice: passato prossimo vs imperfetto and auxiliary choice. */
public class PastTenseFragment extends Fragment {
    private static class Q{String prompt,answer,a,b,c,why,grammar;Q(String p,String ans,String a,String b,String c,String why,String g){this.prompt=p;this.answer=ans;this.a=a;this.b=b;this.c=c;this.why=why;this.grammar=g;}}
    private final List<Q> qs=new ArrayList<>();private int index=0,correct=0;private long started;private ProgressStore progress;
    private TextView counter,prompt,feedback,summary;private MaterialButton b1,b2,b3,next;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        View v=i.inflate(R.layout.fragment_past_tense,c,false);progress=new ProgressStore(requireContext());counter=v.findViewById(R.id.text_past_counter);prompt=v.findViewById(R.id.text_past_prompt);feedback=v.findViewById(R.id.text_past_feedback);summary=v.findViewById(R.id.text_past_summary);b1=v.findViewById(R.id.button_past_1);b2=v.findViewById(R.id.button_past_2);b3=v.findViewById(R.id.button_past_3);next=v.findViewById(R.id.button_past_next);build();b1.setOnClickListener(x->answer(b1));b2.setOnClickListener(x->answer(b2));b3.setOnClickListener(x->answer(b3));next.setOnClickListener(x->{index++;show();});show();return v;
    }
    private void build(){
        add("Ieri ___ al supermercato.","sono andato","sono andato","andavo","ho andato","Ieri 表示一次完成的动作；andare 的近过去时通常用 essere。","passato_prossimo");
        add("Quando ero piccolo, ___ spesso al mare.","andavo","sono andato","andavo","ho andato","过去反复发生的习惯用 imperfetto。","imperfetto");
        add("Stamattina ___ un caffè al bar.","ho preso","prendevo","ho preso","sono preso","今天早上的一次完成动作，用 passato prossimo。","passato_prossimo");
        add("Da bambino ___ sempre latte a colazione.","bevevo","ho bevuto","bevevo","sono bevuto","过去长期习惯用 imperfetto。","imperfetto");
        add("L'anno scorso ___ a Roma per tre giorni.","sono stato","ero","ho stato","sono stato","有明确完成时段时用 passato prossimo；stare/essere 的此处表达用 sono stato。","passato_prossimo");
        add("Mentre studiavo, mia madre ___ la cena.","preparava","ha preparato","preparava","è preparata","两个同时进行的过去背景动作常用 imperfetto。","imperfetto");
        add("Alle otto ___ il treno.","ho preso","prendevo","ho preso","sono preso","明确时间的一次动作，用 passato prossimo。","passato_prossimo");
        add("Ogni estate noi ___ dai nonni.","andavamo","siamo andati","andavamo","abbiamo andato","ogni estate 表示过去重复习惯，用 imperfetto。","imperfetto");
        add("Ieri Maria ___ tardi.","è arrivata","ha arrivato","arrivava","è arrivata","arrivare 的近过去时用 essere，并与 Maria 配合为 arrivata。","passato_prossimo");
        add("Quando vivevo a Milano, ___ sempre la metro.","prendevo","ho preso","prendevo","sono preso","描述过去生活习惯，用 imperfetto。","imperfetto");
        add("Sabato scorso ___ una pizza.","abbiamo mangiato","mangiavamo","siamo mangiati","abbiamo mangiato","sabato scorso 的完成事件，用 passato prossimo。","passato_prossimo");
        add("Da giovane mio padre ___ molto.","lavorava","ha lavorato","lavorava","è lavorato","过去长期状态/习惯用 imperfetto。","imperfetto");
        add("Ieri sera non ___ bene.","ho dormito","dormivo","ho dormito","sono dormito","一次完成的昨晚睡眠情况用 passato prossimo。","passato_prossimo");
        add("Quando faceva caldo, noi ___ le finestre.","aprivamo","abbiamo aperto","aprivamo","siamo aperti","过去条件下反复出现的动作，用 imperfetto。","imperfetto");
        add("Due giorni fa ___ il medico.","ho chiamato","chiamavo","sono chiamato","ho chiamato","due giorni fa 指一次完成动作。","passato_prossimo");
        add("Mentre ___, è iniziato a piovere.","camminavo","ho camminato","camminavo","sono camminato","被另一事件打断的背景动作常用 imperfetto。","imperfetto");
        add("Noi ___ a casa alle dieci.","siamo tornati","abbiamo tornato","tornavamo","siamo tornati","tornare 表示移动时近过去时通常用 essere。","passato_prossimo");
        add("Lei ___ una lettera ieri.","ha scritto","scriveva","è scritta","ha scritto","scrivere 的过去分词是 scritto，用 avere。","passato_prossimo");
        add("Ogni mattina lui ___ il giornale.","leggeva","ha letto","leggeva","è letto","ogni mattina 表过去习惯。","imperfetto");
        add("Ieri voi ___ molto presto.","siete partiti","avete partito","partivate","siete partiti","partire 的近过去时用 essere。","passato_prossimo");
        add("___ mai stato a Firenze?","Sei","Hai","Sei","Eri","essere/stare 的复合时态本身使用 essere：Sei stato...?","passato_prossimo");
        add("Marco ___ comprato il pane.","ha","è","ha","aveva","comprare 的 passato prossimo 使用 avere。","passato_prossimo");
        add("Anna ___ uscita alle sette.","è","ha","era","è","uscire 表移动，复合时态使用 essere。","passato_prossimo");
        add("Noi ___ visto quel film.","abbiamo","siamo","abbiamo","eravamo","vedere 使用 avere：abbiamo visto。","passato_prossimo");
        add("Le ragazze ___ arrivate ieri.","sono","hanno","erano","sono","arrivare 用 essere，复数阴性为 arrivate。","passato_prossimo");
        add("Io ___ fame quando sono arrivato.","avevo","ho avuto","avevo","sono avuto","描述当时的状态“饿”通常用 imperfetto。","imperfetto");
        add("Il tempo ___ bello e faceva caldo.","era","è stato","era","ha stato","过去背景和天气状态用 imperfetto。","imperfetto");
        add("All'improvviso il telefono ___ .","ha squillato","squillava","ha squillato","era squillato","all'improvviso 引出突然发生的完成事件。","passato_prossimo");
        add("Mentre il telefono squillava, io ___ la doccia.","facevo","ho fatto","facevo","sono fatto","正在进行的背景动作，用 imperfetto。","imperfetto");
        add("Alla fine ___ il problema.","abbiamo risolto","risolvevamo","siamo risolti","abbiamo risolto","alla fine 表示事件得到完成结果。","passato_prossimo");
    }
    private void add(String p,String ans,String a,String b,String c,String why,String g){qs.add(new Q(p,ans,a,b,c,why,g));}
    private void show(){feedback.setVisibility(View.GONE);next.setVisibility(View.GONE);if(index>=qs.size()){b1.setVisibility(View.GONE);b2.setVisibility(View.GONE);b3.setVisibility(View.GONE);prompt.setText("专项完成 🎉");counter.setText(qs.size()+" / "+qs.size());summary.setVisibility(View.VISIBLE);summary.setText("正确 "+correct+" / "+qs.size()+" · "+Math.round(correct*100.0/qs.size())+"%\n结果已经写入语法知识地图和个性学习路径。");return;}Q q=qs.get(index);counter.setText((index+1)+" / "+qs.size()+" · 过去时专项");prompt.setText(q.prompt);List<String> opts=new ArrayList<>();opts.add(q.a);opts.add(q.b);opts.add(q.c);Collections.shuffle(opts);set(b1,opts.get(0));set(b2,opts.get(1));set(b3,opts.get(2));started=SystemClock.elapsedRealtime();}
    private void set(MaterialButton b,String s){b.setText(s);b.setVisibility(View.VISIBLE);b.setEnabled(true);}
    private void answer(MaterialButton b){Q q=qs.get(index);long ms=SystemClock.elapsedRealtime()-started;boolean ok=b.getText().toString().equals(q.answer);if(ok)correct++;progress.recordGrammarResult(q.grammar,ok,ms);progress.recordAuxiliaryResult("past_tense",ok,ms);if(!ok)progress.recordErrorCause(ErrorCause.WORD_FORM,0,"past_tense",q.answer,b.getText().toString(),q.why);feedback.setText((ok?"✅ 正确":"❌ 正确答案："+q.answer)+"\n"+q.why);feedback.setVisibility(View.VISIBLE);b1.setEnabled(false);b2.setEnabled(false);b3.setEnabled(false);next.setVisibility(View.VISIBLE);}
}
