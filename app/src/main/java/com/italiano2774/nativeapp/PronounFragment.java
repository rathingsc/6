package com.italiano2774.nativeapp;

import android.os.Bundle;import android.os.SystemClock;import android.view.*;import android.widget.TextView;import androidx.annotation.*;import androidx.fragment.app.Fragment;import com.google.android.material.button.MaterialButton;import java.util.*;
/** v2.5 direct/indirect object, ci and ne practice, entirely local. */
public class PronounFragment extends Fragment{
 private static class Q{String p,ans,a,b,c,why,g;Q(String p,String ans,String a,String b,String c,String why,String g){this.p=p;this.ans=ans;this.a=a;this.b=b;this.c=c;this.why=why;this.g=g;}}
 private final List<Q> qs=new ArrayList<>();private int idx=0,correct=0;private long started;private ProgressStore progress;private TextView count,prompt,feedback,summary;private MaterialButton b1,b2,b3,next;
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){View v=i.inflate(R.layout.fragment_pronouns,c,false);progress=new ProgressStore(requireContext());count=v.findViewById(R.id.text_pronoun_counter);prompt=v.findViewById(R.id.text_pronoun_prompt);feedback=v.findViewById(R.id.text_pronoun_feedback);summary=v.findViewById(R.id.text_pronoun_summary);b1=v.findViewById(R.id.button_pronoun_1);b2=v.findViewById(R.id.button_pronoun_2);b3=v.findViewById(R.id.button_pronoun_3);next=v.findViewById(R.id.button_pronoun_next);build();b1.setOnClickListener(x->answer(b1));b2.setOnClickListener(x->answer(b2));b3.setOnClickListener(x->answer(b3));next.setOnClickListener(x->{idx++;show();});show();return v;}
 private void build(){
  add("Hai visto Marco? Sì, ___ ho visto ieri.","l'","gli","l'","ne","直接宾语 Marco 用 lo；在 ho 前省音为 l'ho。","pronomi_diretti");
  add("Conosci Maria? Sì, ___ conosco bene.","la","le","la","ci","Maria 是阴性单数直接宾语，用 la。","pronomi_diretti");
  add("Compri i biglietti? Sì, ___ compro online.","li","gli","li","ne","i biglietti 是阳性复数直接宾语，用 li。","pronomi_diretti");
  add("Le chiavi? Non ___ trovo.","le","li","le","gli","le chiavi 是阴性复数直接宾语，用 le。","pronomi_diretti");
  add("Puoi aiutare me? Sì, ___ aiuto.","ti","mi","ti","lo","对 tu 的直接宾语形式是 ti。","pronomi_diretti");
  add("Il caffè? ___ prendo senza zucchero.","lo","gli","lo","ne","il caffè 用阳性单数直接宾语 lo。","pronomi_diretti");
  add("La medicina? ___ prendo dopo cena.","la","le","la","ci","la medicina 用 la。","pronomi_diretti");
  add("I documenti? ___ porto domani.","li","le","li","gli","i documenti 用 li。","pronomi_diretti");
  add("Telefono a Marco. ___ telefono stasera.","gli","lo","gli","le","telefonare a qualcuno 是间接宾语；Marco 用 gli。","pronomi_indiretti");
  add("Scrivo a Maria. ___ scrivo una mail.","le","la","le","gli","scrivere a Maria：间接宾语 le。","pronomi_indiretti");
  add("Puoi dire a me la verità? Puoi ___ dire la verità?","mi","me","mi","lo","a me → mi，放在变位动词前。","pronomi_indiretti");
  add("Do il libro a te. ___ do il libro.","ti","te","ti","lo","a te → ti。","pronomi_indiretti");
  add("Il medico ___ ha dato una ricetta.","mi","me","lo","mi","给“我”一张处方：mi ha dato。","pronomi_indiretti");
  add("A Luca ___ piace questo posto.","gli","lo","gli","le","piacere 中体验者是间接宾语；Luca → gli。","pronomi_indiretti");
  add("A Giulia ___ serve aiuto.","le","la","le","gli","Giulia → le serve。","pronomi_indiretti");
  add("Quanti caffè vuoi? ___ voglio due.","Ne","Ci","Li","Ne","ne 可以替代 di + 名词/数量表达：Ne voglio due。","ne_partitivo");
  add("Hai bisogno di soldi? Sì, ___ ho bisogno.","ne","ci","li","ne","avere bisogno di... 可用 ne 替代。","ne_partitivo");
  add("Parli di lavoro? Sì, ___ parlo spesso.","ne","ci","lo","ne","parlare di... 可用 ne 替代。","ne_partitivo");
  add("Quante mele compri? ___ compro tre.","Ne","Le","Ci","Ne","数量表达中用 ne。","ne_partitivo");
  add("Vuoi del pane? Sì, ___ vorrei un po'.","ne","lo","ci","ne","部分数量“一些”用 ne。","ne_partitivo");
  add("Vai a Roma? Sì, ___ vado domani.","ci","ne","la","ci","ci 可以替代 a/in + 地点。","ci_locativo");
  add("Sei mai stato in Sicilia? Sì, ___ sono stato.","ci","ne","la","ci","in Sicilia → ci。","ci_locativo");
  add("Pensi al problema? Sì, ___ penso.","ci","lo","ne","ci","pensare a... 常可用 ci 替代。","ci_locativo");
  add("Credi a questa storia? Non ___ credo.","ci","la","ne","ci","credere a... → crederci。","ci_locativo");
  add("Maria compra la borsa. Maria ___ compra.","la","le","gli","la","替换直接宾语 la borsa → la。","pronomi_diretti");
  add("Porto i documenti al funzionario. ___ porto al funzionario.","Li","Gli","Le","Li","这里替换 i documenti，是直接宾语 li。","pronomi_diretti");
  add("Do i documenti al funzionario. ___ do i documenti.","Gli","Li","Lo","Gli","这里替换 al funzionario，是间接宾语 gli。","pronomi_indiretti");
  add("Hai una penna? Sì, ___ ho una.","ne","la","ci","ne","表示“其中一支”用 ne。","ne_partitivo");
  add("Andiamo al supermercato? Sì, ___ andiamo.","ci","lo","ne","ci","al supermercato 可由 ci 替换。","ci_locativo");
  add("Queste scarpe? Non ___ compro.","le","li","gli","le","scarpe 阴性复数，直接宾语 le。","pronomi_diretti");
 }
 private void add(String p,String ans,String a,String b,String c,String why,String g){qs.add(new Q(p,ans,a,b,c,why,g));}
 private void show(){feedback.setVisibility(View.GONE);next.setVisibility(View.GONE);if(idx>=qs.size()){b1.setVisibility(View.GONE);b2.setVisibility(View.GONE);b3.setVisibility(View.GONE);prompt.setText("代词专项完成 🎉");count.setText(qs.size()+" / "+qs.size());summary.setVisibility(View.VISIBLE);summary.setText("正确 "+correct+" / "+qs.size()+" · "+Math.round(correct*100.0/qs.size())+"%\n直接宾语、间接宾语、ci、ne 的结果已经进入语法知识地图。");return;}Q q=qs.get(idx);count.setText((idx+1)+" / "+qs.size()+" · 代词专项");prompt.setText(q.p);List<String> o=new ArrayList<>(Arrays.asList(q.a,q.b,q.c));Collections.shuffle(o);set(b1,o.get(0));set(b2,o.get(1));set(b3,o.get(2));started=SystemClock.elapsedRealtime();}
 private void set(MaterialButton b,String s){b.setText(s);b.setEnabled(true);b.setVisibility(View.VISIBLE);}
 private void answer(MaterialButton b){Q q=qs.get(idx);long ms=SystemClock.elapsedRealtime()-started;boolean ok=b.getText().toString().equals(q.ans);if(ok)correct++;progress.recordGrammarResult(q.g,ok,ms);progress.recordAuxiliaryResult("pronouns",ok,ms);if(!ok)progress.recordErrorCause(ErrorCause.GRAMMAR,0,"pronouns",q.ans,b.getText().toString(),q.why);feedback.setText((ok?"✅ 正确":"❌ 正确答案："+q.ans)+"\n"+q.why);feedback.setVisibility(View.VISIBLE);b1.setEnabled(false);b2.setEnabled(false);b3.setEnabled(false);next.setVisibility(View.VISIBLE);}
}
