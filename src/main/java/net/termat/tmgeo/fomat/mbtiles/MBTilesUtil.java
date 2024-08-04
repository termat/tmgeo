package net.termat.tmgeo.fomat.mbtiles;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.imintel.mbtiles4j.MBTilesWriteException;
import org.imintel.mbtiles4j.MBTilesWriter;
import org.imintel.mbtiles4j.model.MetadataEntry;
import org.locationtech.jts.geom.GeometryFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Feature.Builder;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtReader;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TagKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;

import net.termat.tmgeo.db.DBUtil;
import net.termat.tmgeo.fomat.mvt.JtsAdapterReverse;
import net.termat.tmgeo.fomat.mvt.MVTUtil;
import net.termat.tmgeo.util.MeshUtil;

public class MBTilesUtil {
	
	public static void joinMBTiles(MBTilesBuilder b1,MBTilesBuilder b2) throws SQLException, IOException {
		List<MBTilesBuilder.tiles> tl=b1.getAllTiles();
		for(MBTilesBuilder.tiles t1 : tl) {
			MBTilesBuilder.tiles t2=b1.getTileColRow(t1.zoom_level, t1.tile_column, t1.tile_row);
			if(t2==null)continue;
			final MvtLayerProps layerProps = new MvtLayerProps();
			IUserDataConverter userDataConverter=new IUserDataConverter() {
				@SuppressWarnings("unchecked")
				@Override
				public void addTags(Object userData, MvtLayerProps layerProps, Builder featureBuilder) {
					JsonObject o=null;
					if(userData instanceof Map) {
						o=createObj((Map<String,Object>)userData);
					}else {
						o=(JsonObject)userData;
					}
					for(String s : o.keySet()) {
						JsonElement je=o.get(s);
						try {
							String val=je.getAsString();
							try {
								featureBuilder.addTags(layerProps.addValue(Double.parseDouble(val)));
							}catch(NumberFormatException e) {
								featureBuilder.addTags(layerProps.addValue(val));
							}
						}catch(java.lang.UnsupportedOperationException ue) {
							System.out.println("NULL="+s);
						}
					}
				}
			};
			final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
			MvtLayerParams layerParams = new MvtLayerParams();
			JtsMvt jtsmvt1=getMvtLayer(t1.tile_data);
			JtsMvt jtsmvt2=getMvtLayer(t2.tile_data);
			Map<String,JtsLayer> ll=jtsmvt1.getLayersByName();
			for(String key : ll.keySet()) {
				JtsLayer lay=ll.get(key);
				final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(lay.getGeometries(), layerProps, userDataConverter);
				final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(key, layerParams);
				layerBuilder.addAllFeatures(features);		
				MvtLayerBuild.writeProps(layerBuilder, layerProps);
				final VectorTile.Tile.Layer layer = layerBuilder.build();
				tileBuilder.addLayers(layer);
			}
			ll=jtsmvt2.getLayersByName();
			for(String key : ll.keySet()) {
				JtsLayer lay=ll.get(key);
				final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(lay.getGeometries(), layerProps, userDataConverter);
				final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(key, layerParams);
				layerBuilder.addAllFeatures(features);		
				MvtLayerBuild.writeProps(layerBuilder, layerProps);
				final VectorTile.Tile.Layer layer = layerBuilder.build();
				tileBuilder.addLayers(layer);
			}
			Tile mvt = tileBuilder.build();
			t1.tile_data=mvt.toByteArray();
			b1.updateTiles(t1);
		}
	}
	
	public static void createMBTiles(MBTilesBuilder mb,MBHelperDB db,Rectangle2D rect,int minZoom,int maxZoom,String ext) throws SQLException, IOException {
		for(int i=maxZoom;i>=minZoom;i--) {
			if(i==maxZoom) {
				List<Point> li=MeshUtil.getTileList(rect, i);
				for(Point p : li) {
					Rectangle2D r=MeshUtil.getTileBounds(i, p.x, p.y);
					BufferedImage img=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
					double[] dd=new double[] {
							r.getWidth()/img.getWidth(),
							0,
							0,
							-r.getHeight()/img.getHeight(),
							r.getX(),
							r.getY()+r.getHeight()
						};
						AffineTransform aa=new AffineTransform(dd);
					Map<BufferedImage,AffineTransform> map=db.getImage(r);
					for(BufferedImage bi : map.keySet()) {
						AffineTransform at=map.get(bi);
						writeTileImage(img,aa,bi,at);
					}
					mb.addTile(i, p.x, p.y,DBUtil.bi2Bytes(img, ext));
				}
			}else {
				
			}
		}
	}
	
