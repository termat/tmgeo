package net.termat.tmgeo.fomat.ply;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;

import net.termat.tmgeo.data.BandReader;
import net.termat.tmgeo.data.BandWriter;
import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.util.PCUtil;
import net.termat.tmgeo.util.VecUtil;

public class Test {
	
	public static void main(String[] args) throws ParseException, IOException {
		VectorReader vr=VectorReader.createReader(6677,new File("H:\\仕事\\R06市原市\\市原GIS\\筆界線.geojson"));
		BandReader br=BandReader.createReader(new File("H:\\仕事\\R06市原市\\JAXA土地利用_市原2022_6677.tif"));
		for(int i=0;i<vr.size();i++) {
			Geometry gg=vr.getGeometry(i);
			Area aa=VecUtil.toShape(gg);
			BandReader tb=br.createSubImage(aa.getBounds2D(),1);
			AffineTransform af=tb.getTransform();
			float[][] dem=tb.getBand(0);
			int ct=0;
			int[] ck=new int[4];
			for(int x=0;x<dem.length;x++) {
				for(int y=0;y<dem[0].length;y++) {
					Point2D p=new Point2D.Double(x, y);
					p=af.transform(p, new Point2D.Double());
					if(aa.contains(p)) {
						if(dem[x][y]<0)continue;	
						if((int)dem[x][y]==6) {
							ck[0]++;
							ct++;
						}else if((int)dem[x][y]==8) {
							ck[0]++;
							ct++;
						}else if((int)dem[x][y]==9) {
							ck[1]++;
							ct++;
						}else if((int)dem[x][y]==11) {
							ck[2]++;
							ct++;
						}else {
							ck[3]++;
							ct++;
						}
					}
				}
			}
			int max=-1;
			int dd=-1;
			for(int k=0;k<ck.length;k++) {
				if(max<ck[k]) {
					dd=k;
					max=ck[k];
				}
			}
			String land="その他";
			if(dd==0) {
				land="広葉樹";
			}else if(dd==1) {
				land="広葉樹";
			}else if(dd==2) {
				land="竹林";
			}		
			vr.getProperty(i).addProperty("land_type", land);
			vr.getProperty(i).addProperty("deciduous", (float)ck[0]/(float)ct);
			vr.getProperty(i).addProperty("conifer", (float)ck[1]/(float)ct);
			vr.getProperty(i).addProperty("bamboo", (float)ck[2]/(float)ct);
			vr.getProperty(i).addProperty("misc", (float)ck[3]/(float)ct);
		}
		vr.writeJson(new File("H:\\仕事\\R06市原市\\市原GIS\\筆界線.geojson"));
	}
	
	public static void main6(String[] args) throws ParseException, IOException {
		VectorReader vr=VectorReader.createReader(new File("H:\\仕事\\R06市原市\\市原GIS\\筆界データ\\筆界データ.shp"));
		BandReader br=BandReader.createReader(new File("H:\\仕事\\R06市原市\\Geotiffなど\\geotiff\\基盤地図情報（数値標高モデル）5mメッシュ（航空レーザ測量）_傾斜量(Prewitt).tif"));
		for(int i=0;i<vr.size();i++) {
			Geometry gg=vr.getGeometry(i);
			Area aa=VecUtil.toShape(gg);
			BandReader tb=br.createSubImage(aa.getBounds2D(),1);
			AffineTransform af=tb.getTransform();
			float[][] dem=tb.getBand(0);
			float ave=0;
			float max=-999;
			float min=999;
			int ct=0;
			int[] ck=new int[4];
			for(int x=0;x<dem.length;x++) {
				for(int y=0;y<dem[0].length;y++) {
					Point2D p=new Point2D.Double(x, y);
					p=af.transform(p, new Point2D.Double());
					if(aa.contains(p)) {
						if(Float.isNaN(dem[x][y]))continue;
						if(dem[x][y]<0)continue;
						ave +=dem[x][y];
						max=Math.max(max, dem[x][y]);
						min=Math.min(min, dem[x][y]);
						ct++;
						
						if(dem[x][y]<15) {
							ck[0]++;
						}else if(dem[x][y]<30) {
							ck[1]++;
						}else if(dem[x][y]<35) {
							ck[2]++;
						}else {
							ck[3]++;
						}
					}
				}
			}
			vr.getProperty(i).addProperty("slope_ave", (float)(ave/ct));
			vr.getProperty(i).addProperty("slope_max", (float)(max));
			vr.getProperty(i).addProperty("slope_min", (float)(min));
			
			vr.getProperty(i).addProperty("gentle_slope", (float)ck[0]/(float)ct);
			vr.getProperty(i).addProperty("moderate_slope", (float)ck[1]/(float)ct);
			vr.getProperty(i).addProperty("steep_slope", (float)ck[2]/(float)ct);
			vr.getProperty(i).addProperty("very_slope", (float)ck[3]/(float)ct);
		}
		vr.writeJson(new File("H:\\仕事\\R06市原市\\市原GIS\\筆界線.geojson"));
	}
	
