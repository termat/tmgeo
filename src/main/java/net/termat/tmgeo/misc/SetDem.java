package net.termat.tmgeo.misc;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.util.MeshUtil;
import net.termat.tmgeo.util.PCUtil;
import net.termat.tmgeo.web.LooseHostnameVerifier;
import net.termat.tmgeo.web.LooseTrustManager;

public class SetDem {
	private static String url="https://cyberjapandata.gsi.go.jp/xyz/dem_png/{z}/{x}/{y}.png";
	
	public static void main(String[] args) throws ParseException, IOException {
		File f=new File("C:\\Users\\terma\\OneDrive\\Documents\\workspace\\react\\potar\\public\\geojson\\20240720.geojson");
		Map<String,BufferedImage> img=new HashMap<>();
		List<BufferedImage> list=new ArrayList<>();
		System.out.println(f.getName());
		VectorReader vr=VectorReader.createReader(4326,f);
		vr=toPoint(vr);
		vr=setDEMProperty(vr,img,list,100);
		vr.writeJson(f);
	}
	
	public static void main2(String[] args) throws ParseException, IOException {
		File dir=new File("C:\\Users\\terma\\OneDrive\\Documents\\workspace\\geojson");
		Map<String,BufferedImage> img=new HashMap<>();
		List<BufferedImage> list=new ArrayList<>();
		for(File f : dir.listFiles()) {
			System.out.println(f.getName());
			if(!f.getName().endsWith(".geojson"))continue;
			VectorReader vr=VectorReader.createReader(4326,f);
			vr=toPoint(vr);
			vr=setDEMProperty(vr,img,list,100);
			vr.writeJson(f);
		}
	}
	
	
	public static VectorReader toPoint(VectorReader vr) throws ParseException {
		List<Geometry> geo=new ArrayList<>();
		for(int i=0;i<vr.size();i++) {
			Geometry g=vr.getGeometry(i);
			JsonObject p=vr.getProperty(i);
			if(g instanceof Point) {
				geo.add(g);
			}else if(g instanceof LineString) {
				LineString ll=(LineString)g;
				for(int j=0;j<ll.getNumPoints();j++) {
					geo.add(ll.getPointN(j));
				}
			}else if(g instanceof MultiPoint) {
				MultiPoint ll=(MultiPoint)g;
			}else if(g instanceof MultiLineString) {
				MultiLineString ls=(MultiLineString)g;
				for(int k=0;k<ls.getNumGeometries();k++) {
					LineString ll=(LineString)ls.getGeometryN(k);
				}
			}
		}
		geo=along(geo,5.0);
		List<JsonObject> prop=new ArrayList<>();
		for(Geometry g : geo) {
			prop.add(new JsonObject());
		}
		return VectorReader.createReader(vr.getEPSG(), geo, prop);
	}
	
	public static List<Geometry> along(List<Geometry> pos,double dist){
		List<com.mapbox.geojson.Point> pl=new ArrayList<>();
		for(Geometry gg : pos) {
			Point pt=(Point)gg;
			pl.add(com.mapbox.geojson.Point.fromLngLat(pt.getX(),pt.getY()));
		}
		com.mapbox.geojson.LineString line=com.mapbox.geojson.LineString.fromLngLats(pl);
		double len=TurfMeasurement.length(line, TurfConstants.UNIT_METERS);
		List<com.mapbox.geojson.Point> ans=new ArrayList<>();
		for(double d=0;d<len;d=d+dist) {
			ans.add(TurfMeasurement.along(line,d,TurfConstants.UNIT_METERS));
		}
		ans.add(pl.get(pl.size()-1));
		List<Geometry> geo=new ArrayList<>();
		GeometryFactory gf=new GeometryFactory();
		for(com.mapbox.geojson.Point p : ans) {
			Point pm=gf.createPoint(new Coordinate(p.longitude(),p.latitude()));
			geo.add(pm);
		}
		return geo;
	}
	

	public static VectorReader setDEMProperty(VectorReader vr,Map<String,BufferedImage> img,List<BufferedImage> list,int limit) throws ParseException {
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
