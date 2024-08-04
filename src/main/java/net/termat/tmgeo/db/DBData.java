package net.termat.tmgeo.db;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;


public class DBData {
	enum Type{IMAGE,ARRAY,TEXT};
	
	@DatabaseField(generatedId=true)
	public long id;

	@DatabaseField
	public long key;

	@DatabaseField
	public Type type;
    
    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    public byte[] dataBytes;
    
    public static DBData create(BufferedImage bi,String ext) throws IOException {
    	DBData ret=new DBData();
    	ret.type=Type.IMAGE;
    	ret.dataBytes=DBUtil.bi2Bytes(bi, ext);
    	return ret;
    }
    
    public static DBData create(float[][] f) {
      	DBData ret=new DBData();
    	ret.dataBytes=DBUtil.floatToByteArray(f);
    	ret.type=Type.ARRAY;
    	return ret;
    }
    
    public static DBData create(float[][][] f) {
      	DBData ret=new DBData();
    	ret.dataBytes=DBUtil.floatToByteArray(f);
    	ret.type=Type.ARRAY;
    	return ret;
    }
    
    public static DBData create(String str) {
      	DBData ret=new DBData();
    	ret.dataBytes=str.getBytes();
    	ret.type=Type.TEXT;
    	return ret;
    }
    
    public Object get(Index ii) throws IOException {
    	switch(type) {
    		case IMAGE:
    			return DBUtil.bytes2Bi(dataBytes);
    		case ARRAY:
    			if(ii.channel>1) {
        			float[][][] ft=DBUtil.byteArrayToDoubleArray(dataBytes, ii.channel ,ii.width, ii.height);
        			return ft;
    			}else {
        			float[][] ft=DBUtil.byteArrayToDoubleArray(dataBytes, ii.width, ii.height);
        			return ft;
    			}
    		case TEXT:
    			return new String(dataBytes);
    		default:
    			return null;
    	}
    }
    
    public static void main(String[] args) {
    	float[][] ff=new float[][]{{0,0},{1,1},{2,2}};
    	String str=Arrays.deepToString(ff);
    	System.out.println(str);
    	List<String> list = Arrays.asList(str); 
    	System.out.println(list.get(0));
    }
    
    
    
}
