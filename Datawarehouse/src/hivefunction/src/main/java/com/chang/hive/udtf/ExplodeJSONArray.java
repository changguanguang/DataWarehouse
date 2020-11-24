package com.chang.hive.udtf;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.json.JSONArray;

import java.util.ArrayList;

/**
 *  一进多出
 */
public class ExplodeJSONArray extends GenericUDTF {

    @Override
    public StructObjectInspector initialize(StructObjectInspector argOIs) throws UDFArgumentException {

        // 进行合法性检查
        if(argOIs.getAllStructFieldRefs().size() != 1){
            throw new UDFArgumentException(" ExplodeJSONArray 只需要一个参数");

        }
        // 第一个参数必须为 String
        if(!"string".equals(argOIs.getAllStructFieldRefs().get(0).getFieldObjectInspector().getTypeName())){
            throw new UDFArgumentException("json_array_ti_struct_array的第一个参数应为string类型");
        }
        // 定义返回值名称和类型
        ArrayList<String> fieldNames = new ArrayList<String>();
        ArrayList<ObjectInspector> fieldOIs = new ArrayList<ObjectInspector>();
        fieldNames.add("items");
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);

        return ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames,fieldOIs);

    }


    @Override
    /**
     *  objects ： object array of arguments
     */
    public void process(Object[] objects) throws HiveException {
        // 1. 获取传入的数据
        String jsonArray = objects[0].toString();

        // 2. 将String转化为json数组
        JSONArray actions = new JSONArray(jsonArray);

        // 3. 循环一次，取出数组中的一个json，并写出
        for(int i=0;i<actions.length();i++){
            // 每一次都拿到一个json字符串，将其作为一行输出出去
            String[] result = new String[1];
            result[0] = actions.getString(i);
            // Passes an output row to the collector.
            forward(result);
        }
    }

    public void close() throws HiveException {

    }
}
