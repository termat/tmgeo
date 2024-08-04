package net.termat.tmgeo.data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
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

import org.gdal.gdal.ColorTable;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import org.gdal.osr.osrConstants;
import org.locationtech.jts.io.ParseException;

import net.termat.components.gradient.Gradient;
import net.termat.components.gradient.Range;
import net.termat.tmgeo.util.LonLatXY;
import net.termat.tmgeo.util.PCUtil;

public class BandUtil {
	
	public static void main4(String[] args) throws IOException, ParseException {
		BandReader b1=BandReader.createReader(new File("C:\\Users\\t-matsuoka\\株式会社ウエスコ Dropbox\\松岡輝樹\\R06市原市\\02センチネル2\\S2\\T54SVE_20240511T012701_B08_10m.tif"));
		BandReader b2=BandReader.createReader(new File("C:\\Users\\t-matsuoka\\株式会社ウエスコ Dropbox\\松岡輝樹\\R06市原市\\02センチネル2\\S2\\T54SVE_20240511T012701_B04_10m.tif"));
		BandReader ret=createNDV(b1,b2);
		BandWriter.writeGeoTifImage(ret, new File("C:\\Users\\t-matsuoka\\株式会社ウエスコ Dropbox\\松岡輝樹\\R06市原市\\02センチネル2\\S2\\T54SVE_20240511T012701_NDVI_10m.tif"));
	}
	
	public static void main2(String[] args) throws IOException, ParseException {
		VectorReader vr=VectorReader.createReader(32652, new File("H:\\仕事\\R06松岡\\01見積もり\\20240521佐賀県衛星\\佐賀県32652.geojson"));
		Rectangle2D rect=vr.getBounds();
		File dir=new File("H:\\仕事\\R06松岡\\01見積もり\\20240521佐賀県衛星\\センチネル2");
		List<File> fs=new ArrayList<>();
		String tag="B11";
		for(File f : dir.listFiles()) {
			if(f.getName().contains(tag)) {
				fs.add(f);
			}
		}
		File[] fl=fs.toArray(new File[fs.size()]);
		BandReader br=reSampling1ch(rect,10,fl);
		BandWriter.writeGeoTifImage(br, new File("H:\\仕事\\R06松岡\\01見積もり\\20240521佐賀県衛星\\佐賀県_"+tag+".tif"));
	}
	
	public static void main3(String[] args) throws IOException, ParseException {
		String tag1="B03";
		String tag2="B11";
		String out="NDSI";
		
		BandReader b1=BandReader.createReader(new File("H:\\仕事\\R06松岡\\01見積もり\\20240521佐賀県衛星\\佐賀県_"+tag1+".tif"));
		BandReader b2=BandReader.createReader(new File("H:\\仕事\\R06松岡\\01見積もり\\20240521佐賀県衛星\\佐賀県_"+tag2+".tif"));
		BandReader ret=createNDV(b1,b2);
		BandWriter.writeGeoTifImage(ret, new File("H:\\仕事\\R06松岡\\01見積もり\\20240521佐賀県衛星\\佐賀県_"+out+".tif"));
	}
	
	public static BandReader createNDV(BandReader b1,BandReader b2) {
		float[][] f1=b1.getBand(0);
		float[][] f2=b2.getBand(0);
		float[][] data=new float[f1.length][f1[0].length];
		for(int i=0;i<f1.length;i++) {
			for(int j=0;j<f1[0].length;j++) {
				float uv=f1[i][j]-f2[i][j];
				float ul=f1[i][j]+f2[i][j];
				if(ul!=0) {
					data[i][j]=uv/ul;
				}
			}
		}
		BandReader ret=BandReader.createReader(b1.getEPSG(), b1.getTransform(), data);
		return ret;
	}
	
