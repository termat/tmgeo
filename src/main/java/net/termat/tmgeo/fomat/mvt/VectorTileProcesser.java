package net.termat.tmgeo.fomat.mvt;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonElement;
import com.google.protobuf.ByteString;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.util.MeshUtil;
import net.termat.tmgeo.web.LooseHostnameVerifier;
import net.termat.tmgeo.web.LooseTrustManager;

public class VectorTileProcesser {
	private VectorReader vr;
	private String url="https://cyberjapandata.gsi.go.jp/xyz/experimental_bvmap/{z}/{x}/{y}.pbf";
	
	public static void main(String[] args) {
		try {
			VectorTileProcesser vp=new VectorTileProcesser(6672,new File("C:\\Dropbox (株式会社ウエスコ)\\R4四国中央温暖化\\GIS最新\\境界データ\\四国中央市区域.geojson"));
//			FeatureCollection fc=vp.process(14, "label",100,653);
//			FeatureCollection fc=vp.process(15, "structurel",8202);	
			FeatureCollection fc=vp.process(14, "symbol",8103);
			BufferedWriter bw=new BufferedWriter(new FileWriter(new File("C:\\Documents\\業務\\作業\\送電施設.geojson")));
			bw.write(fc.toJson());
			bw.close();
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public VectorTileProcesser(int epsg,File f) throws ParseException, IOException {
		vr=VectorReader.createReader(epsg, f);
		if(epsg!=4326) {
			vr=vr.createProjectionData(4326);
		}
	}
	
	public FeatureCollection process(int zoom,String tag) throws IOException {
		List<Feature> fs=new ArrayList<>();
		List<Point> ps=MeshUtil.getTileList(vr.getBounds(), zoom);
		for(Point p : ps) {
			byte[] buf=getTile(zoom,p.x,p.y);
			if(!MVTReader.getMvtLayerName(buf).contains(tag))continue;
			FeatureCollection fc=MVTReader.mvtToJson(buf, zoom, p.x, p.y, tag);
			List<Feature> ft=fc.features();
			for(Feature f :ft) {
				fs.add(f);
			}
		}
		return FeatureCollection.fromFeatures(fs);
	}
	 
	public FeatureCollection process(int zoom,String tag,int ftcode) throws IOException {
		List<Feature> fs=new ArrayList<>();
		List<Point> ps=MeshUtil.getTileList(vr.getBounds(), zoom);
		int iter=0;
		for(Point p : ps) {
			if(++iter%100==0)System.out.println(iter+" / "+ps.size());
			byte[] buf=getTile(zoom,p.x,p.y);
			if(buf==null)continue;
			if(!MVTReader.getMvtLayerName(buf).contains(tag))continue;
			FeatureCollection fc=MVTReader.mvtToJson(buf, zoom, p.x, p.y, tag);
			List<Feature> ft=fc.features();
			for(Feature f :ft) {
				JsonElement je=f.properties().get("ftCode");
				if(je==null)continue;
				if(je.getAsInt()==ftcode)fs.add(f);
			}
		}
		return FeatureCollection.fromFeatures(fs);
	}
	
	public FeatureCollection process(int zoom,String tag,int ftcode,int annoCtg) throws IOException {
		List<Feature> fs=new ArrayList<>();
		List<Point> ps=MeshUtil.getTileList(vr.getBounds(), zoom);
		int iter=0;
		for(Point p : ps) {
			if(++iter%100==0)System.out.println(iter+" / "+ps.size());
			byte[] buf=getTile(zoom,p.x,p.y);
			if(buf==null)continue;
			if(!MVTReader.getMvtLayerName(buf).contains(tag))continue;
			FeatureCollection fc=MVTReader.mvtToJson(buf, zoom, p.x, p.y, tag);
			List<Feature> ft=fc.features();
			for(Feature f :ft) {
				JsonElement je=f.properties().get("ftCode");
				if(je==null)continue;
				if(je.getAsInt()==ftcode) {
					je=f.properties().get("annoCtg");
					if(je.getAsInt()==annoCtg)fs.add(f);
				}
			}
		}
		return FeatureCollection.fromFeatures(fs);
	}
	
	public FeatureCollection process(int zoom,String tag,int[] ftcode) throws IOException {
		List<Feature> fs=new ArrayList<>();
		List<Point> ps=MeshUtil.getTileList(vr.getBounds(), zoom);
		for(Point p : ps) {
			byte[] buf=getTile(zoom,p.x,p.y);
			if(buf==null)continue;
			if(!MVTReader.getMvtLayerName(buf).contains(tag))continue;
			FeatureCollection fc=MVTReader.mvtToJson(buf, zoom, p.x, p.y, tag);
			List<Feature> ft=fc.features();
			for(Feature f :ft) {
				JsonElement je=f.properties().get("ftCode");
				if(je==null)continue;
				int code=je.getAsInt();
				for(int ff : ftcode) {
					if(ff==code) {
						fs.add(f);
						break;
					}
				}
			}
		}
		return FeatureCollection.fromFeatures(fs);
	}
	
	protected byte[] getTile(int zoom,long x,long y) {
		String uu=new String(url).replace("{z}", Integer.toString(zoom));
		uu=uu.replace("{x}", Long.toString(x));
		uu=uu.replace("{y}", Long.toString(y));
		try {
			try {
				HttpsURLConnection con=(HttpsURLConnection)new URL(uu).openConnection();
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null,
						new X509TrustManager[] { new LooseTrustManager() },
						new SecureRandom());

				con.setSSLSocketFactory(sslContext.getSocketFactory());
				con.setHostnameVerifier(new LooseHostnameVerifier());
				ByteString bs=ByteString.readFrom(con.getInputStream());
				byte[] ret=bs.toByteArray();
				con.disconnect();
				return ret;
			}catch(FileNotFoundException e) {
				return null;
			}
		}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
}