	public static void createMBTiles(MBTilesWriter mb,BufferedImage bi,AffineTransform af,int minZoom,int maxZoom,String ext) throws SQLException, IOException, MBTilesWriteException {
		Rectangle2D rect=new Rectangle2D.Double(0, 0, bi.getWidth(),bi.getHeight());
		rect=af.createTransformedShape(rect).getBounds2D();
		for(int i=maxZoom;i>=minZoom;i--) {
			System.out.println(i);
			List<Point> li=MeshUtil.getTileList(rect, i);
			for(Point p : li) {
				Rectangle2D r=MeshUtil.getTileBounds(i, p.x, p.y);
				BufferedImage img=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
				double[] dd=new double[] {
						r.getWidth()/img.getWidth(),
						0,
						0,
						-r.getHeight()/img.getHeight(),
						r.getX(),
						r.getY()+r.getHeight()
					};
				AffineTransform aa=new AffineTransform(dd);
				writeTileImage(img,aa,bi,af);
				mb.addTile(DBUtil.bi2Bytes(img, ext), i, p.x, p.y);
			}
		}
	}
	
	public static void writeTileImage(BufferedImage tile,AffineTransform af1,BufferedImage src,AffineTransform af2) {
		Rectangle2D r1=new Rectangle2D.Double(0, 0, tile.getWidth(), tile.getHeight());
		r1=af1.createTransformedShape(r1).getBounds2D();
		Rectangle2D r2=new Rectangle2D.Double(0, 0, src.getWidth(), src.getHeight());
		r2=af2.createTransformedShape(r2).getBounds2D();
		if(r1.intersects(r2)) {
			AffineTransform at=null;
			try {
				at=af2.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			int sw=src.getWidth();
			int sh=src.getHeight();
			int w=tile.getWidth();
			int h=tile.getHeight();
			for(int i=0;i<w;i++) {
				for(int j=0;j<h;j++) {
					Point2D p=af1.transform(new Point2D.Double(i, j), new Point2D.Double());
					p=at.transform(p, new Point2D.Double());
					int xx=(int)Math.floor(p.getX());
					int yy=(int)Math.floor(p.getY());
					if(xx>=0&&xx<sw&&yy>=0&&yy<sh) {
						tile.setRGB(i, j, src.getRGB(xx, yy));
					}
				}
			}
		}
	}
	
	public static void writeMBTilePNG(BufferedImage img,AffineTransform af,MBTilesWriter mb,int z,int x,int y) throws MBTilesWriteException, IOException {
		Rectangle2D rx=MeshUtil.getTileBounds(z, x, y);
		BufferedImage tile=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
		double[] param=new double[] {
				rx.getWidth()/256,0,
				0,rx.getHeight()/256,
				rx.getX(),rx.getY()+rx.getHeight()
			};
		AffineTransform at=new AffineTransform(param);
		writeTileImage(tile,at,img,af);
		mb.addTile(DBUtil.bi2Bytes(tile, "png"), z, x, y);
	}
	
	public static void writeMBTileJPG(BufferedImage img,AffineTransform af,MBTilesWriter mb,int z,int x,int y) throws MBTilesWriteException, IOException {
		Rectangle2D rx=MeshUtil.getTileBounds(z, x, y);
		BufferedImage tile=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
		double[] param=new double[] {
				rx.getWidth()/256,0,
				0,rx.getHeight()/256,
				rx.getX(),rx.getY()+rx.getHeight()
			};
		AffineTransform at=new AffineTransform(param);
		writeTileImage(tile,at,img,af);
		mb.addTile(DBUtil.bi2Bytes(tile, "jpg"), z, x, y);
	}
	
	public static void createMBTilePNG(File dir,File out,String title,String desc,String descattribut) throws MBTilesWriteException {
		MBTilesWriter w = new MBTilesWriter(out);
		MetadataEntry ent = new MetadataEntry();
		ent.setTilesetName(title)
			.setTilesetType(MetadataEntry.TileSetType.BASE_LAYER)
			.setTilesetVersion("0.2.0")
			.setTilesetDescription(desc)
			.setTileMimeType(MetadataEntry.TileMimeType.PNG)
			.setAttribution(descattribut)
			.setTilesetBounds(-180, -85, 180, 85);
		w.addMetadataEntry(ent);
		for(File zf : dir.listFiles()) {
			Integer z=Integer.parseInt(zf.getName());
			for(File xf : zf.listFiles()) {
				Integer x=Integer.parseInt(xf.getName());
				for(File yf : xf.listFiles()) {
					String n=yf.getName().toLowerCase().replace(".png", "");
					Integer y=Integer.parseInt(n);
					try {
						BufferedImage img=ImageIO.read(yf);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						BufferedOutputStream os = new BufferedOutputStream( bos );
						img.flush();
						ImageIO.write(img,"png",os);
						w.addTile(bos.toByteArray(), z, x, y);
						bos.close();
					}catch(IOException | MBTilesWriteException e) {}
				}
			}
		}
		w.close();
	}
	
	public static void createMBTileJPG(File dir,File out,String title,String desc,String descattribut) throws MBTilesWriteException {
		MBTilesWriter w = new MBTilesWriter(out);
		MetadataEntry ent = new MetadataEntry();
		ent.setTilesetName(title)
			.setTilesetType(MetadataEntry.TileSetType.BASE_LAYER)
			.setTilesetVersion("0.2.0")
			.setTilesetDescription(desc)
			.setTileMimeType(MetadataEntry.TileMimeType.JPG)
			.setAttribution(descattribut)
			.setTilesetBounds(-180, -85, 180, 85);
		w.addMetadataEntry(ent);
		for(File zf : dir.listFiles()) {
			Integer z=Integer.parseInt(zf.getName());
			for(File xf : zf.listFiles()) {
				Integer x=Integer.parseInt(xf.getName());
				for(File yf : xf.listFiles()) {
					String n=yf.getName().toLowerCase().replace(".jpg", "");
					Integer y=Integer.parseInt(n);
					try {
						BufferedImage img=ImageIO.read(yf);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						BufferedOutputStream os = new BufferedOutputStream( bos );
						img.flush();
						ImageIO.write(img,"jpg",os);
						w.addTile(bos.toByteArray(), z, x, y);
						bos.close();
					}catch(IOException | MBTilesWriteException e) {}
				}
			}
		}
		w.close();
	}

	public static void createMBTileMVT(File dir,File out,String title,String desc,String attribut) throws MBTilesWriteException {
		MBTilesWriter w = new MBTilesWriter(out);
		MetadataEntry ent = new MetadataEntry();
		ent.setTilesetName(title)
			.setTilesetType(MetadataEntry.TileSetType.BASE_LAYER)
			.setTilesetVersion("0.2.0")
			.setTilesetDescription(desc)
			.setAttribution(attribut)
			.setTilesetBounds(-180, -85, 180, 85);
		w.addMetadataEntry(ent);
		for(File zf : dir.listFiles()) {
			Integer z=Integer.parseInt(zf.getName());
			for(File xf : zf.listFiles()) {
				Integer x=Integer.parseInt(xf.getName());
				for(File yf : xf.listFiles()) {
					String n=yf.getName().toLowerCase().replace(".pbf", "").replace(".mvt", "");
					Integer y=Integer.parseInt(n);
					try {
						w.addTile(MVTUtil.getProto(yf), z, x, y);
					}catch(IOException | MBTilesWriteException e) {}
				}
			}
		}
		w.close();
	}
	
	private static JtsMvt getMvtLayer(byte[] b) throws IOException{
		ByteArrayInputStream bis=new ByteArrayInputStream(b);
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		return mvt;
	}
	
	private static JsonObject createObj(Map<String,Object> map) {
		JsonObject ret=new JsonObject();
		for(String key  : map.keySet()) {
			Object o=map.get(key);
			if(o instanceof Number) {
				ret.addProperty(key, (Number)o);
			}else if(o instanceof String) {
				ret.addProperty(key, (String)o);
			}else if(o instanceof Character) {
				ret.addProperty(key, (Character)o);
			}else if(o instanceof Boolean) {
				ret.addProperty(key, (Boolean)o);
			}
		}
		return ret;
	}
}
