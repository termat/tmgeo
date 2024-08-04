package net.termat.tmgeo.db;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.luciad.imageio.webp.WebPWriteParam;

/**
 * 画像処理ユーティリティクラス
 * @author t-matsuoka
 * @version 0.5
 */
public class DBUtil {

	private DBUtil(){}

	/**
	 * BufferedImageをbyte[]に変換
	 * @param img BufferedImage
	 * @param ext 画像の拡張子
	 * @return byte[]
	 * @throws IOException
	 */
	public static byte[] bi2Bytes(BufferedImage img,String ext)throws IOException{
		if(ext.equals("webp")) {
			return bi2BytesWebp(img);
		}else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write( img, ext, baos );
			baos.flush();
			return baos.toByteArray();
		}
	}

	public static byte[] bi2BytesWebp(BufferedImage img)throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageOutputStream  ios =  ImageIO.createImageOutputStream(baos);
		ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
		WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
		writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		writeParam.setCompressionType(writeParam.getCompressionTypes()[WebPWriteParam.LOSSLESS_COMPRESSION]);
		writer.setOutput(ios);
		writer.write(null, new IIOImage(img, null, null), writeParam);
		ios.flush();
		baos.flush();
		return baos.toByteArray();
	}
	
	/**
	 * byte[]をBufferedImageに変換
	 * @param raw byte[]
	 * @return BufferedImage
	 * @throws IOException
	 */
	public static BufferedImage bytes2Bi(byte[] raw)throws IOException{
		ByteArrayInputStream bais = new ByteArrayInputStream(raw);
		BufferedImage img=ImageIO.read(bais);
		return img;
	}
	
	public static byte[] floatToByteArray(float[][] dd){
		ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES);
		for(int i=0;i<dd.length;i++){
			for(int j=0;j<dd[i].length;j++){
				byteBuffer.putFloat(dd[i][j]);
			}
		}
		 return byteBuffer.array();
	}
	
	public static byte[] floatToByteArray(float[][][] dd){
		ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES);
		for(int i=0;i<dd.length;i++){
			for(int j=0;j<dd[i].length;j++){
				for(int k=0;k<dd[i][j].length;k++) {
					byteBuffer.putFloat(dd[i][j][k]);
				}
			}
		}
		 return byteBuffer.array();
	}
	
	public static float[][] byteArrayToDoubleArray(byte[] data,int m,int n) {
		float[][] ret=new float[m][n];
		int it=0;
		for(int i=0;i<ret.length;i++){
			for(int j=0;j<ret[0].length;j++){
				ret[i][j]=convertByteArrayToDouble(new byte[]{
					data[(it*Float.BYTES)],
					 data[(it*Float.BYTES)+1],
					 data[(it*Float.BYTES)+2],
					 data[(it*Float.BYTES)+3],
					 data[(it*Float.BYTES)+4],
					 data[(it*Float.BYTES)+5],
					 data[(it*Float.BYTES)+6],
					 data[(it*Float.BYTES)+7],
				});
				it++;
			}
		}
		return ret;
	}
	
	public static float[][][] byteArrayToDoubleArray(byte[] data,int c,int m,int n) {
		float[][][] ret=new float[c][m][n];
		int it=0;
		for(int k=0;k<c;k++) {
			for(int i=0;i<m;i++){
				for(int j=0;j<n;j++){
					ret[k][i][j]=convertByteArrayToDouble(new byte[]{
						data[(it*Float.BYTES)],
						 data[(it*Float.BYTES)+1],
						 data[(it*Float.BYTES)+2],
						 data[(it*Float.BYTES)+3],
						 data[(it*Float.BYTES)+4],
						 data[(it*Float.BYTES)+5],
						 data[(it*Float.BYTES)+6],
						 data[(it*Float.BYTES)+7],
					});
					it++;
				}
			}
		}
		return ret;
	}
	
	private static float convertByteArrayToDouble(byte[] doubleBytes){
		 ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES);
		 byteBuffer.put(doubleBytes);
		 byteBuffer.flip();
		 return byteBuffer.getFloat();
	}
}
