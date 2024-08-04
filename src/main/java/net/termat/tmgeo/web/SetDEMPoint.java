package net.termat.tmgeo.web;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;

import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.util.MeshUtil;
import net.termat.tmgeo.util.PCUtil;

public class SetDEMPoint {
	public  static String URL_10m="https://cyberjapandata.gsi.go.jp/xyz/dem_png/{z}/{x}/{y}.png";
	public  static String URL_05m="https://cyberjapandata.gsi.go.jp/xyz/dem5a_png/{z}/{x}/{y}.png";
	
	public static VectorReader setDemPoint(VectorReader point,String url) throws ParseException {
		Map<String,BufferedImage> img=new HashMap<>();
		List<BufferedImage> list=new ArrayList<>();
		VectorReader vr=point;
		if(vr.getEPSG()!=4326) {
			vr=vr.createProjectionData(4326);
		}
		vr=setDEMProperty(vr,img,list,100,url);
		return vr;
	}
	
	public static VectorReader setDEMProperty(VectorReader vr,Map<String,BufferedImage> img,List<BufferedImage> list,int limit,String url) throws ParseException {
		VectorReader v2=vr;
		if(v2.getEPSG()!=4326) {
			v2=v2.createProjectionData(4326);
		}
		for(int i=0;i<v2.size();i++) {
			Point pos=(Point)v2.getGeometry(i);
			java.awt.Point co=MeshUtil.lonlatToTile(14, pos.getX(), pos.getY());
			String key="14/"+co.getX()+"/"+co.getY();
			if(img.containsKey(key)) {
				BufferedImage bi=img.get(key);
				Point2D px=MeshUtil.lonlatToPixel(14, pos.getX(), pos.getY());
				long xx=(long)Math.floor(px.getX());
				long yy=(long)Math.floor(px.getY());
				xx=xx-256*co.x;
				yy=yy-256*co.y;
				int color=bi.getRGB((int)xx, (int)yy);
				double zz=PCUtil.getZ(color);
				vr.getProperty(i).addProperty("z", rp(zz));
			}else {
				String u=url.replace("{z}","14").replace("{x}", Integer.toString((int)co.getX())).replace("{y}", Integer.toString((int)co.getY()));
				try {
					HttpsURLConnection con=(HttpsURLConnection)new URL(u).openConnection();
					SSLContext sslContext = SSLContext.getInstance("SSL");
					sslContext.init(null,
							new X509TrustManager[] { new LooseTrustManager() },
							new SecureRandom());

					con.setSSLSocketFactory(sslContext.getSocketFactory());
					con.setHostnameVerifier(new LooseHostnameVerifier());
			        BufferedImage bi=ImageIO.read(con.getInputStream());
					Point2D px=MeshUtil.lonlatToPixel(14, pos.getX(), pos.getY());
					long xx=(long)Math.floor(px.getX());
					long yy=(long)Math.floor(px.getY());
					xx=xx-256*co.x;
					yy=yy-256*co.y;
					int color=bi.getRGB((int)xx, (int)yy);
					double zz=PCUtil.getZ(color);
					vr.getProperty(i).addProperty("z", rp(zz));
					list.add(bi);
					img.put(key, bi);
					if(list.size()>limit) {
						BufferedImage pv=list.get(0);
						list.remove(0);
						String tmp="";
						for(String kk : img.keySet()) {
							if(img.get(kk)==pv) {
								tmp=kk;
								break;
							}
						}
						if(!tmp.isEmpty())img.remove(tmp);
					}
				}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
		}
		return vr;
	}
	
	
	private static float rp(double zz) {
		int val=(int)Math.round(zz*10);
		return val/10.0f;
	}
}