	public static void mainQQQ(String[] args) throws ParseException, IOException {
		VectorReader vr=VectorReader.createReader(new File("H:\\仕事\\R06市原市\\市原GIS\\筆界線\\筆界線.shp"));
		List<Area> aa=vr.getShapeList();
		Rectangle2D rect=vr.getBounds();
		int sx=(int)Math.floor(rect.getX());
		int sy=(int)Math.floor(rect.getY());
		int ex=(int)Math.ceil(rect.getX()+rect.getWidth());
		int ey=(int)Math.ceil(rect.getY()+rect.getHeight());
		List<Geometry> sp=new ArrayList<>();
		List<JsonObject> obj=new ArrayList<>();
		int ii=1;
		for(int x=sx;x<=ex;x=x+50) {
			for(int y=sy;y<=ey;y=y+50) {
				Rectangle2D rr=new Rectangle2D.Double(x, y, 50, 50);
				for(Area ax : aa) {
					if(ax.intersects(rr)) {
						sp.add(VecUtil.toJTS(rr));
						JsonObject o=new JsonObject();
						o.addProperty("mid", ii++);
						obj.add(o);
						break;
					}
				}
			}
		}
		VectorReader v2=VectorReader.createReader(6677, sp, obj);
		v2.writeJson(new File("H:\\仕事\\R06市原市\\市原GIS\\mesh50m.geojson"));
	}
	
	
	
	public static void mainX(String[] args) throws IOException {
		BandReader br=BandReader.createReader(new File("H:\\仕事\\R06市原市\\市原GIS\\DEM5mB.tif"));
		float[][] val=br.getBand(0);
		
		float[][] tmp=new float[val.length][val[0].length];
		
		for(int i=0;i<val.length;i++) {
			for(int j=0;j<val[0].length;j++) {
				tmp[i][j]=val[i][j];
			}
		}
		for(int i=1;i<val.length-1;i++) {
			for(int j=1;j<val[0].length-1;j++) {
				float[] tp=new float[9];
				tp[0]=val[i-1][j-1]/16;
				tp[1]=2*val[i][j-1]/16;
				tp[2]=val[i+1][j-1]/16;
				
				tp[3]=2*val[i-1][j]/16;
				tp[4]=4*val[i][j]/16;
				tp[5]=2*val[i+1][j]/16;
				
				tp[6]=val[i-1][j+1]/16;
				tp[7]=2*val[i][j+1]/16;
				tp[8]=val[i+1][j+1]/16;
				
				float vvv=0;
				for(int k=0;k<tp.length;k++) {
					if(Float.isNaN(tp[k])||tp[k]<0) {
						vvv =-999;
					}else {
						vvv +=tp[k];
					}
				}
				if(vvv>=0)tmp[i][j]=vvv;
			}
		}
		BandReader bx=BandReader.createReader(6677, br.getTransform(),tmp);
		BandWriter.writeSingleImage(bx, 0,new File("H:\\仕事\\R06市原市\\市原GIS\\DEM5mC.tif"), false);
	}
	
	
	public static void mainQQ(String[] args) throws ParseException, IOException, NoninvertibleTransformException {
		File f=new File("H:\\仕事\\R06市原市\\ply\\傾斜区分図.png");
		BufferedImage img=ImageIO.read(f);
		int ww=img.getWidth();
		int hh=img.getHeight();		
		AffineTransform af=PCUtil.loadTransform(new File("H:\\仕事\\R06市原市\\ply\\標高図.pgw"));
		af=af.createInverse();
		BufferedReader rr=new BufferedReader(new FileReader(new File("H:\\仕事\\R06市原市\\ply\\xyz.csv")));
		BufferedWriter bw=new BufferedWriter(new FileWriter(new File(f.getAbsolutePath().replace(".png", ".xyz"))));
		String line=null;
		rr.readLine();
		while((line=rr.readLine())!=null) {
			String[] pp=line.split(",");
			Point2D pt=new Point2D.Double(Double.parseDouble(pp[0]),Double.parseDouble(pp[1]));
			pt=af.transform(pt, new Point2D.Double());
			int xx=(int)Math.floor(pt.getX());
			int yy=(int)Math.floor(pt.getY());
			if(xx>=0&&xx<ww&&yy>=0&&yy<hh) {
				Color col=new Color(img.getRGB(xx, yy));
				float fx=Float.parseFloat(pp[0])+(float)(Math.random()*0.001)-0.0005f;
				float fy=Float.parseFloat(pp[1])+(float)(Math.random()*0.001)-0.0005f;
				
				bw.write(fx+","+fy+","+pp[2]+","+col.getRed()+","+col.getGreen()+","+col.getBlue()+"\n");
			}
		}
		bw.flush();
		bw.close();
		rr.close();
	}
	
