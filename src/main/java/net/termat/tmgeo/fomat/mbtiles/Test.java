package net.termat.tmgeo.fomat.mbtiles;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.imintel.mbtiles4j.MBTilesWriteException;
import org.locationtech.jts.io.ParseException;

import net.termat.tmgeo.data.BandReader;
import net.termat.tmgeo.data.BandUtil;
import net.termat.tmgeo.data.BandWriter;
import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.fomat.mvt.MVTBuilder;
import net.termat.tmgeo.util.PCUtil;

public class Test {
	
	public static void main444(String[] args) throws IOException, SQLException, ParseException, NoninvertibleTransformException {
		File f=new File("F:\\仕事\\R06岡山県太陽光発電\\s2\\岡山県32653.geojson");
		VectorReader vr=VectorReader.createReader(32653, f);
		Rectangle2D r=vr.getBounds();
		AffineTransform af=new AffineTransform(new double[] {
				10,0,0,-10,r.getX(),r.getHeight()+r.getY()
		});
		int ww=(int)Math.ceil(r.getWidth()/10.0);
		int hh=(int)Math.ceil(r.getHeight()/10.0);
		float[][][] data=new float[3][ww][hh];
		int[][] ck=new int[ww][hh];
		String[] pp=new String[] {
				"T53SMU_20240303T014649_TCI_10m.jp2",
				"T53SLU_20240217T014731_TCI_10m.jp2",
				"T53SMV_20240407T014651_TCI_10m.jp2",
				"T53SLV_20240407T014651_TCI_10m.jp2"
		};
		for(String s : pp) {
			File ff=new File("F:\\仕事\\R06岡山県太陽光発電\\s2\\"+s);
			BandReader br=BandReader.createReader(ff);
			float[][] rv=br.getBand(0);
			float[][] gv=br.getBand(1);
			float[][] bv=br.getBand(2);
			AffineTransform at=br.getTransform();
			at=at.createInverse();
			for(int i=0;i<ww;i++) {
				for(int j=0;j<hh;j++) {
					if(ck[i][j]==1)continue;
					Point2D p=new Point2D.Double(i, j);
					p=af.transform(p, new Point2D.Double());
					p=at.transform(p, new Point2D.Double());
					int xx=(int)Math.floor(p.getX());
					int yy=(int)Math.floor(p.getY());
					if(xx>=0&&xx<rv.length&&yy>=0&&yy<rv[0].length){
						data[0][i][j]=rv[xx][yy];
						data[1][i][j]=gv[xx][yy];
						data[2][i][j]=bv[xx][yy];
						ck[i][j]=1;
					}
				}
			}
		}
		BandReader br=BandReader.createReader(32653, af, data);
		BandWriter.writeGeoTifImage(br, new File("F:\\仕事\\R06岡山県太陽光発電\\s2\\OKAYAMA_TRUE.tif"));
	}
	
	public static void main7(String[] args) throws IOException, SQLException, ParseException {
		File f=new File("F:\\仕事\\R06岡山県太陽光発電\\viewer\\public\\geojson\\okayama_area.geojson");
		VectorReader vr=VectorReader.createReader(4326, f);
		MBTilesBuilder w=MBTilesBuilder.open(new File("F:\\仕事\\R06岡山県太陽光発電\\viewer\\public\\geojson\\okayama_pref.mbtiles"));
		w.setMetadata(MBTilesBuilder.PARAM_MINZOOM,"14");
		w.setMetadata(MBTilesBuilder.PARAM_MAXZOOM,"16");
		MVTBuilder mb=new MVTBuilder(vr,"city_area");
		mb.createMVTs(w);
	}
	
	public static void main6(String[] args) throws IOException, SQLException, ParseException {
		File f=new File("F:\\仕事\\R06岡山県太陽光発電\\viewer\\public\\geojson\\okayama_pref.geojson");
		VectorReader vr=VectorReader.createReader(4326, f);
		MBTilesBuilder w=MBTilesBuilder.open(new File("F:\\仕事\\R06岡山県太陽光発電\\viewer\\public\\geojson\\okayama_pref.mbtiles"));
		w.setMetadata(MBTilesBuilder.PARAM_NAME, "city_area");
		w.setMetadata(MBTilesBuilder.PARAM_FORMAT, MBTilesBuilder.FORMAT_PBF);
		w.setMetadata(MBTilesBuilder.PARAM_MINZOOM,"7");
		w.setMetadata(MBTilesBuilder.PARAM_MAXZOOM,"9");
		w.setBounds(vr.getBounds(), 7);
		MVTBuilder mb=new MVTBuilder(vr,"city_area");
		mb.createMVTs(w);
	}
	
	
	