	public static BandReader reSampling1ch(Rectangle2D r,float res,File[] fs) {
		int ww=(int)Math.ceil(r.getWidth()/res);
		int hh=(int)Math.ceil(r.getHeight()/res);
		float[][] data=new float[ww][hh];
		int[][] ch=new int[ww][hh];
		AffineTransform af=new AffineTransform(new double[] {
				res,0,0,-res,r.getX(),
				r.getY()+r.getHeight()});
		int epsg=0;

		for(File f :fs) {
			System.out.println(f.getName());
			BandReader br=BandReader.createReader(f);
			epsg=br.getEPSG();
			int wt=br.getBand(0).length;
			int ht=br.getBand(0)[0].length;
			AffineTransform at=br.getTransform();
			try {
				at=at.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			for(int i=0;i<ch.length;i++) {
				for(int j=0;j<ch[0].length;j++) {
					if(ch[i][j]==1)continue;
					Point2D p=new Point2D.Double(i, j);
					p=af.transform(p, new Point2D.Double());
					p=at.transform(p, new Point2D.Double());
					int xx=(int)Math.floor(p.getX());
					int yy=(int)Math.floor(p.getY());
					if(xx>=0&&xx<wt&&yy>=0&&yy<ht) {
						float v0=br.getBand(0)[xx][yy];
						if(v0==0)continue;
						data[i][j]=v0;
						ch[i][j]=1;
					}
				}
			}
		}
		BandReader bw=BandReader.createReader(epsg, af, data);
		return bw;
	}
	
	public static BandReader reSampling3ch(Rectangle2D r,float res,File[] fs) {
		int ww=(int)Math.ceil(r.getWidth()/res);
		int hh=(int)Math.ceil(r.getHeight()/res);
		float[][][] data=new float[3][ww][hh];
		int[][] ch=new int[ww][hh];
		AffineTransform af=new AffineTransform(new double[] {
				res,0,0,-res,r.getX(),
				r.getY()+r.getHeight()});
		int epsg=0;
		for(File f :fs) {
			System.out.println(f.getName());
			BandReader br=BandReader.createReader(f);
			epsg=br.getEPSG();
			int wt=br.getBand(0).length;
			int ht=br.getBand(0)[0].length;
			AffineTransform at=br.getTransform();
			try {
				at=at.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			for(int i=0;i<ch.length;i++) {
				for(int j=0;j<ch[0].length;j++) {
					if(ch[i][j]==1)continue;
					Point2D p=new Point2D.Double(i, j);
					p=af.transform(p, new Point2D.Double());
					p=at.transform(p, new Point2D.Double());
					int xx=(int)Math.floor(p.getX());
					int yy=(int)Math.floor(p.getY());
					if(xx>=0&&xx<wt&&yy>=0&&yy<ht) {
						float v0=br.getBand(0)[xx][yy];
						float v1=br.getBand(1)[xx][yy];
						float v2=br.getBand(2)[xx][yy];
						if(v0==0&&v1==0&&v2==0)continue;
						data[0][i][j]=v0;
						data[1][i][j]=v1;
						data[2][i][j]=v2;
						ch[i][j]=1;
					}
				}
			}
		}
		BandReader bw=BandReader.createReader(epsg, af, data);
		return bw;
	}
	
	public static BufferedImage createImageIndexed(BandReader br) {
		float[][] dd=br.getBand(0);
		BufferedImage bi=new BufferedImage(dd.length,dd[0].length,BufferedImage.TYPE_INT_RGB);
		ColorTable ct=br.getColorTable(0);
		for(int i=0;i<dd.length;i++) {
			for(int j=0;j<dd[0].length;j++) {
				Color c=ct.GetColorEntry((int)dd[i][j]);
				bi.setRGB(i, j, c.getRGB());
			}
		}
		return bi;
	}
	
	public static BufferedImage createImageRGB(BandReader br) {
		float[][] r=br.getBand(0);
		float[][] g=br.getBand(1);
		float[][] b=br.getBand(2);
		BufferedImage bi=new BufferedImage(r.length,r[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<r.length;i++) {
			for(int j=0;j<r[0].length;j++) {
				Color c=new Color((int)r[i][j],(int)g[i][j],(int)b[i][j]);
				bi.setRGB(i, j, c.getRGB());
			}
		}
		return bi;
	}
	
	public static BufferedImage createImageRGB(BandReader br,Color back) {
		float[][] r=br.getBand(0);
		float[][] g=br.getBand(1);
		float[][] b=br.getBand(2);
		BufferedImage bi=new BufferedImage(r.length,r[0].length,BufferedImage.TYPE_INT_RGB);
		Graphics2D gg=bi.createGraphics();
		gg.setBackground(back);
		gg.clearRect(0, 0, bi.getWidth(), bi.getHeight());
		gg.dispose();
		for(int i=0;i<r.length;i++) {
			for(int j=0;j<r[0].length;j++) {
				Color c=new Color((int)r[i][j],(int)g[i][j],(int)b[i][j]);
				if(c!=Color.BLACK)	bi.setRGB(i, j, c.getRGB());
			}
		}
		return bi;
	}
	
	public static BufferedImage createImageRGBToVal(BandReader br) {
		float[][] r=br.getBand(0);
		float[][] g=br.getBand(1);
		float[][] b=br.getBand(2);
		float[] rs=getStat(r);
		float[] gs=getStat(g);
		float[] bs=getStat(b);
		BufferedImage bi=new BufferedImage(r.length,r[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<r.length;i++) {
			for(int j=0;j<r[0].length;j++) {
				float rv=Math.max(rs[4],Math.min(rs[5], r[i][j]));
				float gv=Math.max(gs[4],Math.min(gs[5], g[i][j]));
				float bv=Math.max(bs[4],Math.min(bs[5], b[i][j]));
				rv=(rv-rs[2])/(rs[3]-rs[2])*255;
				gv=(gv-gs[2])/(gs[3]-gs[2])*255;
				bv=(bv-bs[2])/(bs[3]-bs[2])*255;
				Color c=new Color((int)rv,(int)gv,(int)bv);
				bi.setRGB(i, j, c.getRGB());
			}
		}
		return bi;
	}
	
	public static BufferedImage createImageSingle(BandReader br,int index,Gradient g,Range range) {
		float[][] r=br.getBand(index);
		BufferedImage bi=new BufferedImage(r.length,r[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<r.length;i++) {
			for(int j=0;j<r[0].length;j++) {
				double val=range.getValue(r[i][j]);
				Color c=g.getColor(val);
				bi.setRGB(i, j, c.getRGB());
			}
		}
		return bi;
	}
	
	public static List<BandReader> divide(BandReader br){
		List<BandReader> list=new ArrayList<>();
		float[][] ff=br.getBand(0);
		int ww=ff.length/2;
		int hh=ff[0].length/2;
		AffineTransform af=br.getTransform();
		AffineTransform[] nf=new AffineTransform[]{
			new AffineTransform(new double[]{af.getScaleX(),0,0,af.getScaleY(),af.getTranslateX(),af.getTranslateY()}),
			new AffineTransform(new double[]{af.getScaleX(),0,0,af.getScaleY(),af.getTranslateX()+ww*af.getScaleX(),af.getTranslateY()}),
			new AffineTransform(new double[]{af.getScaleX(),0,0,af.getScaleY(),af.getTranslateX(),af.getTranslateY()+hh*af.getScaleY()}),
			new AffineTransform(new double[]{af.getScaleX(),0,0,af.getScaleY(),af.getTranslateX()+ww*af.getScaleX(),af.getTranslateY()+hh*af.getScaleY()}),
		};
		int[][] ma=new int[][]{{0,0},{ww,0},{0,hh},{ww,hh}};
		for(int i=0;i<4;i++) {
			float[][][] bd=new float[br.getBandNum()][ww][hh];
			for(int j=0;j<br.getBandNum();j++) {
				float[][] src=br.getBand(j);
				for(int x=0;x<ww;x++){
					for(int y=0;y<hh;y++){
						int xx=x+ma[i][0];
						int yy=y+ma[i][1];
						bd[j][x][y]=src[xx][yy];
					}
				}
			}
			BandReader bb=BandReader.createReader(br.getEPSG(), nf[i], bd);
			list.add(bb);
		}
		return list;
	}
	
	public static BandReader createResolutionObj(BandReader target,BandReader src) {
		float[][] tf=target.getBand(0);
		float[][] sf=src.getBand(0);
		float tx=(float)sf.length/(float)tf.length;
		float ty=(float)sf[0].length/(float)tf[0].length;
		float[][] ans=new float[tf.length][tf[0].length];
		for(int i=0;i<ans.length;i++) {
			for(int j=0;j<ans[0].length;j++) {
				float xx=tx*i;
				float yy=ty*j;
				int xp=Math.min(Math.max(Math.round(xx),0),tf.length-1);
				int yp=Math.min(Math.max(Math.round(yy),0),tf[0].length-1);
				ans[i][j]=sf[xp][yp];
			}
		}
		BandReader ret=BandReader.createReader(target.getEPSG(), target.getTransform(), ans);
		return ret;
	}
	
	public static Point2D xyToLonlat(int num,double xx,double yy){
		return LonLatXY.xyToLonlat(num,xx,yy);
	}
	
	public static Point2D lonlatToXY(int num,double lon,double lat){
		return LonLatXY.lonlatToXY(num, lon, lat);
	}
	
	public static SpatialReference createSpatialReference(int epsg) {
		SpatialReference srs=new SpatialReference();
		srs.ImportFromEPSG(epsg);
		srs.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
		return srs;
	}
	
	public static CoordinateTransformation getCoordinateTransformation(SpatialReference src,SpatialReference target) {
		return osr.CreateCoordinateTransformation(src, target);
	}
	
	public static CoordinateTransformation getCoordinateTransformation(int src,int target) {
		return osr.CreateCoordinateTransformation(
				createSpatialReference(src), createSpatialReference(target));
	}
	
	public static AffineTransform loadTransform(File path)throws IOException{
		BufferedReader br=new BufferedReader(new FileReader(path));
		List<Double> dd=new ArrayList<Double>();
		String line=null;
		while((line=br.readLine())!=null){
			double d=Double.parseDouble(line);
			dd.add(d);
		}
		br.close();
		double[] p=new double[dd.size()];
		for(int i=0;i<p.length;i++){
			p[i]=dd.get(i);
		}
		return new AffineTransform(p);
	}
	
	public static void writeTransform(AffineTransform af,File path)throws IOException{
		BufferedWriter bw=new BufferedWriter(new FileWriter((path)));
		bw.write(af.getScaleX()+"\n");
		bw.write(af.getShearX()+"\n");
		bw.write(af.getShearY()+"\n");
		bw.write(af.getScaleY()+"\n");
		bw.write(af.getTranslateX()+"\n");
		bw.write(af.getTranslateY()+"\n");
		bw.flush();
		bw.close();
	}
	
	public static void transTifToPng(BandReader br,int ch,DataTranslator trans,File out) throws IOException {
		float[][] val=br.getBand(ch);
		BufferedImage img=new BufferedImage(val.length,val[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<val.length;i++) {
			for(int j=0;j<val[0].length;j++) {
				img.setRGB(i, j, trans.getRGB(val[i][j]));
			}
		}
		ImageIO.write(img, "png", out);
		writeTransform(br.getTransform(),new File(out.getAbsolutePath().replace(".png", ".pgw")));
	}
	
	public interface DataTranslator {
		public int getRGB(float val);
	}
	
	public static BandReader demPng2Tif(int epsg,File png) throws IOException {
		AffineTransform af=loadTransform(new File(png.getAbsolutePath().replace(".png", ".pgw")));
		BufferedImage img=ImageIO.read(png);
		float[][] val=new float[img.getWidth()][img.getHeight()];
		for(int i=0;i<val.length;i++) {
			for(int j=0;j<val[0].length;j++) {
				double zz=PCUtil.getZ(img.getRGB(i, j));
				if(Double.isNaN(zz)) {
					val[i][j]=Float.NaN;
				}else {
					val[i][j]=(float)zz;
				}
			}
		}
		return BandReader.createReader(epsg, af, val);
	}
	
	public static void demTif2Png(File tif) throws IOException {
		BandReader br=BandReader.createReader(tif);
		AffineTransform af=br.getTransform();
		float[][] ff=br.getBand(0);
		BufferedImage img=new BufferedImage(ff.length,ff[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<ff.length;i++) {
			for(int j=0;j<ff[0].length;j++) {
				int col=PCUtil.getRGB(ff[i][j]);
				img.setRGB(i, j, col);
			}
		}
		ImageIO.write(img, "png", new File(tif.getAbsolutePath().replace(".tif", ".png")));
		PCUtil.writeTransform(af, new File(tif.getAbsolutePath().replace(".tif", ".pgw")));
	}
	
	public static void png2Tif(File f) throws IOException {
		BufferedImage img=ImageIO.read(f);
		AffineTransform af=PCUtil.loadTransform(new File(f.getAbsolutePath().replace(".png",".pgw")));
		float[][][] col=new float[3][img.getWidth()][img.getHeight()];
		for(int i=0;i<img.getWidth();i++) {
			for(int j=0;j<img.getHeight();j++) {
				Color c=new Color(img.getRGB(i, j));
				col[0][i][j]=c.getRed();
				col[1][i][j]=c.getGreen();
				col[2][i][j]=c.getBlue();
			}
		}
		
		BandReader br=BandReader.createReader(6675, af, col[0]);
		br.addBand(col[1]);
		br.addBand(col[2]);
		BandWriter.writeMultiImage(br, 0, 1, 1, new File(f.getAbsolutePath().replace(".png", ".tif")));
	}
	
	public static void tif2Png(File tif) throws IOException {
		BandReader br=BandReader.createReader(tif);
		AffineTransform af=br.getTransform();
		float[][] f1=br.getBand(0);
		float[][] f2=br.getBand(1);
		float[][] f3=br.getBand(2);
		float[] s1=getStat(f1);
		float[] s2=getStat(f2);
		float[] s3=getStat(f3);
		BufferedImage img=new BufferedImage(f1.length,f1[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<f1.length;i++) {
			for(int j=0;j<f1[0].length;j++) {
				float v1=(f1[i][j]-s1[4])/(s1[5]-s1[4]);
				float v2=(f2[i][j]-s2[4])/(s2[5]-s2[4]);
				float v3=(f3[i][j]-s3[4])/(s3[5]-s3[4]);
				int i1=(int)Math.max(Math.min(255, v1*255),0);
				int i2=(int)Math.max(Math.min(255, v2*255),0);
				int i3=(int)Math.max(Math.min(255, v3*255),0);
				Color c=new Color(i1,i2,i3);
				img.setRGB(i, j, c.getRGB());
			}
		}
		ImageIO.write(img, "png", new File(tif.getAbsolutePath().replace(".tif", ".png")));
		PCUtil.writeTransform(af, new File(tif.getAbsolutePath().replace(".tif", ".pgw")));
	}
	
	public static void tif2PngIndexed(File tif) throws IOException {
		BandReader br=BandReader.createReader(tif);
		AffineTransform af=br.getTransform();
		float[][] f1=br.getBand(0);
		ColorTable tb=br.getColorTable(0);
		BufferedImage img=new BufferedImage(f1.length,f1[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<f1.length;i++) {
			for(int j=0;j<f1[0].length;j++) {
				Color c=tb.GetColorEntry((int)f1[i][j]);
				img.setRGB(i, j, c.getRGB());
			}
		}
		ImageIO.write(img, "png", new File(tif.getAbsolutePath().replace(".tif", ".png")));
		PCUtil.writeTransform(af, new File(tif.getAbsolutePath().replace(".tif", ".pgw")));
	}
	
	private static float[] getStat(float[][] b) {
		float ave=0;
		float num=0;
		float min=100000;
		float max=-100000;
		for(int i=0;i<b.length;i++) {
			for(int j=0;j<b[0].length;j++) {
				if(Float.isNaN(b[i][j]))continue;
				ave +=b[i][j];
				num++;
				min=Math.min(min, b[i][j]);
				max=Math.max(max, b[i][j]);
			}
		}
		ave=ave/num;
		float val=0;
		for(int i=0;i<b.length;i++) {
			for(int j=0;j<b[0].length;j++) {
				if(Float.isNaN(b[i][j]))continue;
				val +=(b[i][j]-ave)*(b[i][j]-ave);
			}
		}
		val=val/num;
		val=(float)Math.sqrt(val);
		return new float[] {ave,val,min,max,Math.max(min, ave-val*3),Math.min(max, ave+val*3)};
	}
}