	public static void main2(String[] args) throws ParseException, IOException, NoninvertibleTransformException {
		BandReader br=BandReader.createReader(new File("H:\\仕事\\R06市原市\\市原GIS\\DEM5mC.tif"));
		float[][] val=br.getBand(0);
		AffineTransform af=br.getTransform();
		af=af.createInverse();
		BufferedReader rr=new BufferedReader(new FileReader(new File("H:\\仕事\\R06市原市\\ply\\data.csv")));
		BufferedWriter bw=new BufferedWriter(new FileWriter(new File("H:\\仕事\\R06市原市\\ply\\xyz.csv")));
		bw.write("x,y,z,\n");
		String line=null;
		rr.readLine();
		while((line=rr.readLine())!=null) {
			String[] pp=line.split(",");
			Point2D pt=new Point2D.Double(Double.parseDouble(pp[0]),Double.parseDouble(pp[1]));
			pt=af.transform(pt, new Point2D.Double());
			int xx=(int)Math.floor(pt.getX());
			int yy=(int)Math.floor(pt.getY());
			if(xx>=0&&xx<val.length&&yy>=0&&yy<val[0].length) {
				if(!Float.isNaN(val[xx][yy])) {
					bw.write(pp[0]+","+pp[1]+","+val[xx][yy]+"\n");
				}
			}
		}
		bw.flush();
		bw.close();
		rr.close();
	}

	public static void main1(String[] args) throws ParseException, IOException {
		VectorReader vr=VectorReader.createReader(new File("H:\\仕事\\R06市原市\\市原GIS\\対象地域poly\\対象地域poly.shp"));
		List<Area> sp=vr.getShapeList();
		Rectangle2D r=vr.getBounds();
		double sx=Math.floor(r.getX());
		double sy=Math.floor(r.getY());
		double ex=Math.ceil(r.getX()+r.getWidth());
		double ey=Math.ceil(r.getY()+r.getHeight());
		List<Point2D> list=new ArrayList<>();
		for(double x=sx;x<=ex;x++) {
			for(double y=sy;y<=ey;y++) {
				Point2D p=new Point2D.Double(x, y);
				for(Area a:sp) {
				if(a.contains(p)) {
					list.add(p);
					break;
				}
				}
			}
		}
		BufferedWriter bw=new BufferedWriter(new FileWriter(new File("H:\\仕事\\R06市原市\\ply\\data.csv")));
		bw.write("x,y\n");
		for(Point2D p : list) {
			bw.write(((float)p.getX()+Math.random())+","+((float)p.getY())+"\n");
		}
		bw.flush();
		bw.close();
	}
	
}
