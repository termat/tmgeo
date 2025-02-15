package net.termat.tmgeo.fomat.mvt;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Feature.Builder;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IGeometryFilter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtReader;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TagKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TileGeomResult;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;

import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.fomat.mbtiles.MBTilesBuilder;
import net.termat.tmgeo.util.MeshUtil;

public class MVTBuilder {
	private List<Geometry> geom;
	private Rectangle2D bounds;
	private String layerName;
	
	public MVTBuilder(VectorReader vr,String layerName) throws ParseException {
		this.bounds=vr.getBounds();
		this.geom=vr.getGeometrys();
		int n=vr.size();
		for(int i=0;i<n;i++) {
			this.geom.get(i).setUserData(vr.getProperty(i));
		}
		this.layerName=layerName;
	}
	
	public MVTBuilder(List<Geometry> geom,Rectangle2D bounds,String layerName) throws ParseException {
		this.bounds=bounds;
		this.geom=geom;
		this.layerName=layerName;
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
	
	public void createMVT(int zoom,MBTilesBuilder mb) throws IOException, SQLException {
		GeometryFactory geomFactory = new GeometryFactory();
		List<Point> list=MeshUtil.getTileList(this.bounds, zoom);
		for(Point p : list) {
			MBTilesBuilder.tiles t=mb.getTile(zoom, p.x, p.y);
			JtsMvt jtsmvt=null;
			if(t!=null) {
				jtsmvt=getMvtLayer(t.tile_data);
			}
			MvtLayerParams layerParams = new MvtLayerParams();
			Envelope env=getTileBounds(p.x,p.y,zoom);
			IGeometryFilter acceptAllGeomFilter=new IGeometryFilter() {
				@Override
				public boolean accept(Geometry geometry) {
					return true;
				}
			};
			TileGeomResult tileGeom = JtsAdapter.createTileGeom(geom, env, geomFactory, layerParams, acceptAllGeomFilter);
			final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
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
						if(layerProps.keyIndex(layerName)==null) {
							 featureBuilder.addTags(layerProps.addKey(s));
						}
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
			if(tileGeom.mvtGeoms.size()==0)continue;
			if(jtsmvt==null) {
				final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
				final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);
				layerBuilder.addAllFeatures(features);
				MvtLayerBuild.writeProps(layerBuilder, layerProps);
				final VectorTile.Tile.Layer layer = layerBuilder.build();
				tileBuilder.addLayers(layer);
				Tile mvt = tileBuilder.build();
				byte[] bytes=mvt.toByteArray();
				mb.addTile(zoom, p.x, p.y, bytes);
			}else {
				Map<String,JtsLayer> ll=jtsmvt.getLayersByName();
				for(String key : ll.keySet()) {
					JtsLayer lay=ll.get(key);
					if(key.equals(layerName)) {	
						List<Geometry> gg=new ArrayList<>();
						gg.addAll(lay.getGeometries());
						gg.addAll(tileGeom.mvtGeoms);
						final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(gg, layerProps, userDataConverter);
						final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(key, layerParams);
						layerBuilder.addAllFeatures(features);		
						MvtLayerBuild.writeProps(layerBuilder, layerProps);
						final VectorTile.Tile.Layer layer = layerBuilder.build();
						tileBuilder.addLayers(layer);
					}else {
						final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(lay.getGeometries(), layerProps, userDataConverter);
						final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(key, layerParams);
						layerBuilder.addAllFeatures(features);		
						MvtLayerBuild.writeProps(layerBuilder, layerProps);
						final VectorTile.Tile.Layer layer = layerBuilder.build();
						tileBuilder.addLayers(layer);
					}
				}
				if(!ll.keySet().contains(layerName)){
					final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
					final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);
					layerBuilder.addAllFeatures(features);
					MvtLayerBuild.writeProps(layerBuilder, layerProps);
					final VectorTile.Tile.Layer layer = layerBuilder.build();
					tileBuilder.addLayers(layer);
				}
				Tile mvt = tileBuilder.build();
				t.tile_data=mvt.toByteArray();
				mb.updateTiles(t);
			}
		}
	}
	
	public void createMVTs(MBTilesBuilder mb) throws SQLException {
		for(int i=mb.getMinZoom();i<=mb.getMaxZoom();i++) {
			try {
				this.createMVT(i, mb);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static Envelope getTileBounds(int x, int y, int zoom){
	    return new Envelope(getLong(x, zoom), getLong(x + 1, zoom), getLat(y, zoom), getLat(y + 1, zoom));
	}

	private static double getLong(int x, int zoom){
	    return ( x / Math.pow(2, zoom) * 360 - 180 );
	}

	private static double getLat(int y, int zoom){
	    double r2d = 180 / Math.PI;
	    double n = Math.PI - 2 * Math.PI * y / Math.pow(2, zoom);
	    return r2d * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
	}
	
	private static JtsMvt getMvtLayer(byte[] b) throws IOException{
		ByteArrayInputStream bis=new ByteArrayInputStream(b);
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		return mvt;
	}
}
