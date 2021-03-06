package com.mars.mvc.util;

import com.alibaba.fastjson.annotation.JSONField;
import com.mars.core.constant.MarsConstant;
import com.mars.core.enums.DataType;
import com.mars.core.util.StringUtil;
import com.mars.server.server.request.HttpMarsRequest;
import com.mars.server.server.request.HttpMarsResponse;
import com.mars.server.server.request.model.MarsFileUpLoad;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * 构建参数
 */
public class BuildParams {

    /**
     * 构建MarsApi的传参
     * @param method
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public static Object[] builder(Method method, HttpMarsRequest request, HttpMarsResponse response) throws Exception {
        try {
            Class requestClass = HttpMarsRequest.class;
            Class responseClass = HttpMarsResponse.class;
            Class mapClass = Map.class;
            Class[] paramTypes = method.getParameterTypes();
            if(paramTypes == null || paramTypes.length < 1){
                return null;
            }
            Object[] params = new Object[paramTypes.length];
            for(int i = 0;i<paramTypes.length;i++){
                Class cls = paramTypes[i];
                if(requestClass.equals(cls)){
                    params[i] = request;
                } else if(responseClass.equals(cls)){
                    params[i] = response;
                } else if(mapClass.equals(cls)) {
                    Map<String, Object> paramMap = request.getParameters();
                    if(paramMap != null){
                        paramMap.put(MarsConstant.REQUEST_FILE,request.getFiles());
                    }
                    params[i] = paramMap;
                } else {
                    params[i] = getObject(cls,request);
                }
            }
            return params;
        } catch (Exception e){
            throw new Exception("参数注入异常",e);
        }
    }

    /**
     * 构建参数对象
     * @param cls
     * @param request
     * @return
     * @throws Exception
     */
    private static Object getObject(Class cls, HttpMarsRequest request) throws Exception {
        /* 如果参数类型既不是request，也不是response，那么就当做一个对象来处理 */
        Object obj = cls.getDeclaredConstructor().newInstance();
        Field[] fields = cls.getDeclaredFields();
        for(Field f : fields){
            f.setAccessible(true);

            String[] valList = request.getParameterValues(f.getName());
            Map<String,MarsFileUpLoad> marsFileUpLoadMap = request.getFiles();

            if(f.getType().equals(MarsFileUpLoad.class) && marsFileUpLoadMap != null){
                f.set(obj, marsFileUpLoadMap.get(f.getName()));
            } else if(f.getType().equals(MarsFileUpLoad[].class) && marsFileUpLoadMap != null && marsFileUpLoadMap.size() > 0){
                putMarsFileUploads(f,obj,marsFileUpLoadMap);
            } else if(valList != null && valList.length > 0){
                putAttr(f,obj,valList);
            }
        }
        return obj;
    }

    /**
     * 给参数赋值
     * @param field 字段
     * @param obj 对象
     * @param marsFileUpLoadMap 文件
     * @throws Exception 异常
     */
    private static void putMarsFileUploads(Field field, Object obj, Map<String,MarsFileUpLoad> marsFileUpLoadMap) throws Exception{
        MarsFileUpLoad[] marsFileUpLoads = new MarsFileUpLoad[marsFileUpLoadMap.size()];
        int index = 0;
        for(String key : marsFileUpLoadMap.keySet()){
            marsFileUpLoads[index] = marsFileUpLoadMap.get(key);
            index++;
        }
        field.set(obj, marsFileUpLoads);
    }

    /**
     * 给参数赋值
     * @param field 字段
     * @param obj 对象
     * @param valList 数据
     * @throws Exception 异常
     */
    private static void putAttr(Field field, Object obj, String[] valList) throws Exception{
        String fieldTypeName = field.getType().getSimpleName().toUpperCase();
        String valStr = valList[0];
        switch (fieldTypeName){
            case DataType.INT:
            case DataType.INTEGER:
                field.set(obj,Integer.parseInt(valStr));
                break;
            case DataType.BYTE:
                field.set(obj,Byte.parseByte(valStr));
                break;
            case DataType.STRING:
            case DataType.CHAR:
            case DataType.CHARACTER:
                field.set(obj,valStr);
                break;
            case DataType.DOUBLE:
                field.set(obj,Double.parseDouble(valStr));
                break;
            case DataType.FLOAT:
                field.set(obj,Float.parseFloat(valStr));
                break;
            case DataType.LONG:
                field.set(obj,Long.parseLong(valStr));
                break;
            case DataType.SHORT:
                field.set(obj,Short.valueOf(valStr));
                break;
            case DataType.BOOLEAN:
                field.set(obj,Boolean.parseBoolean(valStr));
                break;
            case DataType.DATE:
                String fmt = "yyyy-MM-dd HH:mm:ss";
                JSONField jsonField = field.getAnnotation(JSONField.class);
                if(jsonField != null && !StringUtil.isNull(jsonField.format())){
                    fmt = jsonField.format();
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(fmt);
                field.set(obj,simpleDateFormat.parse(valStr));
                break;
            default:
                if (field.getType().equals(String[].class)){
                    field.set(obj,valList);
                }
                break;
        }
    }
}
