package com.italiano2774.nativeapp;
import org.junit.Test;import static org.junit.Assert.*;
public class VerbConjugatorTest{
 @Test public void presentParlare(){String[] f=VerbConjugator.forms("parlare",VerbConjugator.PRESENT);assertArrayEquals(new String[]{"parlo","parli","parla","parliamo","parlate","parlano"},f);}
 @Test public void futureAndareUsesIrregularStem(){String[] f=VerbConjugator.forms("andare",VerbConjugator.FUTURO);assertEquals("andrò",f[0]);assertEquals("andranno",f[5]);}
 @Test public void futureMangiareDropsI(){String[] f=VerbConjugator.forms("mangiare",VerbConjugator.FUTURO);assertEquals("mangerò",f[0]);}
 @Test public void passatoFareIsFatto(){String[] f=VerbConjugator.forms("fare",VerbConjugator.PASSATO);assertEquals("ho fatto",f[0]);}
 @Test public void essereAuxAgrees(){String[] f=VerbConjugator.forms("andare",VerbConjugator.PASSATO);assertEquals("sono andato/a",f[0]);assertEquals("siamo andati/e",f[3]);}
}
