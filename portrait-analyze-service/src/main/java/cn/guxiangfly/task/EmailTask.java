package cn.guxiangfly.task;
import cn.guxiangfly.entity.CarrierInfo;
import cn.guxiangfly.entity.EmaiInfo;
import cn.guxiangfly.map.CarrierMap;
import cn.guxiangfly.map.EmailMap;
import cn.guxiangfly.reduce.CarrierReduce;
import cn.guxiangfly.reduce.EmailReduce;
import cn.guxiangfly.util.MongoUtils;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;
import org.bson.Document;

import java.util.List;

public class EmailTask {
    public static void main(String[] args) {
        final ParameterTool params = ParameterTool.fromArgs(args);

        // set up the execution environment
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // make parameters available in the web interface
        env.getConfig().setGlobalJobParameters(params);

        // get input data
        DataSet<String> text = env.readTextFile(params.get("input"));

        DataSet<EmaiInfo> mapresult = text.map(new EmailMap());
        DataSet<EmaiInfo> reduceresutl = mapresult.groupBy("groupfield").reduce(new EmailReduce());
        try {
            List<EmaiInfo> reusltlist = reduceresutl.collect();
            for(EmaiInfo emaiInfo:reusltlist){
                String emailtype = emaiInfo.getEmailtype();
                Long count = emaiInfo.getCount();

                Document doc = MongoUtils.findoneby("emailstatics","youfanPortrait",emailtype);
                if(doc == null){
                    doc = new Document();
                    doc.put("info",emailtype);
                    doc.put("count",count);
                }else{
                    Long countpre = doc.getLong("count");
                    Long total = countpre+count;
                    doc.put("count",total);
                }
                MongoUtils.saveorupdatemongo("emailstatics","youfanPortrait",doc);
            }
            env.execute("email analy");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
