package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Curated v4.8.0 life missions. Every task points to a reviewed local scenario/dialogue. */
public final class LifeTaskRepository {
    private static final List<LifeTask> TASKS;
    static{
        List<LifeTask> x=new ArrayList<>();
        x.add(new LifeTask("coffee","bar_restaurant","☕","在咖啡店和餐厅点单","点餐、询价、刷卡、要小票","能独立完成一次从点单到付款的简短交流"));
        x.add(new LifeTask("supermarket","supermarket","🛒","在超市购物","找商品、问价格、袋子、会员卡、结账","能在超市完成找货和结账"));
        x.add(new LifeTask("transport","transport","🚆","坐公交和火车","站台、换乘、晚点、到达时间、公交站","能独立确认一段公共交通行程"));
        x.add(new LifeTask("directions","directions","🧭","问路和找地点","直走、左右转、远近、地标、确认方向","听懂简单指路并继续追问"));
        x.add(new LifeTask("clothing","clothing_store","👕","服装店试穿和退换","尺码、试穿、价格、换货、退款","能完成选尺码、试穿和简单退换货"));
        x.add(new LifeTask("appointment","phone_appointment","📞","打电话预约","说明目的、日期、时间、改约、确认","能用电话完成一次基础预约"));
        x.add(new LifeTask("health","health","🏥","看医生和去药店","症状、持续时间、处方、剂量、药物","能清楚说明常见症状并听懂基本建议"));
        x.add(new LifeTask("post_office","post_office","📦","去邮局寄件","包裹、挂号、地址、费用、回执","能独立寄出一个包裹或挂号信"));
        x.add(new LifeTask("bank","bank","💳","银行和付款","账户、转账、银行卡、现金、手续费","能处理常见付款和基础银行咨询"));
        x.add(new LifeTask("housing","housing","🏠","租房和看房","租金、押金、合同、看房、账单","能问清一套房的核心租赁条件"));
        x.add(new LifeTask("bureaucracy","bureaucracy","🏛","Comune 和 Questura 办事","预约、文件、复印件、窗口、领取","能在公共机构说明来意并确认所需材料"));
        x.add(new LifeTask("work","work","💼","工作和面试","经历、时间、职责、合同、开始工作","能完成基础工作沟通和简短面试回答"));
        TASKS=Collections.unmodifiableList(x);
    }
    private LifeTaskRepository(){}
    public static List<LifeTask> all(){return TASKS;}
    public static LifeTask find(String id){if(id==null)return null;for(LifeTask t:TASKS)if(t.id.equals(id))return t;return null;}
    public static LifeTask findByScenario(String scenarioId){if(scenarioId==null)return null;for(LifeTask t:TASKS)if(t.scenarioId.equals(scenarioId))return t;return null;}
}