	public static void main(String[] args) throws IOException, SQLException, ParseException {
		File f=new File("C:\\Users\\t-matsuoka\\株式会社ウエスコ Dropbox\\松岡輝樹\\R06岡山県太陽光発電\\gis\\浸水想定_計画規模WGS84.geojson");
		VectorReader vr=VectorReader.createReader(4326, f);
		MBTilesBuilder w=MBTilesBuilder.open(new File("F:\\仕事\\R06岡山県太陽光発電\\viewer\\public\\geojson\\"+f.getName().replace(".geojson", ".mbtiles")));
		w.setMetadata(MBTilesBuilder.PARAM_NAME, "flood_plan");
		w.setMetadata(MBTilesBuilder.PARAM_FORMAT, MBTilesBuilder.FORMAT_PBF);
		w.setMetadata(MBTilesBuilder.PARAM_MINZOOM,"11");
		w.setMetadata(MBTilesBuilder.PARAM_MAXZOOM,"15");
		w.setBounds(vr.getBounds(), 11);
		MVTBuilder mb=new MVTBuilder(vr,"flood_plan");
		mb.createMVTs(w);
	}

	public static void mainQ(String[] args) throws IOException, SQLException, MBTilesWriteException {
		File f=new File("F:\\仕事\\R06岡山県太陽光発電\\s2\\OKAYAMA_TRUE.png");
		BufferedImage im=ImageIO.read(f);
		AffineTransform af=PCUtil.loadTransform(new File(f.getAbsolutePath().replace(".png", ".pgw")));
		Rectangle2D rect=new Rectangle2D.Double(0, 0, im.getWidth(), im.getHeight());
		rect=af.createTransformedShape(rect).getBounds2D();
		MBTilesBuilder w=MBTilesBuilder.open(new File("F:\\仕事\\R06岡山県太陽光発電\\s2\\s2_okayama2024.mbtiles"));
		w.setMetadata(MBTilesBuilder.PARAM_NAME, "s2_okayama");
		w.setMetadata(MBTilesBuilder.PARAM_FORMAT, MBTilesBuilder.FORMAT_PNG);
		w.setMetadata(MBTilesBuilder.PARAM_MINZOOM,"9");
		w.setMetadata(MBTilesBuilder.PARAM_MAXZOOM,"15");
		w.setBounds(rect, 9);
		w.createRasterTile(im, af);
	}
	
	public static void main111(String[] args) throws IOException {
		File f=new File("F:\\仕事\\R06岡山県太陽光発電\\s2\\OKAYAMA_TRUE.tif");
		BandReader br=BandReader.createReader(f);
		br=br.createProjectionData(4326);
		BufferedImage img=BandUtil.createImageRGB(br);
		ImageIO.write(img, "png", new File(f.getAbsolutePath().replace(".tif", ".png")));
		PCUtil.writeTransform(br.getTransform(), new File(f.getAbsolutePath().replace(".tif", ".pgw")));
	}
	
	public static void main4(String[] args) throws IOException, SQLException, MBTilesWriteException {
		File f=new File("H:\\仕事\\R06松岡\\01見積もり\\20240521佐賀県衛星\\センチネルTIF\\佐賀県_NDWI_COLOR.png");
		BufferedImage im=ImageIO.read(f);
		AffineTransform af=PCUtil.loadTransform(new File(f.getAbsolutePath().replace(".png", ".pgw")));
		Rectangle2D rect=new Rectangle2D.Double(0, 0, im.getWidth(), im.getHeight());
		rect=af.createTransformedShape(rect).getBounds2D();
		MBTilesBuilder w=MBTilesBuilder.open(new File("H:\\仕事\\R06松岡\\01見積もり\\20240521佐賀県衛星\\センチネルTIF\\佐賀県_NDWI.mbtiles"));
		w.setMetadata(MBTilesBuilder.PARAM_NAME, "佐賀県_NDWI");
		w.setMetadata(MBTilesBuilder.PARAM_FORMAT, MBTilesBuilder.FORMAT_PNG);
		w.setMetadata(MBTilesBuilder.PARAM_MINZOOM,"8");
		w.setMetadata(MBTilesBuilder.PARAM_MAXZOOM,"15");
		w.setBounds(rect, 9);
		w.createRasterTile(im, af);
	}
}
